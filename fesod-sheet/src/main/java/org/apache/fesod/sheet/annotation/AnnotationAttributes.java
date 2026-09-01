/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fesod.sheet.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

/**
 * Implement key-value pairs of annotation attributes based on {@link LinkedHashMap}.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class AnnotationAttributes extends LinkedHashMap<String, Object> {

    private final Class<? extends Annotation> annotationType;
    private final String annotationName;
    private final Set<String> defaultValueAttrNames;

    @Setter
    private int distance;

    public AnnotationAttributes(
            Class<? extends Annotation> annotationType, Map<String, Object> attrs, Set<String> defaultValueAttrNames) {
        super(attrs);
        this.annotationType = annotationType;
        this.annotationName = annotationType.getName();
        this.defaultValueAttrNames = CollectionUtils.isNotEmpty(defaultValueAttrNames)
                ? new HashSet<>(defaultValueAttrNames)
                : Collections.emptySet();
        this.distance = 0;
    }

    public boolean isAnnotationTypeEqual(Class<? extends Annotation> annotationType) {
        return this.annotationType.equals(annotationType);
    }

    public boolean isDefaultValue(String attributeName) {
        return defaultValueAttrNames.contains(attributeName);
    }

    public void markAsNonDefault(String attributeName) {
        if (CollectionUtils.isNotEmpty(defaultValueAttrNames)) {
            defaultValueAttrNames.remove(attributeName);
        }
    }

    public void merge(AnnotationAttributes other) {
        if (other == null) {
            return;
        }

        if (distance < other.getDistance()) {
            for (Map.Entry<String, Object> entry : other.entrySet()) {
                String attrName = entry.getKey();

                if (isDefaultValue(attrName) && !other.isDefaultValue(attrName)) {
                    put(attrName, entry.getValue());
                    markAsNonDefault(attrName);
                }
            }
        } else if (distance > other.getDistance()) {
            distance = other.getDistance();
            for (Map.Entry<String, Object> entry : other.entrySet()) {
                String attrName = entry.getKey();

                if (!other.isDefaultValue(attrName)) {
                    put(attrName, entry.getValue());
                    markAsNonDefault(attrName);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String attrName, Class<T> type) {
        Object result = get(attrName);
        if (Objects.isNull(result)) {
            return null;
        }

        Class<?> wrapped = ClassUtils.primitiveToWrapper(type);

        if (!wrapped.isInstance(result)
                && type.isArray()
                && ClassUtils.primitiveToWrapper(type.getComponentType()).isInstance(result)) {
            Object array = Array.newInstance(type.getComponentType(), 1);
            Array.set(array, 0, result);
            result = array;
        }
        if (!wrapped.isInstance(result)) {
            throw new IllegalArgumentException(String.format(
                    "Attribute '%s' is of type %s, but %s was expected for annotation [%s]",
                    attrName, result.getClass().getSimpleName(), type.getSimpleName(), annotationName));
        }

        return (T) result;
    }

    public <T> T getRequiredAttribute(String attrName, Class<T> type) {
        Validate.notBlank(attrName, "attributeName must not be null or blank");
        T result = getAttribute(attrName, type);
        if (Objects.isNull(result)) {
            throw new IllegalArgumentException(
                    String.format("Attribute '%s' not found for annotation '%s'", attrName, annotationName));
        }
        return result;
    }

    @Override
    public String toString() {
        Iterator<Map.Entry<String, Object>> i = entrySet().iterator();
        if (!i.hasNext()) return "@" + annotationName + "()";

        StringBuilder sb =
                new StringBuilder().append('@').append(annotationName).append('(');

        for (; ; ) {
            Map.Entry<String, Object> e = i.next();
            String key = e.getKey();
            Object value = e.getValue();
            sb.append(key);
            sb.append('=');
            sb.append(toString(value));
            if (!i.hasNext()) return sb.append(')').toString();
            sb.append(',').append(' ');
        }
    }

    private String toString(Object value) {
        Class<?> type = value.getClass();
        if (type.isArray()) {
            StringBuilder builder = new StringBuilder("{");
            int arrayLength = Array.getLength(value);
            for (int i = 0; i < arrayLength; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(toString(Array.get(value, i)));
            }
            builder.append('}');
            return builder.toString();
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (type == Class.class) {
            return ClassUtils.getCanonicalName((Class<?>) value) + ".class";
        }
        return String.valueOf(value);
    }
}
