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
 * @version $Id: StatusMachineInitCascadeService.java, v 0.1 2022-02-22 7:31 PM Tony Zhao Exp $$
 */
public interface StatusMachineInitCascadeService extends StatusMachineInitService {

    long initProcess(final int templateId, final long refUniqueNo, final long parentRefUniqueNo,
                     final DataContext dataContext);

    long initProcess(final int templateId, final long refUniqueNo, final long parentRefUniqueNo,
                     final DataContext dataContext, final Consumer<ProcessContext> callback);

}
