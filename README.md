# application - component

提供组件的DI功能，这是一个遵守javax.inject的注入实现。
~~此组件现在处于开发中，最终将会通过JitPack提供。~~~~我改用Gradle了，
现在里面既有Gradle也有maven，不知道在JitPack会不会有问题，所以
还请自行安装到本地maven。~~

gradle总是有奇奇怪怪的问题，还是maven好用（真香~！）。

## 概述

为啥我要做这个：

首先就是前作FXApplication很混乱，JitPack无法使用一个Repository发布
多个依赖文件，而FXApplication把依赖注入，AOP，模块功能之类的乱七八糟的东西
都集中在一个Project里面，这十分不合理。

因此我打算把容器和注入的部分，配置文件的处理，网络的处理等都单独的分开，
并且这些功能都将会有一个完整的文档，我准备做一个正式的版本了。

而这个注入组件就会是新版本的第一个部分。

这个容器的特点呢，首先他不像spring那样，直接会在启动的时候初始化很多组件，
而是在使用的时候，需要的时候才会进行初始化，当然启动的时候直接初始化我想以后
会有，大多数的注入就是完全根据注解来完成，所以只需要关注注解就行啦。

以后我想为了致敬spring，可能会加入xml之类的其他类型的支持。

~~此依赖注入环境支持Graal，通过GraalNativePlugin可以生成反射元数据，
但是AOP暂时还不行，这个还需要等待Plugin的完善。~~

不知道为什么javafx不理生成的配置，反射全部失效，如果以后有新的发现可能会重新尝试
graal的native，暂时还是标准的java吧。

## 如何使用
请直接clone本项目，然后通过maven配置，并且执行install， 安装到本地。

然后使用此Maven：
```xml
<dependency>
	 <groupId>com.github.SW-Fantastic</groupId>
	 <artifactId>swdc-dependency</artifactId>
	 <version>0.1.3</version>
</dependency>
```

## 进展

 - [x] 通过反射解析类的元数据
 - [x] 构造方法创建和注入
 - [x] 普通方法注入
 - [x] 工厂模式创建和注入
 - [x] 对父类的字段和方法进行注入
 - [x] 组件声明
 - [x] 自定义Scope（增加了Prototype的Scope）
 - [x] 后处理接口
 - [x] AOP支持（Before，After，Around等）
 - [x] Package扫描功能（基于io.github.classgraph,不建议使用）

## Example

假定有ComponentClass类：
```
@Singleton
public class ComponentClass {
    
    @Inject
    public ComponentClass() {
    }
    
}
```

那么只需要这样，容器就会根据类的注解进行注入，生成一个单例对象。

```
AnnotationEnvironment environment = new AnnotationEnvironment();
ComponentClass object = environment.getByClass(ComponentClass.class);
object.doSomething();
```
