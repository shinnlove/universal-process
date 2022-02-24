/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.model.status;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.bilibili.universal.process.model.cache.StatusCache;

/**
 * The critical data structure for parent and child template to seek both reference status.
 * 
 * @author Tony Zhao
 * @version $Id: StatusRefMapping.java, v 0.1 2022-02-24 12:35 PM Tony Zhao Exp $$
 */
public class StatusRefMapping implements Serializable {

    private static final long               serialVersionUID = -7195928561101143212L;

    /** parent_child template id union key */
    private String                          idUnionKey;

    /** parent status -> child status list, list sort by sequence */
    private Map<Integer, List<StatusCache>> parent2Child;

    /** child status -> parent status */
    private Map<Integer, Integer>           child2parent;

    /**
     * Constructor for reflect.
     */
    public StatusRefMapping() {
    }

    public StatusRefMapping(String idUnionKey) {
        this.idUnionKey = idUnionKey;
    }

    public StatusRefMapping(String idUnionKey, Map<Integer, List<StatusCache>> parent2Child,
                            Map<Integer, Integer> child2parent) {
        this.idUnionKey = idUnionKey;
        this.parent2Child = parent2Child;
        this.child2parent = child2parent;
    }

    public String getIdUnionKey() {
        return idUnionKey;
    }

    public void setIdUnionKey(String idUnionKey) {
        this.idUnionKey = idUnionKey;
    }

    public Map<Integer, List<StatusCache>> getParent2Child() {
        return parent2Child;
    }

    public void setParent2Child(Map<Integer, List<StatusCache>> parent2Child) {
        this.parent2Child = parent2Child;
    }

    public Map<Integer, Integer> getChild2parent() {
        return child2parent;
    }

    public void setChild2parent(Map<Integer, Integer> child2parent) {
        this.child2parent = child2parent;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}