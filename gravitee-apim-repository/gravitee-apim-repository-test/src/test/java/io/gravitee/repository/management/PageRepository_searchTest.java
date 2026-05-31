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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageRepository_searchTest extends AbstractManagementRepositoryTest {

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
        assertEquals("FindApiPage", page.getId(), "id");
        assertEquals("Find apiPage by apiId or Id", page.getName(), "name");
        assertEquals("Content of the page", page.getContent(), "content");
        assertEquals("my-api", page.getReferenceId(), "reference id");
        assertEquals(PageReferenceType.API, page.getReferenceType(), "reference type");
        assertEquals("MARKDOWN", page.getType(), "type");
        assertEquals("john_doe", page.getLastContributor(), "last contributor");
        assertEquals(2, page.getOrder(), "order");
        assertTrue(page.isPublished(), "published");

        assertEquals("sourceType", page.getSource().getType(), "source type");
        assertEquals("sourceConfiguration", page.getSource().getConfiguration(), "source configuration");

        assertEquals("true", page.getConfiguration().get("tryIt"), "configuration try it");
        assertEquals("http://company.com", page.getConfiguration().get("tryItURL"), "configuration try it URL");
        assertEquals("true", page.getConfiguration().get("showURL"), "configuration show URL");
        assertEquals("true", page.getConfiguration().get("displayOperationId"), "configuration display operation id");
        assertEquals("FULL", page.getConfiguration().get("docExpansion"), "configuration doc expansion");
        assertEquals("true", page.getConfiguration().get("enableFiltering"), "configuration enable filtering");
        assertEquals("true", page.getConfiguration().get("showExtensions"), "configuration show extensions");
        assertEquals("true", page.getConfiguration().get("showCommonExtensions"), "configuration show common extensions");
        assertEquals("1234", page.getConfiguration().get("maxDisplayedTags"), "configuration maxDisplayedTags");

        assertTrue(page.isHomepage(), "homepage");

        assertEquals(
            new HashSet<>(
                asList(new AccessControl("grp1", "GROUP"), new AccessControl("grp2", "GROUP"), new AccessControl("role1", "ROLE"))
            ),
            page.getAccessControls(),
            "access control list"
        );

        assertTrue(compareDate(new Date(1486771200000L), page.getCreatedAt()), "created at");
        assertTrue(compareDate(new Date(1486771200000L), page.getUpdatedAt()), "updated at");
    }

    @Test
    public void shouldFindApiPageByApiIdAndHomepageFalse() throws Exception {
        Collection<Page> pages = pageRepository.search(
            new PageCriteria.Builder().referenceId("my-api-2").referenceType("API").homepage(Boolean.FALSE).build()
        );
        assertNotNull(pages);
        assertEquals(2, pages.size());
    }

    @Test
    public void shouldFindApiPageByApiIdAndHomepageTrue() throws Exception {
        Collection<Page> pages = pageRepository.search(
            new PageCriteria.Builder().referenceId("my-api-2").referenceType("API").homepage(Boolean.TRUE).build()
        );
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("home", pages.iterator().next().getId());
    }

    @Test
    public void shouldReturnPageWithAutoFetch() throws Exception {
        List<Page> autofetchPages = pageRepository.search(new PageCriteria.Builder().withAutoFetch().build());
        assertNotNull(autofetchPages);
        assertEquals(1, autofetchPages.size(), "Should have One page with autofetch");
        assertEquals("FindApiPage", autofetchPages.get(0).getId(), "AutoFetch Page should be FindApiPage");
    }

    @Test
    public void shouldFindPortalPages() throws Exception {
        Collection<Page> pages = pageRepository.search(
            new PageCriteria.Builder().referenceId("DEFAULT").referenceType("ENVIRONMENT").build()
        );
        assertNotNull(pages);
        assertEquals(2, pages.size());
        Set<String> ids = pages.stream().map(Page::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("FindPortalPage-homepage"));
        assertTrue(ids.contains("FindPortalPage-nothomepage"));
    }

    @Test
    public void shouldFindPortalPageByHomepageFalse() throws Exception {
        Collection<Page> pages = pageRepository.search(
            new PageCriteria.Builder().referenceId("DEFAULT").referenceType("ENVIRONMENT").homepage(Boolean.FALSE).build()
        );
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("FindPortalPage-nothomepage", pages.iterator().next().getId());
    }

    @Test
    public void shouldFindPortalPageByHomepageTrue() throws Exception {
        Collection<Page> pages = pageRepository.search(
            new PageCriteria.Builder().referenceId("DEFAULT").referenceType("ENVIRONMENT").homepage(Boolean.TRUE).build()
        );
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("FindPortalPage-homepage", pages.iterator().next().getId());
    }

    @Test
    public void shouldFindAllWhenCriteriaIsEmpty() throws Exception {
        Collection<Page> pages = pageRepository.search(new PageCriteria.Builder().build());
        assertNotNull(pages);
        assertEquals(13, pages.size());
    }

    @Test
    public void shouldFindPortalPageByVisibilityPublic() throws Exception {
        Collection<Page> pages = pageRepository.search(new PageCriteria.Builder().visibility(Visibility.PUBLIC.name()).build());
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("FindApiPage", pages.iterator().next().getId());
    }

    @Test
    public void shouldFindPagesByAccessControlGroupId() throws Exception {
        List<Page> pages = pageRepository.search(new PageCriteria.Builder().accessControlGroupId("grp1").build());
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("FindApiPage", pages.get(0).getId());
    }

    @Test
    public void shouldFindPagesByAccessControlGroupIdAndReferenceType() throws Exception {
        List<Page> pages = pageRepository.search(new PageCriteria.Builder().referenceType("API").accessControlGroupId("grp2").build());
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("FindApiPage", pages.get(0).getId());
    }

    @Test
    public void shouldReturnEmptyWhenAccessControlGroupIdNotFound() throws Exception {
        List<Page> pages = pageRepository.search(new PageCriteria.Builder().accessControlGroupId("non-existent-group").build());
        assertNotNull(pages);
        assertEquals(0, pages.size());
    }

    @Test
    public void shouldNotMatchRoleAccessControlWhenSearchingByGroupId() throws Exception {
        // "role1" exists as a ROLE access control on FindApiPage, not as a GROUP
        List<Page> pages = pageRepository.search(new PageCriteria.Builder().accessControlGroupId("role1").build());
        assertNotNull(pages);
        assertEquals(0, pages.size());
    }
}
