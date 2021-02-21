package org.swdc.dependency.testpkg;

import org.swdc.dependency.annotations.ImplementBy;

@ImplementBy({ TestC1.class, TestC2.class })
public abstract class TestAC1 {
}
