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
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageRevision;
import org.junit.Test;

import java.util.*;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageRevisionRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/page-revision-tests/";
    }

    @Test
    public void shouldFindApiPageRevisionById() throws Exception {
        final Optional<PageRevision> pageRevision = pageRevisionRepository.findById("FindApiPage", 1);

        assertNotNull(pageRevision);
        assertTrue(pageRevision.isPresent());
        assertFindPageRevision(pageRevision.get());
    }

    private void assertFindPageRevision(PageRevision pageRevision) {
        assertEquals("page id", "FindApiPage", pageRevision.getPageId());
        assertEquals("hash algo", "hexstring", pageRevision.getHash());
        assertEquals("revision", 1, pageRevision.getRevision());
        assertEquals("name", "Find apiPage by apiId or Id", pageRevision.getName());
        assertEquals("content", "Content of the page", pageRevision.getContent());

        assertTrue("created at", compareDate(new Date(1486771200000L), pageRevision.getCreatedAt()));
    }

    @Test
    public void shouldCreateApiPageRevision() throws Exception {
        final PageRevision pageRevision = new PageRevision();
        pageRevision.setPageId("new-page");
        pageRevision.setRevision(5);
        pageRevision.setName("Page name");
        pageRevision.setContent("Page content");
        pageRevision.setHash("54646446654");
        pageRevision.setCreatedAt(new Date());

        Optional<PageRevision> optionalBefore = pageRevisionRepository.findById("new-page", 5);
        pageRevisionRepository.create(pageRevision);
        Optional<PageRevision> optionalAfter = pageRevisionRepository.findById("new-page", 5);
        assertFalse("Page not found before", optionalBefore.isPresent());
        assertTrue("Page saved not found", optionalAfter.isPresent());

        final PageRevision pageSaved = optionalAfter.get();
        assertEquals("Invalid saved pageRevision name.", pageRevision.getName(), pageSaved.getName());
        assertEquals("Invalid pageRevision content.", pageRevision.getContent(), pageSaved.getContent());
        assertEquals("Invalid pageRevision pageId.", pageRevision.getPageId(), pageSaved.getPageId());
        assertEquals("Invalid pageRevision hash.", pageRevision.getHash(), pageSaved.getHash());
        assertEquals("Invalid pageRevision revision.", pageRevision.getRevision(), pageSaved.getRevision());
    }
    @Test
    public void shouldDeleteAllByPageId() throws Exception {
        List<PageRevision> pageShouldExists = pageRevisionRepository.findAllByPageId("deleteByPageId");
        pageRevisionRepository.deleteAllByPageId("deleteByPageId");
        List<PageRevision> pageShouldNotExists =  pageRevisionRepository.findAllByPageId("deleteByPageId");

        assertTrue("should exists before delete", pageShouldExists != null && !pageShouldExists.isEmpty());
        assertTrue("should not exists after delete",pageShouldNotExists != null && pageShouldNotExists.isEmpty());
    }

    @Test
    public void shouldDeleteAllByPageId_unknownId() throws Exception {
        List<PageRevision> pageShouldNotExists =  pageRevisionRepository.findAllByPageId("deleteByPageId_unknown");
        pageRevisionRepository.deleteAllByPageId("deleteByPageId_unknown"); // should not throw exception
        assertTrue("should not exists after delete",pageShouldNotExists != null && pageShouldNotExists.isEmpty());
    }

    @Test
    public void shouldFindAllByPageId() throws Exception {
        List<PageRevision> pageShouldExists = pageRevisionRepository.findAllByPageId("findByPageId");

        assertNotNull(pageShouldExists);
        assertEquals(3, pageShouldExists.size());
        for (PageRevision rev : pageShouldExists) {
            assertEquals("findByPageId", rev.getPageId());
        }
    }

    @Test
    public void shouldNotFindAllByPageId() throws Exception {
        List<PageRevision> pageShouldExists = pageRevisionRepository.findAllByPageId("findByPageId_unknown");

        assertNotNull(pageShouldExists);
        assertEquals(0, pageShouldExists.size());
    }

    @Test
    public void shouldFindLastByPageId() throws Exception {
        Optional<PageRevision> pageShouldExists = pageRevisionRepository.findLastByPageId("findByPageId");

        assertNotNull(pageShouldExists);
        assertTrue(pageShouldExists.isPresent());
        assertEquals("findByPageId", pageShouldExists.get().getPageId());
        assertEquals(3, pageShouldExists.get().getRevision());
        assertEquals("lorem ipsum", pageShouldExists.get().getContent());
        assertEquals("revision 3", pageShouldExists.get().getName());
        assertEquals("findByPageId", pageShouldExists.get().getPageId());
    }

    @Test
    public void shouldNotFindLastByPageId() throws Exception {
        Optional<PageRevision> pageShouldExists = pageRevisionRepository.findLastByPageId("findByPageId_unknown");

        assertNotNull(pageShouldExists);
        assertFalse(pageShouldExists.isPresent());
    }
}
