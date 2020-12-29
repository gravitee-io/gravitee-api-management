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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageRepository_searchTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/page-tests/";
    }

    @Test
    public void shouldFindApiPageByApiId() throws Exception {
        final List<Page> pages = pageRepository.search(new PageCriteria.Builder().referenceId("my-api").referenceType("API").build());

        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertFindPage(pages.get(0));
    }
    private void assertFindPage(Page page) {
        assertEquals("id", "FindApiPage", page.getId());
        assertEquals("name", "Find apiPage by apiId or Id", page.getName());
        assertEquals("content", "Content of the page", page.getContent());
        assertEquals("reference id", "my-api", page.getReferenceId());
        assertEquals("reference type", PageReferenceType.API, page.getReferenceType());
        assertEquals("type", "MARKDOWN", page.getType());
        assertEquals("last contributor", "john_doe", page.getLastContributor());
        assertEquals("order", 2, page.getOrder());
        assertTrue("published", page.isPublished());

        assertEquals("source type", "sourceType", page.getSource().getType());
        assertEquals("source configuration", "sourceConfiguration", page.getSource().getConfiguration());

        assertEquals("configuration try it", "true", page.getConfiguration().get("tryIt"));
        assertEquals("configuration try it URL", "http://company.com", page.getConfiguration().get("tryItURL"));
        assertEquals("configuration show URL", "true", page.getConfiguration().get("showURL"));
        assertEquals("configuration display operation id", "true", page.getConfiguration().get("displayOperationId"));
        assertEquals("configuration doc expansion", "FULL", page.getConfiguration().get("docExpansion"));
        assertEquals("configuration enable filtering", "true", page.getConfiguration().get("enableFiltering"));
        assertEquals("configuration show extensions", "true", page.getConfiguration().get("showExtensions"));
        assertEquals("configuration show common extensions", "true", page.getConfiguration().get("showCommonExtensions"));
        assertEquals("configuration maxDisplayedTags", "1234", page.getConfiguration().get("maxDisplayedTags"));

        assertTrue("homepage", page.isHomepage());
        assertEquals("excludedGroups", Arrays.asList("grp1", "grp2"), page.getExcludedGroups());
        assertTrue("created at", compareDate(new Date(1486771200000L), page.getCreatedAt()));
        assertTrue("updated at", compareDate(new Date(1486771200000L), page.getUpdatedAt()));
    }

    @Test
    public void shouldFindApiPageByApiIdAndHomepageFalse() throws Exception {
        Collection<Page> pages = pageRepository.search(new PageCriteria.Builder().referenceId("my-api-2").referenceType("API").homepage(Boolean.FALSE).build());
        assertNotNull(pages);
        assertEquals(2, pages.size());
    }

    @Test
    public void shouldFindApiPageByApiIdAndHomepageTrue() throws Exception {
        Collection<Page> pages = pageRepository.search(new PageCriteria.Builder().referenceId("my-api-2").referenceType("API").homepage(Boolean.TRUE).build());
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("home", pages.iterator().next().getId());
    }

    @Test
    public void shouldFindPortalPages() throws Exception {
        Collection<Page> pages = pageRepository.search(new PageCriteria.Builder().referenceId("DEFAULT").referenceType("ENVIRONMENT").build());
        assertNotNull(pages);
        assertEquals(2, pages.size());
        Set<String> ids = pages.stream().map(Page::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("FindPortalPage-homepage"));
        assertTrue(ids.contains("FindPortalPage-nothomepage"));
    }

    @Test
    public void shouldFindPortalPageByHomepageFalse() throws Exception {
        Collection<Page> pages = pageRepository.search(new PageCriteria.Builder().referenceId("DEFAULT").referenceType("ENVIRONMENT").homepage(Boolean.FALSE).build());
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("FindPortalPage-nothomepage", pages.iterator().next().getId());
    }

    @Test
    public void shouldFindPortalPageByHomepageTrue() throws Exception {
        Collection<Page> pages = pageRepository.search(new PageCriteria.Builder().referenceId("DEFAULT").referenceType("ENVIRONMENT").homepage(Boolean.TRUE).build());
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("FindPortalPage-homepage", pages.iterator().next().getId());
    }

    @Test
    public void shouldFindAllWhenCriteriaIsEmpty() throws Exception {
        Collection<Page> pages = pageRepository.search(new PageCriteria.Builder().build());
        assertNotNull(pages);
        assertEquals(11, pages.size());
    }
}
