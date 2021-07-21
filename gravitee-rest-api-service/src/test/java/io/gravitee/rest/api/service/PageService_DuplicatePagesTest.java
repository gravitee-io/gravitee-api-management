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
package io.gravitee.rest.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PageService_DuplicatePagesTest {

    private static final String API_ID = "myAPI";
    private static final String ENVIRONMENT_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

    @InjectMocks
    private final PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private PageRevisionService pageRevisionService;

    @Mock
    private AuditService auditService;

    @Captor
    ArgumentCaptor<Page> pageCaptor;

    @Test
    public void shouldDuplicatePages() throws TechnicalException {
        PageEntity page1 = new PageEntity();
        page1.setId(RandomString.generate());
        page1.setName("Page 1");
        page1.setType("SWAGGER");

        PageEntity page2 = new PageEntity();
        page2.setId(RandomString.generate());
        page2.setName("Page 2");
        page2.setType("MARKDOWN");

        PageEntity page3 = new PageEntity();
        page3.setId(RandomString.generate());
        page3.setName("Sub Page 3");
        page3.setType("ASCIIDOC");
        page3.setParentId(page2.getId());

        when(pageRepository.create(any(Page.class))).thenAnswer(returnsFirstArg());

        pageService.duplicatePages(List.of(page1, page2, page3), ENVIRONMENT_ID, API_ID);

        verify(pageRepository, times(3)).create(pageCaptor.capture());

        List<Page> createdPages = pageCaptor.getAllValues();
        assertThat(createdPages.size()).isEqualTo(3);
        assertThat(createdPages).extracting(Page::getName).containsExactlyInAnyOrder("Page 1", "Page 2", "Sub Page 3");

        assertThat(createdPages).extracting(Page::getId).doesNotContain(page1.getId(), page2.getId(), page3.getId());

        // New parent id of page 3 must be new id of page 2
        assertThat(createdPages.get(2)).extracting(Page::getParentId).isEqualTo(createdPages.get(1).getId());
    }
}
