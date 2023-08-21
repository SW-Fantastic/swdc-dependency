package org.swdc.dependency;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.dependency.annotations.AliasFor;
import org.swdc.dependency.annotations.Dependency;
import org.swdc.dependency.annotations.Factory;
import org.swdc.ours.common.annotations.AnnotationDescription;
import org.swdc.ours.common.annotations.AnnotationDescriptions;
import org.swdc.ours.common.annotations.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Annotation的工具类的单元测试。
 */
public class AnnotationUtilsTest {


    /**
     * 测试AliasFor注解，
     * 这个注解的作用同SpringBoot的AliasFor
     * 他的目的是合并多个注解，十分实用。
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Named
    @Resource
    public  @interface AnnoTest {

        /**
         * 注解Name，作为named注解的value属性的别名
         */
        @AliasFor(annotation = Named.class, value = "value")
        String name() default "";

        /**
         * 注解resourcesName，作为Resources的name属性的别名
         */
        @AliasFor(annotation = Resource.class, value = "name")
        String resourceName() default "";

        /**
         * 注解type，作为Resources的type属性的别名
         */
        @AliasFor(annotation = Resource.class,value = "type")
        Class type() default Object.class;

    }

    /**
     * 测试注解合并的类
     */
    @Singleton
    @AnnoTest(type = TestEntry.class,resourceName = "resName",name = "testEnt")
    public static class TestEntry {

        @Inject
        public TestEntry() {

        }

    }

    /**
     * 测试注解的解析的类
     */
    @Named("test")
    public static class TestEntTwo{

        @Inject
        public TestEntTwo(TestEntry ent) {

        }

    }

    public static class TestEntThree {

        public void init() {
            System.out.println("factory init method");
        }

    }

    @Dependency
    public static class Depend {

        @Factory(initMethod = "init")
        public TestEntThree three(TestEntTwo testEntTwo) {
            return new TestEntThree();
        }

    }

    /**
     * 测试注解的查找
     */
    @Test
    public void testAnnotationFind() {
        // 测试获取直接标记在类上面的注解
        AnnotationDescriptions annoMap = Annotations.getAnnotations(Depend.class);
        Assertions.assertTrue(annoMap.containsKey(Dependency.class));

        // 测试获取未标记在类的注解
        annoMap = Annotations.getAnnotations(TestEntTwo.class);
        Assertions.assertFalse(annoMap.containsKey(Scope.class));

        // 测试获取标记在注解中的注解
        annoMap = Annotations.getAnnotations(TestEntry.class);
        Assertions.assertFalse(annoMap.containsKey(Scope.class));
        Assertions.assertNotNull(Annotations.findAnnotationIn(annoMap, Scope.class));

        // 测试元注解
        annoMap = Annotations.getAnnotations(Target.class);
        Assertions.assertTrue(annoMap.values().size() == 0);
    }

    /**
     * 测试注解的读取
     */
    @Test
    public void testAnnotationRead() {
        AnnotationDescription desc = Annotations.findAnnotation(TestEntTwo.class,Named.class);
        // 获取在注解中的属性
        String name = desc.getProperty(String.class,"value");
        Assertions.assertEquals("test",name);
        // 获取不在里面的属性
        name = desc.getProperty(String.class,"name");
        Assertions.assertNull(name);

        // 测试AliasFor的使用
        desc = Annotations.findAnnotation(TestEntry.class, AnnoTest.class);
        Assertions.assertNotNull(desc);

        AnnotationDescription resource = desc.find(Resource.class);
        Assertions.assertNotNull(resource);

        Class type = resource.getProperty(Class.class,"type");
        Assertions.assertEquals(TestEntry.class,type);

        String resourceName = resource.getProperty(String.class,"name");
        Assertions.assertEquals("resName",resourceName);

        AnnotationDescription named = desc.find(Named.class);
        name = named.getProperty(String.class, "value");
        Assertions.assertEquals("testEnt",name);
    }

}
