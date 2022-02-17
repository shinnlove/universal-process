/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.consts;

/**
 * Xml template file parse constants.
 * 
 * @author Tony Zhao
 * @version $Id: XmlParseConstant.java, v 0.1 2022-01-27 5:13 PM Tony Zhao Exp $$
 */
public final class XmlParseConstant {

    private XmlParseConstant() {
        // no constructor
    }

    public static final String TEMPLATE_PATH             = "classpath:META-INF/process/*.xml";

    public static final String SECTION_ROOT_METADATA     = "metadata";

    public static final String SECTION_INNER_STATUS      = "status";

    public static final String SECTION_ROOT_INIT         = "init";

    public static final String SECTION_INNER_PRE         = "pre";

    public static final String SECTION_INNER_DISPATCH    = "dispatch";

    public static final String SECTION_THIRD_DESTINATION = "destination";

    public static final String SECTION_INNER_POST        = "post";

    public static final String SECTION_ROOT_ACCEPT       = "accept";

    public static final String SECTION_ROOT_REJECT       = "reject";

    public static final String SECTION_ROOT_CANCEL       = "cancel";

    public static final String SECTION_ROOT_ACTION       = "action";

    public static final String SECTION_INNER_HANDLER     = "handler";

    public static final String ATTR_ID                   = "id";

    public static final String ATTR_NO                   = "no";

    public static final String ATTR_NAME                 = "name";

    public static final String ATTR_DESC                 = "desc";

    public static final String ATTR_PARENT               = "parent";

    public static final String ATTR_RECONCILE            = "reconcile";

    public static final String ATTR_COORDINATE           = "coordinate";

    public static final String ATTR_PARENT_STATUS        = "ps";

    public static final String ATTR_ACCOMPLISH           = "ac";

    public static final String ATTR_DEFAULT              = "default";

    public static final String ATTR_ENTRANCE             = "entrance";

    public static final String ATTR_SOURCE               = "source";

    public static final String ATTR_DESTINATION          = "destination";

    public static final String ATTR_REFERENCE            = "ref";

    public static final String ATTR_SEQUENCE             = "seq";

    public static final String ATTR_TRANS                = "trans";

    public static final String ATTR_PREPARE              = "prepare";

}