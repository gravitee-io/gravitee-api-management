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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.PageConfigurationKeys;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.PageRevisionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PageActionException;
import io.gravitee.rest.api.service.exceptions.PageUsedAsGeneralConditionsException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_DeleteTest {

    public static final String API_ID = "some-api-id";
    public static final String PLAN_ID = "some-plan-id";
    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";
    public static final String TRANSLATE_PAGE_ID = "TRANSLATE_" + PAGE_ID;

    @Captor
    ArgumentCaptor<String> idCaptor;

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PageRevisionService pageRevisionService;

    @Mock
    private PlanSearchService planSearchService;

    private Page page;
    private PlanEntity planEntity;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Before
    public void before() throws TechnicalException {
        page = new Page();
        page.setId(PAGE_ID);
        lenient().when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));

        planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
    }

    @Test
    public void shouldDeletePage() throws TechnicalException {
        page.setReferenceType(PageReferenceType.ENVIRONMENT);
        page.setReferenceId("envId");
        page.setVisibility("PUBLIC");

        pageService.delete(GraviteeContext.getExecutionContext(), PAGE_ID);

        verify(pageRepository).delete(PAGE_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeletePageBecauseTechnicalException() throws TechnicalException {
        doThrow(TechnicalException.class).when(pageRepository).delete(PAGE_ID);

        pageService.delete(GraviteeContext.getExecutionContext(), PAGE_ID);
    }

    @Test(expected = PageActionException.class)
    public void shouldNotDeletePageBecauseUsedInCategory() throws TechnicalException {
        page.setType(PageType.MARKDOWN.name());

        CategoryEntity category1 = new CategoryEntity();
        category1.setKey("cat_1");

        CategoryEntity category2 = new CategoryEntity();
        category2.setKey("cat_2");
        doReturn(Arrays.asList(category1, category2)).when(categoryService).findByPage(PAGE_ID);
        pageService.delete(GraviteeContext.getExecutionContext(), PAGE_ID);
    }

    @Test
    public void shouldDeletePage_NotUsedByPlan() throws TechnicalException {
        page.setReferenceType(PageReferenceType.API);
        page.setReferenceId(API_ID);
        page.setVisibility("PUBLIC");

        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.emptySet());

        pageService.delete(GraviteeContext.getExecutionContext(), PAGE_ID);

        verify(pageRepository).delete(PAGE_ID);
    }

    @Test
    public void shouldDeletePage_UsedBy_CLOSED_Plan() throws TechnicalException {
        page.setReferenceType(PageReferenceType.API);
        page.setReferenceId(API_ID);
        page.setVisibility("PUBLIC");

        planEntity.setGeneralConditions(PAGE_ID);
        planEntity.setStatus(PlanStatus.CLOSED);

        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Set.of(planEntity));

        pageService.delete(GraviteeContext.getExecutionContext(), PAGE_ID);

        verify(pageRepository).delete(PAGE_ID);
    }

    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotDeletePage_UsedBy_PUBLISHED_Plan() {
        page.setReferenceType(PageReferenceType.API);
        page.setReferenceId(API_ID);

        planEntity.setGeneralConditions(PAGE_ID);
        planEntity.setStatus(PlanStatus.PUBLISHED);

        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Set.of(planEntity));

        pageService.delete(GraviteeContext.getExecutionContext(), PAGE_ID);
    }

    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotDeleteTranslationPage_UsedBy_PUBLISHED_Plan() throws TechnicalException {
        Page translationPage = new Page();
        translationPage.setType(PageType.TRANSLATION.name());
        translationPage.setParentId(PAGE_ID);
        translationPage.setReferenceType(PageReferenceType.API);
        translationPage.setReferenceId(API_ID);

        planEntity.setGeneralConditions(PAGE_ID);
        planEntity.setStatus(PlanStatus.PUBLISHED);

        when(pageRepository.findById(TRANSLATE_PAGE_ID)).thenReturn(Optional.of(translationPage));
        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Set.of(planEntity));

        pageService.delete(GraviteeContext.getExecutionContext(), TRANSLATE_PAGE_ID);
    }

    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotDeletePage_UsedBy_DEPRECATED_Plan() throws TechnicalException {
        page.setReferenceType(PageReferenceType.API);
        page.setReferenceId(API_ID);

        planEntity.setGeneralConditions(PAGE_ID);
        planEntity.setStatus(PlanStatus.DEPRECATED);

        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Set.of(planEntity));

        pageService.delete(GraviteeContext.getExecutionContext(), PAGE_ID);
    }

    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotDeletePage_UsedBy_STAGING_Plan() throws TechnicalException {
        page.setReferenceType(PageReferenceType.API);
        page.setReferenceId(API_ID);

        planEntity.setGeneralConditions(PAGE_ID);
        planEntity.setStatus(PlanStatus.STAGING);

        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Set.of(planEntity));

        pageService.delete(GraviteeContext.getExecutionContext(), PAGE_ID);
    }

    @Test
    public void shouldDeleteAllByApi() throws TechnicalException {
        final SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(mock(UserDetails.class));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Page folder = new Page();
        String folderId = "folder";
        folder.setId(folderId);
        folder.setType(PageType.FOLDER.toString());
        String refId = "refId";
        folder.setReferenceId(refId);
        folder.setOrder(1);
        folder.setReferenceType(PageReferenceType.ENVIRONMENT);
        folder.setVisibility("PUBLIC");
        when(pageRepository.findById(folderId)).thenReturn(Optional.of(folder));

        Page child = new Page();
        String childId = "child";
        child.setId(childId);
        child.setOrder(2);
        child.setType(PageType.FOLDER.toString());
        child.setReferenceId(refId);
        child.setReferenceType(PageReferenceType.ENVIRONMENT);
        child.setVisibility("PUBLIC");
        when(pageRepository.findById(childId)).thenReturn(Optional.of(child));

        Page childPage = new Page();
        String childPageId = "childPageId";
        childPage.setId(childPageId);
        childPage.setType(PageType.SWAGGER.toString());
        childPage.setReferenceType(PageReferenceType.ENVIRONMENT);
        childPage.setReferenceId(refId);
        childPage.setVisibility("PUBLIC");
        when(pageRepository.findById(childPageId)).thenReturn(Optional.of(childPage));

        Page link = new Page();
        String linkId = "link";
        link.setId(linkId);
        link.setContent(PAGE_ID);
        when(pageRepository.search(new PageCriteria.Builder().type("LINK").build())).thenReturn(List.of(link));

        Page page = new Page();
        page.setId(PAGE_ID);
        page.setType(PageType.MARKDOWN.toString());
        page.setReferenceType(PageReferenceType.ENVIRONMENT);
        page.setReferenceId(refId);
        page.setVisibility("PUBLIC");
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));

        Page translation = new Page();
        String translationId = "translation";
        translation.setId(translationId);
        Map<String, String> configuration = new HashMap<>();
        configuration.put(PageConfigurationKeys.TRANSLATION_LANG, "EN");
        translation.setConfiguration(configuration);
        translation.setType(PageType.TRANSLATION.toString());
        translation.setReferenceType(PageReferenceType.ENVIRONMENT);
        translation.setReferenceId(refId);
        translation.setVisibility("PUBLIC");
        when(pageRepository.search(new PageCriteria.Builder().parent(PAGE_ID).type(PageType.TRANSLATION.name()).build())).thenReturn(
            Arrays.asList(translation)
        );

        when(
            pageRepository.search(new PageCriteria.Builder().referenceType(PageReferenceType.API.name()).referenceId("apiId").build())
        ).thenReturn(Arrays.asList(folder, child, childPage, page));

        pageService.deleteAllByApi(GraviteeContext.getExecutionContext(), "apiId");

        verify(pageRepository, times(6)).delete(idCaptor.capture());
        assertEquals(
            "Pages are not deleted in order !",
            Arrays.asList(childPageId, PAGE_ID, linkId, translationId, childId, folderId),
            idCaptor.getAllValues()
        );
    }
}
