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
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Abstract base class for scanning and storing composable annotations.
 */
abstract class HierarchicalAnnotationScanner {

    protected final AnnotationMetadataResolver metadataResolver = new AnnotationMetadataResolver();

    protected HierarchicalAnnotationScanner() {}

    protected AnnotationMap scan(AnnotatedElement element) {
        Annotation[] annotations = element.getAnnotations();
        if (ArrayUtils.isEmpty(annotations)) {
            return AnnotationMap.EMPTY;
        }

        AnnotationMap.Builder builder = AnnotationMap.builder();

        Queue<Annotation> queue = new LinkedList<>(Arrays.asList(annotations));
        // Record visited annotation, to avoid circular dependencies (like: @A -> @B, @B -> @A)
        Set<Class<? extends Annotation>> visited = new HashSet<>();
        List<AliasFor> aliases = new ArrayList<>();
        int distance = 0;

        while (!queue.isEmpty()) {
            int currLevelSize = queue.size();

            for (int i = 0; i < currLevelSize; i++) {
                Annotation ann = queue.poll();
                Class<? extends Annotation> type = ann.annotationType();

                if (metadataResolver.shouldIgnore(type)) {
                    continue;
                }

                AnnotationMetadata metadata = metadataResolver.resolve(ann);
                metadata.setDistance(distance);

                // Handle composable-annotations (low-level attribute value)
                if (metadataResolver.isMetaMarked(ann)) {
                    metadata.addTo(aliases);

                    if (visited.add(type)) {
                        for (Annotation metaAnn : type.getAnnotations()) {
                            if (metadataResolver.shouldIgnore(metaAnn.annotationType())) {
                                continue;
                            }
                            queue.add(metaAnn);
                        }
                    }
                }

                builder.merge(type, metadata.getAttributes());
            }

            distance++;
        }

        AnnotationMap annotationMap = builder.build();

        // Handle alias
        handleAliasesIfNecessary(annotationMap, aliases);

        return annotationMap;
    }

    /**
     * Handle the mapping and overriding logic of annotation attribute aliases (AliasFor).
     * <p>
     * Attribute Override Policy: Annotations closer to the annotated target (with a smaller distance) have higher attribute priority
     * and can override the properties aliased in their meta-annotations (with a larger distance).
     * <p>
     * The judgment logic for distance is as follows:
     * <ul>
     *   <li><b>target distance &ge; marked distance + 1</b>: the target's closest occurrence is the marked
     *       annotation's own meta-declaration (or deeper), so the alias value applies unconditionally,
     *       whether explicitly set or at its default.</li>
     *   <li><b>target distance &lt; marked distance + 1</b>: the target is also annotated closer to the
     *       element (for example directly on the field). The alias only fills attributes that the closer
     *       target usage left at default; explicitly set attributes keep their value.</li>
     * </ul>
     *
     * @param annotationMap A collection of annotation attributes
     * @param aliases       Alias mapping list
     */
    private void handleAliasesIfNecessary(AnnotationMap annotationMap, List<AliasFor> aliases) {
        if (CollectionUtils.isEmpty(aliases)) {
            return;
        }

        for (AliasFor alias : aliases) {
            AnnotationAttributes marked = annotationMap.getAttributes(alias.getMarked());
            AnnotationAttributes target = annotationMap.getAttributes(alias.getTarget());

            if (marked == null || target == null) {
                continue;
            }
            if ((marked.getDistance() + 1) <= target.getDistance() || target.isDefaultValue(alias.getAttribute())) {
                target.put(alias.getAttribute(), marked.getAttribute(alias.getCustomAttribute()));
                target.markAsNonDefault(alias.getAttribute());
            }
        }
    }
}
