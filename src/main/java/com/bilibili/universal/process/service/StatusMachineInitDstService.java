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
 * @version $Id: StatusMachineInitDstService.java, v 0.1 2022-02-22 7:30 PM Tony Zhao Exp $$
 */
public interface StatusMachineInitDstService extends StatusMachineInitCascadeService {

    long initProcess(final int templateId, final int destination, final long refUniqueNo,
                     final DataContext dataContext);

    long initProcess(final int templateId, final int destination, final long refUniqueNo,
                     final DataContext dataContext, final Consumer<ProcessContext> callback);

    long initProcess(final int templateId, final int destination, final long refUniqueNo,
                     final long parentRefUniqueNo, final DataContext dataContext);

    long initProcess(final int templateId, final int destination, final long refUniqueNo,
                     final long parentRefUniqueNo, final DataContext dataContext,
                     final Consumer<ProcessContext> callback);

}
