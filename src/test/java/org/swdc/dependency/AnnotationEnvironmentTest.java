package org.swdc.dependency;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AnnotationEnvironmentTest {

    public static class NoArgConstructorClass {

        public NoArgConstructorClass() {

        }

    }

    public static class ConstructorTestClass {

        private NoArgConstructorClass constructorClass;

        @Inject
        public ConstructorTestClass(NoArgConstructorClass constructorClass) {
            this.constructorClass = constructorClass;
        }


    }

    public static class FieldAndSetterClass {

        @Inject
        protected NoArgConstructorClass constructorClass;

        protected ConstructorTestClass testClass;

        @Inject
        public void setTestClass(ConstructorTestClass testClass) {
            this.testClass = testClass;
        }
    }

    public static class SuperInjectionClass extends FieldAndSetterClass {


    }


    public static class CircleTestClassA {

        public CircleTestClassA() {

        }

        @Inject
        public void setTestClassB(CircleTestClassB testClassB) {

        }

    }

    public static class CircleTestClassB {

        @Inject
        public CircleTestClassB(CircleTestClassA classA) {

        }

    }

    @Test
    public void testInjectConstructor() {
        AnnotationEnvironment environment = new AnnotationEnvironment();
        NoArgConstructorClass constructorClass = environment.getByClass(NoArgConstructorClass.class);
        Assertions.assertNotNull(constructorClass);

        ConstructorTestClass constructorTestClass = environment.getByClass(ConstructorTestClass.class);
        Assertions.assertNotNull(constructorTestClass);
        Assertions.assertNotNull(constructorTestClass.constructorClass);
        Assertions.assertEquals(constructorClass,constructorTestClass.constructorClass);
    }

    @Test
    public void testInjectFieldsAndSetter() {
        AnnotationEnvironment annotationEnvironment = new AnnotationEnvironment();
        FieldAndSetterClass fieldAndSetterClass = annotationEnvironment.getByClass(FieldAndSetterClass.class);

        Assertions.assertNotNull(fieldAndSetterClass);
        Assertions.assertEquals(annotationEnvironment.getByClass(NoArgConstructorClass.class),fieldAndSetterClass.constructorClass);
        Assertions.assertEquals(annotationEnvironment.getByClass(ConstructorTestClass.class),fieldAndSetterClass.testClass);

    }

    @Test
    public void testSuperInjections() {

        AnnotationEnvironment environment = new AnnotationEnvironment();
        SuperInjectionClass superClazz = environment.getByClass(SuperInjectionClass.class);

        Assertions.assertNotNull(superClazz);
        Assertions.assertEquals(environment.getByClass(NoArgConstructorClass.class),superClazz.constructorClass);
        Assertions.assertEquals(environment.getByClass(ConstructorTestClass.class),superClazz.testClass);

    }

    @Test
    public void testResolvableCircle() {
        AnnotationEnvironment environment = new AnnotationEnvironment();

        CircleTestClassA classA = environment.getByClass(CircleTestClassA.class);
        Assertions.assertNotNull(classA);
    }

    @Test
    public void testUnResolvedCircle() {
        try {
            AnnotationEnvironment environment = new AnnotationEnvironment();
            environment.getByClass(CircleTestClassB.class);
            Assertions.fail("can not create because circle dependency");
        } catch (Exception e) {

        }
    }

}
