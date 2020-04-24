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

import io.gravitee.management.model.UpdatePageEntity;
import io.gravitee.management.service.exceptions.PageContentUnsafeException;
import io.gravitee.management.service.exceptions.PageNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.PageServiceImpl;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_UpdateTest {

    private static final String API_ID = "myAPI";
    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private UpdatePageEntity existingPage;

    @Mock
    private Page page1;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenReturn(page1);

        pageService.update(PAGE_ID, existingPage);

        verify(pageRepository).update(argThat(pageToUpdate -> PAGE_ID.equals(pageToUpdate.getId()) &&
            pageToUpdate.getUpdatedAt() != null));
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

        when(pageRepository.search(argThat(o -> o == null || o.getApi().equals(API_ID)))).thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
        when(pageRepository.update(any(Page.class))).thenReturn(pageOrder1);

        final UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setOrder(2);

        pageService.update(PAGE_ID, updatePageEntity);

        verify(pageRepository, times(3)).update(argThat(pageToUpdate -> {
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

        when(pageRepository.search(argThat(o -> o == null || o.getApi().equals(API_ID)))).thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
        when(pageRepository.update(any(Page.class))).thenReturn(pageOrder1);

        final UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setOrder(1);

        pageService.update("3", updatePageEntity);

        verify(pageRepository, times(3)).update(argThat(pageToUpdate -> {
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

    @Test(expected = PageContentUnsafeException.class)
    public void shouldNotUpdateBecausePageContentUnsafeException() throws TechnicalException {

        setField(pageService, "markdownSanitize", true);

        when(existingPage.getContent()).thenReturn("<script />");
        when(page1.getType()).thenReturn(PageType.MARKDOWN);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));

        pageService.update(PAGE_ID, existingPage);

        verify(pageRepository, never()).update(any());
    }
}
