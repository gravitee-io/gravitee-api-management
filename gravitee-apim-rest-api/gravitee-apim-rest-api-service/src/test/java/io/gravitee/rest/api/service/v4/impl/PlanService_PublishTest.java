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
package io.gravitee.rest.api.service.v4.impl;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.definition.FlowFixtures;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.KeylessPlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.NativePlanAuthenticationConflictException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanGeneralConditionStatusException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.PlanMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private SubscriptionEntity subscription;

    @Mock
    private AuditService auditService;

    @Mock
    private PageService pageService;

    @Mock
    private PlanMapper planMapper;

    @Mock
    private ApiMapper apiMapper;

    @Mock
    private FlowService flowService;

    @Mock
    private FlowCrudService flowCrudService;

    @Test(expected = PlanAlreadyPublishedException.class)
    public void shouldNotPublishBecauseAlreadyPublished() throws TechnicalException {
        var plan = Plan.builder().status(Plan.Status.PUBLISHED).build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotPublishBecauseAlreadyClose() throws TechnicalException {
        var plan = Plan.builder().status(Plan.Status.CLOSED).build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotPublishBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test
    public void shouldPublishWithExistingKeylessPlan() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();

        var keylessPlan = Plan.builder().status(Plan.Status.PUBLISHED).security(Plan.PlanSecurityType.KEY_LESS).build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.findByApi(API_ID)).thenReturn(Collections.singleton(keylessPlan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.PUBLISHED).build());
    }

    @Test(expected = KeylessPlanAlreadyPublishedException.class)
    public void shouldNotPublishBecauseExistingKeylessPlan() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .security(Plan.PlanSecurityType.KEY_LESS)
            .api(API_ID)
            .build();

        var keylessPlan = Plan.builder().status(Plan.Status.PUBLISHED).security(Plan.PlanSecurityType.KEY_LESS).build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.findByApi(API_ID)).thenReturn(Collections.singleton(keylessPlan));

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test
    public void shouldPublish() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.PUBLISHED).build());
    }

    @Test
    public void shouldPublishAndUpdatePlan() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.PUBLISHED).build());
    }

    @Test
    public void shouldPublishAndUpdateNativePlan() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .apiType(ApiType.NATIVE)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.PUBLISHED).build());
        verify(flowCrudService, times(1)).getNativePlanFlows(any());
        verify(flowService, never()).findByReference(any(), any());
    }

    @Test
    public void shouldPublishAndUpdateApiKeyPlanWithOtherAuthPlansPublished() throws TechnicalException {
        var apiKeyPlanToPublish = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .apiType(ApiType.NATIVE)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .security(Plan.PlanSecurityType.API_KEY)
            .build();

        var publishedOAuthPlan = Plan.builder()
            .id("oauth-plan")
            .api(API_ID)
            .status(Plan.Status.PUBLISHED)
            .security(Plan.PlanSecurityType.OAUTH2)
            .build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(apiKeyPlanToPublish));
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(apiKeyPlanToPublish, publishedOAuthPlan));
        when(planRepository.update(apiKeyPlanToPublish)).thenAnswer(returnsFirstArg());

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(apiKeyPlanToPublish.toBuilder().status(Plan.Status.PUBLISHED).build());
        verify(flowCrudService, times(1)).getNativePlanFlows(any());
        verify(flowService, never()).findByReference(any(), any());
    }

    @Test(expected = NativePlanAuthenticationConflictException.class)
    public void shouldNotPublishKeylessNativePlanIfAuthPlanPublished() throws TechnicalException {
        var stagedKeylessPlan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .apiType(ApiType.NATIVE)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .security(Plan.PlanSecurityType.KEY_LESS)
            .apiType(ApiType.NATIVE)
            .build();

        var publishedApiKeyPlan = Plan.builder()
            .id("published-api-key")
            .api(API_ID)
            .security(Plan.PlanSecurityType.API_KEY)
            .status(Plan.Status.PUBLISHED)
            .build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(stagedKeylessPlan));
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(stagedKeylessPlan, publishedApiKeyPlan));

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = NativePlanAuthenticationConflictException.class)
    public void shouldNotPublishAuthNativePlanIfKeylessPlanPublished() throws TechnicalException {
        var stagedApiKeyPlan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .apiType(ApiType.NATIVE)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .security(Plan.PlanSecurityType.API_KEY)
            .apiType(ApiType.NATIVE)
            .build();

        var publishedKeylessPlan = Plan.builder()
            .id("published-keyless")
            .api(API_ID)
            .security(Plan.PlanSecurityType.KEY_LESS)
            .status(Plan.Status.PUBLISHED)
            .build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(stagedApiKeyPlan));
        when(planRepository.findByApi(API_ID)).thenReturn(Set.of(stagedApiKeyPlan, publishedKeylessPlan));

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test
    public void shouldPublish_WithPublishGCPage() throws TechnicalException {
        final String GC_PAGE_ID = "GC_PAGE_ID";
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .generalConditions(GC_PAGE_ID)
            .build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        PageEntity page = mock(PageEntity.class);
        when(page.getId()).thenReturn(GC_PAGE_ID);
        when(page.isPublished()).thenReturn(true);
        when(pageService.findById(page.getId())).thenReturn(page);

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.PUBLISHED).build());
    }

    @Test(expected = PlanGeneralConditionStatusException.class)
    public void shouldNotPublish_WithNotPublishGCPage() throws TechnicalException {
        final String GC_PAGE_ID = "GC_PAGE_ID";
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .generalConditions(GC_PAGE_ID)
            .build();

        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        PageEntity page = mock(PageEntity.class);
        when(page.getId()).thenReturn(GC_PAGE_ID);
        when(page.isPublished()).thenReturn(false);
        when(pageService.findById(page.getId())).thenReturn(page);

        planService.publish(GraviteeContext.getExecutionContext(), PLAN_ID);
    }
}
