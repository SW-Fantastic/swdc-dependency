package org.swdc.dependency;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Scope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.dependency.annotations.Dependency;
import org.swdc.dependency.annotations.Factory;
import org.swdc.dependency.annotations.Prototype;
import org.swdc.dependency.annotations.ScopeImplement;
import org.swdc.dependency.parser.AnnotationDependencyParser;
import org.swdc.dependency.registry.ComponentInfo;
import org.swdc.dependency.registry.ConstructorInfo;
import org.swdc.dependency.registry.DefaultDependencyRegistryContext;
import org.swdc.dependency.registry.DependencyInfo;
import org.swdc.dependency.scopes.SingletonDependencyScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认组件注册和解析的测试类
 */
public class DefaultRegisterTest {

    /**
     * 用于测试自定义的Scope - 这个是自定义的scope的实现类
     */
    public static class ScopeImpl extends SingletonDependencyScope {

    }

    /**
     * 用于测试的自定义的Scope
     */
    @Scope
    @ScopeImplement(ScopeImpl.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Single {

    }

    /**
     * 用于测试自定义Scope的组件类
     */
    @Single
    public static class TestScope {

    }

    /**
     * 用于测试的组件类
     */
    public static class TestOne {

        /**
         * 构造方法，正常情况下，无参数构造不会被解析
         * 未标记注解的构造函数的也不会被解析。
         * 因此正常情况下，是没有这个构造函数
         * 的ConstructorInfo被解析出来的。
         */
        public TestOne() {

        }

    }

    /**
     * 一个用于测试的组件类
     */
    public static class TestClass {

        /**
         * 一个有参数构造方法，标有inject，
         * 所以会被解析为ConstructorInfo
         * @param two
         */
        @Inject
        public TestClass(TestOne two) {

        }

    }

    /**
     * 一个组件类，用来测试setter注入和字段注入
     */
    public static class TestTwo {

        private TestClass testClass;

        /**
         * 标有依赖注解的字段，应该
         * 会被解析为DependencyInfo
         */
        @Inject
        private TestOne testOne;

        public TestTwo() {

        }

        /**
         * 标有依赖注解的setter，
         * 应当被解析为DependencyInfo
         * @param test
         */
        @Inject
        public void setTest(TestClass test) {
            this.testClass = test;
        }

    }

    /**
     * 组件类，用来测试继承形式下的注入情况
     * 正常情况下，继承的注入字段和方法都应该可以被解析。
     */
    public static class TestThree extends TestTwo {

        private TestTwo testTwo;

        @Resource
        public void setTestTwo(TestTwo testTwo) {
            this.testTwo = testTwo;
        }

    }

    @Dependency
    public static class DependencyDeclare {

        @Factory
        public TestTwo testTwo() {
            return new TestTwo();
        }

        @Factory(scope = Prototype.class)
        public TestOne testOne() {
            return new TestOne();
        }

        @Factory
        public TestClass testClass(TestOne testOne) {
            return new TestClass(testOne);
        }

    }

    public static class TestProvider implements Provider<TestOne> {

        @Override
        public TestOne get() {
            return new TestOne();
        }
    }

    @Test
    public void testSimpleParse()  {
        DefaultDependencyRegistryContext context = new DefaultDependencyRegistryContext();
        AnnotationDependencyParser parser = new AnnotationDependencyParser();
        parser.parse(TestOne.class,context);
        ComponentInfo info = context.findByClass(TestOne.class);
        Assertions.assertNotNull(info);
        Assertions.assertEquals(TestOne.class, info.getClazz());
        Assertions.assertEquals(TestOne.class.getName(), info.getName());
        Assertions.assertNull(info.getConstructorInfo());
    }

    @Test
    public void testConstructorParse() throws NoSuchMethodException {
        DefaultDependencyRegistryContext context = new DefaultDependencyRegistryContext();
        AnnotationDependencyParser parser = new AnnotationDependencyParser();
        parser.parse(TestClass.class,context);

        ComponentInfo info = context.findByClass(TestClass.class);
        Assertions.assertNotNull(info);

        ComponentInfo dep = context.findByClass(TestOne.class);
        Assertions.assertNotNull(dep);

        ConstructorInfo constructorInfo = info.getConstructorInfo();
        Assertions.assertNotNull(constructorInfo);
        Assertions.assertNull(dep.getConstructorInfo());

        Assertions.assertEquals(TestClass.class.getConstructor(TestOne.class),constructorInfo.getConstructor());
        Assertions.assertEquals(info.getConstructorInfo().getDependencies()[0],dep);
    }

    @Test
    public void testDependencyParse() throws NoSuchFieldException, NoSuchMethodException {
        DefaultDependencyRegistryContext context = new DefaultDependencyRegistryContext();
        AnnotationDependencyParser parser = new AnnotationDependencyParser();
        parser.parse(TestThree.class,context);
        ComponentInfo info = context.findByClass(TestThree.class);
        Map<Method,DependencyInfo> methods = new HashMap<>();
        Map<Field,DependencyInfo> fields = new HashMap<>();
        for (DependencyInfo depInfo: info.getDependencyInfos()) {
            if (depInfo.getSetter() != null) {
                methods.put(depInfo.getSetter(),depInfo);
            } else if (depInfo.getField() != null){
                fields.put(depInfo.getField(),depInfo);
            }
        }

        Assertions.assertTrue(fields.containsKey(TestTwo.class.getDeclaredField("testOne")));
        Assertions.assertTrue(methods.containsKey(TestThree.class.getMethod("setTestTwo", TestTwo.class)));
        Assertions.assertTrue(methods.containsKey(TestTwo.class.getMethod("setTest", TestClass.class)));
    }

    @Test
    public void testCustomScope() {

        DefaultDependencyRegistryContext context = new DefaultDependencyRegistryContext();
        AnnotationDependencyParser parser = new AnnotationDependencyParser();
        parser.parse(TestScope.class,context);
        ComponentInfo info = context.findByClass(TestScope.class);

        Assertions.assertNotNull(info);
        Assertions.assertEquals(Single.class,info.getScope());

    }

    @Test
    public void testDeclareDependencyTest() {

        DefaultDependencyRegistryContext context = new DefaultDependencyRegistryContext();
        AnnotationDependencyParser parser = new AnnotationDependencyParser();
        parser.parse(DependencyDeclare.class,context);

        ComponentInfo testOne = context.findByClass(TestOne.class);
        Assertions.assertNotNull(testOne);

        ComponentInfo testTwo = context.findByClass(TestTwo.class);
        Assertions.assertNotNull(testTwo);

        ComponentInfo testClass = context.findByClass(TestClass.class);
        Assertions.assertNotNull(testClass);

        Assertions.assertEquals(Prototype.class,testOne.getScope());

    }

    @Test
    public void testProvider() {

        DefaultDependencyRegistryContext context = new DefaultDependencyRegistryContext();
        AnnotationDependencyParser parser = new AnnotationDependencyParser();
        parser.parse(TestProvider.class,context);

        Assertions.assertNotNull(context.findByClass(TestOne.class));

    }

}
