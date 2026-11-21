# A Java demo application

This demo repository is used to show how to streamline build and deployment using pipelines in CodeArts.

## Basic Information

+ Language: **Java**
+ Template: a Java web project based on Spring Boot
+ Function: basic scaffolding code for development of Java web applications

## How to Build

### Environment Dependency

Build Dependency
* JDK 8 (OpenJDK is fine) 
* **Maven 3.3** Build Tool

### How to Download Code

Clone code in the environment.
Install the following tools:
* [Maven 3.3]
* [JDK 1.8]

### Build Procedure

In the repository root directory

* Run the following command:
```bash
mvn clean install
```
* Build:
```bash
mvn clean compile
```
* Execute unit test cases:
```bash
mvn test
```
* Execute integration test cases:
```bash
mvn verify
```

* Package:
```bash
mvn package
```

* Install to the local Maven repository:
```bash
mvn install
```

## Build Task

* Build with Maven

* Upload software package to the release repository.

## How to Execute

### Local Execution

Run the following command to start the application:

```bash
java -jar ./target/javaWebDemo-1.0.jar
```

Visit http://localhost:8080/.


## Pipeline

- Whether Automatic Pipeline Creation Is Supported: **Supported**

- Pipeline Structure

> Start Stage
+ Source code repository

> Build Stage
+ Build task
+ Code check task

> Deployment Stage
+ Deployment task
+ API test task

## 商城 Redis 缓存

将 `spring.redis` 指向专用 Redis 实例。示例配置监听 `127.0.0.1:6379`。

启动专用 Redis 实例时加载 [缓存配置](deploy/redis/shop-cache.conf)。

```bash
redis-server deploy/redis/shop-cache.conf
redis-cli CONFIG GET maxmemory maxmemory-policy
```

配置将缓存内存阈值设为 128 MiB，并优先淘汰访问频率较低的键。该策略作用于整个实例。使用托管 Redis 时，在该实例的参数配置中设置相同的内存阈值和策略。

首页、分类和商品详情优先从 Redis 读取。列表基础有效期为 5 分钟，详情为 30 分钟，空结果为 60 秒，各自加上最多 10% 的随机偏移。超过 1 MiB 的单条结果直接返回。商品、库存或评价变更提交后会切换商城缓存版本，旧数据按原有效期或淘汰策略清理。Redis 暂时不可用时，执行写入的后端会在恢复连接后补做失效。该重试记录存在进程内，进程退出后旧数据仍按有效期清理。

商品缓存回源默认每个后端实例最多同时执行 4 次，长期速率为每秒 12 次，允许短时突发。同一缓存键的并发请求共享一次重建。Redis 不可用时仍受回源限额保护。超出预算或等待超时会返回 HTTP 503 和 `Retry-After: 1`。可在本地配置中按数据库容量调整以下参数。

```yaml
code1st:
  product-cache:
    max-concurrent-loads: 4
    loads-per-second: 12
```
