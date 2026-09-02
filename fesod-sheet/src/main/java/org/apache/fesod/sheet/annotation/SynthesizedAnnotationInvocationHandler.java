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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;

/**
 * {@link InvocationHandler} implementation used to support synthesized annotation proxy
 * instances created from {@link AnnotationAttributes}.
 */
class SynthesizedAnnotationInvocationHandler implements InvocationHandler {

    private final Class<? extends Annotation> type;
    private final AnnotationAttributes attributes;

    public SynthesizedAnnotationInvocationHandler(
            Class<? extends Annotation> annotationType, AnnotationAttributes attributes) {
        this.type = annotationType;
        this.attributes = attributes;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = attributes.getAttribute(method.getName(), method.getReturnType());
        if (result != null) {
            return result;
        }

        if (method.getParameterCount() == 0) {
            switch (method.getName()) {
                case "annotationType":
                    return this.type;
                case "hashCode":
                    return attributes.hashCode();
                case "toString":
                    return attributes.toString();
            }
        }
        if ("equals".equals(method.getName()) && method.getParameterCount() == 1) {
            Object other = args[0];
            if (proxy == other) {
                return true;
            }
            if (!this.type.isInstance(other)) {
                return false;
            }
            if (Proxy.isProxyClass(other.getClass())) {
                InvocationHandler handler = Proxy.getInvocationHandler(other);
                if (handler instanceof SynthesizedAnnotationInvocationHandler) {
                    return this.attributes.equals(((SynthesizedAnnotationInvocationHandler) handler).attributes);
                }
            }

            AttributeMethods attributeMethods = AttributeMethods.from(this.type);
            for (Map.Entry<String, Object> entry : attributes.asImmutableMap().entrySet()) {
                try {
                    Method m = attributeMethods.getMethod(entry.getKey());
                    if (!Objects.deepEquals(entry.getValue(), m.invoke(other))) {
                        return false;
                    }
                } catch (Exception ex) {
                    return false;
                }
            }
            return true;
        }

        throw new UnsupportedOperationException(
                String.format("Method [%s] is unsupported for synthesized annotation type [%s]", method, this.type));
    }
}
