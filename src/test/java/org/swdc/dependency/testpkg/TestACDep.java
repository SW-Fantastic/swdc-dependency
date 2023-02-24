package org.swdc.dependency.testpkg;

import jakarta.annotation.Resource;
import jakarta.inject.Named;

public class TestACDep {

    @Resource(name = "testA")
    private TestAC1 ac1;

    @Named("testB")
    private TestAC1 ac2;

    public TestAC1 getAc1() {
        return ac1;
    }

    public TestAC1 getAc2() {
        return ac2;
    }
}
