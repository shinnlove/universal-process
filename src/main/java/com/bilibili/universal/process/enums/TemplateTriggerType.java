/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.enums;

/**
 * @author Tony Zhao
 * @version $Id: TemplateTriggerType.java, v 0.1 2022-01-29 4:49 PM Tony Zhao Exp $$
 */
public enum TemplateTriggerType {

    ACCEPT(1, "流程完成"),

    REJECT(2, "流程拒绝"),

    CANCEL(3, "流程取消"),

    ;

    private int    code;
    private String desc;

    TemplateTriggerType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static String getNameByCode(int code){
        for (TemplateTriggerType type : TemplateTriggerType.values()) {
            if (type.code == code){
                return type.name();
            }
        }
        return null;
    }

}