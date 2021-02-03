package org.swdc.dependency;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.dependency.annotations.Dependency;
import org.swdc.dependency.annotations.Factory;
import org.swdc.dependency.utils.AnnotationDescription;
import org.swdc.dependency.utils.AnnotationUtil;

import java.util.List;

public class AnnotationTest {

    @Singleton
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
    public void testReg() {
        AnnotationEnvironment environment = new AnnotationEnvironment();
        environment.withDependency(Depend.class);
        TestEntThree entThree = environment.getByClass(TestEntThree.class);
        /*environment.afterRegister(info -> {
            System.out.println(info.getClazz());
            return info;
        }).afterCreation(o -> {
           System.out.println(o);
           return o;
        });*/
        Assertions.assertEquals(entThree,environment.getByClass(TestEntThree.class));
        TestEntTwo testEntTwo = environment.getByClass(TestEntTwo.class);
        Assertions.assertNotNull(testEntTwo);
        Assertions.assertEquals(testEntTwo,environment.getByClass(TestEntTwo.class));

    }

}
