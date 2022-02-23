/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service;

import java.util.function.Consumer;

import com.bilibili.universal.process.model.context.DataContext;
import com.bilibili.universal.process.model.context.ProcessContext;

/**
 * Internal interface for status machine to use.
 * 
 * When calls proceedParentProcess, need to cancel default delegate proceed, 
 * and thus we need to set proceedParent as false to prevent circle locks in transactions.
 * 
 * @author Tony Zhao
 * @version $Id: StatusMachineProceedCascadeService.java, v 0.1 2022-02-23 12:34 PM Tony Zhao Exp $$
 */
public interface StatusMachineProceedCascadeService extends StatusMachineProceedService {

    @Deprecated
    ProcessContext proceedProcess(final int actionId, final long refUniqueNo,
                                  final DataContext dataContext, boolean proceedParent,
                                  boolean proceedChildren);

    @Deprecated
    ProcessContext proceedProcess(final int actionId, final long refUniqueNo,
                                  final DataContext dataContext,
                                  final Consumer<ProcessContext> callback, boolean proceedParent,
                                  boolean proceedChildren);

}
