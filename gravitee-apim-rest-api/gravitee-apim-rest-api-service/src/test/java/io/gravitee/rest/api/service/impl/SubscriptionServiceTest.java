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

import static io.gravitee.repository.management.model.Subscription.Status.*;
import static io.gravitee.rest.api.service.impl.AbstractService.ENVIRONMENT_ADMIN;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import java.util.*;
import java.util.function.BiFunction;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
    private static final String PAGE_ID = "my-page-gcu";
    private static final String USER_ID = "user";
    private static final String SUBSCRIBER_ID = "subscriber";
    private static final String ENVIRONMENT_ID = "DEFAULT";

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
    private AuditService auditService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private GroupService groupService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private UserService userService;

    @Mock
    private PageEntity generalConditionPage;

    @Mock
    private PageService pageService;

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
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(subscription.getStatus()).thenReturn(ACCEPTED);
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
        sub1.setStatus(ACCEPTED);
        sub1.setApplication(APPLICATION_ID);

        Subscription sub2 = new Subscription();
        sub2.setId("subscription-2");
        sub2.setStatus(REJECTED);
        sub2.setApplication(APPLICATION_ID);

        when(subscriptionRepository.search(new SubscriptionCriteria.Builder().applications(singleton(APPLICATION_ID)).build()))
            .thenReturn(asList(sub1, sub2));

        Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApplicationAndPlan(
            GraviteeContext.getExecutionContext(),
            APPLICATION_ID,
            null
        );

        assertEquals(2, subscriptions.size());
    }

    @Test
    public void shouldFindByApi() throws TechnicalException {
        Subscription sub1 = new Subscription();
        sub1.setId("subscription-1");
        sub1.setStatus(ACCEPTED);
        sub1.setApi(API_ID);

        Subscription sub2 = new Subscription();
        sub2.setId("subscription-2");
        sub2.setStatus(REJECTED);
        sub2.setApi(API_ID);

        when(subscriptionRepository.search(new SubscriptionCriteria.Builder().apis(singleton(API_ID)).applications(null).build()))
            .thenReturn(asList(sub1, sub2));

        Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApi(GraviteeContext.getExecutionContext(), API_ID);

        assertEquals(2, subscriptions.size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByApplicationBecauseTechnicalException() throws TechnicalException {
        when(subscriptionRepository.search(any(SubscriptionCriteria.class))).thenThrow(TechnicalException.class);

        subscriptionService.findByApplicationAndPlan(GraviteeContext.getExecutionContext(), APPLICATION_ID, null);
    }

    @Test
    public void shouldFindByPlan() throws TechnicalException {
        Subscription sub1 = new Subscription();
        sub1.setId("subscription-1");
        sub1.setStatus(ACCEPTED);

        Subscription sub2 = new Subscription();
        sub2.setId("subscription-2");
        sub2.setStatus(REJECTED);

        when(subscriptionRepository.search(new SubscriptionCriteria.Builder().plans(singleton(PLAN_ID)).build()))
            .thenReturn(asList(sub1, sub2));

        Collection<SubscriptionEntity> subscriptions = subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID);

        assertEquals(2, subscriptions.size());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByPlanBecauseTechnicalException() throws TechnicalException {
        when(subscriptionRepository.search(any(SubscriptionCriteria.class))).thenThrow(TechnicalException.class);

        subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = PlanNotYetPublishedException.class)
    public void shouldNotCreateBecausePlanNotPublished() {
        // Stub
        when(plan.getStatus()).thenReturn(PlanStatus.STAGING);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotCreateBecausePlanAlreadyClosed() {
        // Stub
        when(plan.getStatus()).thenReturn(PlanStatus.CLOSED);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecausePlanAlreadyDeprecated() {
        // Stub
        when(plan.getStatus()).thenReturn(PlanStatus.DEPRECATED);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecausePlanKeyless() {
        // Stub
        when(plan.getSecurity()).thenReturn(PlanSecurityType.KEY_LESS);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableWithSharedApiKeyException.class)
    public void shouldNotCreateBecauseSharedModeAndExistingApiKeyPlanSubscription() throws Exception {
        final String existingApiKeyPlanId = "existing-api-key-plan";

        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);
        when(plan.getApi()).thenReturn(API_ID);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        when(application.hasApiKeySharedMode()).thenReturn(true);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID))).thenReturn(application);

        PlanEntity existingApiKeyPlan = new PlanEntity();
        existingApiKeyPlan.setId(existingApiKeyPlanId);
        existingApiKeyPlan.setSecurity(PlanSecurityType.API_KEY);

        Subscription existingSubscription = buildTestSubscription("sub-1", API_ID, ACCEPTED, existingApiKeyPlanId, APPLICATION_ID, null);

        when(planService.findById(GraviteeContext.getExecutionContext(), existingApiKeyPlanId)).thenReturn(existingApiKeyPlan);

        when(
            subscriptionRepository.search(
                new SubscriptionCriteria.Builder().apis(Set.of(API_ID)).applications(Set.of(APPLICATION_ID)).build()
            )
        )
            .thenReturn(List.of(existingSubscription));
        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test
    public void shouldCreateWithoutProcess() throws Exception {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getValidation()).thenReturn(PlanValidationType.MANUAL);

        // Stub
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID)
        );

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        verify(subscriptionRepository, never()).update(any(Subscription.class));
        verify(apiKeyService, never()).generate(eq(GraviteeContext.getExecutionContext()), any(), any(), anyString());
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void shouldCreateWithoutProcess_AcceptedGCU() throws Exception {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        when(plan.getValidation()).thenReturn(PlanValidationType.MANUAL);
        PageEntity.PageRevisionId pageRevisionId = new PageEntity.PageRevisionId(PAGE_ID, 2);
        when(generalConditionPage.getContentRevisionId()).thenReturn(pageRevisionId);
        // Stub
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());
        when(pageService.findById(PAGE_ID)).thenReturn(generalConditionPage);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setGeneralConditionsContentRevision(new PageEntity.PageRevisionId(PAGE_ID, 2));
        newSubscriptionEntity.setGeneralConditionsAccepted(true);
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            newSubscriptionEntity
        );

        // Verify
        ArgumentCaptor<Subscription> subscriptionCapture = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, times(1)).create(subscriptionCapture.capture());
        verify(subscriptionRepository, never()).update(any(Subscription.class));
        verify(apiKeyService, never()).generate(eq(GraviteeContext.getExecutionContext()), any(), any(), anyString());

        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());

        Subscription capturedSuscription = subscriptionCapture.getValue();
        assertNotNull(capturedSuscription);
        assertEquals(subscriptionEntity.getId(), capturedSuscription.getId());
        assertEquals(subscriptionEntity.getApplication(), capturedSuscription.getApplication());
        assertEquals(newSubscriptionEntity.getGeneralConditionsAccepted(), capturedSuscription.getGeneralConditionsAccepted());
        assertEquals(
            newSubscriptionEntity.getGeneralConditionsContentRevision().getPageId(),
            capturedSuscription.getGeneralConditionsContentPageId()
        );
        assertEquals(
            Integer.valueOf(newSubscriptionEntity.getGeneralConditionsContentRevision().getRevision()),
            capturedSuscription.getGeneralConditionsContentRevision()
        );
    }

    @Test(expected = PlanGeneralConditionRevisionException.class)
    public void shouldNotCreateWithoutProcess_AcceptedOutdatedGCU() {
        // Prepare data
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);
        PageEntity.PageRevisionId pageRevisionId = new PageEntity.PageRevisionId(PAGE_ID, 2);
        when(generalConditionPage.getContentRevisionId()).thenReturn(pageRevisionId);
        // Stub
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(pageService.findById(PAGE_ID)).thenReturn(generalConditionPage);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setGeneralConditionsContentRevision(new PageEntity.PageRevisionId(PAGE_ID, 1));
        newSubscriptionEntity.setGeneralConditionsAccepted(true);
        subscriptionService.create(GraviteeContext.getExecutionContext(), newSubscriptionEntity);
    }

    @Test(expected = PlanGeneralConditionAcceptedException.class)
    public void shouldNotCreateWithoutProcess_NotAcceptedGCU() {
        // Prepare data
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);

        // Stub
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setGeneralConditionsContentRevision(new PageEntity.PageRevisionId(plan.getGeneralConditions() + "-1", 2));
        newSubscriptionEntity.setGeneralConditionsAccepted(false);
        subscriptionService.create(GraviteeContext.getExecutionContext(), newSubscriptionEntity);
    }

    @Test(expected = PlanGeneralConditionAcceptedException.class)
    public void shouldNotCreateWithoutProcess_AcceptedGCU_WithoutGCUContent() {
        // Prepare data
        when(plan.getGeneralConditions()).thenReturn(PAGE_ID);

        // Stub
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setGeneralConditionsContentRevision(null);
        newSubscriptionEntity.setGeneralConditionsAccepted(true);
        subscriptionService.create(GraviteeContext.getExecutionContext(), newSubscriptionEntity);
    }

    @Test
    public void shouldCreateWithAutomaticSubscription_forApiKey() throws Exception {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        Subscription subscription = buildTestSubscription(PENDING);

        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID + "@acme.net");
        when(userService.findById(GraviteeContext.getExecutionContext(), SUBSCRIBER_ID)).thenReturn(subscriberUser);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Stub
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(subscriptionRepository.create(any()))
            .thenAnswer(
                (Answer<Subscription>) invocation -> {
                    Subscription subscription1 = (Subscription) invocation.getArguments()[0];
                    subscription1.setId(SUBSCRIPTION_ID);
                    return subscription1;
                }
            );

        when(subscriptionRepository.findById(SUBSCRIPTION_ID))
            .thenAnswer(
                (Answer<Optional<Subscription>>) invocation -> {
                    subscription.setCreatedAt(new Date());
                    return Optional.of(subscription);
                }
            );

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID)
        );

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, times(1)).generate(eq(GraviteeContext.getExecutionContext()), any(), eq(subscriptionEntity), any());
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void shouldCreateWithAutomaticSubscription_notApiKey() throws Exception {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getValidation()).thenReturn(PlanValidationType.AUTO);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);

        ApplicationSettings settings = new ApplicationSettings();
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        clientSettings.setClientId("my-client-id");
        settings.setoAuthClient(clientSettings);
        when(application.getSettings()).thenReturn(settings);

        Subscription subscription = buildTestSubscription(PENDING);

        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID + "@acme.net");
        when(userService.findById(GraviteeContext.getExecutionContext(), SUBSCRIBER_ID)).thenReturn(subscriberUser);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Stub
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(subscriptionRepository.create(any()))
            .thenAnswer(
                (Answer<Subscription>) invocation -> {
                    Subscription subscription1 = (Subscription) invocation.getArguments()[0];
                    subscription1.setId(SUBSCRIPTION_ID);
                    return subscription1;
                }
            );

        when(subscriptionRepository.findById(SUBSCRIPTION_ID))
            .thenAnswer(
                (Answer<Optional<Subscription>>) invocation -> {
                    subscription.setCreatedAt(new Date());
                    return Optional.of(subscription);
                }
            );

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID)
        );

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, never()).generate(eq(GraviteeContext.getExecutionContext()), any(), any(), anyString());
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    private SecurityContext generateSecurityContext() {
        return new SecurityContext() {
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
                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

                    @Override
                    public String getName() {
                        return null;
                    }
                };
            }

            @Override
            public void setAuthentication(Authentication authentication) {}
        };
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotSubscribe_applicationWithoutClientId() {
        // Prepare data
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Stub
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotUpdateSubscriptionBecauseDoesNoExist() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);
    }

    @Test(expected = SubscriptionNotUpdatableException.class)
    public void shouldNotUpdateSubscriptionBecauseBadStatus() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        Subscription subscription = buildTestSubscription(PENDING);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        // Run
        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);
    }

    @Test
    public void shouldUpdateSubscriptionWithoutEndingDate() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        Subscription subscription = buildTestSubscription(ACCEPTED);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(plan.getApi()).thenReturn(API_ID);

        // Run
        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, never()).findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotCloseSubscriptionBecauseDoesNoExist() throws Exception {
        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.close(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test
    public void shouldCloseSubscription() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setEndingAt(new Date());

        final ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKey("api-key");
        apiKey.setRevoked(false);

        when(plan.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(singletonList(apiKey));
        when(apiService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(application.getPrimaryOwner()).thenReturn(mock(PrimaryOwnerEntity.class));

        subscriptionService.close(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);

        verify(apiKeyService).revoke(GraviteeContext.getExecutionContext(), apiKey, false);
        verify(notifierService).trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.SUBSCRIPTION_CLOSED), anyString(), anyMap());
        verify(notifierService)
            .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApplicationHook.SUBSCRIPTION_CLOSED), nullable(String.class), anyMap());
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotPauseSubscriptionBecauseDoesNoExist() throws Exception {
        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.pause(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test
    public void shouldPauseSubscription() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setEndingAt(new Date());

        final ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKey("api-key");
        apiKey.setRevoked(false);

        when(plan.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(singletonList(apiKey));
        when(apiService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(application.getPrimaryOwner()).thenReturn(mock(PrimaryOwnerEntity.class));

        subscriptionService.pause(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);

        verify(apiKeyService).update(GraviteeContext.getExecutionContext(), apiKey);
        verify(notifierService).trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.SUBSCRIPTION_PAUSED), anyString(), anyMap());
        verify(notifierService)
            .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApplicationHook.SUBSCRIPTION_PAUSED), nullable(String.class), anyMap());
    }

    @Test
    public void shouldThrowApiKeyAlreadyExistingException() throws Exception {
        // Prepare data
        final String customApiKey = "customApiKey";

        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(true);
        processSubscription.setCustomApiKey(customApiKey);

        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);
        subscription.setSubscribedBy(SUBSCRIBER_ID);

        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        ApplicationEntity applicationEntity = new ApplicationEntity();
        applicationEntity.setId(APPLICATION_ID);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(applicationEntity);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(apiKeyService.generate(any(), any(ApplicationEntity.class), any(SubscriptionEntity.class), anyString()))
            .thenThrow(new ApiKeyAlreadyExistingException());

        // Run
        assertThrows(
            ApiKeyAlreadyExistingException.class,
            () -> subscriptionService.process(GraviteeContext.getExecutionContext(), processSubscription, USER_ID)
        );

        verify(subscriptionRepository, times(0)).update(any());
        verify(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);
        verify(planService).findById(GraviteeContext.getExecutionContext(), PLAN_ID);

        ArgumentCaptor<ApplicationEntity> appCaptor = ArgumentCaptor.forClass(ApplicationEntity.class);
        ArgumentCaptor<SubscriptionEntity> subscriptionCaptor = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(apiKeyService)
            .generate(eq(GraviteeContext.getExecutionContext()), appCaptor.capture(), subscriptionCaptor.capture(), eq(customApiKey));
        Assertions.assertThat(appCaptor.getValue()).extracting(ApplicationEntity::getId).isEqualTo(APPLICATION_ID);
        Assertions
            .assertThat(subscriptionCaptor.getValue())
            .extracting(
                SubscriptionEntity::getId,
                SubscriptionEntity::getPlan,
                SubscriptionEntity::getApplication,
                SubscriptionEntity::getStatus
            )
            .containsExactly(SUBSCRIPTION_ID, PLAN_ID, APPLICATION_ID, SubscriptionStatus.ACCEPTED);
    }

    @Test
    public void shouldProcessButReject() throws Exception {
        // Prepare data
        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(false);

        Subscription subscription = buildTestSubscription(PENDING);

        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID + "@acme.net");
        when(userService.findById(GraviteeContext.getExecutionContext(), SUBSCRIBER_ID)).thenReturn(subscriberUser);

        when(plan.getApi()).thenReturn(API_ID);

        // Stub
        when(notifierService.hasEmailNotificationFor(eq(GraviteeContext.getExecutionContext()), any(), any(), anyMap(), anyString()))
            .thenReturn(false);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.process(
            GraviteeContext.getExecutionContext(),
            processSubscription,
            USER_ID
        );

        // Verify
        verify(apiKeyService, never()).generate(eq(GraviteeContext.getExecutionContext()), any(), any(), anyString());
        verify(userService).findById(GraviteeContext.getExecutionContext(), SUBSCRIBER_ID);
        verify(notifierService).triggerEmail(eq(GraviteeContext.getExecutionContext()), any(), anyString(), anyMap(), anyString());

        assertEquals(SubscriptionStatus.REJECTED, subscriptionEntity.getStatus());
        assertEquals(USER_ID, subscriptionEntity.getProcessedBy());
        assertNotNull(subscriptionEntity.getProcessedAt());
    }

    @Test
    public void shouldProcessButReject_EmailNotTriggered() throws Exception {
        // Prepare data
        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(false);

        Subscription subscription = buildTestSubscription(PENDING);

        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID + "@acme.net");
        when(userService.findById(GraviteeContext.getExecutionContext(), SUBSCRIBER_ID)).thenReturn(subscriberUser);
        when(plan.getApi()).thenReturn(API_ID);

        // Stub
        when(notifierService.hasEmailNotificationFor(eq(GraviteeContext.getExecutionContext()), any(), any(), anyMap(), anyString()))
            .thenReturn(true);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.process(
            GraviteeContext.getExecutionContext(),
            processSubscription,
            USER_ID
        );

        // Verify
        verify(apiKeyService, never()).generate(eq(GraviteeContext.getExecutionContext()), any(), any(), anyString());
        verify(userService).findById(GraviteeContext.getExecutionContext(), SUBSCRIBER_ID);
        verify(notifierService, never()).triggerEmail(eq(GraviteeContext.getExecutionContext()), any(), anyString(), anyMap(), anyString());

        assertEquals(SubscriptionStatus.REJECTED, subscriptionEntity.getStatus());
        assertEquals(USER_ID, subscriptionEntity.getProcessedBy());
        assertNotNull(subscriptionEntity.getProcessedAt());
    }

    @Test
    public void shouldProcessWithAvailableCustomApiKeyForAcceptedSubscription() throws Exception {
        // Prepare data
        final String customApiKey = "customApiKey";

        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(true);
        processSubscription.setCustomApiKey(customApiKey);

        Subscription subscription = buildTestSubscription(PENDING);

        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        final UserEntity subscriberUser = new UserEntity();
        subscriberUser.setEmail(SUBSCRIBER_ID + "@acme.net");
        when(userService.findById(GraviteeContext.getExecutionContext(), SUBSCRIBER_ID)).thenReturn(subscriberUser);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.process(
            GraviteeContext.getExecutionContext(),
            processSubscription,
            USER_ID
        );

        // Verify
        verify(apiKeyService, times(1)).generate(eq(GraviteeContext.getExecutionContext()), any(), any(), anyString());
        assertEquals(SubscriptionStatus.ACCEPTED, subscriptionEntity.getStatus());
        assertEquals(USER_ID, subscriptionEntity.getProcessedBy());
        assertNotNull(subscriptionEntity.getProcessedAt());
    }

    @Test
    public void shouldNotProcessBecauseCustomApiKeyAlreadyExists() throws Exception {
        // Prepare data
        final String customApiKey = "customApiKey";

        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(true);
        processSubscription.setCustomApiKey(customApiKey);

        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(Subscription.Status.PENDING);
        subscription.setSubscribedBy(SUBSCRIBER_ID);

        ApplicationEntity applicationEntity = new ApplicationEntity();
        applicationEntity.setId(APPLICATION_ID);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(applicationEntity);

        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(apiKeyService.generate(any(), any(), any(), anyString())).thenThrow(new ApiKeyAlreadyExistingException());

        // Run
        final ApiKeyAlreadyExistingException exception = assertThrows(
            ApiKeyAlreadyExistingException.class,
            () -> subscriptionService.process(GraviteeContext.getExecutionContext(), processSubscription, USER_ID)
        );

        verify(subscriptionRepository, times(0)).update(any());
        verify(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);
        verify(planService).findById(GraviteeContext.getExecutionContext(), PLAN_ID);
        assertEquals("API key already exists", exception.getMessage());
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotProcessBecauseClosedPlan() throws Exception {
        // Prepare data
        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(false);

        Subscription subscription = buildTestSubscription(PENDING);

        when(plan.getStatus()).thenReturn(PlanStatus.CLOSED);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.process(GraviteeContext.getExecutionContext(), processSubscription, USER_ID);
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecauseNoClientId_oauth2() {
        // Stub
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecauseNoClientId_jwt() {
        // Stub
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecauseExistingSubscription_oauth2() {
        when(plan.getSecurity()).thenReturn(PlanSecurityType.OAUTH2);
        when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);

        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test
    public void shouldTransferSubscription() throws Exception {
        final TransferSubscriptionEntity transferSubscription = new TransferSubscriptionEntity();
        transferSubscription.setId(SUBSCRIPTION_ID);
        transferSubscription.setPlan(PLAN_ID);

        when(subscription.getApplication()).thenReturn(APPLICATION_ID);
        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.getStatus()).thenReturn(ACCEPTED);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenReturn(subscription);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);

        subscriptionService.transfer(GraviteeContext.getExecutionContext(), transferSubscription, USER_ID);

        verify(notifierService)
            .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.SUBSCRIPTION_TRANSFERRED), anyString(), anyMap());
        verify(notifierService)
            .trigger(
                eq(GraviteeContext.getExecutionContext()),
                eq(ApplicationHook.SUBSCRIPTION_TRANSFERRED),
                nullable(String.class),
                anyMap()
            );
        verify(subscription).setUpdatedAt(any());
    }

    @Test(expected = TransferNotAllowedException.class)
    public void shouldNotTransferSubscription_onPlanWithGeneralConditions() throws Exception {
        final TransferSubscriptionEntity transferSubscription = new TransferSubscriptionEntity();
        transferSubscription.setId(SUBSCRIPTION_ID);
        transferSubscription.setPlan(PLAN_ID);

        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(plan.getStatus()).thenReturn(PlanStatus.PUBLISHED);
        when(plan.getGeneralConditions()).thenReturn("SOME_PAGE");
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        subscriptionService.transfer(GraviteeContext.getExecutionContext(), transferSubscription, USER_ID);
    }

    @Test(expected = PlanRestrictedException.class)
    public void shouldNotCreateBecauseRestricted() {
        // Stub
        when(plan.getExcludedGroups()).thenReturn(asList("excl1", "excl2"));
        when(plan.getApi()).thenReturn("api1");
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        final GroupEntity group = new GroupEntity();
        group.setId("excl2");

        final SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authMock = new TestingAuthenticationToken(null, null, "USER");
        when(securityContext.getAuthentication()).thenReturn(authMock);
        SecurityContextHolder.setContext(securityContext);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test
    public void shouldCreateWithGroupRestriction_BecauseAdmin() throws Exception {
        // Prepare data
        when(plan.getExcludedGroups()).thenReturn(asList("excl1", "excl2"));
        when(plan.getApi()).thenReturn("api1");
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        // Stub
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiService.findByIdForTemplates(GraviteeContext.getExecutionContext(), "api1")).thenReturn(apiModelEntity);

        when(subscriptionRepository.create(any()))
            .thenAnswer(
                (Answer<Subscription>) invocation -> {
                    Subscription subscription = (Subscription) invocation.getArguments()[0];
                    subscription.setId(SUBSCRIPTION_ID);
                    return subscription;
                }
            );

        final SecurityContext securityContext = mock(SecurityContext.class);
        UserDetails principal = new UserDetails("toto", "pwdtoto", asList(new SimpleGrantedAuthority(ENVIRONMENT_ADMIN)));
        Authentication authMock = new TestingAuthenticationToken(principal, null, ENVIRONMENT_ADMIN);
        when(securityContext.getAuthentication()).thenReturn(authMock);
        SecurityContextHolder.setContext(securityContext);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID)
        );

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void search_pageable_with_apikey_should_find_api_keys_by_key_and_filter_results_according_to_query() throws TechnicalException {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApiKey("searched-api-key");
        query.setApis(List.of("api-id-1", "api-id-3"));
        query.setStatuses(List.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACCEPTED));

        when(apiKeyService.findByKey(GraviteeContext.getExecutionContext(), "searched-api-key"))
            .thenReturn(
                List.of(
                    buildTestApiKeyForSubscription("subscription-1"),
                    buildTestApiKeyForSubscription("subscription-2"),
                    buildTestApiKeyForSubscription("subscription-3"),
                    buildTestApiKeyForSubscription("subscription-4")
                )
            );

        // subscription1 should be returned cause matches api and status query
        Subscription subscription1 = buildTestSubscription("sub1", "api-id-1", ACCEPTED);
        when(subscriptionRepository.findByIdIn(List.of("subscription-1"))).thenReturn(List.of(subscription1));

        // subscription2 should be filtered cause API id doesn't match
        Subscription subscription2 = buildTestSubscription("sub2", "api-id-2", PENDING);
        when(subscriptionRepository.findByIdIn(List.of("subscription-2"))).thenReturn(List.of(subscription2));

        // subscription3 should be returned cause matches api and status query
        Subscription subscription3 = buildTestSubscription("sub3", "api-id-3", PENDING);
        when(subscriptionRepository.findByIdIn(List.of("subscription-3"))).thenReturn(List.of(subscription3));

        // subscription4 should be filtered cause status doesn't match
        Subscription subscription4 = buildTestSubscription("sub4", "api-id-4", PAUSED);
        when(subscriptionRepository.findByIdIn(List.of("subscription-4"))).thenReturn(List.of(subscription4));

        Page<SubscriptionEntity> page = subscriptionService.search(
            GraviteeContext.getExecutionContext(),
            query,
            Mockito.mock(Pageable.class)
        );

        assertEquals(2, page.getTotalElements());
        assertEquals("sub1", page.getContent().get(0).getId());
        assertEquals("sub3", page.getContent().get(1).getId());
    }

    @Test
    public void shouldGetMetadataWithNoSubscriptions() {
        SubscriptionMetadataQuery query = new SubscriptionMetadataQuery("DEFAULT", "DEFAULT", List.of());
        Metadata metadata = subscriptionService.getMetadata(GraviteeContext.getExecutionContext(), query);
        assertNotNull(metadata);
        assertTrue(metadata.toMap().isEmpty());
    }

    @Test
    public void shouldGetAllMetadataWithSubscriptions() {
        when(apiEntity.getId()).thenReturn(API_ID);
        when(apiService.findByEnvironmentAndIdIn(GraviteeContext.getExecutionContext(), Set.of(API_ID))).thenReturn(Set.of(apiEntity));
        final SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION_ID);
        subscriptionEntity.setApplication(APPLICATION_ID);
        subscriptionEntity.setApi(API_ID);
        subscriptionEntity.setSubscribedBy(SUBSCRIBER_ID);
        subscriptionEntity.setPlan(PLAN_ID);
        SubscriptionMetadataQuery query = new SubscriptionMetadataQuery("DEFAULT", "DEFAULT", Arrays.asList(subscriptionEntity))
            .withApplications(true)
            .withPlans(true)
            .withApis(true)
            .withSubscribers(true)
            .includeDetails();

        Metadata metadata = subscriptionService.getMetadata(GraviteeContext.getExecutionContext(), query);
        assertFalse(metadata.toMap().isEmpty());

        assertNotNull(metadata);
        Mockito.verify(applicationService, times(1)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(APPLICATION_ID)));
        Mockito.verify(apiService, times(1)).findByEnvironmentAndIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(API_ID)));
        Mockito.verify(planService, times(1)).findByIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(PLAN_ID)));
        Mockito.verify(userService, times(1)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(SUBSCRIBER_ID)));
        Mockito.verify(apiService, times(1)).calculateEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);
    }

    @Test
    public void shouldGetEmptyMetadataWithSubscriptions() {
        final SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION_ID);
        subscriptionEntity.setApplication(APPLICATION_ID);
        subscriptionEntity.setApi(API_ID);
        subscriptionEntity.setSubscribedBy(SUBSCRIBER_ID);
        subscriptionEntity.setPlan(PLAN_ID);
        SubscriptionMetadataQuery query = new SubscriptionMetadataQuery("DEFAULT", "DEFAULT", Arrays.asList(subscriptionEntity))
            .withApplications(false)
            .withPlans(false)
            .withApis(false)
            .withSubscribers(false)
            .excludeDetails();

        Metadata metadata = subscriptionService.getMetadata(GraviteeContext.getExecutionContext(), query);

        assertNotNull(metadata);
        Mockito.verify(applicationService, times(0)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(APPLICATION_ID)));
        Mockito.verify(apiService, times(0)).findByEnvironmentAndIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(API_ID)));
        Mockito.verify(planService, times(0)).findByIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(PLAN_ID)));
        Mockito.verify(userService, times(0)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(SUBSCRIBER_ID)));
        Mockito.verify(apiService, times(0)).calculateEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);
    }

    @Test
    public void shouldFillApiMetadataAfterService() {
        when(apiEntity.getId()).thenReturn(API_ID);
        when(apiService.findByEnvironmentAndIdIn(GraviteeContext.getExecutionContext(), Set.of(API_ID))).thenReturn(Set.of(apiEntity));
        final SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION_ID);
        subscriptionEntity.setApplication(APPLICATION_ID);
        subscriptionEntity.setApi(API_ID);
        subscriptionEntity.setSubscribedBy(SUBSCRIBER_ID);
        subscriptionEntity.setPlan(PLAN_ID);
        BiFunction<Metadata, ApiEntity, ApiEntity> delegate = mock(BiFunction.class);
        SubscriptionMetadataQuery query = new SubscriptionMetadataQuery("DEFAULT", "DEFAULT", Arrays.asList(subscriptionEntity))
            .withApis(true)
            .fillApiMetadata(delegate);

        Metadata metadata = subscriptionService.getMetadata(GraviteeContext.getExecutionContext(), query);
        assertFalse(metadata.toMap().isEmpty());

        assertNotNull(metadata);
        Mockito.verify(applicationService, times(0)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(APPLICATION_ID)));
        Mockito.verify(apiService, times(1)).findByEnvironmentAndIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(API_ID)));
        Mockito.verify(planService, times(0)).findByIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(PLAN_ID)));
        Mockito.verify(userService, times(0)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(SUBSCRIBER_ID)));
        Mockito.verify(apiService, times(0)).calculateEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);
        Mockito.verify(delegate, times(1)).apply(any(Metadata.class), eq(apiEntity));
    }

    @Test
    public void search_should_not_fill_planSecurity_nor_apiKeys_if_boolean_to_false() throws TechnicalException {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApis(List.of("api-id-1"));

        Subscription subscription1 = buildTestSubscription("sub1", "api-id-1", ACCEPTED, "plan-id", null, null);
        when(subscriptionRepository.search(any(), any())).thenReturn(new Page<>(List.of(subscription1), 1, 1, 1));

        Page<SubscriptionEntity> page = subscriptionService.search(
            GraviteeContext.getExecutionContext(),
            query,
            Mockito.mock(Pageable.class),
            false,
            false
        );

        assertEquals("sub1", page.getContent().get(0).getId());
        assertNull(page.getContent().get(0).getSecurity());
        assertNull(page.getContent().get(0).getKeys());
        verifyNoInteractions(planService);
        verifyNoInteractions(apiKeyService);
    }

    @Test
    public void search_should_fill_plan_security_if_boolean_to_true() throws TechnicalException {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApis(List.of("api-id-1"));

        Subscription subscription1 = buildTestSubscription("sub1", "api-id-1", ACCEPTED, "plan-id", null, null);
        when(subscriptionRepository.search(any(), any())).thenReturn(new Page<>(List.of(subscription1), 1, 1, 1));

        PlanEntity foundPlan = new PlanEntity();
        foundPlan.setId("plan-id");
        foundPlan.setSecurity(PlanSecurityType.OAUTH2);
        when(planService.findByIdIn(GraviteeContext.getExecutionContext(), Set.of("plan-id"))).thenReturn(Set.of(foundPlan));

        Page<SubscriptionEntity> page = subscriptionService.search(
            GraviteeContext.getExecutionContext(),
            query,
            Mockito.mock(Pageable.class),
            false,
            true
        );

        assertEquals("sub1", page.getContent().get(0).getId());
        assertEquals("OAUTH2", page.getContent().get(0).getSecurity());
        assertNull(page.getContent().get(0).getKeys());
        verifyNoInteractions(apiKeyService);
    }

    @Test
    public void search_should_fill_apikeys_if_boolean_to_true() throws TechnicalException {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApis(List.of("api-id-1"));

        Subscription subscription1 = buildTestSubscription("sub1", "api-id-1", ACCEPTED, "plan-id", null, null);
        when(subscriptionRepository.search(any(), any())).thenReturn(new Page<>(List.of(subscription1), 1, 1, 1));

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setKey("my-api-key");
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), "sub1")).thenReturn(List.of(apiKey));

        Page<SubscriptionEntity> page = subscriptionService.search(
            GraviteeContext.getExecutionContext(),
            query,
            Mockito.mock(Pageable.class),
            true,
            false
        );

        assertEquals("sub1", page.getContent().get(0).getId());
        assertNull(page.getContent().get(0).getSecurity());
        assertEquals("my-api-key", page.getContent().get(0).getKeys().get(0));
        verifyNoInteractions(planService);
    }

    @Test
    public void close_should_revoke_apikeys_if_not_shared_mode() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setEndingAt(new Date());

        when(application.hasApiKeySharedMode()).thenReturn(false);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID))).thenReturn(application);

        List<ApiKeyEntity> apiKeys = List.of(
            buildTestApiKey("apikey-1", false, false),
            buildTestApiKey("apikey-2", true, false),
            buildTestApiKey("apikey-3", false, false),
            buildTestApiKey("apikey-4", false, true)
        );
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(apiKeys);

        subscriptionService.close(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);

        // assert api keys 1 and 3 have been revoked, but not 2 and 4 because it's already revoked or expired
        verify(apiKeyService, times(1)).findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        verify(apiKeyService, times(1)).revoke(GraviteeContext.getExecutionContext(), apiKeys.get(0), false);
        verify(apiKeyService, times(1)).revoke(GraviteeContext.getExecutionContext(), apiKeys.get(2), false);
        verifyNoMoreInteractions(apiKeyService);
    }

    @Test
    public void close_should_not_revoke_apikeys_if_shared_mode() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setEndingAt(new Date());

        when(application.hasApiKeySharedMode()).thenReturn(true);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID))).thenReturn(application);

        List<ApiKeyEntity> apiKeys = List.of(
            buildTestApiKey("apikey-1", false, false),
            buildTestApiKey("apikey-2", true, false),
            buildTestApiKey("apikey-3", false, false),
            buildTestApiKey("apikey-4", false, true)
        );
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(apiKeys);

        subscriptionService.close(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);

        // no key has been revoked, as their application use shared api key mode
        // but non revoked keys have been updated to ensure cache refresh
        verify(apiKeyService, times(1)).findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        verify(apiKeyService, times(1)).update(GraviteeContext.getExecutionContext(), apiKeys.get(0));
        verify(apiKeyService, times(1)).update(GraviteeContext.getExecutionContext(), apiKeys.get(2));
        verifyNoMoreInteractions(apiKeyService);
    }

    @Test
    public void update_should_update_subscription_with_endingDate_and_set_apiKey_expiration_if_not_shared_apikey() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        when(application.hasApiKeySharedMode()).thenReturn(false);

        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        List<ApiKeyEntity> apiKeys = List.of(
            buildTestApiKey("apikey-1", false, false),
            buildTestApiKey("apikey-2", true, false),
            buildTestApiKey("apikey-3", false, false),
            buildTestApiKey("apikey-4", false, true)
        );
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(apiKeys);

        // Run
        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);

        // verify apikey 1 and 3 expiration date has been updated as they are not revoked nor expired
        verify(subscriptionRepository, times(1)).update(subscription);
        verify(apiKeyService, times(1)).findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        verify(apiKeyService, times(1))
            .update(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiKey -> apiKey.getId().equals("apikey-1") && apiKey.getExpireAt().equals(updatedSubscription.getEndingAt()))
            );
        verify(apiKeyService, times(1))
            .update(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiKey -> apiKey.getId().equals("apikey-3") && apiKey.getExpireAt().equals(updatedSubscription.getEndingAt()))
            );
        verifyNoMoreInteractions(apiKeyService);
    }

    @Test
    public void update_should_update_subscription_with_endingDate_and_dont_set_apiKey_expiration_if_shared_apikey() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        when(application.hasApiKeySharedMode()).thenReturn(true);

        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(planService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);
        when(plan.getApi()).thenReturn(API_ID);
        when(plan.getSecurity()).thenReturn(PlanSecurityType.API_KEY);

        List<ApiKeyEntity> apiKeys = List.of(
            buildTestApiKey("apikey-1", false, false),
            buildTestApiKey("apikey-2", true, false),
            buildTestApiKey("apikey-3", false, false),
            buildTestApiKey("apikey-4", false, true)
        );
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(apiKeys);

        // Run
        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);

        // subscription has been updated, but no api key has been updated cause they are shared with other subscriptions
        verify(subscriptionRepository, times(1)).update(subscription);
        verify(apiKeyService, times(1)).findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        verifyNoMoreInteractions(apiKeyService);
    }

    private ApiKeyEntity buildTestApiKey(String id, boolean revoked, boolean expired) {
        ApiKeyEntity apikey = buildTestApiKeyForSubscription(SUBSCRIPTION_ID);
        apikey.setId(id);
        apikey.setRevoked(revoked);
        apikey.setExpired(expired);
        return apikey;
    }

    private ApiKeyEntity buildTestApiKeyForSubscription(String subscriptionId) {
        ApiKeyEntity apikey = new ApiKeyEntity();
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(subscriptionId);
        apikey.setSubscriptions(Set.of(subscription));
        apikey.setApplication(application);
        return apikey;
    }

    private Subscription buildTestSubscription(Subscription.Status status) {
        return buildTestSubscription(SUBSCRIPTION_ID, null, status, PLAN_ID, APPLICATION_ID, SUBSCRIBER_ID);
    }

    private Subscription buildTestSubscription(String id, String api, Subscription.Status status) {
        return buildTestSubscription(id, api, status, null, null, null);
    }

    private Subscription buildTestSubscription(
        String id,
        String api,
        Subscription.Status status,
        String plan,
        String application,
        String subscribedBy
    ) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setApi(api);
        subscription.setStatus(status);
        subscription.setPlan(plan);
        subscription.setApplication(application);
        subscription.setSubscribedBy(subscribedBy);
        return subscription;
    }
}
