/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.annotation;

import java.lang.annotation.*;

/**
 * Mark with handlers indicate which field should be assign to the input parameter.
 *
 * @author Tony Zhao
 * @version $Id: ValueField.java, v 0.1 2022-05-10 3:31 PM Tony Zhao Exp $$
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValueField {

    /**
     * Indicate value that assign field.
     *
     * @return
     */
    String value() default "";

}