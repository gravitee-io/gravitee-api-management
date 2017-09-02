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

import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.*;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.impl.SubscriptionServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SubscriptionServiceTest {

    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String APPLICATION_ID = "my-application";
    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";
    private static final String SUBSCRIPTION_VALIDATOR = "validator";

    @InjectMocks
    private SubscriptionService subscriptionService = new SubscriptionServiceImpl();

    @Mock
    private PlanService planService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private Subscription subscription;

    @Mock
    private PlanEntity plan;

    @Mock
    private ApplicationEntity application;

    @Mock
    private ApiEntity apiEntity;

    @Mock
    private ApiModelEntity apiModelEntity;

    @Mock
    private ApiKeyEntity apiKeyEntity;

    @Mock
    private EmailService emailService;

    @Test
    public void shouldFindById() throws TechnicalException {
        when(subscription.getStatus()).thenReturn(Subscription.Status.ACCEPTED);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        final SubscriptionEntity subscriptionEntity = subscriptionService.findById(SUBSCRIPTION_ID);

        assertNotNull(subscriptionEntity);
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotFindByIdBecauseNotExists() throws TechnicalException {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.findById(SUBSCRIPTION_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenThrow(TechnicalException.class);

        subscriptionService.findById(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldFindByApplication() throws TechnicalException {
        Subscription sub1 = new Subscription();
        sub1.setId("subscription-1");
        sub1.setStatus(Subscription.Status.ACCEPTED);

        Subscription sub2 = new Subscription();
        sub2.setId("subscription-2");
        sub2.setStatus(Subscription.Status.REJECTED);

        when(subscriptionRepository.findByApplication(APPLICATION_ID)).thenReturn(
                new HashSet<>(Arrays.asList(sub1, sub2)));

        Set<SubscriptionEntity> subscriptions = subscriptionService.findByApplicationAndPlan(APPLICATION_ID, null);

        assertEquals(2, subscriptions.size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApplicationBecauseTechnicalException() throws TechnicalException {
        when(subscriptionRepository.findByApplication(APPLICATION_ID)).thenThrow(TechnicalException.class);

        subscriptionService.findByApplicationAndPlan(APPLICATION_ID, null);
    }

    @Test
    public void shouldFindByPlan() throws TechnicalException {
        Subscription sub1 = new Subscription();
        sub1.setId("subscription-1");
        sub1.setStatus(Subscription.Status.ACCEPTED);

        Subscription sub2 = new Subscription();
        sub2.setId("subscription-2");
        sub2.setStatus(Subscription.Status.REJECTED);

        when(subscriptionRepository.findByPlan(PLAN_ID)).thenReturn(
                new HashSet<>(Arrays.asList(sub1, sub2)));

        Set<SubscriptionEntity> subscriptions = subscriptionService.findByPlan(PLAN_ID);

        assertEquals(2, subscriptions.size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByPlanBecauseTechnicalException() throws TechnicalException {
        when(subscriptionRepository.findByPlan(PLAN_ID)).thenThrow(TechnicalException.class);

        subscriptionService.findByPlan(PLAN_ID);
    }

    @Test(expected = PlanNotYetPublishedException.class)
    public void shouldNotCreateBecausePlanNotPublished() throws Exception {
        // Stub
        when(plan.getStatus()).thenReturn(PlanStatus.STAGING);
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(PLAN_ID, APPLICATION_ID);
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotCreateBecausePlanAlreadyClosed() throws Exception {
        // Stub
        when(plan.getStatus()).thenReturn(PlanStatus.CLOSED);
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(PLAN_ID, APPLICATION_ID);
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecausePlanKeyless() throws Exception {
        // Stub
        when(plan.getSecurity()).thenReturn(PlanSecurityType.KEY_LESS);
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(PLAN_ID, APPLICATION_ID);
    }

    @Test
    public void shouldCreateWithoutProcess() throws Exception {
        // Prepare data
        when(plan.getApis()).thenReturn(Collections.singleton(API_ID));
        when(plan.getValidation()).thenReturn(PlanValidationType.MANUAL);

        // Stub
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        SecurityContextHolder.setContext(new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return new Authentication() {
                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return null;
                    }

                    @Override
                    public Object getCredentials() {
                        return null;
                    }

                    @Override
                    public Object getDetails() {
                        return null;
                    }

                    @Override
                    public Object getPrincipal() {
                        return new UserDetails("tester", "password", Collections.emptyList());
                    }

                    @Override
                    public boolean isAuthenticated() {
                        return false;
                    }

                    @Override
                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                };
            }

            @Override
            public void setAuthentication(Authentication authentication) {

            }
        });

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(PLAN_ID, APPLICATION_ID);

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        verify(subscriptionRepository, never()).update(any(Subscription.class));
        verify(apiKeyService, never()).generate(any());
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void shouldCreateWithAutomaticSubscription() throws Exception {
        // Prepare data
        when(plan.getApis()).thenReturn(Collections.singleton(API_ID));
        when(plan.getValidation()).thenReturn(PlanValidationType.AUTO);

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);

        SecurityContextHolder.setContext(new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return new Authentication() {
                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return null;
                    }

                    @Override
                    public Object getCredentials() {
                        return null;
                    }

                    @Override
                    public Object getDetails() {
                        return null;
                    }

                    @Override
                    public Object getPrincipal() {
                        return new UserDetails("tester", "password", Collections.emptyList());
                    }

                    @Override
                    public boolean isAuthenticated() {
                        return false;
                    }

                    @Override
                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                };
            }

            @Override
            public void setAuthentication(Authentication authentication) {

            }
        });

        // Stub
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(subscriptionRepository.create(any())).thenAnswer(new Answer<Subscription>() {
            @Override
            public Subscription answer(InvocationOnMock invocation) throws Throwable {
                Subscription subscription = (Subscription) invocation.getArguments()[0];
                subscription.setId(SUBSCRIPTION_ID);
                return subscription;
            }
        });

        when(subscriptionRepository.findById(SUBSCRIPTION_ID))
                .thenAnswer(new Answer<Optional<Subscription>>() {
                    @Override
                    public Optional<Subscription> answer(InvocationOnMock invocation) throws Throwable {
                        subscription.setCreatedAt(new Date());
                        return Optional.of(subscription);
                    }
                });

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(PLAN_ID, APPLICATION_ID);

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, times(1)).generate(any());
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotUpdateSubscriptionBecauseDoesNoExist() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.update(updatedSubscription);
    }

    @Test(expected = SubscriptionNotUpdatableException.class)
    public void shouldNotUpdateSubscriptionBecauseBadStatus() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());

        // Run
        subscriptionService.update(updatedSubscription);
    }

    @Test
    public void shouldUpdateSubscriptionWithoutEndingDate() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());

        // Run
        subscriptionService.update(updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, never()).findBySubscription(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldUpdateSubscriptionWithEndingDate() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(apiKeyService.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKeyEntity));
        when(apiKeyEntity.isRevoked()).thenReturn(false);
        when(apiKeyEntity.getExpireAt()).thenReturn(null);

        // Run
        subscriptionService.update(updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(subscription);
        verify(apiKeyService, times(1)).findBySubscription(SUBSCRIPTION_ID);
        verify(apiKeyService, times(1)).update(apiKeyEntity);
    }

    @Test
    public void shouldUpdateSubscriptionWithEndingDateButRevokedApiKey() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenAnswer(returnsFirstArg());
        when(apiKeyService.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKeyEntity));
        when(apiKeyEntity.isRevoked()).thenReturn(true);
        when(apiKeyEntity.getExpireAt()).thenReturn(null);

        // Run
        subscriptionService.update(updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(subscription);
        verify(apiKeyService, times(1)).findBySubscription(SUBSCRIPTION_ID);
        verify(apiKeyService, never()).update(apiKeyEntity);
    }

    @Test
    public void shouldUpdateSubscriptionWithEndingDateButExpiredApiKey() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        // subscription object is not a mock since its state is updated by the call to subscriptionService.create()
        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenAnswer(returnsFirstArg());
        when(apiKeyService.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKeyEntity));
        when(apiKeyEntity.isRevoked()).thenReturn(false);
        when(apiKeyEntity.getExpireAt()).thenReturn(new Date());

        // Run
        subscriptionService.update(updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(subscription);
        verify(apiKeyService, times(1)).findBySubscription(SUBSCRIPTION_ID);
        verify(apiKeyService, times(1)).update(apiKeyEntity);
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotCloseSubscriptionBecauseDoesNoExist() throws Exception {
        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.close(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldCloseSubscription() throws Exception {
        final Date now = new Date();

        final Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setEndingAt(now);
        subscription.setPlan(PLAN_ID);
        subscription.setApplication(APPLICATION_ID);

        final ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKey("api-key");
        apiKey.setRevoked(false);

        when(plan.getApis()).thenReturn(Collections.singleton(API_ID));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        when(apiKeyService.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKey));
        when(apiService.findByIdForTemplates(API_ID)).thenReturn(apiModelEntity);
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(application.getPrimaryOwner()).thenReturn(mock(PrimaryOwnerEntity.class));

        subscriptionService.close(SUBSCRIPTION_ID);

        verify(apiKeyService).revoke("api-key", false);
        verify(emailService).sendAsyncEmailNotification(any(EmailNotification.class));
    }

    @Test
    public void shouldProcessButReject() throws Exception {
        // Prepare data
        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(false);

        Subscription subscription = new Subscription();
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);

        when(plan.getApis()).thenReturn(Collections.singleton(API_ID));

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(APPLICATION_ID)).thenReturn(application);
        when(apiService.findById(API_ID)).thenReturn(apiEntity);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.process(processSubscription, SUBSCRIPTION_VALIDATOR);

        // Verify
        verify(apiKeyService, never()).generate(any());
        assertEquals(SubscriptionStatus.REJECTED, subscriptionEntity.getStatus());
        assertEquals(SUBSCRIPTION_VALIDATOR, subscriptionEntity.getProcessedBy());
        assertNotNull(subscriptionEntity.getProcessedAt());
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotProcessBecauseClosedPlan() throws Exception {
        // Prepare data
        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(false);

        Subscription subscription = new Subscription();
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);

        when(plan.getStatus()).thenReturn(PlanStatus.CLOSED);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.process(processSubscription, SUBSCRIPTION_VALIDATOR);
    }
}
