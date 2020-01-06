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
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

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
        pageOrder1.setReferenceId(API_ID);
        pageOrder1.setReferenceType(PageReferenceType.API);
        pageOrder1.setPublished(true);

        final Page pageOrder2 = new Page();
        pageOrder2.setId("2");
        pageOrder2.setOrder(2);
        pageOrder2.setReferenceId(API_ID);
        pageOrder2.setReferenceType(PageReferenceType.API);

        final Page pageOrder3 = new Page();
        pageOrder3.setId("3");
        pageOrder3.setOrder(3);
        pageOrder3.setReferenceId(API_ID);
        pageOrder3.setReferenceType(PageReferenceType.API);

        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(pageOrder1));
        when(pageRepository.search(argThat(o -> o == null || "LINK".equals(o.getType())))).thenReturn(Collections.emptyList());
        when(pageRepository.search(argThat(o -> o == null || API_ID.equals(o.getReferenceId())))).thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
        when(pageRepository.update(any(Page.class))).thenReturn(pageOrder1);

        final UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setOrder(2);
        updatePageEntity.setPublished(false);

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
        pageOrder1.setReferenceId(API_ID);
        pageOrder1.setReferenceType(PageReferenceType.API);
        pageOrder1.setPublished(true);
        
        final Page pageOrder2 = new Page();
        pageOrder2.setId("2");
        pageOrder2.setOrder(2);
        pageOrder2.setReferenceId(API_ID);
        pageOrder2.setReferenceType(PageReferenceType.API);

        final Page pageOrder3 = new Page();
        pageOrder3.setId("3");
        pageOrder3.setOrder(3);
        pageOrder3.setReferenceId(API_ID);
        pageOrder3.setReferenceType(PageReferenceType.API);

        when(pageRepository.findById("3")).thenReturn(Optional.of(pageOrder3));

        when(pageRepository.search(argThat(o -> o == null || o.getReferenceId().equals(API_ID)))).thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
        when(pageRepository.update(any(Page.class))).thenReturn(pageOrder1);

        final UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setOrder(1);
        updatePageEntity.setPublished(false);

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

    @Test
    public void shouldNotUpdatLinkPublicationStatus() throws TechnicalException {
        Map<String, String> conf = new HashMap<String, String>();
        conf.put("resourceRef", "A");
        
        Page linkPage = new Page();
        linkPage.setId(PAGE_ID);
        linkPage.setOrder(1);
        linkPage.setPublished(true);
        linkPage.setReferenceId("DEFAULT");
        linkPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        linkPage.setType(PageType.LINK);
        linkPage.setConfiguration(conf);
        doReturn(Optional.of(linkPage)).when(pageRepository).findById(PAGE_ID);
        
        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setPublished(false);
        updatePageEntity.setOrder(1);
        updatePageEntity.setConfiguration(conf);

        
        Page updatedPage = new Page();
        updatedPage.setId(PAGE_ID);
        updatedPage.setOrder(1);
        updatedPage.setReferenceId("DEFAULT");
        updatedPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        updatedPage.setType(PageType.LINK);
        doReturn(updatedPage).when(pageRepository).update(any());

        pageService.update(PAGE_ID, updatePageEntity);
        
        verify(pageRepository).update(argThat(p -> p.isPublished() == linkPage.isPublished()));
    }
}
