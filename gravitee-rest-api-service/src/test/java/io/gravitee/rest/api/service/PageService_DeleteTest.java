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

import com.google.common.collect.Sets;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.service.exceptions.PageUsedAsGeneralConditionsException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PageServiceImpl;
import io.gravitee.rest.api.service.search.SearchEngineService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageService_DeleteTest {

    private static final String PAGE_ID = "ba01aef0-e3da-4499-81ae-f0e3daa4995a";
    public static final String TRANSLATE_PAGE_ID = "TRANSLATE_" + PAGE_ID;
    public static final String API_ID = "some-api-id";

    @InjectMocks
    private PageServiceImpl pageService = new PageServiceImpl();

    @Mock
    private PageRepository pageRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private PageRevisionService pageRevisionService;

    @Mock
    private PlanService planService;
    
    @Test
    public void shouldDeletePage() throws TechnicalException {
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));

        pageService.delete(PAGE_ID);

        verify(pageRepository).delete(PAGE_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeletePageBecauseTechnicalException() throws TechnicalException {
        Page page = mock(Page.class);
        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        doThrow(TechnicalException.class).when(pageRepository).delete(PAGE_ID);

        pageService.delete(PAGE_ID);
    }

    @Test
    public void shouldDeletePage_NotUsedByPlan() throws TechnicalException {
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getReferenceType()).thenReturn(PageReferenceType.API);
        when(page.getReferenceId()).thenReturn(API_ID);

        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        when(planService.findByApi(API_ID)).thenReturn(Collections.emptySet());

        pageService.delete(PAGE_ID);

        verify(pageRepository).delete(PAGE_ID);
    }

    @Test
    public void shouldDeletePage_UsedBy_CLOSED_Plan() throws TechnicalException {
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getReferenceType()).thenReturn(PageReferenceType.API);
        when(page.getReferenceId()).thenReturn(API_ID);

        PlanEntity plan = mock(PlanEntity.class);
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(plan.getStatus()).thenReturn(PlanStatus.CLOSED);

        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        when(planService.findByApi(API_ID)).thenReturn(Sets.newHashSet(plan));

        pageService.delete(PAGE_ID);

        verify(pageRepository).delete(PAGE_ID);
    }

    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotDeletePage_UsedBy_PUBLISHED_Plan() throws TechnicalException {
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getReferenceType()).thenReturn(PageReferenceType.API);
        when(page.getReferenceId()).thenReturn(API_ID);

        PlanEntity plan = mock(PlanEntity.class);
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);

        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        when(planService.findByApi(API_ID)).thenReturn(Sets.newHashSet(plan));

        pageService.delete(PAGE_ID);
    }

    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotDeleteTranslationPage_UsedBy_PUBLISHED_Plan() throws TechnicalException {
        Page translationPage = mock(Page.class);
        when(translationPage.getType()).thenReturn(PageType.TRANSLATION.name());
        when(translationPage.getParentId()).thenReturn(PAGE_ID);
        when(translationPage.getReferenceType()).thenReturn(PageReferenceType.API);
        when(translationPage.getReferenceId()).thenReturn(API_ID);

        PlanEntity plan = mock(PlanEntity.class);
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);

        when(pageRepository.findById(TRANSLATE_PAGE_ID)).thenReturn(Optional.of(translationPage));
        when(planService.findByApi(API_ID)).thenReturn(Sets.newHashSet(plan));

        pageService.delete(TRANSLATE_PAGE_ID);
    }

    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotDeletePage_UsedBy_DEPRECATED_Plan() throws TechnicalException {
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getReferenceType()).thenReturn(PageReferenceType.API);
        when(page.getReferenceId()).thenReturn(API_ID);

        PlanEntity plan = mock(PlanEntity.class);
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(plan.getStatus()).thenReturn(PlanStatus.DEPRECATED);

        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        when(planService.findByApi(API_ID)).thenReturn(Sets.newHashSet(plan));

        pageService.delete(PAGE_ID);
    }
    @Test(expected = PageUsedAsGeneralConditionsException.class)
    public void shouldNotDeletePage_UsedBy_STAGING_Plan() throws TechnicalException {
        Page page = mock(Page.class);
        when(page.getId()).thenReturn(PAGE_ID);
        when(page.getReferenceType()).thenReturn(PageReferenceType.API);
        when(page.getReferenceId()).thenReturn(API_ID);

        PlanEntity plan = mock(PlanEntity.class);
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(plan.getStatus()).thenReturn(PlanStatus.STAGING);

        when(pageRepository.findById(PAGE_ID)).thenReturn(Optional.of(page));
        when(planService.findByApi(API_ID)).thenReturn(Sets.newHashSet(plan));

        pageService.delete(PAGE_ID);
    }
}
