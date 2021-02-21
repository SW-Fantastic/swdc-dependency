package org.swdc.dependency.testpkg;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.swdc.dependency.annotations.MultipleImplement;

@Singleton
@MultipleImplement(TestAC1.class)
@Named("testA")
public class TestC1 extends TestAC1 {


}
