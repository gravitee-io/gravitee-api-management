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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.PageRevision;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.Test;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageRevisionRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/page-revision-tests/";
    }

    @Test
    public void shouldFindApiPageRevisionById() throws Exception {
        final Optional<PageRevision> pageRevision = pageRevisionRepository.findById("FindApiPage", 1);

        assertThat(pageRevision).isPresent();
        assertFindPageRevision(pageRevision.get());
    }

    private void assertFindPageRevision(PageRevision pageRevision) {
        assertThat(pageRevision.getPageId()).isEqualTo("FindApiPage");
        assertThat(pageRevision.getHash()).isEqualTo("hexstring");
        assertThat(pageRevision.getRevision()).isEqualTo(1);
        assertThat(pageRevision.getName()).isEqualTo("Find apiPage by apiId or Id");
        assertThat(pageRevision.getContent()).isEqualTo("Content of the page");

        assertThat(pageRevision.getCreatedAt()).isEqualTo(Instant.ofEpochMilli(1486771200000L));
    }

    @Test
    public void shouldFindAll_MaxInteger() throws TechnicalException {
        Page<PageRevision> revisions = pageRevisionRepository.findAll(
            new PageableBuilder().pageNumber(0).pageSize(Integer.MAX_VALUE).build()
        );
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(revisions).isNotNull();
            softly.assertThat(revisions.getPageNumber()).isEqualTo(0);
            softly.assertThat(revisions.getPageElements()).isEqualTo(8);
            softly.assertThat(revisions.getTotalElements()).isEqualTo(8);
            softly.assertThat(revisions.getContent()).hasSize(8);
        }
    }

    @Test
    public void shouldFindAll_PageSize4() throws TechnicalException {
        int pageNumber = 0;
        do {
            Page<PageRevision> revisions = pageRevisionRepository.findAll(new PageableBuilder().pageNumber(pageNumber).pageSize(4).build());

            try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
                softly.assertThat(revisions).isNotNull();
                softly.assertThat(revisions.getContent()).hasSize(4);
                softly.assertThat(revisions.getPageNumber()).isEqualTo(pageNumber);
                softly.assertThat(revisions.getPageElements()).isEqualTo(4);
                softly.assertThat(revisions.getTotalElements()).isEqualTo(8);
            }
        } while (++pageNumber < 2);
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
        assertThat(optionalBefore).isEmpty();
        assertThat(optionalAfter).isPresent();

        final PageRevision pageSaved = optionalAfter.get();
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(pageSaved.getName()).isEqualTo(pageRevision.getName());
            softly.assertThat(pageSaved.getContent()).isEqualTo(pageRevision.getContent());
            softly.assertThat(pageSaved.getPageId()).isEqualTo(pageRevision.getPageId());
            softly.assertThat(pageSaved.getHash()).isEqualTo(pageRevision.getHash());
            softly.assertThat(pageSaved.getRevision()).isEqualTo(pageRevision.getRevision());
        }
    }

    @Test
    public void shouldFindAllByPageId() throws Exception {
        List<PageRevision> pageShouldExists = pageRevisionRepository.findAllByPageId("findByPageId");

        assertThat(pageShouldExists).hasSize(3).extracting(PageRevision::getPageId).containsOnly("findByPageId");
    }

    @Test
    public void shouldNotFindAllByPageId() throws Exception {
        List<PageRevision> pageShouldExists = pageRevisionRepository.findAllByPageId("findByPageId_unknown");

        assertThat(pageShouldExists).isNotNull().isEmpty();
    }

    @Test
    public void shouldFindLastByPageId() throws Exception {
        Optional<PageRevision> pageShouldExists = pageRevisionRepository.findLastByPageId("findByPageId");

        assertThat(pageShouldExists).isNotNull().isPresent();
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(pageShouldExists.get().getPageId()).isEqualTo("findByPageId");
            softly.assertThat(pageShouldExists.get().getRevision()).isEqualTo(3);
            softly.assertThat(pageShouldExists.get().getContent()).isEqualTo("lorem ipsum");
            softly.assertThat(pageShouldExists.get().getName()).isEqualTo("revision 3");
            softly.assertThat(pageShouldExists.get().getPageId()).isEqualTo("findByPageId");
        }
    }

    @Test
    public void shouldNotFindLastByPageId() throws Exception {
        Optional<PageRevision> pageShouldExists = pageRevisionRepository.findLastByPageId("findByPageId_unknown");

        assertThat(pageShouldExists).isNotNull().isNotPresent();
    }

    @Test
    public void should_delete_by_page_id() throws Exception {
        int nbBeforeDeletion = pageRevisionRepository.findAllByPageId("ToBeDeleted").size();
        List<String> deleted = pageRevisionRepository.deleteByPageId("ToBeDeleted");
        int nbAfterDeletion = pageRevisionRepository.findAllByPageId("ToBeDeleted").size();

        assertThat(nbBeforeDeletion).isEqualTo(2);
        assertThat(deleted.size()).isEqualTo(2);
        assertThat(nbAfterDeletion).isEqualTo(0);
    }

    @Test
    public void shouldDeleteAllByPageId() throws TechnicalException {
        List<PageRevision> revisionsBefore = pageRevisionRepository.findAllByPageId("findByPageId");
        assertNotNull(revisionsBefore);
        assertEquals(3, revisionsBefore.size());

        pageRevisionRepository.deleteAllByPageId("findByPageId");

        List<PageRevision> revisionsAfter = pageRevisionRepository.findAllByPageId("findByPageId");
        assertNotNull(revisionsAfter);
        assertEquals(0, revisionsAfter.size());
    }

    @Test
    public void shouldDoNothingWhenNoRevisionsFoundWhileDeleting() throws TechnicalException {
        pageRevisionRepository.deleteAllByPageId("nonExistingPageId");

        List<PageRevision> revisionsAfter = pageRevisionRepository.findAllByPageId("nonExistingPageId");
        assertNotNull(revisionsAfter);
        assertEquals(0, revisionsAfter.size());
    }
}
