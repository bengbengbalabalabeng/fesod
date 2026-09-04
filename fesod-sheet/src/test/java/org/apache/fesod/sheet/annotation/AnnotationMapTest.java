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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.fesod.sheet.testkit.Tags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link AnnotationMap}.
 */
@Tag(Tags.UNIT)
class AnnotationMapTest {

    @Test
    void shouldExposeAbsentSemanticsOnEmptyMap() {
        Assertions.assertTrue(AnnotationMap.EMPTY.isEmpty());
        Assertions.assertEquals(0, AnnotationMap.EMPTY.size());
        Assertions.assertFalse(AnnotationMap.EMPTY.hasAnnotation(ExcelProperty.class));
        Assertions.assertNull(AnnotationMap.EMPTY.getAttributes(ExcelProperty.class));
        Assertions.assertNull(AnnotationMap.EMPTY.synthesize(ExcelProperty.class));
    }

    @Test
    void shouldMergeDuplicateTypeEntriesAcrossDistances() {
        AnnotationAttributes near = attributes(2, new String[] {"near"}, Collections.singleton("value"));
        near.setDistance(0);
        AnnotationAttributes far = attributes(3, new String[] {"meta-head"}, Collections.emptySet());
        far.setDistance(1);

        AnnotationMap map = AnnotationMap.builder()
                .merge(ExcelProperty.class, near)
                .merge(ExcelProperty.class, far)
                .build();

        AnnotationAttributes merged = map.getAttributes(ExcelProperty.class);
        Assertions.assertNotNull(merged);
        // the closer entry's explicit value wins, and its defaulted attribute is
        // filled by the farther declaration
        Assertions.assertEquals(2, merged.getRequiredAttribute("index", Integer.class));
        Assertions.assertArrayEquals(new String[] {"meta-head"}, merged.getRequiredAttribute("value", String[].class));
        Assertions.assertNotNull(map.synthesize(ExcelProperty.class));
    }

    private AnnotationAttributes attributes(int index, String[] value, Set<String> defaults) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("index", index);
        attrs.put("value", value);
        return new AnnotationAttributes(ExcelProperty.class, attrs, defaults);
    }
}
