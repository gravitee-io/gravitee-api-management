/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import freemarker.template.TemplateException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.sanitizer.HtmlSanitizer;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.env.MockEnvironment;

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

    @Mock
    private PageRevisionService pageRevisionService;

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private ApiTemplateService apiTemplateService;

    @Mock
    private HtmlSanitizer htmlSanitizer;

    @Before
    public void setUp() {
        when(page1.getVisibility()).thenReturn("PUBLIC");
        when(existingPage.getVisibility()).thenReturn(Visibility.PUBLIC);
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        when(page1.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(page1.getReferenceId()).thenReturn("envId");

        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenReturn(page1);

        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "envId"), PAGE_ID, existingPage);

        verify(pageRepository).update(argThat(pageToUpdate -> PAGE_ID.equals(pageToUpdate.getId()) && pageToUpdate.getUpdatedAt() != null));

        // neither content nor name are updated
        verify(pageRevisionService, times(0)).create(any());
    }

    @Test
    public void shouldUpdateWithRevision_becauseOfContentChange() throws TechnicalException {
        when(page1.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(page1.getReferenceId()).thenReturn("envId");
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenReturn(page1);

        when(page1.getType()).thenReturn(PageType.MARKDOWN.name());
        when(page1.getContent()).thenReturn("some");
        when(existingPage.getContent()).thenReturn("awesome");
        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "envId"), PAGE_ID, existingPage);

        verify(pageRepository).update(argThat(pageToUpdate -> PAGE_ID.equals(pageToUpdate.getId()) && pageToUpdate.getUpdatedAt() != null));

        verify(pageRevisionService, times(1)).create(any());
    }

    @Test
    public void shouldUpdateWithRevision_becauseOfNameChange() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenReturn(page1);

        when(page1.getType()).thenReturn(PageType.MARKDOWN.name());
        when(page1.getName()).thenReturn("some");
        when(page1.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(page1.getReferenceId()).thenReturn("envId");
        when(existingPage.getName()).thenReturn("awesome");
        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "envId"), PAGE_ID, existingPage);

        verify(pageRepository).update(argThat(pageToUpdate -> PAGE_ID.equals(pageToUpdate.getId()) && pageToUpdate.getUpdatedAt() != null));

        verify(pageRevisionService, times(1)).create(any());
    }

    @Test
    public void shouldUpdateWithoutRevision_becauseOfNoRevisionForType() throws TechnicalException {
        when(page1.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(page1.getReferenceId()).thenReturn("envId");
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenReturn(page1);

        when(page1.getType()).thenReturn(PageType.FOLDER.name());
        when(page1.getName()).thenReturn("some");
        when(page1.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(page1.getReferenceId()).thenReturn("envId");
        when(page1.getVisibility()).thenReturn("PUBLIC");
        when(existingPage.getName()).thenReturn("awesome");
        when(existingPage.getVisibility()).thenReturn(Visibility.PUBLIC);
        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "envId"), PAGE_ID, existingPage);

        verify(pageRepository).update(argThat(pageToUpdate -> PAGE_ID.equals(pageToUpdate.getId()) && pageToUpdate.getUpdatedAt() != null));

        // no revision for folder
        verify(pageRevisionService, times(0)).create(any());
    }

    @Test
    public void shouldUpdateOrderIncrement() throws TechnicalException {
        final Page pageOrder1 = new Page();
        pageOrder1.setId(PAGE_ID);
        pageOrder1.setOrder(1);
        pageOrder1.setReferenceId(API_ID);
        pageOrder1.setReferenceType(PageReferenceType.API);
        pageOrder1.setPublished(true);
        pageOrder1.setVisibility("PUBLIC");

        final Page pageOrder2 = new Page();
        pageOrder2.setId("2");
        pageOrder2.setOrder(2);
        pageOrder2.setReferenceId(API_ID);
        pageOrder2.setReferenceType(PageReferenceType.API);
        pageOrder2.setVisibility("PUBLIC");

        final Page pageOrder3 = new Page();
        pageOrder3.setId("3");
        pageOrder3.setOrder(3);
        pageOrder3.setReferenceId(API_ID);
        pageOrder3.setReferenceType(PageReferenceType.API);
        pageOrder3.setVisibility("PUBLIC");

        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(pageOrder1));
        when(pageRepository.search(argThat(o -> o == null || "LINK".equals(o.getType())))).thenReturn(Collections.emptyList());
        when(pageRepository.search(argThat(o -> o == null || API_ID.equals(o.getReferenceId()))))
            .thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
        when(pageRepository.update(any(Page.class))).thenReturn(pageOrder1);

        final UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setOrder(2);
        updatePageEntity.setVisibility(Visibility.PUBLIC);

        pageService.update(GraviteeContext.getExecutionContext(), PAGE_ID, updatePageEntity);

        verify(pageRepository, times(4))
            .update(
                argThat(pageToUpdate -> {
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
                })
            );
        // neither content nor name are updated
        verify(pageRevisionService, times(0)).create(any());
    }

    @Test
    public void shouldUpdateOrderDecrement() throws TechnicalException {
        final Page pageOrder1 = new Page();
        pageOrder1.setId(PAGE_ID);
        pageOrder1.setOrder(1);
        pageOrder1.setReferenceId(API_ID);
        pageOrder1.setReferenceType(PageReferenceType.API);
        pageOrder1.setPublished(true);
        pageOrder1.setVisibility("PUBLIC");

        final Page pageOrder2 = new Page();
        pageOrder2.setId("2");
        pageOrder2.setOrder(2);
        pageOrder2.setReferenceId(API_ID);
        pageOrder2.setReferenceType(PageReferenceType.API);
        pageOrder2.setVisibility("PUBLIC");

        final Page pageOrder3 = new Page();
        pageOrder3.setId("3");
        pageOrder3.setOrder(3);
        pageOrder3.setReferenceId(API_ID);
        pageOrder3.setReferenceType(PageReferenceType.API);
        pageOrder3.setVisibility("PUBLIC");

        when(pageRepository.findById("3")).thenReturn(Optional.of(pageOrder3));

        when(pageRepository.search(argThat(o -> o == null || o.getReferenceId().equals(API_ID))))
            .thenReturn(asList(pageOrder1, pageOrder2, pageOrder3));
        when(pageRepository.update(any(Page.class))).thenReturn(pageOrder1);

        final UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setOrder(1);
        updatePageEntity.setVisibility(Visibility.PUBLIC);

        pageService.update(GraviteeContext.getExecutionContext(), "3", updatePageEntity);

        verify(pageRepository, times(4))
            .update(
                argThat(pageToUpdate -> {
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
                })
            );
        // neither content nor name are updated
        verify(pageRevisionService, times(0)).create(any());
    }

    @Test(expected = PageNotFoundException.class)
    public void shouldNotUpdateBecauseNotExists() throws TechnicalException {
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.empty());

        pageService.update(GraviteeContext.getExecutionContext(), PAGE_ID, existingPage);

        verify(pageRepository, never()).update(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        when(page1.getReferenceType()).thenReturn(PageReferenceType.ENVIRONMENT);
        when(page1.getReferenceId()).thenReturn("envId");
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(pageRepository.update(any(Page.class))).thenThrow(TechnicalException.class);

        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "envId"), PAGE_ID, existingPage);

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
        linkPage.setVisibility("PUBLIC");
        doReturn(Optional.of(linkPage)).when(pageRepository).findById(PAGE_ID);

        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setPublished(false);
        updatePageEntity.setOrder(1);
        updatePageEntity.setContent("A");
        updatePageEntity.setVisibility(Visibility.PUBLIC);

        Page updatedPage = new Page();
        updatedPage.setId(PAGE_ID);
        updatedPage.setOrder(1);
        updatedPage.setReferenceId("DEFAULT");
        updatedPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        updatedPage.setType("LINK");
        updatedPage.setVisibility("PUBLIC");
        doReturn(updatedPage).when(pageRepository).update(any());

        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "DEFAULT"), PAGE_ID, updatePageEntity);

        verify(pageRepository).update(argThat(p -> p.isPublished() == linkPage.isPublished()));
        verify(pageRevisionService, times(0)).create(any());
    }

    @Test
    public void shouldUpdateRelatedPagesPublicationStatus() throws TechnicalException {
        Page unpublishedPage = new Page();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setReferenceId("DEFAULT");
        unpublishedPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(false);
        unpublishedPage.setVisibility("PUBLIC");
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
        linkPage.setVisibility("PUBLIC");

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
        translationPage.setVisibility("PUBLIC");

        Page linkTranslationPage = new Page();
        linkTranslationPage.setId("LINK_TRANSLATION_ID");
        linkTranslationPage.setParentId("LINK_ID");
        linkTranslationPage.setOrder(2);
        linkTranslationPage.setPublished(false);
        linkTranslationPage.setReferenceId("DEFAULT");
        linkTranslationPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        linkTranslationPage.setType("TRANSLATION");
        linkTranslationPage.setConfiguration(translationConf);
        linkTranslationPage.setVisibility("PUBLIC");

        doReturn(asList(linkPage)).when(pageRepository).search(argThat(p -> PageType.LINK.name().equals(p.getType())));
        doReturn(asList(translationPage))
            .when(pageRepository)
            .search(argThat(p -> PageType.TRANSLATION.name().equals(p.getType()) && PAGE_ID.equals(p.getParent())));
        doReturn(asList(linkTranslationPage))
            .when(pageRepository)
            .search(argThat(p -> PageType.TRANSLATION.name().equals(p.getType()) && "LINK_ID".equals(p.getParent())));

        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setPublished(true);
        updatePageEntity.setContent("");
        updatePageEntity.setOrder(1);
        updatePageEntity.setConfiguration(conf);
        updatePageEntity.setVisibility(Visibility.PUBLIC);

        Page updatedPage = new Page();
        updatedPage.setId(PAGE_ID);
        updatedPage.setOrder(1);
        updatedPage.setReferenceId("DEFAULT");
        updatedPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        updatedPage.setType("TRANSLATION");
        updatedPage.setPublished(true);
        updatedPage.setVisibility("PUBLIC");
        doReturn(updatedPage).when(pageRepository).update(argThat(p -> p.getId().equals(PAGE_ID)));

        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "DEFAULT"), PAGE_ID, updatePageEntity);
        // neither content nor name are updated
        verify(pageRevisionService, times(0)).create(any());

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

        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "DEFAULT"), "TRANSLATION_ID", updateTranslation);
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

        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "DEFAULT"), "TRANSLATION_ID", updateTranslation);
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

        pageService.update(new ExecutionContext(GraviteeContext.getDefaultOrganization(), "DEFAULT"), "TRANSLATION_ID", updateTranslation);
    }

    @Test(expected = PageContentUnsafeException.class)
    public void shouldNotUpdateBecausePageContentUnsafeException() throws TechnicalException {
        setField(pageService, "markdownSanitize", true);

        String content = "<script />";
        when(existingPage.getContent()).thenReturn(content);
        when(page1.getType()).thenReturn(PageType.MARKDOWN.name());
        when(page1.getReferenceType()).thenReturn(PageReferenceType.API);
        when(page1.getReferenceId()).thenReturn(API_ID);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));
        when(this.notificationTemplateService.resolveInlineTemplateWithParam(anyString(), anyString(), eq(content), any(), anyBoolean()))
            .thenReturn(content);
        when(htmlSanitizer.isSafe(anyString())).thenReturn(new HtmlSanitizer.SanitizeInfos(false, "Tag not allowed: script"));

        pageService.update(GraviteeContext.getExecutionContext(), PAGE_ID, existingPage);

        verify(pageRepository, never()).update(any());
    }

    @Test
    public void shouldNotUpdateBecausePageContentTemplatingException() throws TechnicalException, TemplateException {
        setField(pageService, "markdownSanitize", true);

        String content = "<script />";
        when(existingPage.getContent()).thenReturn(content);
        when(page1.getType()).thenReturn(PageType.MARKDOWN.name());
        when(page1.getReferenceType()).thenReturn(PageReferenceType.API);
        when(page1.getReferenceId()).thenReturn(API_ID);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page1));

        when(pageRepository.update(any())).thenReturn(page1);

        when(this.notificationTemplateService.resolveInlineTemplateWithParam(anyString(), anyString(), eq(content), any(), anyBoolean()))
            .thenThrow(new TemplateProcessingException(new TemplateException(null)));

        pageService.update(GraviteeContext.getExecutionContext(), PAGE_ID, existingPage);

        verify(pageRepository).update(any());
    }

    @Test
    public void should_UnpublishPage_LinkedToStagingPlan() throws TechnicalException {
        shouldUnpublishPage(PlanStatus.STAGING);
    }

    @Test
    public void should_UnpublishPage_LinkedToClosedPlan() throws TechnicalException {
        shouldUnpublishPage(PlanStatus.CLOSED);
    }

    private void shouldUnpublishPage(PlanStatus planStatus) throws TechnicalException {
        final String API_ID = "API_ID_TEST";
        Page unpublishedPage = new Page();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setReferenceId(API_ID);
        unpublishedPage.setReferenceType(PageReferenceType.API);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(true);
        unpublishedPage.setVisibility("PUBLIC");
        doReturn(Optional.of(unpublishedPage)).when(pageRepository).findById(PAGE_ID);

        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setPublished(false);
        updatePageEntity.setOrder(1);
        updatePageEntity.setVisibility(Visibility.PUBLIC);

        Page updatedPage = new Page();
        updatedPage.setId(PAGE_ID);
        updatedPage.setOrder(1);
        updatedPage.setReferenceId(API_ID);
        updatedPage.setReferenceType(PageReferenceType.API);
        updatedPage.setType("MARKDOWN");
        updatedPage.setPublished(false);
        updatedPage.setVisibility("PUBLIC");
        doReturn(updatedPage).when(pageRepository).update(argThat(p -> p.getId().equals(PAGE_ID)));

        PlanEntity planEntity = new PlanEntity();
        planEntity.setGeneralConditions(PAGE_ID);
        planEntity.setStatus(planStatus);
        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Set.of(planEntity));

        pageService.update(GraviteeContext.getExecutionContext(), PAGE_ID, updatePageEntity);

        verify(pageRepository).update(argThat(p -> p.getId().equals(PAGE_ID) && !p.isPublished()));
        verify(planSearchService).findByApi(eq(GraviteeContext.getExecutionContext()), argThat(p -> p.equals(API_ID)));
    }

    @Test(expected = PageUsedByCategoryException.class)
    public void shouldNotUnpublishPage_LinkedToCategory() throws TechnicalException {
        final String API_ID = "API_ID_TEST";
        Page unpublishedPage = new Page();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setReferenceId(API_ID);
        unpublishedPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(true);
        unpublishedPage.setVisibility("PUBLIC");
        doReturn(Optional.of(unpublishedPage)).when(pageRepository).findById(PAGE_ID);

        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setPublished(false);
        updatePageEntity.setOrder(1);
        updatePageEntity.setVisibility(Visibility.PUBLIC);

        when(categoryService.findByPage(PAGE_ID)).thenReturn(Collections.singletonList(new CategoryEntity()));

        pageService.update(GraviteeContext.getExecutionContext(), PAGE_ID, updatePageEntity);

        verify(planSearchService).findByApi(GraviteeContext.getExecutionContext(), argThat(p -> p.equals(API_ID)));
    }

    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotUnpublishPage_LinkedToPublishedPlan() throws TechnicalException {
        shouldNotUnpublishPage(PlanStatus.PUBLISHED);
    }

    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotUnpublishPage_LinkedToDepreciatedPlan() throws TechnicalException {
        shouldNotUnpublishPage(PlanStatus.DEPRECATED);
    }

    private void shouldNotUnpublishPage(PlanStatus planStatus) throws TechnicalException {
        final String API_ID = "API_ID_TEST";
        Page unpublishedPage = new Page();
        unpublishedPage.setId(PAGE_ID);
        unpublishedPage.setOrder(1);
        unpublishedPage.setReferenceId(API_ID);
        unpublishedPage.setReferenceType(PageReferenceType.API);
        unpublishedPage.setType("MARKDOWN");
        unpublishedPage.setPublished(true);
        unpublishedPage.setVisibility("PUBLIC");
        doReturn(Optional.of(unpublishedPage)).when(pageRepository).findById(PAGE_ID);

        UpdatePageEntity updatePageEntity = new UpdatePageEntity();
        updatePageEntity.setPublished(false);
        updatePageEntity.setOrder(1);
        updatePageEntity.setVisibility(Visibility.PUBLIC);

        PlanEntity planEntity = new PlanEntity();
        planEntity.setGeneralConditions(PAGE_ID);
        planEntity.setStatus(planStatus);
        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Set.of(planEntity));

        pageService.update(GraviteeContext.getExecutionContext(), PAGE_ID, updatePageEntity);

        verify(planSearchService).findByApi(GraviteeContext.getExecutionContext(), argThat(p -> p.equals(API_ID)));
    }
}
