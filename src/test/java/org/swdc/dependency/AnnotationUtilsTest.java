package org.swdc.dependency;

import jakarta.annotation.PostConstruct;
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
import org.swdc.dependency.utils.AnnotationDescription;
import org.swdc.dependency.utils.AnnotationUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;


public class AnnotationUtilsTest {


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Named
    @Resource
    public  @interface AnnoTest {

        @AliasFor(annotation = Named.class, value = "value")
        String name() default "";

        @AliasFor(annotation = Resource.class, value = "name")
        String resourceName() default "";

        @AliasFor(annotation = Resource.class,value = "type")
        Class type() default Object.class;

    }

    @Singleton
    @AnnoTest(type = TestEntry.class,resourceName = "resName",name = "testEnt")
    public static class TestEntry {

        @Inject
        public TestEntry() {

        }

        @PostConstruct
        public void initTest() {
            System.out.println("test init method");
        }

    }

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

    @Test
    public void testAnnotationFind() {
        // 测试获取直接标记在类上面的注解
        Map<Class, AnnotationDescription> annoMap = AnnotationUtil.getAnnotations(Depend.class);
        Assertions.assertTrue(annoMap.containsKey(Dependency.class));

        // 测试获取未标记在类的注解
        annoMap = AnnotationUtil.getAnnotations(TestEntTwo.class);
        Assertions.assertFalse(annoMap.containsKey(Scope.class));

        // 测试获取标记在注解中的注解
        annoMap = AnnotationUtil.getAnnotations(TestEntry.class);
        Assertions.assertFalse(annoMap.containsKey(Scope.class));
        Assertions.assertNotNull(AnnotationUtil.findAnnotationIn(annoMap, Scope.class));

        // 测试元注解
        annoMap = AnnotationUtil.getAnnotations(Target.class);
        Assertions.assertTrue(annoMap.isEmpty());
    }

    @Test
    public void testAnnotationRead() {
        AnnotationDescription desc = AnnotationUtil.findAnnotation(TestEntTwo.class,Named.class);
        // 获取在注解中的属性
        String name = desc.getProperty(String.class,"value");
        Assertions.assertEquals("test",name);
        // 获取不在里面的属性
        name = desc.getProperty(String.class,"name");
        Assertions.assertNull(name);

        // 测试AliasFor的使用
        desc = AnnotationUtil.findAnnotation(TestEntry.class, AnnoTest.class);
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
