package org.swdc.dependency;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.dependency.layer.Layer;
import org.swdc.dependency.layer.LayerDependencyContext;
import org.swdc.dependency.layer.Layerable;
import org.swdc.dependency.testpkg.TestAC1;
import org.swdc.dependency.testpkg.TestC1;
import org.swdc.dependency.testpkg.TestC2;

public class LayerTest {

    public static class NoArgConstructorClass {

        @Inject
        private TestC1 testC1;

        public NoArgConstructorClass() {

        }

        public TestC1 getTestC1() {
            return testC1;
        }
    }

    public static class ConstructorTestClass {

        private AnnotationEnvironmentTest.NoArgConstructorClass constructorClass;

        @Inject
        public ConstructorTestClass(AnnotationEnvironmentTest.NoArgConstructorClass constructorClass) {
            this.constructorClass = constructorClass;
        }


    }

    @Test
    public void testLayer () {

        // 应用层
        DependencyContext appContext = new AnnotationLoader()
                .load();

        // 封装的层
        DependencyContext context = new AnnotationLoader()
                .withComponent(TestAC1.class)
                .layerExport(TestC1.class)
                .load();

        // Layer分层
        DependencyContext layerContext = new Layer(appContext)
                .use(context)
                .asContext();

        Assertions.assertNotNull(layerContext.getByClass(NoArgConstructorClass.class));

        Assertions.assertNull(layerContext.getByClass(TestC2.class));

    }

}
