# QAs

## 自动完成的方法没有右括号

在设置中，勾选 "Insert paired brackets" 即可：

![](2019-06-25-21-02-16.png)

## Maven 安装的本地包 not found

删除项目的 .iml 文件后，重新导入项目。

## Maven target 目录不可见

取消勾选 "Exclude build directory"

![maven](images/2020-05-12-14-43-41.png)

不过勾选后，由于不需要对 "target" 目录构建索引，Intellij IDEA 在导入项目时会快一些。

## template 不可用

在创建 class 时出现如下弹窗错误：

![template](images/2020-09-14-17-23-56.png)

重启 Intellij IDEA 后解决。

## 设置运行程序内存

运行 java 类，参数：

```java
java [options] class [arguments]
```

运行 jar 包：

```java
java [options] -jar xxx.jar [arguments]
```

其中：

- [options] 表示 JRE 的可选配置信息
- [arguments] 表示程序自身的参数，会传到 main 函数的参数组里面
