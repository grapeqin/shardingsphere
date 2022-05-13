package org.apache.shardingsphere.example.sharding.raw.jdbc.algorithm;

import com.google.common.base.Preconditions;
import lombok.Setter;
import org.apache.curator.shaded.com.google.common.primitives.Ints;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class MyComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm<Comparable<?>> {

    private static final String SHARDING_COUNT_KEY = "sharding-count";

    @Setter
    private Properties props = new Properties();

    private int shardingCount;

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        Collection<Comparable<?>> userIdCollection = shardingValue.getColumnNameAndShardingValuesMap().get("user_id");
        Collection<Comparable<?>> orderNoCollection = shardingValue.getColumnNameAndShardingValuesMap().get("order_no");
        Integer userId = null;
        if (null != userIdCollection) {
            userId = getIntegerValue(userIdCollection.stream().findFirst().get());
            userId %= 10000;
        }
        if (null != orderNoCollection) {
            String orderNo = getStringValue(orderNoCollection.stream().findFirst().get());
            String userIdSuffix = orderNo.substring(10, 14);
            userId = Ints.tryParse(userIdSuffix);
        }
        if (null != userId) {
            for (String each : availableTargetNames){
                String suffix = String.valueOf(userId % shardingCount);
                if(each.endsWith(suffix)){
                    Collection<String> result = new ArrayList<>(1);
                    result.add(each);
                    return result;
                }
            }
        }
        return null;
    }

    private String getStringValue(Comparable<?> comparable) {
        return comparable instanceof String ? comparable.toString() : String.valueOf(comparable);
    }

    private int getIntegerValue(Comparable<?> comparable) {
        return comparable instanceof Number ? ((Number) comparable).intValue() : Integer.valueOf(comparable.toString());
    }

    @Override
    public void init() {
        shardingCount = getShardingCount();
    }

    private int getShardingCount() {
        Preconditions.checkArgument(props.containsKey(SHARDING_COUNT_KEY), "Sharding count cannot be null.");
        return Integer.parseInt(props.get(SHARDING_COUNT_KEY).toString());
    }

    @Override
    public String getType() {
        return "COMPLEX_USERID_ORDERNO";
    }
}
