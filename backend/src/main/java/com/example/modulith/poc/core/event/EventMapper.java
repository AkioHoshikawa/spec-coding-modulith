package com.example.modulith.poc.core.event;

import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;

public class EventMapper {
    public static <T> T mapByFieldName(Object source, Class<T> targetClass) {
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
