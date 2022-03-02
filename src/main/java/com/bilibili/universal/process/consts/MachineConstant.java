/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.consts;

/**
 * @author Tony Zhao
 * @version $Id: MachineConstant.java, v 0.1 2022-01-10 11:52 PM Tony Zhao Exp $$
 */
public final class MachineConstant {

    private MachineConstant() {

    }

    public static final int    DEFAULT_TEMPLATE_ID         = -1;

    public static final int    DEFAULT_ACTION_ID           = -1;

    public static final int    DEFAULT_STATUS              = -1;

    public static final String INVALID_PARAMETERS          = "invalid parameters.";

    public static final int    DEFAULT_OPERATOR_TYPE       = -1;

    public static final int    DEFAULT_OPERATOR_ID         = -1;

    public static final String DEFAULT_OPERATOR            = "CommercialOrder System";

    public static final String DEFAULT_REMARK              = "System delegate to initialize or proceed the process.";

    public static final String NO_PROCESS_IN_SYSTEM        = "不存在对应的业务流程";

    public static final String SOURCE_STATUS_INCORRECT     = "template_id/action_id起始变迁状态不符";

    public static final String STATUS_HAS_BLOCKING_PROCESS = "当前流程有Blocking流程没有达到终态无法推进";

}