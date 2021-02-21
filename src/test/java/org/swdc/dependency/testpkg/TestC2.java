package org.swdc.dependency.testpkg;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.swdc.dependency.annotations.MultipleImplement;

@Singleton
@MultipleImplement(TestAC1.class)
@Named("testB")
public class TestC2 extends TestAC1 {
}
