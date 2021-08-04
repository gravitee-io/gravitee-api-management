/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model;

import static org.junit.Assert.*;

import java.util.Collections;
import org.junit.Test;

public class UpdatePageEntityTest {

    @Test
    public void shouldCreateObjectBasedOnPageEntity() {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setId("page#2");
        pageEntity.setName("Sub Page 3");
        pageEntity.setType("ASCIIDOC");
        pageEntity.setParentId("page#1");
        pageEntity.setReferenceType("API");
        pageEntity.setReferenceId("api#1");
        pageEntity.setConfiguration(Collections.emptyMap());
        pageEntity.setContent("content");
        pageEntity.setExcludedAccessControls(true);
        pageEntity.setAccessControls(Collections.emptySet());
        pageEntity.setHomepage(false);
        pageEntity.setLastContributor("contributor");
        pageEntity.setOrder(1);
        pageEntity.setPublished(true);
        PageSourceEntity pageSourceEntity = new PageSourceEntity();
        pageSourceEntity.setType("API");
        pageEntity.setSource(pageSourceEntity);
        pageEntity.setAttachedMedia(Collections.emptyList());

        UpdatePageEntity updatePageEntity = UpdatePageEntity.from(pageEntity);

        assertEquals("Sub Page 3", updatePageEntity.getName());
        assertEquals("page#1", updatePageEntity.getParentId());
        assertEquals(Collections.emptyMap(), updatePageEntity.getConfiguration());
        assertEquals("content", updatePageEntity.getContent());
        assertTrue(updatePageEntity.isExcludedAccessControls());
        assertEquals(Collections.emptySet(), updatePageEntity.getAccessControls());
        assertFalse(updatePageEntity.isHomepage());
        assertEquals("contributor", updatePageEntity.getLastContributor());
        assertEquals((Integer) 1, updatePageEntity.getOrder());
        assertTrue(updatePageEntity.isPublished());
        assertEquals("API", updatePageEntity.getSource().getType());
        assertEquals(Collections.emptyList(), updatePageEntity.getAttachedMedia());
    }
}
