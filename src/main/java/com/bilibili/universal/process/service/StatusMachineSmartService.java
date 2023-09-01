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
 * @version $Id: StatusMachineSmartService.java, v 0.1 2022-03-02 11:59 AM Tony Zhao Exp $$
 */
public interface StatusMachineSmartService {

    ProcessContext smartProceedNext(int source, long refUniqueNo, DataContext dataContext);

    ProcessContext smartProceedNext(int source, long refUniqueNo, DataContext dataContext,
                                    Consumer<ProcessContext> callback);

    ProcessContext continuousDeduceProceed(long refUniqueNo, DataContext dataContext);

    ProcessContext continuousDeduceProceed(long refUniqueNo, DataContext dataContext,
                                           Consumer<ProcessContext> callback);

    /**
     * jump proceed process, from one source to next.
     *
     * @param refUniqueNo
     * @param source
     * @param destination
     * @param dataContext
     * @return
     */
    ProcessContext jumpProceedProcess(final Long refUniqueNo, final int source,
                                      final int destination, final DataContext dataContext);

    ProcessContext jumpProceedProcess(final Long refUniqueNo, final int source,
                                      final int destination, final DataContext dataContext,
                                      final Consumer<ProcessContext> callback);

}
