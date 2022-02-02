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
package io.gravitee.rest.api.service.impl;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_PublishTest {

    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Plan plan;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private AuditService auditService;

    @Mock
    private PageService pageService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiEntity apiEntity;

    @Mock
    private PlanConverter planConverter;

    @Mock
    private ApiConverter apiConverter;

    @Test(expected = PlanAlreadyPublishedException.class)
    public void shouldNotPublishBecauseAlreadyPublished() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.publish(PLAN_ID);
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotPublishBecauseAlreadyClose() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.CLOSED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.publish(PLAN_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotPublishBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

        planService.publish(PLAN_ID);
    }

    public void shouldPublishWithExistingKeylessPlan() throws TechnicalException {
        Plan keylessPlan = mock(Plan.class);
        when(keylessPlan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(keylessPlan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);

        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.findByApi(API_ID)).thenReturn(Collections.singleton(keylessPlan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));

        planService.publish(PLAN_ID);
    }

    @Test(expected = KeylessPlanAlreadyPublishedException.class)
    public void shouldNotPublishBecauseExistingKeylessPlan() throws TechnicalException {
        Plan keylessPlan = mock(Plan.class);
        when(keylessPlan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(keylessPlan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);

        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.findByApi(API_ID)).thenReturn(Collections.singleton(keylessPlan));

        planService.publish(PLAN_ID);
    }

    @Test
    public void shouldPublish() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        planService.publish(PLAN_ID);

        verify(plan, times(1)).setStatus(Plan.Status.PUBLISHED);
        verify(planRepository, times(1)).update(plan);
    }

    @Test
    public void shouldPublishAndUpdateApiDefinition() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(apiEntity.getGraviteeDefinitionVersion()).thenReturn(DefinitionVersion.V2.getLabel());
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        planService.publish(PLAN_ID);

        verify(plan, times(1)).setStatus(Plan.Status.PUBLISHED);
        verify(planRepository, times(1)).update(plan);
        verify(apiService).update(anyString(), any());
    }

    @Test
    public void shouldPublish_WithPublishGCPage() throws TechnicalException {
        final String GC_PAGE_ID = "GC_PAGE_ID";
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getGeneralConditions()).thenReturn(GC_PAGE_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(apiService.findById(API_ID)).thenReturn(apiEntity);

        PageEntity page = mock(PageEntity.class);
        when(page.getId()).thenReturn(GC_PAGE_ID);
        when(page.isPublished()).thenReturn(true);
        when(pageService.findById(page.getId())).thenReturn(page);

        planService.publish(PLAN_ID);

        verify(plan, times(1)).setStatus(Plan.Status.PUBLISHED);
        verify(planRepository, times(1)).update(plan);
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotPublish_WithNotPublishGCPage() throws TechnicalException {
        final String GC_PAGE_ID = "GC_PAGE_ID";
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getGeneralConditions()).thenReturn(GC_PAGE_ID);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        PageEntity page = mock(PageEntity.class);
        when(page.getId()).thenReturn(GC_PAGE_ID);
        when(page.isPublished()).thenReturn(false);
        when(pageService.findById(page.getId())).thenReturn(page);

        planService.publish(PLAN_ID);
    }
}
