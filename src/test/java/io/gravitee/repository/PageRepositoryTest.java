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
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageType;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class PageRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/page-tests/";
    }

    @Test
    public void shouldFindApiPageByApiId() throws Exception {
        final Collection<Page> pages = pageRepository.findApiPageByApiId("my-api");

        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertFindPage(pages.iterator().next());
    }

    @Test
    public void shouldFindApiPageById() throws Exception {
        final Optional<Page> page = pageRepository.findById("FindApiPage");

        assertNotNull(page);
        assertTrue(page.isPresent());
        assertFindPage(page.get());
    }

    private void assertFindPage(Page page) {
        assertEquals("id", "FindApiPage", page.getId());
        assertEquals("name", "Find apiPage by apiId or Id", page.getName());
        assertEquals("content", "Content of the page", page.getContent());
        assertEquals("api", "my-api", page.getApi());
        assertEquals("type", PageType.MARKDOWN, page.getType());
        assertEquals("last contributor", "john_doe", page.getLastContributor());
        assertEquals("order", 2, page.getOrder());
        assertTrue("published", page.isPublished());
        assertEquals("source type", "sourceType", page.getSource().getType());
        assertEquals("source configuration", "sourceConfiguration", page.getSource().getConfiguration());
        assertTrue("configuration try it", page.getConfiguration().isTryIt());
        assertEquals("configuration try it URL", "http://company.com", page.getConfiguration().getTryItURL());
        assertTrue("homepage", page.isHomepage());
        assertEquals("excludedGroups", Arrays.asList("grp1", "grp2"), page.getExcludedGroups());
        assertEquals("created at", new Date(1486771200000L), page.getCreatedAt());
        assertEquals("updated at", new Date(1486771200000L), page.getUpdatedAt());
    }

    @Test
    public void shouldCreateApiPage() throws Exception {
        final Page page = new Page();
        page.setId("new-page");
        page.setName("Page name");
        page.setContent("Page content");
        page.setOrder(3);
        page.setApi("my-api");
        page.setHomepage(true);
        page.setType(PageType.MARKDOWN);
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());

        Optional<Page> optionalBefore = pageRepository.findById("new-page");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-page");
        assertFalse("Page not found before", optionalBefore.isPresent());
        assertTrue("Page saved not found", optionalAfter.isPresent());

        final Page pageSaved = optionalAfter.get();
        assertEquals("Invalid saved page name.", page.getName(), pageSaved.getName());
        assertEquals("Invalid page content.", page.getContent(), pageSaved.getContent());
        assertEquals("Invalid page order.", page.getOrder(), pageSaved.getOrder());
        assertEquals("Invalid page type.", page.getType(), pageSaved.getType());
        assertEquals("Invalid homepage flag.", page.isHomepage(), pageSaved.isHomepage());
        assertNull("Invalid page source.", page.getSource());
    }

    @Test
    public void shouldCreatePortalPage() throws Exception {
        final Page page = new Page();
        page.setId("new-portal-page");
        page.setName("Page name");
        page.setContent("Page content");
        page.setOrder(3);
        page.setType(PageType.MARKDOWN);
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());

        Optional<Page> optionalBefore = pageRepository.findById("new-portal-page");
        pageRepository.create(page);
        Optional<Page> optionalAfter = pageRepository.findById("new-portal-page");
        assertFalse("Page not found before", optionalBefore.isPresent());
        assertTrue("Page saved not found", optionalAfter.isPresent());

        final Page pageSaved = optionalAfter.get();
        assertEquals("Invalid saved page name.", page.getName(), pageSaved.getName());
        assertEquals("Invalid page content.", page.getContent(), pageSaved.getContent());
        assertEquals("Invalid page order.", page.getOrder(), pageSaved.getOrder());
        assertEquals("Invalid page type.", page.getType(), pageSaved.getType());
        assertEquals("Invalid homepage flag.", page.isHomepage(), pageSaved.isHomepage());
        assertNull("Invalid page source.", page.getSource());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Page> optionalBefore = pageRepository.findById("updatePage");
        assertTrue("Page to update not found", optionalBefore.isPresent());
        assertEquals("Invalid page name.", "Update Page", optionalBefore.get().getName());
        assertEquals("Invalid page content.", "Content of the update page", optionalBefore.get().getContent());
        final Page page = optionalBefore.get();
        page.setName("New name");
        page.setContent("New content");

        pageRepository.update(page);

        Optional<Page> optionalUpdated = pageRepository.findById("updatePage");
        assertTrue("Page to update not found", optionalUpdated.isPresent());
        assertEquals("Invalid saved page name.", "New name", optionalUpdated.get().getName());
        assertEquals("Invalid page content.", "New content", optionalUpdated.get().getContent());
    }

    @Test
    public void shouldDelete() throws Exception {
        Optional<Page> pageShouldExists = pageRepository.findById("page-to-be-deleted");
        pageRepository.delete("page-to-be-deleted");
        Optional<Page> pageShouldNotExists = pageRepository.findById("page-to-be-deleted");

        assertTrue("should exists before delete", pageShouldExists.isPresent());
        assertFalse("should not exists after delete", pageShouldNotExists.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownPage() throws Exception {
        Page unknownPage = new Page();
        unknownPage.setId("unknown");
        pageRepository.update(unknownPage);
        fail("An unknown page should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        pageRepository.update(null);
        fail("A null page should not be updated");
    }

    @Test
    public void shouldFindApiPageByApiIdAndHomepageFalse() throws Exception {
        Collection<Page> pages = pageRepository.findApiPageByApiIdAndHomepage("my-api-2", false);
        assertNotNull(pages);
        assertEquals(2, pages.size());
    }

    @Test
    public void shouldFindApiPageByApiIdAndHomepageTrue() throws Exception {
        Collection<Page> pages = pageRepository.findApiPageByApiIdAndHomepage("my-api-2", true);
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("home", pages.iterator().next().getId());
    }


    @Test
    public void shouldFindPortalPages() throws Exception {
        Collection<Page> pages = pageRepository.findPortalPages();
        assertNotNull(pages);
        assertEquals(2, pages.size());
        Set<String> ids = pages.stream().map(Page::getId).collect(Collectors.toSet());
        assertTrue(ids.contains("FindPortalPage-homepage"));
        assertTrue(ids.contains("FindPortalPage-nothomepage"));
    }

    @Test
    public void shouldFindPortalPageByHomepageFalse() throws Exception {
        Collection<Page> pages = pageRepository.findPortalPageByHomepage(false);
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("FindPortalPage-nothomepage", pages.iterator().next().getId());
    }

    @Test
    public void shouldFindPortalPageByHomepageTrue() throws Exception {
        Collection<Page> pages = pageRepository.findPortalPageByHomepage(true);
        assertNotNull(pages);
        assertEquals(1, pages.size());
        assertEquals("FindPortalPage-homepage", pages.iterator().next().getId());
    }

    @Test
    public void shouldFindMaxApiPageOrderByApiId() throws TechnicalException {
        Integer maxApiPageOrderByApiId = pageRepository.findMaxApiPageOrderByApiId("my-api-2");
        assertEquals(Integer.valueOf(2), maxApiPageOrderByApiId);
    }

    @Test
    public void shouldFindDefaultMaxApiPageOrderByApiId() throws TechnicalException {
        Integer maxApiPageOrderByApiId = pageRepository.findMaxApiPageOrderByApiId("unknown api id");
        assertEquals(Integer.valueOf(0), maxApiPageOrderByApiId);
    }

    @Test
    public void shouldFindMaxPortalPageOrder() throws TechnicalException {
        Integer maxPortalPageOrder = pageRepository.findMaxPortalPageOrder();
        assertEquals(Integer.valueOf(20), maxPortalPageOrder);
    }
}
