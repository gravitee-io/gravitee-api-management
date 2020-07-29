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
package io.gravitee.repository.config.mock;

import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.PageRevisionRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageRevision;
import io.gravitee.repository.management.model.PageSource;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageRevisionRepositoryMock extends AbstractRepositoryMock<PageRevisionRepository> {

    public PageRevisionRepositoryMock() {
        super(PageRevisionRepository.class);
    }

    @Override
    void prepare(PageRevisionRepository pageRepository) throws Exception {
        PageRevision findApiPage = mock(PageRevision.class);
        when(findApiPage.getPageId()).thenReturn("FindApiPage");
        when(findApiPage.getRevision()).thenReturn(1);
        when(findApiPage.getContributor()).thenReturn("john_doe");
        when(findApiPage.getName()).thenReturn("Find apiPage by apiId or Id");
        when(findApiPage.getContent()).thenReturn("Content of the page");
        when(findApiPage.getHash()).thenReturn("hexstring");
        when(findApiPage.getCreatedAt()).thenReturn(new Date(1486771200000L));

        // shouldFindApiPageById
        when(pageRepository.findById("FindApiPage", 1)).thenReturn(of(findApiPage));

        // shouldCreateApiPage
        final PageRevision createPage = mock(PageRevision.class);
        when(createPage.getName()).thenReturn("Page name");
        when(createPage.getContent()).thenReturn("Page content");
        when(createPage.getPageId()).thenReturn("new-page");
        when(createPage.getRevision()).thenReturn(5);
        when(createPage.getHash()).thenReturn("54646446654");

        when(pageRepository.findById("new-page", 5)).thenReturn(empty(), of(createPage));

        // shouldDeleteAllByPageId & then unknown
        when(pageRepository.findAllByPageId("deleteByPageId")).thenReturn(Arrays.asList(mock(PageRevision.class)), emptyList());

        when(pageRepository.findAllByPageId("deleteByPageId_unknown")).thenReturn(emptyList());


        // findAllByPageId
        PageRevision rev = mock(PageRevision.class);
        when(rev.getPageId()).thenReturn("findByPageId");
        when(pageRepository.findAllByPageId("findByPageId")).thenReturn(Arrays.asList(rev, rev, rev));

        //shouldNotFindAllByPageId
        when(pageRepository.findAllByPageId("findByPageId_unknown")).thenReturn(Collections.emptyList());

        // shouldFindLastByPageId
        PageRevision lastRev = mock(PageRevision.class);
        when(lastRev.getPageId()).thenReturn("findByPageId");
        when(lastRev.getContent()).thenReturn("lorem ipsum");
        when(lastRev.getName()).thenReturn("revision 3");
        when(lastRev.getRevision()).thenReturn(3);
        when(pageRepository.findLastByPageId("findByPageId")).thenReturn(of(lastRev));

        // shouldNotFindLastByPageId

        when(pageRepository.findLastByPageId("findByPageId_unknown")).thenReturn(empty());
    }
}
