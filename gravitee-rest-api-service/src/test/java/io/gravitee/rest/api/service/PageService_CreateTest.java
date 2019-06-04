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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.PageType;
import io.gravitee.rest.api.model.NewPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_CreateTest {

    private static final String API_ID = "myAPI";
    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private NewPageEntity newPage;
    @Mock
    private Page page1;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Test
    public void shouldCreatePage() throws TechnicalException {
        final String name = "MARKDOWN";
        final String contrib = "contrib";
        final String content = "content";
        final String type = "MARKDOWN";

        when(page1.getId()).thenReturn(PAGE_ID);
        when(page1.getName()).thenReturn(name);
        when(page1.getReferenceId()).thenReturn(API_ID);
        when(page1.getType()).thenReturn(PageType.valueOf(type));
        when(page1.getLastContributor()).thenReturn(contrib);
        when(page1.getOrder()).thenReturn(1);
        when(page1.getContent()).thenReturn(content);

        when(pageRepository.create(any())).thenReturn(page1);

        when(newPage.getName()).thenReturn(name);
        when(newPage.getOrder()).thenReturn(1);
        when(newPage.getContent()).thenReturn(content);
        when(newPage.getLastContributor()).thenReturn(contrib);
        when(newPage.getType()).thenReturn(io.gravitee.rest.api.model.PageType.SWAGGER);

        final PageEntity createdPage = pageService.createPage(API_ID, newPage);

        verify(pageRepository).create(argThat(pageToCreate -> pageToCreate.getId().split("-").length == 5 &&
            API_ID.equals(pageToCreate.getReferenceId()) &&
            PageReferenceType.API.equals(pageToCreate.getReferenceType()) &&
            name.equals(pageToCreate.getName()) &&
            contrib.equals(pageToCreate.getLastContributor()) &&
            content.equals(pageToCreate.getContent()) &&
            io.gravitee.rest.api.model.PageType.SWAGGER.name().equals(pageToCreate.getType().name()) &&
            pageToCreate.getCreatedAt() != null &&
            pageToCreate.getUpdatedAt() != null &&
            pageToCreate.getCreatedAt().equals(pageToCreate.getUpdatedAt())));
        assertNotNull(createdPage);
        assertEquals(5, createdPage.getId().split("-").length);
        assertEquals(1, createdPage.getOrder());
        assertEquals(content, createdPage.getContent());
        assertEquals(contrib, createdPage.getLastContributor());
        assertEquals(type, createdPage.getType());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseTechnicalException() throws TechnicalException {
        final String name = "PAGE_NAME";
        when(newPage.getName()).thenReturn(name);

        when(pageRepository.create(any(Page.class))).thenThrow(TechnicalException.class);

        pageService.createPage(API_ID, newPage);

        verify(pageRepository, never()).create(any());
    }

}
