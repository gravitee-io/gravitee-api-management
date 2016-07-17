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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.gravitee.repository.management.model.PageType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.gravitee.management.model.NewPageEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.PageListItem;
import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.service.exceptions.PageAlreadyExistsException;
import io.gravitee.management.service.exceptions.PageNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.PageServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PageServiceTest {

    private static final String API_ID = "myAPI";
    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

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

    @Test
    public void shouldFindByApi() throws TechnicalException {
        final Set<Page> pages = new HashSet<>();
        pages.add(page1);
        pages.add(page2);

        when(page1.getId()).thenReturn("Page 1");
        when(page2.getId()).thenReturn("Page 2");
        when(page1.getOrder()).thenReturn(1);
        when(page1.getType()).thenReturn(PageType.MARKDOWN);
        when(page2.getOrder()).thenReturn(2);
        when(page2.getType()).thenReturn(PageType.RAML);
        when(pageRepository.findByApi(API_ID)).thenReturn(pages);

        final List<PageListItem> pageEntities = pageService.findByApi(API_ID);

        assertNotNull(pageEntities);
        assertEquals(2, pageEntities.size());
        assertEquals(1, pageEntities.iterator().next().getOrder());
    }

    @Test
    public void shouldNotFindByApiBecauseNotFound() throws TechnicalException {
        when(pageRepository.findByApi(API_ID)).thenReturn(null);

        final List<PageListItem> pageEntities = pageService.findByApi(API_ID);

        assertNotNull(pageEntities);
        assertTrue(pageEntities.isEmpty());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApiNameBecauseTechnicalException() throws TechnicalException {
        when(pageRepository.findByApi(API_ID)).thenThrow(TechnicalException.class);

        pageService.findByApi(API_ID);
    }

    @Test
    public void shouldFindByName() throws TechnicalException {
        when(page1.getId()).thenReturn(PAGE_ID);
        when(page1.getOrder()).thenReturn(1);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));

        final PageEntity pageEntity = pageService.findById(PAGE_ID);

        assertNotNull(pageEntity);
        assertEquals(1, pageEntity.getOrder());
        assertEquals(PAGE_ID, pageEntity.getId());
    }

    @Test(expected = PageNotFoundException.class)
    public void shouldNotFindByNameBecauseNotFound() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.empty());

        pageService.findById(PAGE_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByNameBecauseTechnicalException() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenThrow(TechnicalException.class);

        pageService.findById(PAGE_ID);
    }

    @Test
    public void shouldCreatePage() throws TechnicalException {
        final String name = "MARKDOWN";
        final String contrib = "contrib";
        final String content = "content";
        final String type = "MARKDOWN";

        when(page1.getId()).thenReturn(PAGE_ID);
        when(page1.getName()).thenReturn(name);
        when(page1.getApi()).thenReturn(API_ID);
        when(page1.getType()).thenReturn(io.gravitee.repository.management.model.PageType.valueOf(type));
        when(page1.getLastContributor()).thenReturn(contrib);
        when(page1.getOrder()).thenReturn(1);
        when(page1.getContent()).thenReturn(content);

        when(pageRepository.findById(anyString())).thenReturn(Optional.empty());
        when(pageRepository.create(any())).thenReturn(page1);

        when(newPage.getName()).thenReturn(name);
        when(newPage.getOrder()).thenReturn(1);
        when(newPage.getContent()).thenReturn(content);
        when(newPage.getLastContributor()).thenReturn(contrib);
        when(newPage.getType()).thenReturn(type);

        final PageEntity createdPage = pageService.create(API_ID, newPage);

        verify(pageRepository).create(argThat(new ArgumentMatcher<Page>() {
            public boolean matches(Object argument) {
                final Page pageToCreate = (Page) argument;
                return pageToCreate.getId().split("-").length == 5 &&
                    API_ID.equals(pageToCreate.getApi()) &&
                    name.equals(pageToCreate.getName()) &&
                    contrib.equals(pageToCreate.getLastContributor()) &&
                    content.equals(pageToCreate.getContent()) &&
                    type.equals(pageToCreate.getType().name()) &&
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

    @Test(expected = PageAlreadyExistsException.class)
    public void shouldNotCreateBecauseExists() throws TechnicalException {
        final String name = "PAGE_NAME";
        when(newPage.getName()).thenReturn(name);
        when(pageRepository.findById(anyString())).thenReturn(Optional.of(new Page()));

        pageService.create(API_ID, newPage);

        verify(pageRepository, never()).create(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseTechnicalException() throws TechnicalException {
        final String name = "PAGE_NAME";
        when(newPage.getName()).thenReturn(name);

        when(pageRepository.findById(anyString())).thenReturn(Optional.empty());
        when(pageRepository.create(any(Page.class))).thenThrow(TechnicalException.class);

        pageService.create(API_ID, newPage);

        verify(pageRepository, never()).create(any());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenReturn(page1);

        pageService.update(PAGE_ID, existingPage);

        verify(pageRepository).update(argThat(new ArgumentMatcher<Page>() {
            public boolean matches(Object argument) {
                final Page pageToUpdate = (Page) argument;
                return PAGE_ID.equals(pageToUpdate.getId()) &&
                    pageToUpdate.getUpdatedAt() != null;
            }
        }));
    }

    @Test
    public void shouldUpdateOrderIncrement() throws TechnicalException {
        final Page pageOrder1 = new Page();
        pageOrder1.setId(PAGE_ID);
        pageOrder1.setOrder(1);
        pageOrder1.setApi(API_ID);

        final Page pageOrder2 = new Page();
        pageOrder2.setId("2");
        pageOrder2.setOrder(2);
        pageOrder2.setApi(API_ID);

        final Page pageOrder3 = new Page();
        pageOrder3.setId("3");
        pageOrder3.setOrder(3);
        pageOrder3.setApi(API_ID);

        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(pageOrder1));

        when(pageRepository.findByApi(API_ID)).thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
        when(pageRepository.update(any(Page.class))).thenReturn(pageOrder1);

        final UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setOrder(2);

        pageService.update(PAGE_ID, updatePageEntity);

        verify(pageRepository, times(3)).update(argThat(new ArgumentMatcher<Page>() {
            public boolean matches(Object argument) {
                final Page pageToUpdate = (Page) argument;

                if (PAGE_ID.equals(pageToUpdate.getId())) {
                    return pageToUpdate.getOrder() == 2;
                }
                if ("2".equals(pageToUpdate.getId())) {
                    return pageToUpdate.getOrder() == 1;
                }
                if ("3".equals(pageToUpdate.getId())) {
                    return pageToUpdate.getOrder() == 3;
                }
                return false;
            }
        }));
    }

    @Test
    public void shouldUpdateOrderDecrement() throws TechnicalException {
        final Page pageOrder1 = new Page();
        pageOrder1.setId(PAGE_ID);
        pageOrder1.setOrder(1);
        pageOrder1.setApi(API_ID);

        final Page pageOrder2 = new Page();
        pageOrder2.setId("2");
        pageOrder2.setOrder(2);
        pageOrder2.setApi(API_ID);

        final Page pageOrder3 = new Page();
        pageOrder3.setId("3");
        pageOrder3.setOrder(3);
        pageOrder3.setApi(API_ID);

        when(pageRepository.findById("3")).thenReturn(Optional.of(pageOrder3));

        when(pageRepository.findByApi(API_ID)).thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
        when(pageRepository.update(any(Page.class))).thenReturn(pageOrder1);

        final UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setOrder(1);

        pageService.update("3", updatePageEntity);

        verify(pageRepository, times(3)).update(argThat(new ArgumentMatcher<Page>() {
            public boolean matches(Object argument) {
                final Page pageToUpdate = (Page) argument;

                if (PAGE_ID.equals(pageToUpdate.getId())) {
                    return pageToUpdate.getOrder() == 2;
                }
                if ("2".equals(pageToUpdate.getId())) {
                    return pageToUpdate.getOrder() == 3;
                }
                if ("3".equals(pageToUpdate.getId())) {
                    return pageToUpdate.getOrder() == 1;
                }
                return false;
            }
        }));
    }

    @Test(expected = PageNotFoundException.class)
    public void shouldNotUpdateBecauseNotExists() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.empty());

        pageService.update(PAGE_ID, existingPage);

        verify(pageRepository, never()).update(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenThrow(TechnicalException.class);

        pageService.update(PAGE_ID, existingPage);

        verify(pageRepository, never()).update(any());
    }

    @Test
    public void shouldDeletePage() throws TechnicalException {
        pageService.delete(PAGE_ID);

        verify(pageRepository).delete(PAGE_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeletePageBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(pageRepository).delete(PAGE_ID);

        pageService.delete(PAGE_ID);
    }

    @Test
    public void shouldFindMaxPageOrderByApiName() throws TechnicalException {
        when(pageRepository.findMaxPageOrderByApi(API_ID)).thenReturn(10);

        assertEquals(10, pageService.findMaxPageOrderByApi(API_ID));
    }

    @Test
    public void shouldFindMaxPageOrderByApiNameWhenNull() throws TechnicalException {
        when(pageRepository.findMaxPageOrderByApi(API_ID)).thenReturn(null);

        assertEquals(0, pageService.findMaxPageOrderByApi(API_ID));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindMaxPageOrderByApiNameBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(pageRepository).findMaxPageOrderByApi(API_ID);

        pageService.findMaxPageOrderByApi(API_ID);
    }
}
