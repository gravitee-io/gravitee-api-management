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
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PageRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/page-tests/";
    }

    @Test
    public void shouldFindByApi() throws Exception {
        final Collection<Page> pages = pageRepository.findByApi("my-api");

        assertNotNull(pages);
        assertEquals(3, pages.size());
    }

    @Test
    public void shouldCreate() throws Exception {
        final Page page = new Page();
        page.setId("new-page");
        page.setName("Page name");
        page.setContent("Page content");
        page.setOrder(0);
        page.setApi("my-api");
        page.setType(PageType.MARKDOWN);
        page.setCreatedAt(new Date());
        page.setUpdatedAt(new Date());

        int nbPagesBeforeCreation = pageRepository.findByApi("my-api").size();
        pageRepository.create(page);
        int nbPagesAfterCreation = pageRepository.findByApi("my-api").size();

        Assert.assertEquals(nbPagesBeforeCreation + 1, nbPagesAfterCreation);

        Optional<Page> optional = pageRepository.findById("new-page");
        Assert.assertTrue("Page saved not found", optional.isPresent());

        final Page pageSaved = optional.get();
        Assert.assertEquals("Invalid saved page name.", page.getName(), pageSaved.getName());
        Assert.assertEquals("Invalid page content.", page.getContent(), pageSaved.getContent());
        Assert.assertEquals("Invalid page order.", page.getOrder(), pageSaved.getOrder());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Page> optional = pageRepository.findById("page2");
        Assert.assertTrue("Page to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved page name.", "Page 2", optional.get().getName());

        final Page page = optional.get();
        page.setName("New page");
        page.setContent("New content");

        int nbPagesBeforeUpdate = pageRepository.findByApi("my-api").size();
        pageRepository.update(page);
        int nbPagesAfterUpdate = pageRepository.findByApi("my-api").size();

        Assert.assertEquals(nbPagesBeforeUpdate, nbPagesAfterUpdate);

        Optional<Page> optionalUpdated = pageRepository.findById("page2");
        Assert.assertTrue("Page to update not found", optionalUpdated.isPresent());

        final Page pageUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved page name.", "New page", pageUpdated.getName());
        Assert.assertEquals("Invalid page content.", "New content", pageUpdated.getContent());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbPagesBeforeDeletion = pageRepository.findByApi("my-api").size();
        pageRepository.delete("page1");
        int nbPagesAfterDeletion = pageRepository.findByApi("my-api").size();

        Assert.assertEquals(nbPagesBeforeDeletion - 1, nbPagesAfterDeletion);
    }
}
