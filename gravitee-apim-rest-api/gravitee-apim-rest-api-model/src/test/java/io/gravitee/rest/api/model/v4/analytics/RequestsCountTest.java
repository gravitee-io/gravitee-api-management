/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model.v4.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class RequestsCountTest {

    @Test
    void shouldCreateUsingAllArgsConstructor() {
        Map<String, Long> entrypoints = Map.of("entry1", 100L, "entry2", 200L);
        RequestsCount count = new RequestsCount(300L, entrypoints);

        assertEquals(300L, count.getTotal());
        assertEquals(2, count.getCountsByEntrypoint().size());
        assertEquals(100L, count.getCountsByEntrypoint().get("entry1"));
    }

    @Test
    void shouldCreateUsingNoArgsConstructorAndSetters() {
        RequestsCount count = new RequestsCount();
        count.setTotal(50L);
        count.setCountsByEntrypoint(Map.of("e1", 50L));

        assertEquals(50L, count.getTotal());
        assertEquals(1, count.getCountsByEntrypoint().size());
    }

    @Test
    void shouldCreateUsingBuilder() {
        RequestsCount count = RequestsCount.builder().total(150L).countsByEntrypoint(Map.of("entryA", 150L)).build();

        assertEquals(150L, count.getTotal());
        assertTrue(count.getCountsByEntrypoint().containsKey("entryA"));
    }

    @Test
    void shouldSupportToBuilder() {
        RequestsCount original = RequestsCount.builder().total(400L).countsByEntrypoint(Map.of("entryX", 400L)).build();

        RequestsCount modified = original.toBuilder().total(500L).build();

        assertEquals(500L, modified.getTotal());
        assertEquals("entryX", modified.getCountsByEntrypoint().keySet().iterator().next());
    }

    @Test
    void shouldHandleNullCountsByEntrypoint() {
        RequestsCount count = RequestsCount.builder().total(0L).countsByEntrypoint(null).build();

        assertEquals(0L, count.getTotal());
        assertNull(count.getCountsByEntrypoint());
    }

    @Test
    void equalsAndHashCodeShouldWorkForSameValues() {
        Map<String, Long> map = Map.of("entry1", 123L);
        RequestsCount count1 = new RequestsCount(123L, map);
        RequestsCount count2 = new RequestsCount(123L, map);

        assertEquals(count1, count2);
        assertEquals(count1.hashCode(), count2.hashCode());
    }
}
