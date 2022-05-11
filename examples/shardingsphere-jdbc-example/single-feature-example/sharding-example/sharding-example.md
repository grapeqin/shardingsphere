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
构建DataSource的配置文件位于`/META-INF/sharding-auto-tables.yaml`,内容如下所示:
详细的配置文件请参考[这里](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/yaml-config/rules/sharding/#配置项说明)
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
以上配置会将逻辑表分为4张实际表,分片逻辑通过`org.apache.shardingsphere.sharding.algorithm.sharding.mod.ModShardingAlgorithm`的实现予以证实。那关于db的选择是怎样的呢？
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
以上配置会将逻辑表分为4张实际表,分片逻辑通过`org.apache.shardingsphere.sharding.algorithm.sharding.mod.HashModShardingAlgorithm`的实现予以证实。

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



### 1.2.2 [时间范围分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#时间范围分片算法)

## 1.3.复合分片算法

### 1.3.1 [复合行表达式分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#复合行表达式分片算法)

## 1.4.Hint 分片算法

### 1.4.1 [Hint 行表达式分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#hint-行表达式分片算法)

## 1.5.自定义类分片算法

### 1.5.1 [自定义类分片算法](https://shardingsphere.apache.org/document/current/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/sharding/#自定义类分片算法)