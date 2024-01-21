
<img src="https://foruda.gitee.com/images/1693470775312764207/27440c57_12260.png" alt="flowlong" width="100px" height="113px">

# 项目介绍
FlowLong🐉飞龙工作流

- 项目说明  `flowlong` 中文名 `飞龙` 在天美好愿景！

> ⭕本项目采用 `AGPL` 开源协议（抄袭牟利索赔100万）且不允许二次封装开源。

> 使用必须遵守国家法律法规，⛔不允许非法项目使用，后果自负❗

[使用源码登记入口](https://gitee.com/aizuda/flowlong/issues/I7XGP5) 

[打开官方开发文档](https://flowlong.gitee.io)

[点击设计器在线演示](https://flowlong.gitee.io/flowlong-designer)

[点击设计器源码下载](https://gitee.com/flowlong/flowlong-designer)


英文字母 `flw` 为 `flowlong workflow` 飞龙工作流的缩写

# 引入依赖

- Snapshot

快照 SNAPSHOT 版本需要添加仓库，且版本号为快照版本[点击查看](https://s01.oss.sonatype.org/content/repositories/snapshots/com/flowlong/flowlong-spring-boot-starter/)最新快照版本号。

> Maven 添加仓库及依赖
```
<repository>
    <id>snapshots</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
</repository>
```
```
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus</artifactId>
    <version>最新版本</version>
</dependency>
```

> Gradle 添加仓库及依赖
```
repositories {
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
}
```
```
//Gradle Version:<4.1
compile group: 'com.baomidou', name: 'mybatis-plus', version: '最新版本'
//Gradle Version:>=4.1 (The function compile has been deprecated since Gradle 4.10, and removed since Gradle 7.0. Please use implementation instead.)
implementation 'com.baomidou:mybatis-plus:最新版本'
```


# 🚩中国特色流程操作概念

| 支持功能 | 功能描述                                                                    | 完成程度 |
|------|-------------------------------------------------------------------------|------|
| 顺序会签 | 指同一个审批节点设置多个人，如A、B、C三人，三人按顺序依次收到待办，即A先审批，A提交后B才能审批，需全部同意之后，审批才可到下一审批节点。 | ✅    |
| 并行会签 | 指同一个审批节点设置多个人，如A、B、C三人，三人会同时收到待办任务，需全部同意之后，审批才可到下一审批节点。                 | ✅    |
| 或签   | 一个流程审批节点里有多个处理人，任意一个人处理后就能进入下一个节点                                       | ✅    |
| 票签   | 指同一个审批节点设置多个人，如A、B、C三人，分别定义不同的权重，当投票权重比例大于 50% 就能进入下一个节点                | ✅    |
| 抄送   | 将审批结果通知给抄送列表对应的人，同一个流程实例默认不重复抄送给同一人                                     | ✅    |
| 驳回   | 将审批重置发送给某节点，重新审批。驳回也叫退回，也可以分退回申请人、退回上一步、任意退回等                           | ✅    |
| 分配   | 允许用户自行决定任务转办、委派、主办 及其它                                                  | ✅    |
| 转办   | A转给其B审批，B审批后，进入下一节点                                                     | ✅    |
| 委派   | A转给其B审批，B审批后，转给A，A审批后进入下一节点                                             | ✅    |
| 跳转   | 可以将当前流程实例跳转到任意办理节点                                                      | ✅    |
| 拿回   | 在当前办理人尚未处理文件前，允许上一节点提交人员执行拿回                                            | ✅    |
| 撤销   | 流程发起者可以对流程进行撤销处理                                                        | ✅    |
| 加签   | 允许当前办理人根据需要自行增加当前办理节点的办理人员                                              | ✅    |
| 减签   | 在当前办理人操作之前减少办理人                                                         | ✅    |
| 认领   | 公共任务认领                                                                  | ✅    |
| 已阅   | 任务是否查看状态显示                                                              | ✅    |
| 催办   | 通知当前活动任务处理人办理任务                                                         | ✅    |
| 沟通   | 与当前活动任务处理人沟通                                                            | ✅    |
| 终止   | 在任意节点终止流程实例                                                             | ✅    |


# 贡献力量

- [运行单元测试](https://gitee.com/aizuda/flowlong/wikis/%E8%BF%90%E8%A1%8C%E5%8D%95%E5%85%83%E6%B5%8B%E8%AF%95)
- PR 请参考现在代码规范注释说明

# 使用文档

- 设计器源码 https://gitee.com/flowlong/flowlong-designer

<img src="https://foruda.gitee.com/images/1683680723972384655/f957e75d_12260.png" alt="flowlong" width="500px" height="262px">

# 其它说明

- 基于 [MybatisPlus](https://baomidou.com) 为 `ORM` 层实现
- 后端设计参考了 [snakerflow](https://gitee.com/yuqs/snakerflow) 开源工作流实体划分
- 参考了包括 flowable camunda 等主流工作流的设计思想
