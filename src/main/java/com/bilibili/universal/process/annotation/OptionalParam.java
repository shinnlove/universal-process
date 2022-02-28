/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The parameter is optional, will not validate.
 * 
 * @author Tony Zhao
 * @version $Id: OptionalParam.java, v 0.1 2022-02-22 11:44 AM Tony Zhao Exp $$
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalParam {
}