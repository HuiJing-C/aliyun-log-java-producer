# 常见问题

**Q:** Aliyun LOG Java Producer 依赖 [Aliyun LOG Java SDK](https://github.com/aliyun/aliyun-log-java-sdk)，而 Aliyun LOG Java SDK 依赖了 2.5.0 版本的 protobuf 库，如果该版本的 protobuf 与用户应用程序中自身带的 protobuf 库冲突怎么办？

A: 可以使用 Aliyun LOG Java SDK 提供的一个特殊版本`jar-with-dependencies`，它包含第三方依赖库并重写了这些库的命名空间。配置方式如下：
```
<dependency>
    <groupId>com.aliyun.openservices</groupId>
    <artifactId>aliyun-log</artifactId>
    <version>${aliyun-log.version}</version>
    <classifier>jar-with-dependencies</classifier>
    <exclusions>
        <exclusion>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.aliyun.openservices</groupId>
    <artifactId>aliyun-log-producer</artifactId>
    <version>${aliyun-log-producer.version}</version>
    <exclusions>
        <exclusion>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.aliyun.openservices</groupId>
            <artifactId>aliyun-log</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**Q:** 日志写入过程中返回如下错误`com.aliyun.openservices.log.exception.LogException: denied by sts or ram, action: log:PostLogStoreLogs, resource: acs:log:${regionName}:${projectOwnerAliUid}:project/${projectName}/logstore/${logstoreName}`？

A：子账号没有目标 project、logStore 的写权限，请参考 [RAM 子账号访问](https://github.com/aliyun/aliyun-log-java-producer#ram-%E5%AD%90%E8%B4%A6%E5%8F%B7%E8%AE%BF%E9%97%AE)配置相应权限。

**Q:** 应该在何时调用 producer 的 close() 方法？

A: 请在程序退出之前调用 producer 的 close() 方法，以防止缓存在内存中的数据丢失。

**Q:** Producer 会缓存待发送的数据，并将数据合并成 batch 后批量发往服务端。什么样的数据有机会合并在相同的 batch 里？

A: 具有相同 project，logStore，topic，source，shardHash 的数据会被合并在一起。为了让数据合并功能充分发挥作用，同时也为了节省内存，建议您控制这 5 个字段的取值范围。如果某个字段如 topic 的取值确实非常多，建议您将其加入 logItem 而不是直接使用 topic。

**Q:** 分裂 shard 后，原来 shard 变成只读了，为了让数据写入新的 shard 需要重启写入程序吗？

A: 不需要。服务端会自动将待写入的数据路由到新分裂出来的可写 shard 上。

**Q:** Producer 能否保证日志上传顺序？即先发送的日志先写入服务端？

A: Producer 异步多线程发送数据，无法保证日志上传顺序。但您可以通过日志中的 time 字段判断日志产生顺序。

**Q:** Producer 支持通过 HTTPS 发送日志吗？

A: 支持。将 ProjectConfig.endpoint 设置成`https://<endpoint>`即可。

**Q:** 程序运行过程中抛出如下异常`Caused by: java.lang.NoSuchMethodError: com.google.common.hash.Hashing.farmHashFingerprint64()`？

A: 这是因为项目引入的 guava lib 不包含 farmHash 函数。Producer 使用的 guava 版本为 [27.0-jre](https://github.com/aliyun/aliyun-log-java-producer/blob/master/pom.xml#L45)，请将项目引入的 guava lib 至少升级到 27.0-jre。 
```
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>27.0-jre</version>
</dependency>
```

**Q:** 程序运行过程中抛出如下异常`java.lang.VerifyError: class com.aliyun.openservices.log.common.Logs$LogGroup overrides final method getUnknownFields.()Lcom/google/protobuf/UnknownFieldSet;
at java.lang.ClassLoader.defineClass1(Native Method)
...`？

A: 检查项目中引入的 protobuf 版本是否低于 2.5.0。这些版本中，类`Logs$LogGroup`的方法`getUnknownFields`被申明为 final，无法override，从而报错。解决方法是引入 2.5.0 版本的 protobuf。
```
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>2.5.0</version>
</dependency>
```

**Q:** 程序运行过程中抛出如下异常`java.lang.NoSuchMethodError: com.aliyun.openservices.log.request.PutLogsRequest.SetTags(Ljava/util/List;)V...`？

A: 这是因为项目引入的`aliyun-log`版本过低造成的，请确保其版本至少为`0.6.31`。
```
<dependency>
    <groupId>com.aliyun.openservices</groupId>
    <artifactId>aliyun-log</artifactId>
    <version>0.6.31</version>
</dependency>
```





