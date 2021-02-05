module swdc.application.environment {

    requires jakarta.inject;
    requires jakarta.annotation;

    exports org.swdc.dependency;
    exports org.swdc.dependency.utils;
    exports org.swdc.dependency.annotations;
    exports org.swdc.dependency.listeners;

}