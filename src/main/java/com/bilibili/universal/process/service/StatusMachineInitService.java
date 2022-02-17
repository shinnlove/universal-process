/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.service;

import com.bilibili.universal.process.callback.ProcessCallback;
import com.bilibili.universal.process.model.context.DataContext;

/**
 * @author Tony Zhao
 * @version $Id: StatusMachineInitService.java, v 0.1 2022-02-10 11:52 AM Tony Zhao Exp $$
 */
public interface StatusMachineInitService {

    long initProcess(final int templateId, final long refUniqueNo, final DataContext dataContext);

    long initProcess(final int templateId, final long refUniqueNo, final DataContext dataContext,
                     final ProcessCallback callback);

    long initProcess(final int templateId, final int destination, final long refUniqueNo,
                     final DataContext dataContext);

    long initProcess(final int templateId, final int destination, final long refUniqueNo,
                     final DataContext dataContext, final ProcessCallback callback);

}
