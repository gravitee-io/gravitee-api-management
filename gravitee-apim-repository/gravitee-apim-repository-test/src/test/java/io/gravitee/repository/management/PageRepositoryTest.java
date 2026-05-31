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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/page-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final io.gravitee.common.data.domain.Page<Page> pages = pageRepository.findAll(
            new PageableBuilder().pageNumber(0).pageSize(100).build()
        );

        assertNotNull(pages);
        assertEquals(13, pages.getTotalElements());
        assertEquals(13, pages.getPageElements());
        assertEquals(13, pages.getContent().size());

        Page findApiPage = pages
            .getContent()
            .stream()
            .filter(p -> p.getId().equals("FindApiPage"))
            .findFirst()
            .get();
        assertFindPage(findApiPage);
    }

    @Test
    public void shouldFindAll_Paging() throws Exception {
        boolean findApiTested = false;

        Set<String> ids = new HashSet<>();
        int pageNumber = 0;
        do {
            Pageable build = new PageableBuilder().pageNumber(pageNumber).pageSize(1).build();
            io.gravitee.common.data.domain.Page<Page> pages = pageRepository.findAll(build);
            assertNotNull(pages);
            assertEquals(13, pages.getTotalElements());
            assertEquals(1, pages.getPageElements());
            assertEquals(1, pages.getContent().size());

            Page foundPage = pages.getContent().get(0);
            ids.add(foundPage.getId());
            if (foundPage.getId().equals("FindApiPage")) {
                assertFindPage(foundPage);
                findApiTested = true;
            }

            pageNumber++;
        } while (pageNumber < 13);

        assertTrue(findApiTested);
        assertEquals(13, ids.size()); // all pages were different
    }

    @Test
    public void shouldFindApiPageById() throws Exception {
        final Optional<Page> page = pageRepository.findById("FindApiPage");

        assertNotNull(page);
        assertTrue(page.isPresent());
        assertFindPage(page.get());
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
        assertEquals("false", page.getConfiguration().get("disableSyntaxHighlight"), "configuration disable syntax highlight");
        assertEquals("http://company.com", page.getConfiguration().get("tryItURL"), "configuration try it URL");
        assertEquals("true", page.getConfiguration().get("showURL"), "configuration show URL");
        assertEquals("true", page.getConfiguration().get("displayOperationId"), "configuration display operation id");
        assertEquals("FULL", page.getConfiguration().get("docExpansion"), "configuration doc expansion");
        assertEquals("true", page.getConfiguration().get("enableFiltering"), "configuration enable filtering");
        assertEquals("true", page.getConfiguration().get("showExtensions"), "configuration show extensions");
        assertEquals("true", page.getConfiguration().get("showCommonExtensions"), "configuration show common extensions");
        assertEquals("1234", page.getConfiguration().get("maxDisplayedTags"), "configuration maxDisplayedTags");
        assertEquals("http://provider.com/edit/page", page.getMetadata().get("edit_url"), "metadata edit_url");
        assertEquals("256", page.getMetadata().get("size"), "metadata size");

        assertTrue(page.isHomepage(), "homepage");
        assertEquals(true, page.isExcludedAccessControls(), "excluded access controls");
        assertEquals(
            new HashSet<>(
                asList(new AccessControl("grp1", "GROUP"), new AccessControl("grp2", "GROUP"), new AccessControl("role1", "ROLE"))
            ),
            page.getAccessControls(),
            "access controls"
        );

        final List<PageMedia> attachedMedia = page.getAttachedMedia();
        assertNotNull(attachedMedia);
        assertEquals(2, attachedMedia.size(), "attachedMedia");
        assertEquals(
            asList(
                new PageMedia("media_id_1", "media_name_1", new Date(1586771200000L)),
                new PageMedia("media_id_2", "media_name_2", new Date(1587771200000L))
            ),
            attachedMedia,
            "attachedMedia"
        );

        assertTrue(compareDate(new Date(1486771200000L), page.getCreatedAt()), "created at");
        assertTrue(compareDate(new Date(1486771200000L), page.getUpdatedAt()), "updated at");
        assertTrue(page.getUseAutoFetch().booleanValue(), "no autofetch");
    }

    @Test
    public void shouldCreateApiPage() throws Exception {
        final Page page = new Page();
        page.setId("new-page");
        page.setName("Page name");
        page.setContent("Page content");
        page.setOrder(3);
        page.setReferenceId("my-api");
        page.setReferenceType(PageReferenceType.API);
        page.setHomepage(true);
        page.setType("MARKDOWN");
        page.setParentId("2");
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());
        page.setUseAutoFetch(Boolean.FALSE);
        page.setVisibility(Visibility.PUBLIC.name());

        final Map<String, String> configuration = new HashMap<>();
        configuration.put("displayOperationId", "true");
        configuration.put("docExpansion", "FULL");
        configuration.put("showCommonExtensions", "true");
        configuration.put("maxDisplayedTags", "1234");
        configuration.put("enableFiltering", "true");
        configuration.put("tryItURL", "http://company.com");
        configuration.put("showURL", "true");
        configuration.put("showExtensions", "true");
        configuration.put("tryIt", "true");
        configuration.put("disableSyntaxHighlight", "false");
        page.setConfiguration(configuration);

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("edit_url", "url");
        metadata.put("size", "10");
        page.setMetadata(metadata);

        Optional<Page> optionalBefore = pageRepository.findById("new-page");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-page");
        assertFalse(optionalBefore.isPresent(), "Page not found before");
        assertTrue(optionalAfter.isPresent(), "Page saved not found");

        final Page pageSaved = optionalAfter.get();
        assertEquals(page.getName(), pageSaved.getName(), "Invalid saved page name.");
        assertEquals(page.getContent(), pageSaved.getContent(), "Invalid page content.");
        assertEquals(page.getOrder(), pageSaved.getOrder(), "Invalid page order.");
        assertEquals(page.getType(), pageSaved.getType(), "Invalid page type.");
        assertEquals(page.isHomepage(), pageSaved.isHomepage(), "Invalid homepage flag.");
        assertEquals(page.getParentId(), pageSaved.getParentId(), "Invalid parentId.");
        assertNull(page.getSource(), "Invalid page source.");
        assertEquals(page.getConfiguration(), pageSaved.getConfiguration(), "Invalid configuration.");
        assertEquals(page.getMetadata(), pageSaved.getMetadata(), "Invalid metadata.");
        assertEquals(page.getUseAutoFetch().booleanValue(), pageSaved.getUseAutoFetch().booleanValue(), "Invalid useAutoFetch.");
    }

    @Test
    public void shouldCreateApiFolderPage() throws Exception {
        final Page page = new Page();
        page.setId("new-page-folder");
        page.setName("Folder name");
        page.setOrder(3);
        page.setReferenceId("my-api");
        page.setReferenceType(PageReferenceType.API);
        page.setHomepage(false);
        page.setVisibility(Visibility.PUBLIC.name());
        page.setParentId("");
        page.setType("FOLDER");
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());

        Optional<Page> optionalBefore = pageRepository.findById("new-page-folder");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-page-folder");
        assertFalse(optionalBefore.isPresent(), "Page not found before");
        assertTrue(optionalAfter.isPresent(), "Page saved not found");

        final Page pageSaved = optionalAfter.get();
        assertEquals(page.getName(), pageSaved.getName(), "Invalid saved page name.");
        assertEquals(page.getContent(), pageSaved.getContent(), "Invalid page content.");
        assertEquals(page.getOrder(), pageSaved.getOrder(), "Invalid page order.");
        assertEquals(page.getType(), pageSaved.getType(), "Invalid page type.");
        assertEquals(page.getParentId(), pageSaved.getParentId(), "Invalid ParentId.");
        assertNull(page.getSource(), "Invalid page source.");
    }

    @Test
    public void shouldCreatePortalPage() throws Exception {
        final Page page = new Page();
        page.setId("new-portal-page");
        page.setReferenceId("DEFAULT");
        page.setReferenceType(PageReferenceType.ENVIRONMENT);
        page.setName("Page name");
        page.setContent("Page content");
        page.setOrder(3);
        page.setType("MARKDOWN");
        page.setParentId("2");
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());
        page.setVisibility(Visibility.PUBLIC.name());

        final Map<String, String> configuration = new HashMap<>();
        configuration.put("displayOperationId", "true");
        configuration.put("docExpansion", "FULL");
        configuration.put("showCommonExtensions", "true");
        configuration.put("maxDisplayedTags", "1234");
        configuration.put("enableFiltering", "true");
        configuration.put("tryItURL", "http://company.com");
        configuration.put("disableSyntaxHighlight", "false");
        configuration.put("showURL", "true");
        configuration.put("showExtensions", "true");
        configuration.put("tryIt", "true");
        page.setConfiguration(configuration);

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("edit_url", "url");
        metadata.put("size", "10");
        page.setMetadata(metadata);

        Optional<Page> optionalBefore = pageRepository.findById("new-portal-page");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-portal-page");
        assertFalse(optionalBefore.isPresent(), "Page not found before");
        assertTrue(optionalAfter.isPresent(), "Page saved not found");

        final Page pageSaved = optionalAfter.get();
        assertEquals(page.getName(), pageSaved.getName(), "Invalid saved page name.");
        assertEquals(page.getContent(), pageSaved.getContent(), "Invalid page content.");
        assertEquals(page.getOrder(), pageSaved.getOrder(), "Invalid page order.");
        assertEquals(page.getType(), pageSaved.getType(), "Invalid page type.");
        assertEquals(page.getParentId(), pageSaved.getParentId(), "Invalid parentId.");
        assertEquals(page.isHomepage(), pageSaved.isHomepage(), "Invalid homepage flag.");
        assertNull(page.getSource(), "Invalid page source.");
        assertEquals(page.getConfiguration(), pageSaved.getConfiguration(), "Invalid configuration.");
        assertEquals(page.getMetadata(), pageSaved.getMetadata(), "Invalid metadata.");
    }

    @Test
    public void shouldCreatePortalFolderPage() throws Exception {
        final Page page = new Page();
        page.setId("new-portal-page-folder");
        page.setReferenceId("DEFAULT");
        page.setReferenceType(PageReferenceType.ENVIRONMENT);
        page.setName("Folder name");
        page.setOrder(3);
        page.setType("FOLDER");
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());
        page.setParentId("");
        page.setParentId("");
        page.setUseAutoFetch(false);
        page.setVisibility(Visibility.PUBLIC.name());

        Optional<Page> optionalBefore = pageRepository.findById("new-portal-page-folder");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-portal-page-folder");
        assertFalse(optionalBefore.isPresent(), "Page not found before");
        assertTrue(optionalAfter.isPresent(), "Page saved not found");

        final Page pageSaved = optionalAfter.get();
        assertEquals(page.getName(), pageSaved.getName(), "Invalid saved page name.");
        assertEquals(page.getContent(), pageSaved.getContent(), "Invalid page content.");
        assertEquals(page.getOrder(), pageSaved.getOrder(), "Invalid page order.");
        assertEquals(page.getType(), pageSaved.getType(), "Invalid page type.");
        assertEquals(page.getParentId(), pageSaved.getParentId(), "Invalid ParentId.");
        assertNull(page.getSource(), "Invalid page source.");
        assertFalse(page.getUseAutoFetch(), "Invalid useAutoFetch.");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Page> optionalBefore = pageRepository.findById("updatePage");
        assertTrue(optionalBefore.isPresent(), "Page to update not found");
        assertEquals("Update Page", optionalBefore.get().getName(), "Invalid page name.");
        assertEquals("Content of the update page", optionalBefore.get().getContent(), "Invalid page content.");
        final Page page = optionalBefore.get();
        page.setId("updatePage");
        page.setHrid("updatePage-hrid");
        page.setName("New name");
        page.setContent("New content");
        page.setReferenceId("my-api-2");
        page.setReferenceType(PageReferenceType.API);
        page.setType("SWAGGER");
        page.setOrder(1);
        page.setUpdatedAt(new Date(1486771200000L));
        page.setCreatedAt(new Date(1486772200000L));
        page.setParentId("parent-123");
        page.setParentHrid("parent-123-hrid");
        page.setHomepage(true);
        page.setExcludedAccessControls(true);
        page.setAccessControls(
            new HashSet<>(
                asList(new AccessControl("grp1", "GROUP"), new AccessControl("grp2", "GROUP"), new AccessControl("role1", "ROLE"))
            )
        );
        page.setAttachedMedia(Collections.singletonList(new PageMedia("media_id", "media_name", new Date(1586771200000L))));
        page.setLastContributor("me");
        page.setPublished(true);
        page.setConfiguration(new HashMap<>());
        page.getConfiguration().put("tryIt", "true");
        page.getConfiguration().put("disableSyntaxHighlight", "false");
        page.getConfiguration().put("tryItURL", "http://company.com");
        page.getConfiguration().put("showURL", "true");
        page.getConfiguration().put("displayOperationId", "true");
        page.getConfiguration().put("docExpansion", "FULL");
        page.getConfiguration().put("enableFiltering", "true");
        page.getConfiguration().put("showExtensions", "true");
        page.getConfiguration().put("showCommonExtensions", "true");
        page.getConfiguration().put("maxDisplayedTags", "1234");

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("edit_url", "url");
        metadata.put("size", "10");
        page.setMetadata(metadata);

        assertUpdatedPage(pageRepository.update(page));
        assertUpdatedPage(pageRepository.findById("updatePage").get());
    }

    private void assertUpdatedPage(final Page updatedPage) {
        assertNotNull(updatedPage, "Page to update not found");
        assertEquals("updatePage-hrid", updatedPage.getHrid(), "Invalid parent id.");
        assertEquals("New name", updatedPage.getName(), "Invalid saved page name.");
        assertEquals("New content", updatedPage.getContent(), "Invalid page content.");
        assertEquals("my-api-2", updatedPage.getReferenceId(), "Invalid reference id.");
        assertEquals(PageReferenceType.API, updatedPage.getReferenceType(), "Invalid reference type.");
        assertEquals("SWAGGER", updatedPage.getType(), "Invalid type.");
        assertEquals(1, updatedPage.getOrder(), "Invalid order.");
        assertTrue(compareDate(new Date(1486771200000L), updatedPage.getUpdatedAt()), "Invalid updatedAt.");
        assertTrue(compareDate(new Date(1486772200000L), updatedPage.getCreatedAt()), "Invalid createdAt.");
        assertEquals("parent-123", updatedPage.getParentId(), "Invalid parent id.");
        assertEquals("parent-123-hrid", updatedPage.getParentHrid(), "Invalid parent id.");
        assertTrue(updatedPage.isHomepage(), "Invalid homepage.");
        assertTrue(updatedPage.isExcludedAccessControls(), "Invalid ACL excluded value.");
        assertTrue(!updatedPage.getAccessControls().isEmpty(), "Invalid ACL controls.");
        assertEquals(3, updatedPage.getAccessControls().size(), "Invalid ACL size");
        assertTrue(!updatedPage.getAttachedMedia().isEmpty(), "Invalid attached media.");
        assertEquals("me", updatedPage.getLastContributor(), "Invalid last contributor.");
        assertTrue(updatedPage.isPublished(), "Invalid published.");

        assertEquals("true", updatedPage.getConfiguration().get("tryIt"), "configuration try it");
        assertEquals("false", updatedPage.getConfiguration().get("disableSyntaxHighlight"), "configuration disable syntax highlight");
        assertEquals("http://company.com", updatedPage.getConfiguration().get("tryItURL"), "configuration try it URL");
        assertEquals("true", updatedPage.getConfiguration().get("showURL"), "configuration show URL");
        assertEquals("true", updatedPage.getConfiguration().get("displayOperationId"), "configuration display operation id");
        assertEquals("FULL", updatedPage.getConfiguration().get("docExpansion"), "configuration doc expansion");
        assertEquals("true", updatedPage.getConfiguration().get("enableFiltering"), "configuration enable filtering");
        assertEquals("true", updatedPage.getConfiguration().get("showExtensions"), "configuration show extensions");
        assertEquals("true", updatedPage.getConfiguration().get("showCommonExtensions"), "configuration show common extensions");
        assertEquals("1234", updatedPage.getConfiguration().get("maxDisplayedTags"), "configuration maxDisplayedTags");
        assertEquals("url", updatedPage.getMetadata().get("edit_url"), "metadata edit_url");
        assertEquals("10", updatedPage.getMetadata().get("size"), "metadata size");
    }

    @Test
    public void shouldCountExistingParentIdAndIsPublished() throws TechnicalException {
        var count = pageRepository.countByParentIdAndIsPublished("2");
        assertEquals(1, count);
    }

    @Test
    public void shouldCountNonExistingParentId() throws TechnicalException {
        var count = pageRepository.countByParentIdAndIsPublished("does-not-exist");
        assertEquals(0, count);
    }

    @Test
    public void shouldUpdateFolderPage() throws Exception {
        Optional<Page> optionalBefore = pageRepository.findById("updatePageFolder");
        assertTrue(optionalBefore.isPresent(), "Page to update not found");
        assertEquals("Update Page Folder", optionalBefore.get().getName(), "Invalid page name.");
        assertEquals("Content of the update page folder", optionalBefore.get().getContent(), "Invalid page content.");
        assertEquals("2", optionalBefore.get().getParentId(), "Invalid page parentId.");

        final Page page = optionalBefore.get();
        page.setId("updatePageFolder");
        page.setName("New name page folder");
        page.setContent("New content page folder");
        page.setParentId("3");

        assertUpdatePageFolder(pageRepository.update(page));
        assertUpdatePageFolder(pageRepository.findById("updatePageFolder").get());
    }

    private void assertUpdatePageFolder(final Page updatedPage) {
        assertNotNull(updatedPage, "Page to update not found");
        assertEquals("New name page folder", updatedPage.getName(), "Invalid saved page name.");
        assertEquals("New content page folder", updatedPage.getContent(), "Invalid page content.");
        assertEquals("3", updatedPage.getParentId(), "Invalid page parentId.");
        assertEquals(PageReferenceType.API, updatedPage.getReferenceType(), "Invalid page reference type.");
        assertEquals("my-api-3", updatedPage.getReferenceId(), "Invalid page reference id.");
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<Page> pageShouldExists = pageRepository.findById("page-to-be-deleted");
        pageRepository.delete("page-to-be-deleted");
        Optional<Page> pageShouldNotExists = pageRepository.findById("page-to-be-deleted");

        assertTrue(pageShouldExists.isPresent(), "should exists before delete");
        assertFalse(pageShouldNotExists.isPresent(), "should not exists after delete");
    }

    @Test
    public void shouldNotUpdateUnknownPage() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Page unknownPage = new Page();
            unknownPage.setId("unknown");
            unknownPage.setReferenceId("DEFAULT");
            unknownPage.setReferenceType(PageReferenceType.ENVIRONMENT);
            pageRepository.update(unknownPage);
            fail("An unknown page should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            pageRepository.update(null);
            fail("A null page should not be updated");
        });
    }

    @Test
    public void shouldFindMaxApiPageOrderByApiId() throws TechnicalException {
        Integer maxApiPageOrderByApiId = pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder("my-api-2", PageReferenceType.API);
        assertEquals(Integer.valueOf(2), maxApiPageOrderByApiId);
    }

    @Test
    public void shouldFindDefaultMaxApiPageOrderByApiId() throws TechnicalException {
        Integer maxApiPageOrderByApiId = pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder(
            "unknown api id",
            PageReferenceType.API
        );
        assertEquals(Integer.valueOf(0), maxApiPageOrderByApiId);
    }

    @Test
    public void shouldFindMaxPortalPageOrder() throws TechnicalException {
        Integer maxPortalPageOrder = pageRepository.findMaxPageReferenceIdAndReferenceTypeOrder("DEFAULT", PageReferenceType.ENVIRONMENT);
        assertEquals(Integer.valueOf(20), maxPortalPageOrder);
    }

    @Test
    public void shouldHaveEmptyAccessControls() throws TechnicalException {
        Optional<Page> homePage = pageRepository.findById("home");
        assertTrue(homePage.isPresent());
        assertNotNull(homePage.get().getAccessControls());
        assertEquals(0, homePage.get().getAccessControls().size());
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        assertEquals(
            2,
            pageRepository
                .search(new PageCriteria.Builder().referenceType(PageReferenceType.API.name()).referenceId("api-deleted").build())
                .size()
        );

        Map<String, List<String>> deleted = pageRepository.deleteByReferenceIdAndReferenceType("api-deleted", PageReferenceType.API);

        assertEquals(2, deleted.size());
        assertTrue(deleted.get("page-to-be-removed-1").isEmpty());
        assertEquals(2, deleted.get("page-to-be-removed-2").size());
        assertTrue(deleted.get("page-to-be-removed-2").containsAll(List.of("media_id_1", "media_id_2")));
        assertEquals(
            0,
            pageRepository
                .search(new PageCriteria.Builder().referenceType(PageReferenceType.API.name()).referenceId("api-deleted").build())
                .size()
        );
    }

    @Test
    public void should_update_pages_cross_ids() throws Exception {
        // Given existing pages with cross ids updated
        Optional<Page> page3Optional = pageRepository.findById("page3");
        assertTrue(page3Optional.isPresent());
        assertNull(page3Optional.get().getCrossId());
        var page3 = page3Optional.get();
        page3.setCrossId("page3-cross-id-updated");

        Optional<Page> homeOptional = pageRepository.findById("home");
        assertTrue(homeOptional.isPresent());
        assertNull(homeOptional.get().getCrossId());
        var homePage = homeOptional.get();
        homePage.setCrossId("home-cross-id-updated");

        // When updating in the database the cross ids of the plans
        pageRepository.updateCrossIds(List.of(page3, homePage));

        // Then the plans cross IDs are updated
        Optional<Page> updatedPage3 = pageRepository.findById("page3");
        assertTrue(updatedPage3.isPresent());
        assertEquals("page3-cross-id-updated", updatedPage3.get().getCrossId());

        Optional<Page> updatedHomePage = pageRepository.findById("home");
        assertTrue(updatedHomePage.isPresent());
        assertEquals("home-cross-id-updated", updatedHomePage.get().getCrossId());
    }
}
