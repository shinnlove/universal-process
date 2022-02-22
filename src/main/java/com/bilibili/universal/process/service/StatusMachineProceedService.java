/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service;

import java.util.function.Consumer;

import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;

/**
 * @author Tony Zhao
 * @version $Id: StatusMachineProceedService.java, v 0.1 2022-02-17 4:49 PM Tony Zhao Exp $$
 */
public interface StatusMachineProceedService {

    /**
     * @param actionId 
     * @param refUniqueNo
     * @param dataContext
     * @return
     */
    ProcessContext proceedProcess(final int actionId, final long refUniqueNo,
                                  final DataContext dataContext);

    /**
     * @param actionId 
     * @param refUniqueNo
     * @param dataContext
     * @param callback
     * @return
     */
    ProcessContext proceedProcess(final int actionId, final long refUniqueNo,
                                  final DataContext dataContext,
                                  final Consumer<ProcessContext> callback);

}
