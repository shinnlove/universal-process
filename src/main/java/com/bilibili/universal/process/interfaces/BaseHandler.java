/**
 * Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.bilibili.universal.process.interfaces;

import static com.bilibili.universal.process.consts.MachineConstant.EMPTY;
import static com.bilibili.universal.process.consts.MetadataConstant.*;

import java.lang.reflect.Field;
import java.util.Objects;

import org.springframework.util.ReflectionUtils;

import com.bilibili.universal.process.annotation.ValueField;
import com.bilibili.universal.process.wrap.ReflectWrapWithResult;

/**
 * A base handler holds common method for action handler to use.
 * 
 * @author Tony Zhao
 * @version $Id: BaseHandler.java, v 0.1 2022-02-12 10:23 PM Tony Zhao Exp $$
 */
public interface BaseHandler {

    /**
     * Cast result object to specific generic type.
     *
     * @param c
     * @param o
     * @param <V>
     * @return
     */
    default <V> V cast(Class<V> c, Object o) {
        if (c != null && c.isInstance(o)) {
            return c.cast(o);
        }
        return null;
    }

    default Object fValue(final Object o, final Field f) {
        if (o == null || f == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(f);
        return reflect(() -> f.get(o));
    }

    default boolean isBasicType(Class<?> clazz) {
        return isBasicType(clazz.getName());
    }

    default Object deconstruction(Object data, Class<?> handlerType, ValueField fieldMark) {
        if (Objects.isNull(data)) {
            return null;
        }

        if (Objects.isNull(fieldMark) || EMPTY.equals(fieldMark.value())) {
            return deconstruction0(data, handlerType);
        }

        String fName = fieldMark.value();
        Class<?> dataClass = data.getClass();
        // search for field
        for (Field f : dataClass.getDeclaredFields()) {
            if (fName.equals(f.getName())) {
                // matched the field's name
                return fValue(data, f);
            }
        }

        return data;
    }

    default Object deconstruction0(Object data, Class<?> handlerType) {
        Class<?> dataClass = data.getClass();
        if (!dataClass.isAssignableFrom(handlerType)) {
            // search for inner type
            for (Field f : dataClass.getDeclaredFields()) {
                Class<?> cls = f.getType();
                if (!isBasicType(cls) && cls.isAssignableFrom(handlerType)) {
                    return fValue(data, f);
                }
            }
        }

        return data;
    }

    default boolean isBasicType(String fieldTypeName) {
        if (fieldTypeName != null) {
            if (LONG_CLASS.equals(fieldTypeName) || INTEGER_CLASS.equals(fieldTypeName)
                || BOOLEAN_CLASS.equals(fieldTypeName) || FLOAT_CLASS.equals(fieldTypeName)
                || DOUBLE_CLASS.equals(fieldTypeName) || SMALL_INT_CLASS.equals(fieldTypeName)
                || SMALL_LONG_CLASS.equals(fieldTypeName) || SMALL_FLOAT_CLASS.equals(fieldTypeName)
                || SMALL_DOUBLE_CLASS.equals(fieldTypeName)
                || SMALL_BOOLEAN_CLASS.equals(fieldTypeName)
                || STRING_CLASS.equals(fieldTypeName)) {
                return true;
            }
        }
        return false;
    }

    default Object reflect(ReflectWrapWithResult wrap) {
        try {
            return wrap.call();
        } catch (IllegalAccessException e) {

        } catch (NoSuchFieldException e) {

        } catch (Exception e) {

        }
        return null;
    }

}
