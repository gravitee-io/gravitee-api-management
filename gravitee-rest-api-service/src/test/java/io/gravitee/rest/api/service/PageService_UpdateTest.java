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
import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.service.exceptions.PageActionException;
import io.gravitee.rest.api.service.exceptions.PageContentUnsafeException;
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
    private ApiService apiService;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenReturn(page1);

        pageService.update(PAGE_ID, existingPage);

        verify(pageRepository).update(
                argThat(pageToUpdate -> PAGE_ID.equals(pageToUpdate.getId()) && pageToUpdate.getUpdatedAt() != null));
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
        when(pageRepository.search(argThat(o -> o == null || "LINK".equals(o.getType()))))
                .thenReturn(Collections.emptyList());
        when(pageRepository.search(argThat(o -> o == null || API_ID.equals(o.getReferenceId()))))
                .thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
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

        when(pageRepository.search(argThat(o -> o == null || o.getReferenceId().equals(API_ID))))
                .thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
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
    public void shouldNotUpdateLinkPublicationStatus() throws TechnicalException {
        Page linkPage = new Page();
        linkPage.setId(PAGE_ID);
        linkPage.setOrder(1);
        linkPage.setPublished(true);
        linkPage.setReferenceId("DEFAULT");
        linkPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        linkPage.setType("LINK");
        linkPage.setContent("A");
        doReturn(Optional.of(linkPage)).when(pageRepository).findById(PAGE_ID);

        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setPublished(false);
        updatePageEntity.setOrder(1);
        updatePageEntity.setContent("A");

        Page updatedPage = new Page();
        updatedPage.setId(PAGE_ID);
        updatedPage.setOrder(1);
        updatedPage.setReferenceId("DEFAULT");
        updatedPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        updatedPage.setType("LINK");
        doReturn(updatedPage).when(pageRepository).update(any());

        pageService.update(PAGE_ID, updatePageEntity);

        verify(pageRepository).update(argThat(p -> p.isPublished() == linkPage.isPublished()));
    }

    @Test
    public void shouldUpdateRelatedPagesPublicationStatus() throws TechnicalException {
        Page unpublishedPage = new Page();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setReferenceId("DEFAULT");
        unpublishedPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        unpublishedPage.setType("SWAGGER");
        unpublishedPage.setPublished(false);
        doReturn(Optional.of(unpublishedPage)).when(pageRepository).findById(PAGE_ID);

        Page linkPage = new Page();
        linkPage.setId("LINK_ID");
        linkPage.setOrder(1);
        linkPage.setPublished(false);
        linkPage.setReferenceId("DEFAULT");
        linkPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        linkPage.setType("LINK");
        Map<String, String> conf = new HashMap<String, String>();
        conf.put(PageConfigurationKeys.LINK_RESOURCE_TYPE, "page");
        linkPage.setConfiguration(conf);
        linkPage.setContent(PAGE_ID);

        Map<String, String> translationConf = new HashMap<String, String>();
        translationConf.put(PageConfigurationKeys.TRANSLATION_LANG, "fr");

        Page translationPage = new Page();
        translationPage.setId("TRANSLATION_ID");
        translationPage.setParentId(PAGE_ID);
        translationPage.setOrder(1);
        translationPage.setPublished(false);
        translationPage.setReferenceId("DEFAULT");
        translationPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        translationPage.setType("TRANSLATION");
        translationPage.setConfiguration(translationConf);

        Page linkTranslationPage = new Page();
        linkTranslationPage.setId("LINK_TRANSLATION_ID");
        linkTranslationPage.setParentId("LINK_ID");
        linkTranslationPage.setOrder(2);
        linkTranslationPage.setPublished(false);
        linkTranslationPage.setReferenceId("DEFAULT");
        linkTranslationPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        linkTranslationPage.setType("TRANSLATION");
        linkTranslationPage.setConfiguration(translationConf);

        doReturn(asList(linkPage)).when(pageRepository).search(argThat(p -> PageType.LINK.name().equals(p.getType())));
        doReturn(asList(translationPage)).when(pageRepository).search(argThat(p -> PageType.TRANSLATION.name().equals(p.getType()) && PAGE_ID.equals(p.getParent())));
        doReturn(asList(linkTranslationPage)).when(pageRepository).search(argThat(p -> PageType.TRANSLATION.name().equals(p.getType()) && "LINK_ID".equals(p.getParent())));


        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setPublished(true);
        updatePageEntity.setOrder(1);
        updatePageEntity.setConfiguration(conf);

        Page updatedPage = new Page();
        updatedPage.setId(PAGE_ID);
        updatedPage.setOrder(1);
        updatedPage.setReferenceId("DEFAULT");
        updatedPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        updatedPage.setType("TRANSLATION");
        updatedPage.setPublished(true);
        doReturn(updatedPage).when(pageRepository).update(argThat(p -> p.getId().equals(PAGE_ID)));

        pageService.update(PAGE_ID, updatePageEntity);

        verify(pageRepository).update(argThat(p -> p.getId().equals(PAGE_ID) && p.isPublished()));
        verify(pageRepository).update(argThat(p -> p.getId().equals("LINK_ID") && p.isPublished()));
        verify(pageRepository).update(argThat(p -> p.getId().equals("TRANSLATION_ID") && p.isPublished()));
        verify(pageRepository).update(argThat(p -> p.getId().equals("LINK_TRANSLATION_ID") && p.isPublished()));
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageWithoutLang() throws TechnicalException {
        Page translationPage = new Page();
        translationPage.setId("TRANSLATION_ID");
        translationPage.setParentId(PAGE_ID);
        translationPage.setOrder(1);
        translationPage.setPublished(false);
        translationPage.setReferenceId("DEFAULT");
        translationPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        translationPage.setType("TRANSLATION");
        Map<String, String> translationConf = new HashMap<String, String>();
        translationConf.put(PageConfigurationKeys.TRANSLATION_LANG, "fr");
        translationPage.setConfiguration(translationConf);
        doReturn(Optional.of(translationPage)).when(pageRepository).findById("TRANSLATION_ID");

        UpdatePageEntity updateTranslation = new UpdatePageEntity();
        updateTranslation.setConfiguration(new HashMap<String, String>());
        updateTranslation.setOrder(1);

        pageService.update("TRANSLATION_ID", updateTranslation);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageIfParentIsSystemFolder() throws TechnicalException {
        Page parentPage = new Page();
        parentPage.setType("SYSTEM_FOLDER");
        doReturn(Optional.of(parentPage)).when(pageRepository).findById("SYS_FOLDER");

        Page translationPage = new Page();
        translationPage.setId("TRANSLATION_ID");
        translationPage.setParentId(PAGE_ID);
        translationPage.setOrder(1);
        translationPage.setPublished(false);
        translationPage.setReferenceId("DEFAULT");
        translationPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        translationPage.setType("TRANSLATION");
        Map<String, String> translationConf = new HashMap<String, String>();
        translationConf.put(PageConfigurationKeys.TRANSLATION_LANG, "fr");
        translationPage.setConfiguration(translationConf);
        doReturn(Optional.of(translationPage)).when(pageRepository).findById("TRANSLATION_ID");

        UpdatePageEntity updateTranslation = new UpdatePageEntity();
        updateTranslation.setParentId("SYS_FOLDER");
        updateTranslation.setOrder(1);

        pageService.update("TRANSLATION_ID", updateTranslation);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotCreateTranslationPageIfParentIsRoot() throws TechnicalException {
        Page parentPage = new Page();
        parentPage.setType("ROOT");
        doReturn(Optional.of(parentPage)).when(pageRepository).findById("ROOT");

        Page translationPage = new Page();
        translationPage.setId("TRANSLATION_ID");
        translationPage.setParentId(PAGE_ID);
        translationPage.setOrder(1);
        translationPage.setPublished(false);
        translationPage.setReferenceId("DEFAULT");
        translationPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        translationPage.setType("TRANSLATION");
        Map<String, String> translationConf = new HashMap<String, String>();
        translationConf.put(PageConfigurationKeys.TRANSLATION_LANG, "fr");
        translationPage.setConfiguration(translationConf);
        doReturn(Optional.of(translationPage)).when(pageRepository).findById("TRANSLATION_ID");

        UpdatePageEntity updateTranslation = new UpdatePageEntity();
        updateTranslation.setParentId("ROOT");
        updateTranslation.setOrder(1);

        pageService.update("TRANSLATION_ID", updateTranslation);

    }

    @Test(expected = PageContentUnsafeException.class)
    public void shouldNotUpdateBecausePageContentUnsafeException() throws TechnicalException {

        setField(pageService, "markdownSanitize", true);

        when(existingPage.getContent()).thenReturn("<script />");
        when(page1.getType()).thenReturn(PageType.MARKDOWN.name());
        when(page1.getReferenceType()).thenReturn(PageReferenceType.API);
        when(page1.getReferenceId()).thenReturn(API_ID);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));

        pageService.update(PAGE_ID, existingPage);

        verify(pageRepository, never()).update(any());
    }
}
