/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.callback;

import com.bilibili.universal.process.model.context.ProcessContext;

/**
 * A process callback used by business caller for receive response data and do business in status machine's transaction.
 *
 * Special warning: all codes executed in doCallback method is included in the whole transaction!
 *
 * @author Tony Zhao
 * @version $Id: ProcessCallBack.java, v 0.1 2021-07-28 2:45 PM Tony Zhao Exp $$
 */
@FunctionalInterface
public interface ProcessCallback {

    /**
     * status machine use this method to pass back data context resp to business caller.
     *
     * @param resp
     */
    void doCallback(final ProcessContext resp);

}
