/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.future;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bilibili.universal.util.code.SystemCode;
import com.bilibili.universal.util.exception.SystemException;
import com.bilibili.universal.util.log.LoggerUtil;

/**
 * @author Tony Zhao
 * @version $Id: FutureUtil.java, v 0.1 2021-08-04 6:20 PM Tony Zhao Exp $$
 */
public class FutureUtil {

    private final static Logger logger = LoggerFactory.getLogger(FutureUtil.class);

    public static <T> T getResult(Future<T> future) {
        T result = null;
        try {
            result = future.get();
        } catch (Exception e) {
            LoggerUtil.error(logger, e, e.getMessage());
            throw new SystemException(SystemCode.SYSTEM_ERROR, e, e.getMessage());
        }

        return result;
    }

}