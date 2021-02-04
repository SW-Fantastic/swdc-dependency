package org.swdc.dependency;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.dependency.utils.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class ReflectionUtilTest {

    public static class ClassTest {


    }

    public static class ClassTestTwo {

        @Inject
        public void setTest(ClassTest test) {

        }

    }

    public static class ClassTestFive extends  ClassTestThree {

        @Resource
        private ClassTestFour four;

    }

    public static class ClassTestFour extends ClassTestTwo {

        @Inject
        public void setTestThree(ClassTestThree three) {

        }

    }

    public static class ClassTestThree {

        @Inject
        private ClassTest test;

        @Inject
        private ClassTestTwo testTwo;

    }

    @Test
    public void testReflectDependencyMethods () throws NoSuchMethodException {

        List<Method> methods = ReflectionUtil.findDependencyMethods(ClassTestFour.class);
        Assertions.assertTrue(methods.contains(ClassTestTwo.class.getMethod("setTest", ClassTest.class)));
        Assertions.assertTrue(methods.contains(ClassTestFour.class.getMethod("setTestThree", ClassTestThree.class)));

    }

    @Test
    public void testReflectionDependencyFields() throws NoSuchFieldException {
        List<Field> fields = ReflectionUtil.findDependencyFields(ClassTestFive.class);

        Assertions.assertTrue(fields.contains(ClassTestFive.class.getDeclaredField("four")));
        Assertions.assertTrue(fields.contains(ClassTestThree.class.getDeclaredField("test")));
        Assertions.assertTrue(fields.contains(ClassTestThree.class.getDeclaredField("testTwo")));

    }


}
