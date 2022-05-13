# 1.sharding-jdbc分片示例

sharding-jdbc 提供如下几种分片算法：

1. [自动分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#自动分片算法)
2. [标准分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#标准分片算法)
3. [复合分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#复合分片算法)
4. [提示分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#hint-分片算法)
5. [自定义分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#自定义类分片算法)

t_order表
```sqlite-sql
    CREATE TABLE IF NOT EXISTS t_order (
    order_id BIGINT NOT NULL AUTO_INCREMENT, 
    user_id INT NOT NULL, 
    address_id BIGINT NOT NULL, 
    status VARCHAR(50), 
    add_time datetime NOT NULL,
    PRIMARY KEY (order_id))
```

t_order_item表:
```sqlite-sql
    CREATE TABLE IF NOT EXISTS t_order_item (
    order_item_id BIGINT NOT NULL AUTO_INCREMENT, 
    order_id BIGINT NOT NULL, 
    user_id INT NOT NULL, 
    status VARCHAR(50), 
    add_time datetime NOT NULL,
    PRIMARY KEY (order_item_id))
```

t_account表:
```sqlite-sql
    CREATE TABLE IF NOT EXISTS t_account (
    account_id BIGINT NOT NULL AUTO_INCREMENT, 
    user_id INT NOT NULL, 
    status VARCHAR(50), 
    add_time datetime NOT NULL,
    PRIMARY KEY (account_id))
```

## 1.1.自动分片算法

### 1.1.1 [取模分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#取模分片算法)

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingRawYamlConfigurationExample`

选择 `private static ShardingType shardingType = ShardingType.SHARDING_AUTO_TABLES;`,

详细的配置文件请参考[这里](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/yaml-config/rules/sharding/#配置项说明)

构建DataSource的配置文件位于`/META-INF/sharding-auto-tables.yaml`,内容如下所示:
```yaml
# 数据源配置，表示存在2个数据源
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_0?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_1?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

# 分片规则配置
rules:
- !SHARDING
# 自动分片
  autoTables:
    # 逻辑表
    t_order:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      # 分片策略
      shardingStrategy:
        # 标准分片:基于单个列的分片
        standard:
          # 分片列
          shardingColumn: order_id
          # 分片算法
          shardingAlgorithmName: auto-mod
      # id生成策略配置
      keyGenerateStrategy:
        # 自动生成id的列名
        column: order_id
        # id生成算法名
        keyGeneratorName: snowflake
    # 逻辑表        
    t_order_item:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      shardingStrategy:
        standard:
          shardingColumn: order_id
          shardingAlgorithmName: auto-mod
      keyGenerateStrategy:
        column: order_item_id
        keyGeneratorName: snowflake
    # 逻辑表
    t_account:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      shardingStrategy:
        standard:
          shardingAlgorithmName: auto-mod
      keyGenerateStrategy:
        column: account_id
        keyGeneratorName: snowflake        
  defaultShardingColumn: account_id
  # 分片算法
  shardingAlgorithms:
    auto-mod:
      # 取模分片
      type: MOD
      props:
        # 分片个数,用逻辑表分片列的值与sharding-count取模,得到实际表
        sharding-count: 4
  
  keyGenerators:
    snowflake:
      type: SNOWFLAKE

props:
  # 打印实际SQL
  sql-show: true
```
以上配置会将逻辑表分为4张实际表,分片逻辑请参考`org.apache.shardingsphere.sharding.algorithm.sharding.mod.ModShardingAlgorithm`。那关于db的选择是怎样的呢？

我们分别将`actualDataSources`设置为1、2、3、4、5个数据源，看看`t_order`这张表是如何在db中分布的

| dataSources个数 | dataSources配置            | actual table分布                                                          |
|---------------|--------------------------|-------------------------------------------------------------------------|
| 1             | ds_0                     | ds_0.t_order_0<br/>ds_0.t_order_1<br/>ds_0.t_order_2<br/>ds_0.t_order_3 |
| 2             | ds_0,ds_1                | ds_0.t_order_0<br/>ds_1.t_order_1<br/>ds_0.t_order_2<br/>ds_1.t_order_3 |
| 3             | ds_0,ds_1,ds_2           | ds_0.t_order_0<br/>ds_1.t_order_1<br/>ds_2.t_order_2<br/>ds_0.t_order_3 |
| 4             | ds_0,ds_1,ds_2,ds_3      | ds_0.t_order_0<br/>ds_1.t_order_1<br/>ds_2.t_order_2<br/>ds_3.t_order_3 |
| 5             | ds_0,ds_1,ds_2,ds_3,ds_4 | ds_0.t_order_0<br/>ds_1.t_order_1<br/>ds_2.t_order_2<br/>ds_3.t_order_3 |

通过上表我们得出结论：**sharding-jdbc autoTable 分库的策略是基于配置的`actualDataSources`进行轮询。**

### 1.1.2 [哈希取模分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#哈希取模分片算法)

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingRawYamlConfigurationExample`

选择 `private static ShardingType shardingType = ShardingType.SHARDING_AUTO_TABLES;`,

构建DataSource的配置文件位于`/META-INF/sharding-auto-tables.yaml`,内容如下所示:
```yaml
# 数据源配置，表示存在2个数据源
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_0?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_1?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

# 分片规则配置
rules:
- !SHARDING
# 自动分片
  autoTables:
    # 逻辑表
    t_order:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      # 分片策略
      shardingStrategy:
        # 标准分片:基于单个列的分片
        standard:
          # 分片列
          shardingColumn: order_id
          # 分片算法
          shardingAlgorithmName: auto-mod
      # id生成策略配置
      keyGenerateStrategy:
        # 自动生成id的列名
        column: order_id
        # id生成算法名
        keyGeneratorName: snowflake
    # 逻辑表        
    t_order_item:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      shardingStrategy:
        standard:
          shardingColumn: order_id
          shardingAlgorithmName: auto-mod
      keyGenerateStrategy:
        column: order_item_id
        keyGeneratorName: snowflake
    # 逻辑表
    t_account:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      shardingStrategy:
        standard:
          shardingAlgorithmName: auto-mod
      keyGenerateStrategy:
        column: account_id
        keyGeneratorName: snowflake        
  defaultShardingColumn: account_id
  # 分片算法
  shardingAlgorithms:
    auto-mod:
      # 取模分片
      type: HASH_MOD
      props:
        # 分片个数,用逻辑表分片列值的hashcode取绝对值与sharding-count取模,得到实际表
        sharding-count: 4
  
  keyGenerators:
    snowflake:
      type: SNOWFLAKE

props:
  # 打印实际SQL
  sql-show: true
```
以上配置会将逻辑表分为4张实际表,分片逻辑请参考`org.apache.shardingsphere.sharding.algorithm.sharding.mod.HashModShardingAlgorithm`。

### 1.1.3 [基于分片容量的范围分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#基于分片容量的范围分片算法)

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingRawYamlConfigurationExample`

选择 `private static ShardingType shardingType = ShardingType.SHARDING_AUTO_TABLES;`,

构建DataSource的配置文件位于`/META-INF/sharding-auto-tables.yaml`,内容如下所示:
```yaml
# 数据源配置，表示存在2个数据源
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_0?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_1?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

# 分片规则配置
rules:
- !SHARDING
# 自动分片
  autoTables:
    # 逻辑表
    t_order:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      # 分片策略
      shardingStrategy:
        # 标准分片:基于单个列的分片
        standard:
          # 分片列
          shardingColumn: order_id
          # 分片算法
          shardingAlgorithmName: volume-range
      # id生成策略配置
      keyGenerateStrategy:
        # 自动生成id的列名
        column: order_id
        # id生成算法名
        keyGeneratorName: snowflake
    # 逻辑表        
    t_order_item:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      shardingStrategy:
        standard:
          shardingColumn: order_id
          shardingAlgorithmName: volume-range
      keyGenerateStrategy:
        column: order_item_id
        keyGeneratorName: snowflake
    # 逻辑表
    t_account:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      shardingStrategy:
        standard:
          shardingAlgorithmName: volume-range
      keyGenerateStrategy:
        column: account_id
        keyGeneratorName: snowflake        
  defaultShardingColumn: account_id
  # 分片算法
  shardingAlgorithms:
    volume-range:
      # 基于分片容量的范围分片算法
      type: VOLUME_RANGE
      props:
        range-lower: 0
        range-upper: 10
        sharding-volume: 3
  
  keyGenerators:
    snowflake:
      type: SNOWFLAKE

props:
  # 打印实际SQL
  sql-show: true
```
上面的配置将自动分成6张表,分片键值落到对应的区间,该行记录就落在对应的rangeIndex所在的分表中。实现代码请参考`org.apache.shardingsphere.sharding.algorithm.sharding.range.VolumeBasedRangeShardingAlgorithm`

| rangeIndex |   range    |
|------------|----------- |
| 0          |  [-∞,0)         |
| 1          |  [0,3)          |
| 2          |  [3,6)          |
| 3          |  [6,9)          |
| 4          |  [9,10)           |
| 5          |  [10,+∞)          |

### 1.1.4 [基于分片边界的范围分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#基于分片边界的范围分片算法)

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingRawYamlConfigurationExample`

选择 `private static ShardingType shardingType = ShardingType.SHARDING_AUTO_TABLES;`,

构建DataSource的配置文件位于`/META-INF/sharding-auto-tables.yaml`,内容如下所示:
```yaml
# 数据源配置，表示存在2个数据源
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_0?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_1?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

# 分片规则配置
rules:
- !SHARDING
# 自动分片
  autoTables:
    # 逻辑表
    t_order:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      # 分片策略
      shardingStrategy:
        # 标准分片:基于单个列的分片
        standard:
          # 分片列
          shardingColumn: order_id
          # 分片算法
          shardingAlgorithmName: boundary-based
      # id生成策略配置
      keyGenerateStrategy:
        # 自动生成id的列名
        column: order_id
        # id生成算法名
        keyGeneratorName: snowflake
    # 逻辑表        
    t_order_item:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      shardingStrategy:
        standard:
          shardingColumn: order_id
          shardingAlgorithmName: boundary-based
      keyGenerateStrategy:
        column: order_item_id
        keyGeneratorName: snowflake
    # 逻辑表
    t_account:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      shardingStrategy:
        standard:
          shardingAlgorithmName: boundary-based
      keyGenerateStrategy:
        column: account_id
        keyGeneratorName: snowflake        
  defaultShardingColumn: account_id
  # 分片算法
  shardingAlgorithms:
    boundary-based:
      # 基于分片容量的范围分片算法
      type: BOUNDARY_RANGE
      props:
        sharding-ranges: 100000000000000000,500000000000000000,1000000000000000000
  
  keyGenerators:
    snowflake:
      type: SNOWFLAKE

props:
  # 打印实际SQL
  sql-show: true
```
上面的配置将自动分成4张表，如下所示：

| rangeIndex | range                                    |
|------------|------------------------------------------|
| 0          | [-∞,100000000000000000)                  |
| 1          | [100000000000000000,500000000000000000)  |
| 2          | [500000000000000000,1000000000000000000) |
| 3          | [1000000000000000000,+∞)                 |

分片键值落在对应区间,该记录就保存在对应rangeIndex的分表中。实现请参考`org.apache.shardingsphere.sharding.algorithm.sharding.range.BoundaryBasedRangeShardingAlgorithm`

### 1.1.5 [自动时间段分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#自动时间段分片算法)

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingRawYamlConfigurationExample`

选择 `private static ShardingType shardingType = ShardingType.SHARDING_AUTO_TABLES;`,

构建DataSource的配置文件位于`/META-INF/sharding-auto-tables.yaml`,内容如下所示:
```yaml
# 数据源配置，表示存在2个数据源
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_0?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_1?serverTimezone=Asia/Shanghai&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

# 分片规则配置
rules:
- !SHARDING
# 自动分片
  autoTables:
    # 逻辑表
    t_order:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      # 分片策略
      shardingStrategy:
        # 标准分片:基于单个列的分片
        standard:
          # 分片列
          shardingColumn: add_time
          # 分片算法
          shardingAlgorithmName: auto-interval
      # id生成策略配置
      keyGenerateStrategy:
        # 自动生成id的列名
        column: order_id
        # id生成算法名
        keyGeneratorName: snowflake
    # 逻辑表        
    t_order_item:
      # 实际的数据源，这里表示分2个库
      actualDataSources: ds_0,ds_1
      shardingStrategy:
        standard:
          shardingColumn: add_time
          shardingAlgorithmName: auto-interval
      keyGenerateStrategy:
        column: order_item_id
        keyGeneratorName: snowflake       
  defaultShardingColumn: add_time
  # 分片算法
  shardingAlgorithms:
    auto-interval:
      # 基于分片容量的范围分片算法
      type: AUTO_INTERVAL
      props:
        datetime-lower: "2022-05-01 00:00:00"
        datetime-upper: "2022-06-01 00:00:00"
        sharding-seconds: "86400"
  
  keyGenerators:
    snowflake:
      type: SNOWFLAKE

props:
  # 打印实际SQL
  sql-show: true
```

上面的配置将自动分成32张表,按照1天(86400秒)的时间间隔进行分表。可以看到这种分片方式实际当中不是太实用,`sharding-seconds`以秒为单位，除了一分钟、一小时、一条这种间隔的秒数是固定的外,

更常见的按月分表，由于不同月的间隔秒数不一致，不太好采用这种方式来分表.参考代码`org.apache.shardingsphere.sharding.algorithm.sharding.datetime.AutoIntervalShardingAlgorithm`



## 1.2.标准分片算法

### 1.2.1 [行表达式分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#行表达式分片算法) 

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingRawYamlConfigurationExample`

选择 `private static ShardingType shardingType = ShardingType.SHARDING_DATABASES_AND_TABLES;`,

构建DataSource的配置文件位于`/META-INF/sharding-databases-tables.yaml`,有关行表达式相关的内容,请参考[这里](https://shardingsphere.apache.org/document/current/cn/features/sharding/concept/inline-expression/),
配置内容如下所示:
```yaml
# 数据源配置
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_0?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_1?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

rules:
  - !SHARDING
    # 标准分片模式
    tables:
      # 逻辑表名
      t_order:
        # 实际数据节点列表ds_0.t_order_0,ds_0.t_order_1,ds_1.t_order_0,ds_1.t_order_1
        actualDataNodes: ds_${0..1}.t_order_${0..1}
        # 分表策略
        tableStrategy:
          # 标准分片模式
          standard:
            # 分片列
            shardingColumn: order_id
            # 分片算法
            shardingAlgorithmName: t-order-inline
        # key生成策略    
        keyGenerateStrategy:
          # 自动生成id的列名
          column: order_id
          # 自动生成id的算法
          keyGeneratorName: snowflake
      # 逻辑表名    
      t_order_item:
        # 实际数据节点列表ds_0.t_order_item_0,ds_0.t_order_item_1,ds_1.t_order_item_0,ds_1.t_order_item_1
        actualDataNodes: ds_${0..1}.t_order_item_${0..1}
        tableStrategy:
          standard:
            shardingColumn: order_id
            shardingAlgorithmName: t_order-item-inline
        keyGenerateStrategy:
          column: order_item_id
          keyGeneratorName: snowflake
    # 默认分片列
    defaultShardingColumn: account_id
    # 默认分库策略
    defaultDatabaseStrategy:
      #标准分片模式
      standard:
        # 分片列
        shardingColumn: user_id
        # 分片算法
        shardingAlgorithmName: database-inline
    defaultTableStrategy:
      none:

    #分片算法
    shardingAlgorithms:
      database-inline:
        # 内置行表达式算法
        type: INLINE
        props:
          algorithm-expression: ds_${user_id % 2}
      t-order-inline:
        type: INLINE
        props:
          algorithm-expression: t_order_${order_id % 2}
      t_order-item-inline:
        type: INLINE
        props:
          algorithm-expression: t_order_item_${order_id % 2}
    #key生成算法      
    keyGenerators:
      snowflake:
        type: SNOWFLAKE

props:
  sql-show: true
```

上面的配置表示分2个库每个库2张表,通过表达式`user_id % 2`来分库,通过表达式 `order_id % 2`来分表

1. `Logic SQL: INSERT INTO t_order (user_id, address_id, status, add_time) VALUES (?, ?, ?, ?)`对应的值`[1, 1, INSERT_TEST, 2022-04-01 04:00:00, 731207984494936064]`

`user_id % 2`等于1,所以选择库`ds_1`, `order_id % 2`等于0,所以选择表`t_order_0`,最终选择的节点为`ds_1.t_order_0`

`Actual SQL: ds_1 ::: INSERT INTO t_order_0 (user_id, address_id, status, add_time, order_id) VALUES (?, ?, ?, ?, ?) ::: [1, 1, INSERT_TEST, 2022-04-01 04:00:00, 731207984494936064]`

2. `Logic SQL: SELECT * FROM t_order` 这条SQL没有带分区键,会广播所有库+所有表,执行结果如下:

`Actual SQL: ds_0 ::: SELECT * FROM t_order_0 UNION ALL SELECT * FROM t_order_1`
`Actual SQL: ds_1 ::: SELECT * FROM t_order_0 UNION ALL SELECT * FROM t_order_1`

3. `Logic SQL: DELETE FROM t_order WHERE order_id=?` 这条SQL没有带分库键,会广播所有库;同时它带有分表键,所以能根据行表达式找到对应的分表,执行结果如下:

`Actual SQL: ds_0 ::: DELETE FROM t_order_0 WHERE order_id=? ::: [731207984494936064]`
`Actual SQL: ds_1 ::: DELETE FROM t_order_0 WHERE order_id=? ::: [731207984494936064]`

### 1.2.2 [时间范围分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#时间范围分片算法)

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingRawYamlConfigurationExample`

选择 `private static ShardingType shardingType = ShardingType.SHARDING_INTERVAL_TABLES`,

构建DataSource的配置文件位于`/META-INF/sharding-interval-tables.yaml`, 配置内容如下所示:
```yaml
# 数据源配置
dataSources:
  ds:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

rules:
  - !SHARDING
    # 标准分片模式
    tables:
      # 逻辑表名
      t_order:
        # 实际数据节点列表
        actualDataNodes: ds.t_order_20220${1..9},ds.t_order_20221${0..2}
        # 分表策略
        tableStrategy:
          # 标准分片模式
          standard:
            # 分片列
            shardingColumn: add_time
            # 分片算法
            shardingAlgorithmName: interval-algorithm
        # key生成策略
        keyGenerateStrategy:
          # 自动生成id的列名
          column: order_id
          # 自动生成id的算法
          keyGeneratorName: snowflake
      # 逻辑表名
      t_order_item:
        # 实际数据节点列表
        actualDataNodes: ds.t_order_item_20220${1..9},ds.t_order_item_20221${0..2}
        tableStrategy:
          standard:
            shardingColumn: add_time
            shardingAlgorithmName: interval-algorithm
        keyGenerateStrategy:
          column: order_item_id
          keyGeneratorName: snowflake
    # 默认分库策略
    defaultDatabaseStrategy:
      none:
    defaultTableStrategy:
      none:

    #分片算法
    shardingAlgorithms:
      interval-algorithm:
        type: INTERVAL
        props:
          datetime-pattern: "yyyy-MM-dd HH:mm:ss"
          datetime-lower: "2022-05-01 00:00:00"
          sharding-suffix-pattern: "yyyyMM"
          datetime-interval-amount: 1
          datetime-interval-unit: "MONTHS"
    #key生成算法
    keyGenerators:
      snowflake:
        type: SNOWFLAKE

props:
  sql-show: true
```

上述yaml配置了2022年按月分12张分表,`datetime-lower`配置为`2022-05-01`,如果我们插入一条时间早于该值的记录会报错如下所示:
```shell
Exception in thread "main" java.lang.IllegalStateException: Insert statement does not support sharding table routing to multiple data nodes.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:508)
	at org.apache.shardingsphere.sharding.route.engine.validator.dml.impl.ShardingInsertStatementValidator.postValidate(ShardingInsertStatementValidator.java:101)
	at org.apache.shardingsphere.sharding.route.engine.ShardingSQLRouter.lambda$createRouteContext$1(ShardingSQLRouter.java:57)
	at java.util.Optional.ifPresent(Optional.java:159)
	at org.apache.shardingsphere.sharding.route.engine.ShardingSQLRouter.createRouteContext(ShardingSQLRouter.java:57)
	at org.apache.shardingsphere.sharding.route.engine.ShardingSQLRouter.createRouteContext(ShardingSQLRouter.java:44)
	at org.apache.shardingsphere.infra.route.engine.impl.PartialSQLRouteExecutor.route(PartialSQLRouteExecutor.java:73)
	at org.apache.shardingsphere.infra.route.engine.SQLRouteEngine.route(SQLRouteEngine.java:53)
	at org.apache.shardingsphere.infra.context.kernel.KernelProcessor.route(KernelProcessor.java:54)
	at org.apache.shardingsphere.infra.context.kernel.KernelProcessor.generateExecutionContext(KernelProcessor.java:46)
	at org.apache.shardingsphere.driver.jdbc.core.statement.ShardingSpherePreparedStatement.createExecutionContext(ShardingSpherePreparedStatement.java:470)
	at org.apache.shardingsphere.driver.jdbc.core.statement.ShardingSpherePreparedStatement.executeUpdate(ShardingSpherePreparedStatement.java:309)
```
那我们不禁要问插入的最大值是多少呢？文档中指出`datetime-upper`不配置的话,默认值为当前时间;根据实验，在按月分表的情况下,只要格式化(yyyyMM)后值为`202205`，都能正常插入数据,
也就是说最大插入的时间为`2022-05-31 23:59:59`。但是如果插入日期为`2022-06-01 00:00:00`的数据,就会报错如下所示:
```
Exception in thread "main" java.lang.IllegalStateException: Insert statement does not support sharding table routing to multiple data nodes.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:508)
	at org.apache.shardingsphere.sharding.route.engine.validator.dml.impl.ShardingInsertStatementValidator.postValidate(ShardingInsertStatementValidator.java:101)
	at org.apache.shardingsphere.sharding.route.engine.ShardingSQLRouter.lambda$createRouteContext$1(ShardingSQLRouter.java:57)
	at java.util.Optional.ifPresent(Optional.java:159)
	at org.apache.shardingsphere.sharding.route.engine.ShardingSQLRouter.createRouteContext(ShardingSQLRouter.java:57)
	at org.apache.shardingsphere.sharding.route.engine.ShardingSQLRouter.createRouteContext(ShardingSQLRouter.java:44)
	at org.apache.shardingsphere.infra.route.engine.impl.PartialSQLRouteExecutor.route(PartialSQLRouteExecutor.java:73)
	at org.apache.shardingsphere.infra.route.engine.SQLRouteEngine.route(SQLRouteEngine.java:53)
	at org.apache.shardingsphere.infra.context.kernel.KernelProcessor.route(KernelProcessor.java:54)
	at org.apache.shardingsphere.infra.context.kernel.KernelProcessor.generateExecutionContext(KernelProcessor.java:46)
	at org.apache.shardingsphere.driver.jdbc.core.statement.ShardingSpherePreparedStatement.createExecutionContext(ShardingSpherePreparedStatement.java:470)
	at org.apache.shardingsphere.driver.jdbc.core.statement.ShardingSpherePreparedStatement.executeUpdate(ShardingSpherePreparedStatement.java:309)
```

我们换成按天分表重新来验证，配置如下所示:
```yaml
# 数据源配置
dataSources:
  ds:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

rules:
  - !SHARDING
    # 标准分片模式
    tables:
      # 逻辑表名
      t_order:
        # 实际数据节点列表
        actualDataNodes: ds.t_order_2022050${1..9},ds.t_order_2022051${1..9},ds.t_order_2022052${1..9},ds.t_order_2022053${0..1}
        # 分表策略
        tableStrategy:
          # 标准分片模式
          standard:
            # 分片列
            shardingColumn: add_time
            # 分片算法
            shardingAlgorithmName: interval-algorithm
        # key生成策略
        keyGenerateStrategy:
          # 自动生成id的列名
          column: order_id
          # 自动生成id的算法
          keyGeneratorName: snowflake
      # 逻辑表名
      t_order_item:
        # 实际数据节点列表
        actualDataNodes: ds.t_order_item_2022050${1..9},ds.t_order_item_2022051${1..9},ds.t_order_item_2022052${1..9},ds.t_order_item_2022053${0..1}
        tableStrategy:
          standard:
            shardingColumn: add_time
            shardingAlgorithmName: interval-algorithm
        keyGenerateStrategy:
          column: order_item_id
          keyGeneratorName: snowflake
    # 默认分库策略
    defaultDatabaseStrategy:
      none:
    defaultTableStrategy:
      none:

    #分片算法
    shardingAlgorithms:
      interval-algorithm:
        type: INTERVAL
        props:
          datetime-pattern: "yyyy-MM-dd HH:mm:ss"
          datetime-lower: "2022-05-01 00:00:00"
          sharding-suffix-pattern: "yyyyMMdd"
          datetime-interval-amount: 1
          datetime-interval-unit: "DAYS"
    #key生成算法
    keyGenerators:
      snowflake:
        type: SNOWFLAKE

props:
  sql-show: true
```

这一次，我们插入`2022-05-13 00:00:00`的记录，sharding-jdbc会抛出如下异常:
```
Exception in thread "main" java.lang.IllegalStateException: Insert statement does not support sharding table routing to multiple data nodes.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:508)
	at org.apache.shardingsphere.sharding.route.engine.validator.dml.impl.ShardingInsertStatementValidator.postValidate(ShardingInsertStatementValidator.java:101)
	at org.apache.shardingsphere.sharding.route.engine.ShardingSQLRouter.lambda$createRouteContext$1(ShardingSQLRouter.java:57)
	at java.util.Optional.ifPresent(Optional.java:159)
	at org.apache.shardingsphere.sharding.route.engine.ShardingSQLRouter.createRouteContext(ShardingSQLRouter.java:57)
	at org.apache.shardingsphere.sharding.route.engine.ShardingSQLRouter.createRouteContext(ShardingSQLRouter.java:44)
	at org.apache.shardingsphere.infra.route.engine.impl.PartialSQLRouteExecutor.route(PartialSQLRouteExecutor.java:73)
	at org.apache.shardingsphere.infra.route.engine.SQLRouteEngine.route(SQLRouteEngine.java:53)
	at org.apache.shardingsphere.infra.context.kernel.KernelProcessor.route(KernelProcessor.java:54)
	at org.apache.shardingsphere.infra.context.kernel.KernelProcessor.generateExecutionContext(KernelProcessor.java:46)
	at org.apache.shardingsphere.driver.jdbc.core.statement.ShardingSpherePreparedStatement.createExecutionContext(ShardingSpherePreparedStatement.java:470)
	at org.apache.shardingsphere.driver.jdbc.core.statement.ShardingSpherePreparedStatement.executeUpdate(ShardingSpherePreparedStatement.java:309)
```

通过按月和按日分表的实验，可以发现对于`datetime-upper`这项配置的默认值,取决于分表的时间维度,只要插入的记录时间与当前时间经过分表规则判断一致时，都可以正常记录，但插入记录的时间经过分表规则计算后，

如果大于以当前时间经过分表规则计算后的分区，则会插入失败。

另外，通过实验，发现`datetime-upper`的默认值就是sharding-jdbc允许的上限了，如果你不是要缩小这个上限，直接忽略该配置即可。否则的话，可以通过设置`datetime-upper`为一个比默认值小的时间即可，

设置完后，它仍然遵循前面推到出来的规则。

## 1.3.复合分片算法

### 1.3.1 [复合行表达式分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#复合行表达式分片算法)

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingRawYamlConfigurationExample`

选择 `private static ShardingType shardingType = ShardingType.ShardingType.SHARDING_COMPLEX_INLINE_TABLES`,

构建DataSource的配置文件位于`/META-INF/sharding-complex-inline-tables.yaml`, 配置内容如下所示:
```yaml
dataSources:
  ds:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

rules:
- !SHARDING
  tables:
    t_order: 
      actualDataNodes: ds.t_order_${0..1}
      tableStrategy: 
        # 分片策略选择complex,表示有多个分片键
        complex:
          shardingColumns: user_id,order_id
          shardingAlgorithmName: t-order-complex-inline
      keyGenerateStrategy:
        column: order_id
        keyGeneratorName: snowflake
    t_order_item:
      actualDataNodes: ds.t_order_item_${0..1}
      tableStrategy:
        # 分片策略选择complex,表示有多个分片键
        complex:
          shardingColumns: user_id,order_id
          shardingAlgorithmName: t-order-item-complex-inline
      keyGenerateStrategy:
        column: order_item_id
        keyGeneratorName: snowflake
  bindingTables:
    - t_order,t_order_item
  broadcastTables:
    - t_address
  
  shardingAlgorithms:
    t-order-complex-inline:
      type: COMPLEX_INLINE
      props:
        algorithm-expression: t_order_${(user_id + order_id)  % 2}
        sharding-columns: user_id,order_id
    t-order-item-complex-inline:
      type: COMPLEX_INLINE
      props:
        algorithm-expression: t_order_item_${(user_id + order_id)  % 2}
        sharding-columns: user_id,order_id
  keyGenerators:
    snowflake:
      type: SNOWFLAKE

props:
  sql-show: true
```

上述配置对表`t_order`和`t_order_item`分别设置了2个分片键,`user_id`和`order_id`,其分片逻辑请参考`org.apache.shardingsphere.sharding.algorithm.sharding.complex.ComplexInlineShardingAlgorithm`

该算法要求逻辑SQL中要么一个分片键没有，要么所有的分片键都有，不允许出现部分分片键出现的情况。

## 1.4.Hint 分片算法

### 1.4.1 [Hint 行表达式分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#hint-行表达式分片算法)

在有些情况下，分片列并不是待分片表的一部分，这种场景下sharding-jdbc提供了Hint分片算法，它可以通过在操作数据前设置Hint值，然后配合分片算法完成分片逻辑。

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingHintRawExample`

选择 `private static final ShardingType TYPE = ShardingType.SHARDING_HINT_DATABASES_TABLES`,

构建DataSource的配置文件位于`/META-INF/sharding-hint-databases-tables.yaml`, 配置内容如下所示:
```yaml
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_0?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_1?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

rules:
- !SHARDING
  tables:
    t_order: 
      actualDataNodes: ds_${0..1}.t_order_${0..1}
      # 分库策略基于hit
      databaseStrategy:
        hint:
          shardingAlgorithmName: hint-test
      # 分表策略基于hit
      tableStrategy:
        hint:
          shardingAlgorithmName: hint-test
      keyGenerateStrategy:
        column: order_id
        keyGeneratorName: snowflake
    t_order_item:
      actualDataNodes: ds_${0..1}.t_order_item_${0..1}

  # 默认的分库策略
  defaultDatabaseStrategy:
    standard:
      # 分库列
      shardingColumn: user_id
      # 分库算法
      shardingAlgorithmName: database-inline

  defaultTableStrategy:
    none:

  shardingAlgorithms:
    # 自定义的HIT分片算法
    hint-test:
      type: HINT_TEST
    # 默认的数据库分片算法
    database-inline:
      type: INLINE
      props:
        algorithm-expression: ds_${user_id % 2}
    
  keyGenerators:
    snowflake:
      type: SNOWFLAKE

props:
  sql-show: true
```

上述配置将`t_order`和`t_order_item`都分为了两个库两张表.

其中`t_order`的分库和分表算法都是基于自定义的`HINT_TEST`,代码实现请参考`org.apache.shardingsphere.example.sharding.raw.jdbc.hint.ModuloHintShardingAlgorithm`

`t_order_item`的分库策略是对`user_id`列采用行表达式算法取模。

以下SQL的分库分表策略主要取决于Hint值,按如下所示进行设置：

```
hintManager.addDatabaseShardingValue("t_order", 1L);
hintManager.addTableShardingValue("t_order", 1L);
```


1. 对于SQL `select * from t_order` 

基于对`HINT_TEST`算法的逻辑分析，我们可以推断实际分片为`ds_1.t_order_1`。执行代码运行结果如下所示：

```
Actual SQL: ds_1 ::: select * from t_order_1
```

2. 对于SQL `SELECT i.* FROM t_order o, t_order_item i WHERE o.order_id = i.order_id`

`t_order`还是一样，但`t_order_item`由于不含分片键,需要广播所有的表。这里就有个疑问，`t_order_item`一共有4张表，那它广播的方式是怎样呢？通过观察执行结果如下所示:

```
Actual SQL: ds_1 ::: SELECT i.* FROM t_order_1 o, t_order_item_0 i WHERE o.order_id = i.order_id
Actual SQL: ds_1 ::: SELECT i.* FROM t_order_1 o, t_order_item_1 i WHERE o.order_id = i.order_id
```

可知它只广播已经确定的库`ds_1`中2张表`t_order_item_0`和`t_order_item_1`。

3. 对于SQL `select * from t_order_item`

没有带分片键，从理论上分析，应该是广播所有库所有分表，实际运行结果如下所示:

```
Actual SQL: ds_0 ::: select * from t_order_item_0 UNION ALL select * from t_order_item_1
Actual SQL: ds_1 ::: select * from t_order_item_0 UNION ALL select * from t_order_item_1
```

4. 对于SQL `INSERT INTO t_order (user_id, address_id, status,add_time) VALUES (1, 1, 'init',now())`

同1,直接写数据到`ds_1.t_order_1`，执行结果如下所示：

```
Actual SQL: ds_1 ::: INSERT INTO t_order_1 (user_id, address_id, status,add_time, order_id) VALUES (1, 1, 'init', now(), 731578033290346496)
```

## 1.5.自定义类分片算法

### 1.5.1 [自定义类分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#自定义类分片算法)

假设有这样一张订单表,有两种维度的查询请求，一种是根据`user_id`，还有一种是根据`order_no`。我们要对它进行分库分表，那应该如何设计呢？

比较容易想到的是以`user_id`作为分片键，但这时根据`order_no`来查询就会有问题，一个`user_id`通常会对应多个`order_no`,一个`order_no`只会对应一个`user_id`,
我们可以将`user_id`的作为`order_no`的一部分,这样在根据`order_no`查询的时候，就可以通过分析`order_no`获得`user_id`，进而拿到分片键值，完成分库分表规则的路由。

为了不增加`order_no`的长度，我们可以以`user_id`的后四位作为分库分表的因子，进而得到一种`order_no`的生成规则,假设`order_no`一共20位:
`2205131049 1234 01 0001` 前10位是时间的`yyMMddHHmm`格式化字符串,第10-13位是`user_id`的后4位,第13-14位表示部署服务器的机器号,最后4位为随机数。

针对这种分片算法sharding-jdbc提供了自定义分片算法的接口,我们先实现自定义分片算法,代码请参考`org.apache.shardingsphere.example.sharding.raw.jdbc.algorithm.MyComplexShardingAlgorithm`.

示例类:`org.apache.shardingsphere.example.sharding.raw.jdbc.ShardingRawYamlConfigurationExample`

选择 `private static ShardingType shardingType = ShardingType.SHARDING_COMPLEX_CUSTOM_DATABASES_TABLES`,

构建DataSource的配置文件位于`/META-INF/sharding-complex-databases-tables.yaml`, 配置内容如下所示:
```yaml
# 数据源配置
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_0?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_1?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

rules:
  - !SHARDING
    # 标准分片模式
    tables:
      # 逻辑表名
      t_my_order:
        # 实际数据节点列表
        actualDataNodes: ds_0.t_my_order_${[0,2]},ds_1.t_my_order_${[1,3]}
        # 分表策略
        tableStrategy:
          # 标准分片模式
          complex:
            # 分片列
            shardingColumns: user_id,order_no
            # 分片算法
            shardingAlgorithmName: table-complex
        # key生成策略
        keyGenerateStrategy:
          # 自动生成id的列名
          column: order_id
          # 自动生成id的算法
          keyGeneratorName: snowflake
    # 默认分库策略
    defaultDatabaseStrategy:
      # 复合分片模式
      complex:
        # 分片列
        shardingColumns: user_id,order_no
        # 分片算法
        shardingAlgorithmName: database-complex
    defaultTableStrategy:
      none:

    #分片算法
    shardingAlgorithms:
      database-complex:
        # 自定义分片算法
        type: CLASS_BASED
        props:
          strategy: complex
          algorithmClassName: org.apache.shardingsphere.example.sharding.raw.jdbc.algorithm.MyComplexShardingAlgorithm
          sharding-count: 2
      table-complex:
        type: CLASS_BASED
        props:
          strategy: complex
          algorithmClassName: org.apache.shardingsphere.example.sharding.raw.jdbc.algorithm.MyComplexShardingAlgorithm
          sharding-count: 4

    #key生成算法
    keyGenerators:
      snowflake:
        type: SNOWFLAKE
props:
  sql-show: true
```
上述配置分2个库4张表:`ds_0.t_my_order_0,ds_0.t_my_order_2,ds_1.t_my_order_1,ds_1.t_my_order_3`

接下来我们依次看下SQL执行的情况

1. `INSERT INTO t_my_order (order_no,user_id, address_id, status) VALUES (?,?, ?, ?)` 值为 `22051311480001049898, 1, 1, INSERT_TEST, 731839484919808000`

该SQL两个分片键`user_id`和`order_no`都存在,分库经过自定义算法会选择`ds_1`，同理分表会选择`t_my_order_1`，实际运行结果如下所示：

```
Actual SQL: ds_1 ::: INSERT INTO t_my_order_1 (order_no,user_id, address_id, status, order_id) VALUES (?, ?, ?, ?, ?) ::: [22051311480001049898, 1, 1, INSERT_TEST, 731839484919808000]
```

2. `SELECT * FROM t_my_order order by order_id` 

该SQL无分片键，会执行广播路由，实际执行情况如下所示：

```
Actual SQL: ds_0 ::: SELECT * FROM t_my_order_0 order by order_id
Actual SQL: ds_0 ::: SELECT * FROM t_my_order_2 order by order_id
Actual SQL: ds_1 ::: SELECT * FROM t_my_order_1 order by order_id
Actual SQL: ds_1 ::: SELECT * FROM t_my_order_3 order by order_id
```

3. `DELETE FROM t_my_order WHERE order_id=?`

该SQL所带条件order_id无分片键,会执行广播路由,实际执行情况如下所示：

```
Actual SQL: ds_0 ::: DELETE FROM t_my_order_0 WHERE order_id=? ::: [731839484919808000]
Actual SQL: ds_0 ::: DELETE FROM t_my_order_2 WHERE order_id=? ::: [731839484919808000]
Actual SQL: ds_1 ::: DELETE FROM t_my_order_1 WHERE order_id=? ::: [731839484919808000]
Actual SQL: ds_1 ::: DELETE FROM t_my_order_3 WHERE order_id=? ::: [731839484919808000]
```

4. `DELETE FROM t_my_order WHERE user_id = ?`

该SQL带有分片键`user_id`,会执行分库分表路由，执行结果如下所示：

```
Actual SQL: ds_1 ::: DELETE FROM t_my_order_3 WHERE user_id = ? ::: [3]
```

5. `DELETE FROM t_my_order WHERE order_no = ?`

该SQL带有分片键`order_no`，会执行分库分表路由，执行结果如下所示：

``` 
Actual SQL: ds_1 ::: DELETE FROM t_my_order_3 WHERE order_no = ? ::: [22051311480003284699]
```

关于自定义分片算法除了上述`type: CLASS_BASED` 的配置方式，还可以通过SPI的方式配置，具体配置如下所示：
```yaml
# 数据源配置
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_0?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/demo_ds_1?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
    username: root
    password: 123456

rules:
  - !SHARDING
    # 标准分片模式
    tables:
      # 逻辑表名
      t_my_order:
        # 实际数据节点列表
        actualDataNodes: ds_0.t_my_order_${[0,2]},ds_1.t_my_order_${[1,3]}
        # 分表策略
        tableStrategy:
          # 标准分片模式
          complex:
            # 分片列
            shardingColumns: user_id,order_no
            # 分片算法
            shardingAlgorithmName: table-complex
        # key生成策略
        keyGenerateStrategy:
          # 自动生成id的列名
          column: order_id
          # 自动生成id的算法
          keyGeneratorName: snowflake
    # 默认分库策略
    defaultDatabaseStrategy:
      # 复合分片模式
      complex:
        # 分片列
        shardingColumns: user_id,order_no
        # 分片算法
        shardingAlgorithmName: database-complex
    defaultTableStrategy:
      none:

    #分片算法
    shardingAlgorithms:
      database-complex:
        # 自定义分片算法
        type: COMPLEX_USERID_ORDERNO
        props:
          sharding-count: 2
      table-complex:
        type: COMPLEX_USERID_ORDERNO
        props:
          sharding-count: 4

    #key生成算法
    keyGenerators:
      snowflake:
        type: SNOWFLAKE
props:
  sql-show: true
```

