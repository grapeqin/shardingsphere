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

package org.apache.shardingsphere.example.core.jdbc.service;

import org.apache.shardingsphere.example.core.api.entity.MyOrder;
import org.apache.shardingsphere.example.core.api.repository.MyOrderRepository;
import org.apache.shardingsphere.example.core.api.service.ExampleService;
import org.apache.shardingsphere.example.core.jdbc.repository.MyOrderRepositoryImpl;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class MyOrderServiceImpl implements ExampleService {

    private final static DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmm");

    private final static SecureRandom RANDOM = new SecureRandom();

    private final MyOrderRepository orderRepository;

    public MyOrderServiceImpl(final DataSource dataSource) {
        orderRepository = new MyOrderRepositoryImpl(dataSource);
    }

    public MyOrderServiceImpl(final MyOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void initEnvironment() throws SQLException {
        orderRepository.createTableIfNotExists();
        orderRepository.truncateTable();
    }

    @Override
    public void cleanEnvironment() throws SQLException {
        orderRepository.dropTable();
    }

    @Override
    public void processSuccess() throws SQLException {
        System.out.println("-------------- Process Success Begin ---------------");
        List<MyOrder> myOrderList = insertData();
        printData();
        deleteData(myOrderList);
        printData();
        System.out.println("-------------- Process Success Finish --------------");
    }

    @Override
    public void processFailure() throws SQLException {
        System.out.println("-------------- Process Failure Begin ---------------");
        insertData();
        System.out.println("-------------- Process Failure Finish --------------");
        throw new RuntimeException("Exception occur for transaction test.");
    }

    private List<MyOrder> insertData() throws SQLException {
        System.out.println("---------------------------- Insert Data ----------------------------");
        List<MyOrder> result = new ArrayList<>(10);
        for (int i = 1; i <= 10; i++) {
            MyOrder order = insertOrder(i);
            result.add(order);
        }
        return result;
    }

    private MyOrder insertOrder(final int i) throws SQLException {
        MyOrder order = new MyOrder();
        order.setUserId(i);
        order.setOrderNo(generateOrderNo(order.getUserId()));
        order.setAddressId(i);
        order.setStatus("INSERT_TEST");
        orderRepository.insert(order);
        return order;
    }

    private String generateOrderNo(int uid) {
        StringBuilder orderNo = new StringBuilder();
        orderNo.append(DATETIME_FORMATTER.format(LocalDateTime.now()));
        orderNo.append(String.format("%04d", uid % 10000));
        orderNo.append(String.format("%02d", RANDOM.nextInt(100)));
        orderNo.append(String.format("%04d", RANDOM.nextInt(10000)));
        return orderNo.toString();
    }

    private void deleteData(final List<MyOrder> myOrderList) throws SQLException {
        System.out.println("---------------------------- Delete Data ----------------------------");
        for (MyOrder each : myOrderList) {
            orderRepository.delete(each.getOrderId());
            orderRepository.delete(each.getUserId());
            orderRepository.delete(each.getOrderNo());
        }
    }

    @Override
    public void printData() throws SQLException {
        System.out.println("---------------------------- Print Order Data -----------------------");
        for (Object each : orderRepository.selectAll()) {
            System.out.println(each);
        }
    }
}
