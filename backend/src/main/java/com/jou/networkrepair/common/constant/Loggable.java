package com.jou.networkrepair.common.constant;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {
    String module();
    String operationType();
    String operationDesc();
}
