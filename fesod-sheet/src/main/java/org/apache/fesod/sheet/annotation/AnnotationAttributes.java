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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

/**
 * Resolved key-value pairs attributes for an annotation.
 * Provides type-safe lookup of annotation attributes.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnnotationAttributes {

    @Getter
    @EqualsAndHashCode.Include
    private final Class<? extends Annotation> annotationType;

    @Getter
    private final String annotationName;

    @EqualsAndHashCode.Include
    private final Map<String, Object> attributes;

    private final Set<String> defaultValueAttrNames;

    @Getter(AccessLevel.PACKAGE)
    @Setter(AccessLevel.PACKAGE)
    private int distance;

    AnnotationAttributes(
            Class<? extends Annotation> annotationType, Map<String, Object> attrs, Set<String> defaultValueAttrNames) {
        this.annotationType = annotationType;
        this.annotationName = annotationType.getName();
        this.attributes = new LinkedHashMap<>(attrs);
        this.defaultValueAttrNames = new HashSet<>(defaultValueAttrNames);
        this.distance = 0;
    }

    public boolean isAnnotationTypeEqual(Class<? extends Annotation> annotationType) {
        return this.annotationType.equals(annotationType);
    }

    boolean isDefaultValue(String attributeName) {
        return defaultValueAttrNames.contains(attributeName);
    }

    void markAsNonDefault(String attributeName) {
        if (CollectionUtils.isNotEmpty(defaultValueAttrNames)) {
            defaultValueAttrNames.remove(attributeName);
        }
    }

    void merge(AnnotationAttributes other) {
        if (other == null) {
            return;
        }

        if (distance < other.getDistance()) {
            for (Map.Entry<String, Object> entry : other.attributes.entrySet()) {
                String attrName = entry.getKey();

                if (isDefaultValue(attrName) && !other.isDefaultValue(attrName)) {
                    put(attrName, entry.getValue());
                    markAsNonDefault(attrName);
                }
            }
        } else if (distance > other.getDistance()) {
            distance = other.getDistance();
            for (Map.Entry<String, Object> entry : other.attributes.entrySet()) {
                String attrName = entry.getKey();

                if (!other.isDefaultValue(attrName)) {
                    put(attrName, entry.getValue());
                    markAsNonDefault(attrName);
                }
            }
        }
    }

    void put(String attrName, Object value) {
        attributes.put(attrName, value);
    }

    public Object getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String attributeName, Class<T> type) {
        Object result = getAttribute(attributeName);
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
                    attributeName, result.getClass().getSimpleName(), type.getSimpleName(), annotationName));
        }

        return (T) result;
    }

    public <T> T getRequiredAttribute(String attributeName, Class<T> type) {
        Validate.notBlank(attributeName, "attributeName must not be null or blank");
        T result = getAttribute(attributeName, type);
        if (Objects.isNull(result)) {
            throw new IllegalArgumentException(
                    String.format("Attribute '%s' not found for annotation '%s'", attributeName, annotationName));
        }
        return result;
    }

    public Map<String, Object> asImmutableMap() {
        return Collections.unmodifiableMap(attributes);
    }
}
