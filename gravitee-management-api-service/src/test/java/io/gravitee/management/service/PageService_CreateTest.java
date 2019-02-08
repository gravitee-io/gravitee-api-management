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
package io.gravitee.management.service;

import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.PageServiceImpl;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
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
        when(page1.getApi()).thenReturn(API_ID);
        when(page1.getType()).thenReturn(PageType.valueOf(type));
        when(page1.getLastContributor()).thenReturn(contrib);
        when(page1.getOrder()).thenReturn(1);
        when(page1.getContent()).thenReturn(content);

        when(pageRepository.findById(anyString())).thenReturn(Optional.empty());
        when(pageRepository.create(any())).thenReturn(page1);

        when(newPage.getName()).thenReturn(name);
        when(newPage.getOrder()).thenReturn(1);
        when(newPage.getContent()).thenReturn(content);
        when(newPage.getLastContributor()).thenReturn(contrib);
        when(newPage.getType()).thenReturn(io.gravitee.management.model.PageType.SWAGGER);

        final PageEntity createdPage = pageService.createPage(API_ID, newPage);

        verify(pageRepository).create(argThat(new ArgumentMatcher<Page>() {
            public boolean matches(Object argument) {
                final Page pageToCreate = (Page) argument;
                return pageToCreate.getId().split("-").length == 5 &&
                    API_ID.equals(pageToCreate.getApi()) &&
                    name.equals(pageToCreate.getName()) &&
                    contrib.equals(pageToCreate.getLastContributor()) &&
                    content.equals(pageToCreate.getContent()) &&
                    io.gravitee.management.model.PageType.SWAGGER.name().equals(pageToCreate.getType().name()) &&
                    pageToCreate.getCreatedAt() != null &&
                    pageToCreate.getUpdatedAt() != null &&
                    pageToCreate.getCreatedAt().equals(pageToCreate.getUpdatedAt());
            }
        }));
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

        when(pageRepository.findById(anyString())).thenReturn(Optional.empty());
        when(pageRepository.create(any(Page.class))).thenThrow(TechnicalException.class);

        pageService.createPage(API_ID, newPage);

        verify(pageRepository, never()).create(any());
    }

}
