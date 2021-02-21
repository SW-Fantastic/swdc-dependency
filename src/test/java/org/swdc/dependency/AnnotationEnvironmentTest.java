package org.swdc.dependency;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ModuleVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.dependency.annotations.*;
import org.swdc.dependency.interceptor.AspectAt;
import org.swdc.dependency.interceptor.InvocationPoint;
import org.swdc.dependency.interceptor.ProcessPoint;
import org.swdc.dependency.testpkg.TestAC1;

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

    @Prototype
    public static class PrototypeClass {

        public PrototypeClass() {

        }

    }

    public static class TestProvider implements Provider<ConstructorTestClass> {

        @Inject
        private NoArgConstructorClass constructorClass;

        @Override
        public ConstructorTestClass get() {
            return new ConstructorTestClass(constructorClass);
        }

    }

    @Prototype
    public static class CleanerTest {

        @PreDestroy
        public void cleaner() {
            System.out.println("Cleaned");
        }

    }

    @Dependency
    public static class DeclareClass {

        @Factory
        public NoArgConstructorClass noArgConstructor() {
            return new NoArgConstructorClass();
        }

        @Factory
        public ConstructorTestClass testClass(NoArgConstructorClass noArgConstructorClass) {
            return new ConstructorTestClass(noArgConstructorClass);
        }

    }

    @Interceptor
    public static class Advice {

        @Aspect(byNameRegex = "test*",at = AspectAt.BEFORE)
        public void hello(InvocationPoint point) {
            System.out.println("正在执行方法：" + point.getMethod().getName());
        }

        @Aspect(byNameRegex = "test*",at = AspectAt.AFTER)
        public void helloExit(InvocationPoint point) {
            System.out.println("方法已经结束：" + point.getMethod().getName());
        }

        @Aspect(byNameRegex = "test*",at = AspectAt.AROUND)
        public Object helloAround(ProcessPoint processPoint) throws Throwable {
            System.out.println("方法（Around）正在开始：" + processPoint.getMethod().getName());
            Object obj = processPoint.process();
            System.out.println("方法（Around）已经结束：" + processPoint.getMethod().getName());
            return obj;
        }

        @Aspect(byNameRegex = "test*",at = AspectAt.AROUND)
        public Object helloAroundMultiple(ProcessPoint processPoint) throws Throwable {
            System.out.println("方法（AroundMultiple）正在开始：" + processPoint.getMethod().getName());
            Object obj = processPoint.process();
            System.out.println("方法（AroundMultiple）已经结束：" + processPoint.getMethod().getName());
            return obj;
        }

    }

    @With(aspectBy = Advice.class)
    public static class TestAdvice {

        public void testMethod() {
            System.out.println("test");
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

    @Test
    public void testCustomScope() {
        AnnotationEnvironment environment = new AnnotationEnvironment();
        PrototypeClass prototypeClass = environment.getByClass(PrototypeClass.class);
        Assertions.assertNotEquals(prototypeClass,environment.getByClass(PrototypeClass.class));
    }

    @Test
    public void testDeclared() {
        AnnotationLoader loader = new AnnotationLoader();
        DependencyContext context = loader.withProvider(TestProvider.class).load();
        ConstructorTestClass testClass = context.getByClass(ConstructorTestClass.class);
        Assertions.assertNotNull(testClass);
        Assertions.assertEquals(testClass.constructorClass,context.getByClass(NoArgConstructorClass.class));
    }

    @Test
    public void testProvider() {
        AnnotationLoader loader = new AnnotationLoader();
        DependencyContext context = loader.withProvider(TestProvider.class).load();

        ConstructorTestClass constructorTestClass = context.getByClass(ConstructorTestClass.class);
        Assertions.assertEquals(context.getByClass(NoArgConstructorClass.class),constructorTestClass.constructorClass);
        Assertions.assertNull(context.getByClass(TestProvider.class));
    }

    @Test
    public void testAdvice() {
        AnnotationLoader loader = new AnnotationLoader();
        DependencyContext context = loader.load();
        TestAdvice advice = context.getByClass(TestAdvice.class);
        advice.testMethod();
    }

    @Test
    public void testMultiple() {
        AnnotationLoader loader = new AnnotationLoader().withComponent(TestAC1.class);
        DependencyContext context = loader.load();

        Assertions.assertNotNull(context.getByName("testA"));
        Assertions.assertNotNull(context.getByName("testB"));
        Assertions.assertNotNull(context.getByAbstract(TestAC1.class));
    }

    @Test
    public void testPackage() {
        AnnotationLoader loader = new AnnotationLoader().withPackage("org.swdc.dependency.testpkg");
        DependencyContext context = loader.load();
        Assertions.assertNotNull(context.getByName("testA"));
        Assertions.assertNotNull(context.getByName("testB"));
        Assertions.assertNotNull(context.getByAbstract(TestAC1.class));
    }

}
