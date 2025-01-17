/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sharding.algorithm.sharding.mod;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.shardingsphere.sharding.api.sharding.ShardingAutoTableAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Properties;

/**
 * Modulo sharding algorithm.
 */
public final class ModShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>>, ShardingAutoTableAlgorithm {
    
    private static final String SHARDING_COUNT_KEY = "sharding-count";
    
    @Getter
    private Properties props;
    
    private int shardingCount;
    
    @Override
    public void init(final Properties props) {
        this.props = props;
        shardingCount = getShardingCount(props);
    }
    
    private int getShardingCount(final Properties props) {
        Preconditions.checkArgument(props.containsKey(SHARDING_COUNT_KEY), "Sharding count cannot be null.");
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }
    
    @Override
    public String doSharding(final Collection<String> availableTargetNames, final PreciseShardingValue<Comparable<?>> shardingValue) {
        String suffix = String.valueOf(getLongValue(shardingValue.getValue()) % shardingCount);
        return findMatchedTargetName(availableTargetNames, suffix, shardingValue.getDataNodeInfo()).orElse(null);
    }
    
    @Override
    public Collection<String> doSharding(final Collection<String> availableTargetNames, final RangeShardingValue<Comparable<?>> shardingValue) {
        return isContainAllTargets(shardingValue) ? availableTargetNames : getAvailableTargetNames(availableTargetNames, shardingValue);
    }
    
    private boolean isContainAllTargets(final RangeShardingValue<Comparable<?>> shardingValue) {
        return !shardingValue.getValueRange().hasUpperBound() || shardingValue.getValueRange().hasLowerBound()
                && getLongValue(shardingValue.getValueRange().upperEndpoint()) - getLongValue(shardingValue.getValueRange().lowerEndpoint()) >= shardingCount - 1;
    }
    
    private Collection<String> getAvailableTargetNames(final Collection<String> availableTargetNames, final RangeShardingValue<Comparable<?>> shardingValue) {
        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());
        for (long i = getLongValue(shardingValue.getValueRange().lowerEndpoint()); i <= getLongValue(shardingValue.getValueRange().upperEndpoint()); i++) {
            String suffix = String.valueOf(i % shardingCount);
            findMatchedTargetName(availableTargetNames, suffix, shardingValue.getDataNodeInfo()).ifPresent(result::add);
        }
        return result;
    }
    
    private long getLongValue(final Comparable<?> value) {
        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
    }
    
    @Override
    public int getAutoTablesAmount() {
        return shardingCount;
    }
    
    @Override
    public String getType() {
        return "MOD";
    }
}
