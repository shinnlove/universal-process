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
 * @version $Id: StatusMachineInitService.java, v 0.1 2022-02-10 11:52 AM Tony Zhao Exp $$
 */
public interface StatusMachineInitService {

    /**
     * @param templateId 
     * @param refUniqueNo
     * @param dataContext
     * @return
     */
    long initProcess(final int templateId, final long refUniqueNo, final DataContext dataContext);

    /**
     * @param templateId 
     * @param refUniqueNo
     * @param dataContext
     * @param callback
     * @return
     */
    long initProcess(final int templateId, final long refUniqueNo, final DataContext dataContext,
                     final Consumer<ProcessContext> callback);

    /**
     * @param templateId 
     * @param destination
     * @param refUniqueNo
     * @param dataContext
     * @return
     */
    long initProcess(final int templateId, final int destination, final long refUniqueNo,
                     final DataContext dataContext);

    /**
     * @param templateId 
     * @param destination
     * @param refUniqueNo
     * @param dataContext
     * @param callback
     * @return
     */
    long initProcess(final int templateId, final int destination, final long refUniqueNo,
                     final DataContext dataContext, final Consumer<ProcessContext> callback);

}
