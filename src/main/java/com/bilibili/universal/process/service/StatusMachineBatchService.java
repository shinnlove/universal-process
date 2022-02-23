/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service;

import java.util.function.Consumer;

import com.bilibili.universal.process.model.batch.BatchInitParam;
import com.bilibili.universal.process.model.batch.BatchInitResult;
import com.bilibili.universal.process.model.context.ProcessContext;

/**
 * @author Tony Zhao
 * @version $Id: StatusMachineBatchService.java, v 0.1 2022-02-22 11:23 AM Tony Zhao Exp $$
 */
public interface StatusMachineBatchService {

    /**
     * @param param 
     * @return
     */
    BatchInitResult batchInitProcess(final BatchInitParam param);

    /**
     * @param param 
     * @param callback
     * @return
     */
    BatchInitResult batchInitProcess(final BatchInitParam param,
                                     final Consumer<ProcessContext> callback);

}
