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
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PageService_CreateOrUpdatePagesTest {

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

    @Mock
    private PlanService planService;

    @Test
    public void shouldCreateOrUpdatePages() throws TechnicalException {
        PageEntity page1 = new PageEntity();
        page1.setId(RandomString.generate());
        page1.setName("Page 1");
        page1.setType("SWAGGER");
        page1.setReferenceType("API");
        page1.setReferenceId(API_ID);

        PageEntity page2 = new PageEntity();
        page2.setId(RandomString.generate());
        page2.setName("Page 2");
        page2.setType("MARKDOWN");
        page2.setReferenceType("API");
        page2.setReferenceId(API_ID);

        PageEntity page3 = new PageEntity();
        page3.setId(RandomString.generate());
        page3.setName("Sub Page 3");
        page3.setType("ASCIIDOC");
        page3.setParentId(page1.getId());
        page3.setReferenceType("API");
        page3.setReferenceId(API_ID);

        when(pageRepository.create(any(Page.class))).thenAnswer(returnsFirstArg());
        when(pageRepository.update(any(Page.class))).thenAnswer(returnsFirstArg());

        String page1NewId = RandomString.generateForEnvironment(ENVIRONMENT_ID, API_ID, page1.getId());
        Page page = new Page();
        page.setId(page1NewId);
        page.setName(page1.getName());
        page.setType(page1.getType());
        page.setReferenceType(PageReferenceType.valueOf(page1.getReferenceType()));
        page.setReferenceId(page1.getReferenceId());
        page.setVisibility("PUBLIC");

        // Simulate the fact that page 1 is already created
        when(pageRepository.findById(page1NewId)).thenReturn(Optional.of(page));
        when(pageRepository.findById(argThat(id -> !id.equals(page1NewId)))).thenThrow(new PageNotFoundException(""));

        when(planService.findByApi(anyString())).thenReturn(Collections.emptySet());

        pageService.createOrUpdatePages(List.of(page1, page2, page3), ENVIRONMENT_ID, API_ID);

        ArgumentCaptor<Page> createdPagesCaptor = ArgumentCaptor.forClass(Page.class);

        verify(pageRepository, times(2)).create(createdPagesCaptor.capture());
        List<Page> createdPages = createdPagesCaptor.getAllValues();
        assertThat(createdPages.size()).isEqualTo(2);
        assertThat(createdPages).extracting(Page::getName).containsExactly("Sub Page 3", "Page 2");

        // New parent id of page 3 must be new id of page 1
        assertThat(createdPages.get(0)).extracting(Page::getParentId).isEqualTo(page1NewId);

        assertThat(createdPages).extracting(Page::getId).doesNotContain(page2.getId(), page3.getId());

        ArgumentCaptor<Page> updatedPageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(pageRepository, times(1)).update(updatedPageCaptor.capture());
        List<Page> updatedPages = updatedPageCaptor.getAllValues();
        assertThat(updatedPages.size()).isEqualTo(1);
        assertThat(updatedPages).extracting(Page::getName, Page::getId).containsExactly(tuple("Page 1", page1NewId));
    }
}
