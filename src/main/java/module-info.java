
module swdc.application.dependency {

    requires jakarta.inject;
    requires jakarta.annotation;
    requires net.bytebuddy;
    requires io.github.classgraph;
    requires slf4j.api;
    requires jul.to.slf4j;

    exports org.swdc.dependency;
    exports org.swdc.dependency.utils;
    exports org.swdc.dependency.annotations;
    exports org.swdc.dependency.listeners;
    exports org.swdc.dependency.interceptor;
    exports org.swdc.dependency.application;

}