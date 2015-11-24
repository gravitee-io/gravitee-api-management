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
import io.gravitee.management.model.PageListItem;
import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.service.exceptions.PageAlreadyExistsException;
import io.gravitee.management.service.exceptions.PageNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.IdGeneratorImpl;
import io.gravitee.management.service.impl.PageServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PageServiceTest {

    private static final String API_NAME = "myAPI";
    private static final String PAGE_NAME = "myPage";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private NewPageEntity newPage;
    @Mock
    private UpdatePageEntity existingPage;
    @Mock
    private Page page1;
    @Mock
    private Page page2;

    @Before
    public void setUp() {
        pageService.setIdGenerator(new IdGeneratorImpl());
    }

    @Test
    public void shouldFindByApi() throws TechnicalException {
        final Set<Page> pages = new HashSet<>();
        pages.add(page1);
        pages.add(page2);

        when(page1.getId()).thenReturn("Page 1");
        when(page2.getId()).thenReturn("Page 2");
        when(page1.getOrder()).thenReturn(1);
        when(page2.getOrder()).thenReturn(2);
        when(pageRepository.findByApi(API_NAME)).thenReturn(pages);

        final List<PageListItem> pageEntities = pageService.findByApi(API_NAME);

        assertNotNull(pageEntities);
        assertEquals(2, pageEntities.size());
        assertEquals(1, pageEntities.iterator().next().getOrder());
    }

    @Test
    public void shouldNotFindByApiBecauseNotFound() throws TechnicalException {
        when(pageRepository.findByApi(API_NAME)).thenReturn(null);

        final List<PageListItem> pageEntities = pageService.findByApi(API_NAME);

        assertNotNull(pageEntities);
        assertTrue(pageEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApiNameBecauseTechnicalException() throws TechnicalException {
        when(pageRepository.findByApi(API_NAME)).thenThrow(TechnicalException.class);

        pageService.findByApi(API_NAME);
    }

    @Test
    public void shouldFindByName() throws TechnicalException {
        when(page1.getName()).thenReturn(PAGE_NAME);
        when(page1.getOrder()).thenReturn(1);
        when(pageRepository.findById(PAGE_NAME)).thenReturn(Optional.of(page1));

        final PageEntity pageEntity = pageService.findById(PAGE_NAME);

        assertNotNull(pageEntity);
        assertEquals(1, pageEntity.getOrder());
        assertEquals(PAGE_NAME, pageEntity.getName());
    }

    @Test(expected = PageNotFoundException.class)
    public void shouldNotFindByNameBecauseNotFound() throws TechnicalException {
        when(pageRepository.findById(PAGE_NAME)).thenReturn(Optional.empty());

        pageService.findById(PAGE_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByNameBecauseTechnicalException() throws TechnicalException {
        when(pageRepository.findById(PAGE_NAME)).thenThrow(TechnicalException.class);

        pageService.findById(PAGE_NAME);
    }

    @Test
    public void shouldCreatePage() throws TechnicalException {
        final String title = "title";
        final String contrib = "contrib";
        final String content = "content";
        final String type = "MARKDOWN";

        when(page1.getName()).thenReturn(PAGE_NAME);
        when(page1.getApi()).thenReturn(API_NAME);
        when(page1.getType()).thenReturn("MARKDOWN");
        when(page1.getTitle()).thenReturn(title);
        when(page1.getLastContributor()).thenReturn(contrib);
        when(page1.getOrder()).thenReturn(1);
        when(page1.getContent()).thenReturn(content);

        when(pageRepository.create(any())).thenReturn(page1);
        when(pageRepository.findById(PAGE_NAME)).thenReturn(Optional.empty());

        when(newPage.getName()).thenReturn(PAGE_NAME);
        when(newPage.getOrder()).thenReturn(1);
        when(newPage.getContent()).thenReturn(content);
        when(newPage.getLastContributor()).thenReturn(contrib);
        when(newPage.getTitle()).thenReturn(title);
        when(newPage.getType()).thenReturn(type);

        final PageEntity createdPage = pageService.create(API_NAME, newPage);

        verify(pageRepository).create(argThat(new ArgumentMatcher<Page>() {
            public boolean matches(Object argument) {
                final Page pageToCreate = (Page) argument;
                return PAGE_NAME.equals(pageToCreate.getName()) &&
                    API_NAME.equals(pageToCreate.getApi()) &&
                    title.equals(pageToCreate.getTitle()) &&
                    contrib.equals(pageToCreate.getLastContributor()) &&
                    content.equals(pageToCreate.getContent()) &&
                    type.equals(pageToCreate.getType()) &&
                    pageToCreate.getCreatedAt() != null &&
                    pageToCreate.getUpdatedAt() != null &&
                    pageToCreate.getCreatedAt().equals(pageToCreate.getUpdatedAt());
            }
        }));
        assertNotNull(createdPage);
        assertEquals(PAGE_NAME, createdPage.getName());
        assertEquals(1, createdPage.getOrder());
        assertEquals(content, createdPage.getContent());
        assertEquals(contrib, createdPage.getLastContributor());
        assertEquals(title, createdPage.getTitle());
        assertEquals(type, createdPage.getType());
    }

    @Test(expected = PageAlreadyExistsException.class)
    public void shouldNotCreateBecauseExists() throws TechnicalException {
        when(newPage.getName()).thenReturn(PAGE_NAME);
        when(pageRepository.findById(PAGE_NAME)).thenReturn(Optional.of(new Page()));

        pageService.create(API_NAME, newPage);

        verify(pageRepository, never()).create(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseTechnicalException() throws TechnicalException {
        when(newPage.getName()).thenReturn(PAGE_NAME);
        when(pageRepository.findById(PAGE_NAME)).thenReturn(Optional.empty());
        when(pageRepository.create(any(Page.class))).thenThrow(TechnicalException.class);

        pageService.create(API_NAME, newPage);

        verify(pageRepository, never()).create(any());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(pageRepository.findById(PAGE_NAME)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenReturn(page1);

        pageService.update(PAGE_NAME, existingPage);

        verify(pageRepository).update(argThat(new ArgumentMatcher<Page>() {
            public boolean matches(Object argument) {
                final Page pageToUpdate = (Page) argument;
                return PAGE_NAME.equals(pageToUpdate.getName()) &&
                    pageToUpdate.getUpdatedAt() != null;
            }
        }));
    }

    @Test(expected = PageNotFoundException.class)
    public void shouldNotUpdateBecauseNotExists() throws TechnicalException {
        when(pageRepository.findById(PAGE_NAME)).thenReturn(Optional.empty());

        pageService.update(PAGE_NAME, existingPage);

        verify(pageRepository, never()).update(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        when(pageRepository.findById(PAGE_NAME)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenThrow(TechnicalException.class);

        pageService.update(PAGE_NAME, existingPage);

        verify(pageRepository, never()).update(any());
    }

    @Test
    public void shouldDeletePage() throws TechnicalException {
        pageService.delete(PAGE_NAME);

        verify(pageRepository).delete(PAGE_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeletePageBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(pageRepository).delete(PAGE_NAME);

        pageService.delete(PAGE_NAME);
    }

    @Test
    public void shouldFindMaxPageOrderByApiName() throws TechnicalException {
        when(pageRepository.findMaxPageOrderByApi(API_NAME)).thenReturn(10);

        assertEquals(10, pageService.findMaxPageOrderByApi(API_NAME));
    }

    @Test
    public void shouldFindMaxPageOrderByApiNameWhenNull() throws TechnicalException {
        when(pageRepository.findMaxPageOrderByApi(API_NAME)).thenReturn(null);

        assertEquals(0, pageService.findMaxPageOrderByApi(API_NAME));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindMaxPageOrderByApiNameBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(pageRepository).findMaxPageOrderByApi(API_NAME);

        pageService.findMaxPageOrderByApi(API_NAME);
    }
}
