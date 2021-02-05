# application - component

提供组件的DI功能，这是一个遵守javax.inject的注入实现。
此组件现在处于开发中，最终将会通过JitPack提供。

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

## 进展

 - [x] 通过反射解析类的元数据
 - [x] 构造方法创建和注入
 - [x] 普通方法注入
 - [x] 工厂模式创建和注入
 - [x] 对父类的字段和方法进行注入
 
（未经过全面测试 - 尚不稳定的功能）

 - [x] 组件声明
 - [x] 后处理接口


（尚未实现）

 - [ ] 配置注入（需要配置处理模块，还没有做） 

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