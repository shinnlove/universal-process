/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.ex;

import com.bilibili.universal.util.code.SystemResultCode;
import com.bilibili.universal.util.exception.SystemException;

/**
 * @author Tony Zhao
 * @version $Id: BizHandlerExecuteException.java, v 0.1 2022-03-03 3:31 PM Tony Zhao Exp $$
 */
public class BizHandlerExecuteException extends SystemException {

    public BizHandlerExecuteException(SystemResultCode resultCode, Throwable cause,
                                      String message) {
        super(resultCode, cause, message);
    }

    public BizHandlerExecuteException(SystemResultCode resultCode, Throwable cause) {
        super(resultCode, cause);
    }

    public BizHandlerExecuteException(String message) {
        super(message);
    }

}