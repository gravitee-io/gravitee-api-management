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

import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;
import static io.gravitee.repository.management.model.Subscription.Status.PAUSED;
import static io.gravitee.repository.management.model.Subscription.Status.PENDING;
import static io.gravitee.repository.management.model.Subscription.Status.REJECTED;
import static io.gravitee.rest.api.model.v4.plan.PlanValidationType.AUTO;
import static io.gravitee.rest.api.model.v4.plan.PlanValidationType.MANUAL;
import static io.gravitee.rest.api.service.impl.AbstractService.ENVIRONMENT_ADMIN;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.subscription.domain_service.AcceptSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.apim.infra.adapter.SubscriptionAdapterImpl;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApiModel;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.PlanValidationType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.ProcessSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.TransferSubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.application.TlsSettings;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanGeneralConditionAcceptedException;
import io.gravitee.rest.api.service.exceptions.PlanGeneralConditionRevisionException;
import io.gravitee.rest.api.service.exceptions.PlanMtlsAlreadySubscribedException;
import io.gravitee.rest.api.service.exceptions.PlanNotSubscribableException;
import io.gravitee.rest.api.service.exceptions.PlanNotSubscribableWithSharedApiKeyException;
import io.gravitee.rest.api.service.exceptions.PlanNotYetPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanRestrictedException;
import io.gravitee.rest.api.service.exceptions.SubscriptionConsumerStatusNotUpdatableException;
import io.gravitee.rest.api.service.exceptions.SubscriptionFailureException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotPausableException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotPausedException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotUpdatableException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.TransferNotAllowedException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.validation.SubscriptionValidationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.apache.commons.lang3.time.FastDateFormat;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
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
    private static final String RFC_3339_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final FastDateFormat dateFormatter = FastDateFormat.getInstance(RFC_3339_DATE_FORMAT);

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService = new SubscriptionServiceImpl();

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiTemplateService apiTemplateService;

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private Subscription subscription;

    private ApplicationEntity application;

    @Mock
    private ApiEntity apiEntity;

    @Mock
    private ApiModel apiModelEntity;

    @Mock
    private AuditService auditService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private GroupService groupService;

    @Mock
    private InstallationAccessQueryService installationAccessQueryService;

    @Mock
    private UserService userService;

    @Mock
    private PageEntity generalConditionPage;

    @Mock
    private PageService pageService;

    @Mock
    private ApiEntrypointService apiEntrypointService;

    @Mock
    private SubscriptionValidationService subscriptionValidationService;

    @Mock
    private SubscriptionAdapter subscriptionAdapter;

    @Mock
    private AcceptSubscriptionDomainService acceptSubscriptionDomainService;

    @Mock
    private RejectSubscriptionDomainService rejectSubscriptionDomainService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private PlanEntity planEntity;

    private io.gravitee.rest.api.model.v4.plan.PlanEntity planEntityV4;

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

    @Before
    public void before() {
        planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setApi(API_ID);
        planEntity.setEnvironmentId(GraviteeContext.getDefaultEnvironment());

        planEntityV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        planEntityV4.setId(PLAN_ID);
        planEntityV4.setApiId(API_ID);
        planEntityV4.setEnvironmentId(GraviteeContext.getDefaultEnvironment());

        application = new ApplicationEntity();
        application.setId(APPLICATION_ID);
        application.setEnvironmentId(GraviteeContext.getDefaultEnvironment());

        when(subscriptionAdapter.map(any())).thenAnswer(invocation -> new SubscriptionAdapterImpl().map(invocation.getArgument(0)));
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

        when(subscriptionRepository.search(SubscriptionCriteria.builder().applications(singleton(APPLICATION_ID)).build()))
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

        when(subscriptionRepository.search(SubscriptionCriteria.builder().apis(singleton(API_ID)).applications(null).build()))
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

        when(subscriptionRepository.search(SubscriptionCriteria.builder().plans(singleton(PLAN_ID)).build()))
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
        planEntity.setStatus(PlanStatus.STAGING);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotCreateBecausePlanAlreadyClosed() {
        // Stub
        planEntity.setStatus(PlanStatus.CLOSED);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecausePlanAlreadyDeprecated() {
        // Stub
        planEntity.setStatus(PlanStatus.DEPRECATED);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecausePlanKeyless() {
        // Stub
        planEntity.setSecurity(PlanSecurityType.KEY_LESS);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableWithSharedApiKeyException.class)
    public void shouldNotCreateBecauseSharedModeAndExistingApiKeyPlanSubscription() throws Exception {
        final String existingApiKeyPlanId = "existing-api-key-plan";

        planEntity.setSecurity(PlanSecurityType.API_KEY);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        application.setApiKeyMode(ApiKeyMode.SHARED);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID))).thenReturn(application);

        PlanEntity existingApiKeyPlan = new PlanEntity();
        existingApiKeyPlan.setId(existingApiKeyPlanId);
        existingApiKeyPlan.setSecurity(PlanSecurityType.API_KEY);

        Subscription existingSubscription = buildTestSubscription("sub-1", API_ID, ACCEPTED, existingApiKeyPlanId, APPLICATION_ID, null);

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), existingApiKeyPlanId)).thenReturn(existingApiKeyPlan);

        when(
            subscriptionRepository.search(SubscriptionCriteria.builder().apis(Set.of(API_ID)).applications(Set.of(APPLICATION_ID)).build())
        )
            .thenReturn(List.of(existingSubscription));
        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanMtlsAlreadySubscribedException.class)
    public void shouldNotCreateBecauseExistingMtlsPlanSubscription() throws Exception {
        final String existingMtlsPlanId = "existing-mtls-plan";

        planEntity.setSecurity(PlanSecurityType.MTLS);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID))).thenReturn(application);

        PlanEntity existingApiKeyPlan = new PlanEntity();
        existingApiKeyPlan.setId(existingMtlsPlanId);
        existingApiKeyPlan.setSecurity(PlanSecurityType.MTLS);

        Subscription existingSubscription = buildTestSubscription("sub-1", API_ID, ACCEPTED, existingMtlsPlanId, APPLICATION_ID, null);

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), existingMtlsPlanId)).thenReturn(existingApiKeyPlan);

        when(
            subscriptionRepository.search(SubscriptionCriteria.builder().apis(Set.of(API_ID)).applications(Set.of(APPLICATION_ID)).build())
        )
            .thenReturn(List.of(existingSubscription));
        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test
    public void shouldCreateWithoutProcess() throws Exception {
        // Prepare data
        planEntity.setValidation(PlanValidationType.MANUAL);

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        SecurityContextHolder.setContext(generateSecurityContext());

        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            newSubscriptionEntity
        );

        // Verify
        verify(subscriptionRepository, times(1)).create(any(Subscription.class));
        verify(subscriptionRepository, never()).update(any(Subscription.class));
        verifyNoInteractions(rejectSubscriptionDomainService, acceptSubscriptionDomainService);
        verify(subscriptionValidationService, times(1)).validateAndSanitize(any(), eq(newSubscriptionEntity));
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void shouldCreateWithClientCertificateForMtlsPlan() throws Exception {
        // Prepare data
        planEntity.setValidation(PlanValidationType.MANUAL);
        planEntity.setSecurity(PlanSecurityType.MTLS);
        application.setSettings(ApplicationSettings.builder().tls(TlsSettings.builder().clientCertificate("certificate").build()).build());

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        SecurityContextHolder.setContext(generateSecurityContext());

        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            newSubscriptionEntity
        );

        // Verify
        ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, times(1)).create(subscriptionArgumentCaptor.capture());
        verify(subscriptionRepository, never()).update(any(Subscription.class));
        verifyNoInteractions(rejectSubscriptionDomainService, acceptSubscriptionDomainService);
        verify(subscriptionValidationService, times(1)).validateAndSanitize(any(), eq(newSubscriptionEntity));
        assertThat(subscriptionEntity.getId()).isNotNull();
        assertThat(subscriptionEntity.getApplication()).isNotNull();
        assertThat(subscriptionEntity.getCreatedAt()).isNotNull();
        assertThat(subscriptionEntity.getClientCertificate()).isEqualTo("certificate");
        assertThat(subscriptionArgumentCaptor.getValue().getClientCertificate())
            .isEqualTo(Base64.getEncoder().encodeToString("certificate".getBytes()));
    }

    @Test
    public void shouldCreateWithClientCertificateWithoutSavingCertificateForOtherPlanThanMtls() throws Exception {
        // Prepare data
        planEntity.setValidation(PlanValidationType.MANUAL);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        application.setSettings(ApplicationSettings.builder().tls(TlsSettings.builder().clientCertificate("certificate").build()).build());

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        SecurityContextHolder.setContext(generateSecurityContext());

        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            newSubscriptionEntity
        );

        // Verify
        ArgumentCaptor<Subscription> subscriptionArgumentCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, times(1)).create(subscriptionArgumentCaptor.capture());
        verify(subscriptionRepository, never()).update(any(Subscription.class));
        verifyNoInteractions(rejectSubscriptionDomainService, acceptSubscriptionDomainService);
        verify(subscriptionValidationService, times(1)).validateAndSanitize(any(), eq(newSubscriptionEntity));
        assertThat(subscriptionEntity.getId()).isNotNull();
        assertThat(subscriptionEntity.getApplication()).isNotNull();
        assertThat(subscriptionEntity.getCreatedAt()).isNotNull();
        assertThat(subscriptionEntity.getClientCertificate()).isNull();
        assertThat(subscriptionArgumentCaptor.getValue().getClientCertificate()).isNull();
    }

    @Test
    public void should_create_with_exclusive_apiKeyMode() throws Exception {
        // Prepare data
        planEntity.setValidation(PlanValidationType.MANUAL);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        application.setApiKeyMode(ApiKeyMode.UNSPECIFIED);

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        SecurityContextHolder.setContext(generateSecurityContext());

        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setApiKeyMode(ApiKeyMode.EXCLUSIVE);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            newSubscriptionEntity
        );

        // Verify
        verify(applicationService, times(1)).updateApiKeyMode(GraviteeContext.getExecutionContext(), APPLICATION_ID, ApiKeyMode.EXCLUSIVE);
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void should_create_second_subscription_with_shared_apiKeyMode() throws Exception {
        // Prepare data
        planEntity.setValidation(PlanValidationType.MANUAL);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        application.setApiKeyMode(ApiKeyMode.UNSPECIFIED);

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        var otherPlan = new PlanEntity();
        otherPlan.setId("planId");
        otherPlan.setSecurity(PlanSecurityType.API_KEY);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), otherPlan.getId())).thenReturn(otherPlan);

        var otherSubscription = new Subscription();
        otherSubscription.setPlan(otherPlan.getId());
        otherSubscription.setStatus(ACCEPTED);
        when(subscriptionRepository.search(any(SubscriptionCriteria.class))).thenReturn(List.of(otherSubscription));

        SecurityContextHolder.setContext(generateSecurityContext());

        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setApiKeyMode(ApiKeyMode.SHARED);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            newSubscriptionEntity
        );

        // Verify
        verify(subscriptionRepository, times(1))
            .search(
                argThat(criteria ->
                    criteria.getApplications().contains(APPLICATION_ID) &&
                    criteria.getStatuses() != null &&
                    criteria.getStatuses().containsAll(List.of(ACCEPTED.name(), PAUSED.name(), PENDING.name()))
                )
            );
        verify(applicationService, times(1)).updateApiKeyMode(GraviteeContext.getExecutionContext(), APPLICATION_ID, ApiKeyMode.SHARED);
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void should_create_second_subscription_with_exclusive_apiKeyMode_by_default() throws Exception {
        // Prepare data
        planEntity.setValidation(PlanValidationType.MANUAL);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        application.setApiKeyMode(ApiKeyMode.UNSPECIFIED);

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        var otherPlan = new PlanEntity();
        otherPlan.setId("planId");
        otherPlan.setSecurity(PlanSecurityType.API_KEY);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), otherPlan.getId())).thenReturn(otherPlan);

        var otherSubscription = new Subscription();
        otherSubscription.setPlan(otherPlan.getId());
        otherSubscription.setStatus(ACCEPTED);
        when(subscriptionRepository.search(any(SubscriptionCriteria.class))).thenReturn(List.of(otherSubscription));

        SecurityContextHolder.setContext(generateSecurityContext());

        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            newSubscriptionEntity
        );

        // Verify
        verify(subscriptionRepository, times(1))
            .search(
                argThat(criteria ->
                    criteria.getApplications().contains(APPLICATION_ID) &&
                    criteria.getStatuses() != null &&
                    criteria.getStatuses().containsAll(List.of(ACCEPTED.name(), PAUSED.name(), PENDING.name()))
                )
            );
        verify(applicationService, times(1)).updateApiKeyMode(GraviteeContext.getExecutionContext(), APPLICATION_ID, ApiKeyMode.EXCLUSIVE);
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void shouldCreateWithSubscriptionPlanWithoutProcess() throws Exception {
        // Prepare data
        planEntityV4.setValidation(io.gravitee.rest.api.model.v4.plan.PlanValidationType.MANUAL);
        planEntityV4.setMode(PlanMode.PUSH);

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntityV4);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any())).thenAnswer(returnsFirstArg());

        SecurityContextHolder.setContext(generateSecurityContext());

        ArgumentCaptor<Subscription> subscriptionCapture = ArgumentCaptor.forClass(Subscription.class);

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID)
        );

        // Verify
        verify(subscriptionRepository, times(1)).create(subscriptionCapture.capture());
        verify(subscriptionRepository, never()).update(any(Subscription.class));
        verify(apiKeyService, never()).generate(eq(GraviteeContext.getExecutionContext()), any(), any(), anyString());
        verify(subscriptionValidationService, times(1)).validateAndSanitize(any(), any(NewSubscriptionEntity.class));
        assertEquals(Subscription.Type.PUSH, subscriptionCapture.getValue().getType());
        assertNotNull(subscriptionEntity.getId());
        assertNotNull(subscriptionEntity.getApplication());
        assertNotNull(subscriptionEntity.getCreatedAt());
    }

    @Test
    public void shouldCreateWithoutProcess_AcceptedGCU() throws Exception {
        // Prepare data
        planEntity.setGeneralConditions(PAGE_ID);
        planEntity.setValidation(PlanValidationType.MANUAL);
        PageEntity.PageRevisionId pageRevisionId = new PageEntity.PageRevisionId(PAGE_ID, 2);
        when(generalConditionPage.getContentRevisionId()).thenReturn(pageRevisionId);

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
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

        Subscription capturedSubscription = subscriptionCapture.getValue();
        assertNotNull(capturedSubscription);
        assertEquals(subscriptionEntity.getId(), capturedSubscription.getId());
        assertEquals(subscriptionEntity.getApplication(), capturedSubscription.getApplication());
        assertEquals(newSubscriptionEntity.getGeneralConditionsAccepted(), capturedSubscription.getGeneralConditionsAccepted());
        assertEquals(
            newSubscriptionEntity.getGeneralConditionsContentRevision().getPageId(),
            capturedSubscription.getGeneralConditionsContentPageId()
        );
        assertEquals(
            Integer.valueOf(newSubscriptionEntity.getGeneralConditionsContentRevision().getRevision()),
            capturedSubscription.getGeneralConditionsContentRevision()
        );
    }

    @Test(expected = PlanGeneralConditionRevisionException.class)
    public void shouldNotCreateWithoutProcess_AcceptedOutdatedGCU() {
        // Prepare data
        planEntity.setGeneralConditions(PAGE_ID);
        PageEntity.PageRevisionId pageRevisionId = new PageEntity.PageRevisionId(PAGE_ID, 2);
        when(generalConditionPage.getContentRevisionId()).thenReturn(pageRevisionId);

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
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
        planEntity.setGeneralConditions(PAGE_ID);

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Run
        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setGeneralConditionsContentRevision(
            new PageEntity.PageRevisionId(planEntity.getGeneralConditions() + "-1", 2)
        );
        newSubscriptionEntity.setGeneralConditionsAccepted(false);
        subscriptionService.create(GraviteeContext.getExecutionContext(), newSubscriptionEntity);
    }

    @Test(expected = PlanGeneralConditionAcceptedException.class)
    public void shouldNotCreateWithoutProcess_AcceptedGCU_WithoutGCUContent() {
        // Prepare data
        planEntity.setGeneralConditions(PAGE_ID);
        SecurityContextHolder.setContext(generateSecurityContext());

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        final NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID);
        newSubscriptionEntity.setGeneralConditionsContentRevision(null);
        newSubscriptionEntity.setGeneralConditionsAccepted(true);
        subscriptionService.create(GraviteeContext.getExecutionContext(), newSubscriptionEntity);
    }

    @Test
    public void shouldCreateWithAutomaticSubscription() throws Exception {
        // Prepare data
        planEntity.setValidation(PlanValidationType.AUTO);
        planEntity.setSecurity(PlanSecurityType.API_KEY);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        ApplicationSettings settings = new ApplicationSettings();
        SimpleApplicationSettings clientSettings = new SimpleApplicationSettings();
        clientSettings.setClientId("my-client-id");
        settings.setApp(clientSettings);
        ApplicationEntity subscriberApplication = new ApplicationEntity();
        subscriberApplication.setId(SUBSCRIBER_ID);
        subscriberApplication.setType("SIMPLE");
        subscriberApplication.setSettings(settings);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(subscriberApplication);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(subscriptionRepository.create(any()))
            .thenAnswer(
                (Answer<Subscription>) invocation -> {
                    Subscription subscription1 = (Subscription) invocation.getArguments()[0];
                    subscription1.setId(SUBSCRIPTION_ID);
                    return subscription1;
                }
            );

        when(acceptSubscriptionDomainService.autoAccept(any(String.class), any(), any(), any(), any(), any()))
            .thenReturn(
                io.gravitee.apim.core.subscription.model.SubscriptionEntity
                    .builder()
                    .id(SUBSCRIPTION_ID)
                    .applicationId(APPLICATION_ID)
                    .build()
            );

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID)
        );

        // Verify
        verify(subscriptionRepository, times(1)).create(argThat(sub -> sub.getClientId() == null));
        verify(acceptSubscriptionDomainService, times(1)).autoAccept(eq(SUBSCRIPTION_ID), any(), eq(null), any(), eq(null), any());
        assertThat(subscriptionEntity.getId()).isNotNull();
        assertThat(subscriptionEntity.getApplication()).isNotNull();
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
        planEntity.setSecurity(PlanSecurityType.OAUTH2);

        SecurityContextHolder.setContext(generateSecurityContext());

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
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

        Subscription subscription = buildTestSubscription(REJECTED);

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
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, never()).findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        verify(subscriptionValidationService, times(1)).validateAndSanitize(any(), any(UpdateSubscriptionEntity.class));
    }

    @Test
    public void shouldUpdateSubscriptionWithConfiguration() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        SubscriptionConfigurationEntity subscriptionConfiguration = new SubscriptionConfigurationEntity();
        subscriptionConfiguration.setEntrypointId("entrypointId");
        subscriptionConfiguration.setEntrypointConfiguration("configuration");
        updatedSubscription.setConfiguration(subscriptionConfiguration);

        Subscription subscription = buildTestSubscription(ACCEPTED);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(any(Subscription.class));
        verify(apiKeyService, never()).findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        verify(subscriptionValidationService, times(1)).validateAndSanitize(any(), eq(updatedSubscription));
    }

    @Test
    public void shouldUpdateSubscriptionWithClientId() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setClientId("old-client-id");

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.update(
            GraviteeContext.getExecutionContext(),
            updatedSubscription,
            s -> {
                if (s.getClientId() != null) {
                    s.setClientId("my-client-id");
                }
            }
        );

        // Verify
        verify(subscriptionRepository, times(1)).update(argThat(s -> "my-client-id".equals(s.getClientId())));
    }

    @Test
    public void shouldUpdateSubscriptionWithPendingStatus() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        Subscription subscription = buildTestSubscription(PENDING);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(any());
    }

    @Test
    public void shouldUpdateSubscriptionWithPausedStatus() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);

        Subscription subscription = buildTestSubscription(PAUSED);

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);

        // Verify
        verify(subscriptionRepository, times(1)).update(any());
    }

    @Test
    public void shouldFailSubscription() throws TechnicalException {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setId(SUBSCRIPTION_ID);
        final long yesterday = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        final Date initialUpdateDate = new Date(yesterday);
        subscription.setUpdatedAt(initialUpdateDate);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);

        final String failureCause = "💥 Endpoint not available";
        subscriptionService.fail(SUBSCRIPTION_ID, failureCause);

        verify(subscriptionRepository).findById(SUBSCRIPTION_ID);
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).update(subscriptionCaptor.capture());

        final Subscription subscriptionCaptured = subscriptionCaptor.getValue();
        assertThat(subscriptionCaptured.getConsumerPausedAt()).isNull();
        assertThat(subscriptionCaptured.getConsumerStatus()).isEqualTo(Subscription.ConsumerStatus.FAILURE);
        assertThat(subscriptionCaptured.getFailureCause()).isEqualTo(failureCause);
        assertThat(subscriptionCaptured.getUpdatedAt()).isAfter(initialUpdateDate);
    }

    @Test
    public void shouldNotFailSubscriptionBecauseDoesNotExist() throws TechnicalException {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setId(SUBSCRIPTION_ID);
        final Date initialUpdateDate = new Date();
        subscription.setUpdatedAt(initialUpdateDate);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        Assertions
            .assertThatThrownBy(() -> subscriptionService.fail(SUBSCRIPTION_ID, "💥 Endpoint not available"))
            .isInstanceOf(SubscriptionNotFoundException.class);

        verify(subscriptionRepository, never()).update(any());
    }

    @Test
    public void shouldNotFailSubscriptionBecauseTechnicalException() throws TechnicalException {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setId(SUBSCRIPTION_ID);
        final Date initialUpdateDate = new Date();
        subscription.setUpdatedAt(initialUpdateDate);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        final TechnicalException exceptionThrown = new TechnicalException("🛠 Technical exception");
        when(subscriptionRepository.update(subscription)).thenThrow(exceptionThrown);

        assertThatThrownBy(() -> subscriptionService.fail(SUBSCRIPTION_ID, "💥 Endpoint not available"))
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageStartingWith("An error occurs while trying to fail subscription ")
            .hasMessageEndingWith(SUBSCRIPTION_ID)
            .hasCause(exceptionThrown);
    }

    @Test
    public void shouldNotPauseByConsumerBecauseInFailure() throws Exception {
        final String failureCause = "failure-cause";
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setConsumerStatus(Subscription.ConsumerStatus.FAILURE);
        subscription.setFailureCause(failureCause);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        io.gravitee.rest.api.model.v4.api.ApiModel apiModel = mock(io.gravitee.rest.api.model.v4.api.ApiModel.class);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModel);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);

        assertThatThrownBy(() -> subscriptionService.pauseConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID))
            .isInstanceOf(SubscriptionFailureException.class)
            .hasMessage("Subscription [" + SUBSCRIPTION_ID + "] is in failure state: " + failureCause);
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

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(singletonList(apiKey));
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        application.setPrimaryOwner(new PrimaryOwnerEntity());

        subscriptionService.pause(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);

        verify(apiKeyService).update(GraviteeContext.getExecutionContext(), apiKey);
        verify(notifierService).trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.SUBSCRIPTION_PAUSED), anyString(), anyMap());
        verify(notifierService)
            .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApplicationHook.SUBSCRIPTION_PAUSED), nullable(String.class), anyMap());
    }

    @Test
    public void shouldProcessButReject() {
        // Prepare data
        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(false);
        processSubscription.setReason("my reason");

        // Stub
        when(rejectSubscriptionDomainService.reject(any(String.class), any(), any()))
            .thenReturn(
                io.gravitee.apim.core.subscription.model.SubscriptionEntity
                    .builder()
                    .id(SUBSCRIPTION_ID)
                    .applicationId(APPLICATION_ID)
                    .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.REJECTED)
                    .processedBy(USER_ID)
                    .build()
            );

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.process(
            GraviteeContext.getExecutionContext(),
            processSubscription,
            USER_ID
        );

        // Verify
        verify(rejectSubscriptionDomainService).reject(eq(SUBSCRIPTION_ID), eq("my reason"), any());

        assertThat(subscriptionEntity.getStatus()).isEqualTo(SubscriptionStatus.REJECTED);
        assertThat(subscriptionEntity.getProcessedBy()).isEqualTo(USER_ID);
    }

    @Test
    public void shouldProcessWithAvailableCustomApiKeyForAcceptedSubscription() {
        // Prepare data
        final String customApiKey = "customApiKey";

        ProcessSubscriptionEntity processSubscription = new ProcessSubscriptionEntity();
        processSubscription.setId(SUBSCRIPTION_ID);
        processSubscription.setAccepted(true);
        processSubscription.setCustomApiKey(customApiKey);

        planEntity.setSecurity(PlanSecurityType.API_KEY);

        // Stub
        when(acceptSubscriptionDomainService.autoAccept(any(String.class), any(), any(), any(), any(), any()))
            .thenReturn(
                io.gravitee.apim.core.subscription.model.SubscriptionEntity
                    .builder()
                    .id(SUBSCRIPTION_ID)
                    .processedBy(USER_ID)
                    .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.ACCEPTED)
                    .build()
            );

        // Run
        final SubscriptionEntity subscriptionEntity = subscriptionService.process(
            GraviteeContext.getExecutionContext(),
            processSubscription,
            USER_ID
        );

        // Verify
        verify(acceptSubscriptionDomainService, times(1))
            .autoAccept(eq(SUBSCRIPTION_ID), any(), eq(null), eq(null), eq(customApiKey), any());
        assertThat(subscriptionEntity.getStatus()).isEqualTo(SubscriptionStatus.ACCEPTED);
        assertThat(subscriptionEntity.getProcessedBy()).isEqualTo(USER_ID);
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecauseNoClientId_oauth2() {
        // Stub
        planEntity.setSecurity(PlanSecurityType.OAUTH2);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecauseNoClientId_jwt() {
        // Stub
        planEntity.setSecurity(PlanSecurityType.OAUTH2);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);

        // Run
        subscriptionService.create(GraviteeContext.getExecutionContext(), new NewSubscriptionEntity(PLAN_ID, APPLICATION_ID));
    }

    @Test(expected = PlanNotSubscribableException.class)
    public void shouldNotCreateBecauseExistingSubscription_oauth2() {
        planEntity.setSecurity(PlanSecurityType.OAUTH2);
        planEntity.setStatus(PlanStatus.PUBLISHED);

        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

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
        when(subscription.getApi()).thenReturn(API_ID);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenReturn(subscription);
        planEntity.setStatus(PlanStatus.PUBLISHED);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
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
        when(subscription.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        planEntity.setStatus(PlanStatus.PUBLISHED);
        planEntity.setGeneralConditions("SOME_PAGE");
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        subscriptionService.transfer(GraviteeContext.getExecutionContext(), transferSubscription, USER_ID);
    }

    @Test(expected = TransferNotAllowedException.class)
    public void shouldNotTransferSubscription_onPlanAttachedToAnotherApi() throws Exception {
        final TransferSubscriptionEntity transferSubscription = new TransferSubscriptionEntity();
        transferSubscription.setId(SUBSCRIPTION_ID);
        transferSubscription.setPlan(PLAN_ID);

        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        planEntity.setStatus(PlanStatus.PUBLISHED);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        planEntity.setApi("another");
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        subscriptionService.transfer(GraviteeContext.getExecutionContext(), transferSubscription, USER_ID);
    }

    @Test(expected = TransferNotAllowedException.class)
    public void shouldNotTransferSubscription_onNonPublishedPlan() throws Exception {
        final TransferSubscriptionEntity transferSubscription = new TransferSubscriptionEntity();
        transferSubscription.setId(SUBSCRIPTION_ID);
        transferSubscription.setPlan(PLAN_ID);

        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        planEntity.setStatus(PlanStatus.STAGING);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        planEntity.setApi("another");
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        subscriptionService.transfer(GraviteeContext.getExecutionContext(), transferSubscription, USER_ID);
    }

    @Test(expected = TransferNotAllowedException.class)
    public void shouldNotTransferSubscription_onStandardPlanFromPushPlan() throws Exception {
        final TransferSubscriptionEntity transferSubscription = new TransferSubscriptionEntity();
        transferSubscription.setId(SUBSCRIPTION_ID);
        transferSubscription.setPlan(PLAN_ID);

        when(subscription.getPlan()).thenReturn("push-plan-id");
        when(subscription.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        planEntity.setStatus(PlanStatus.PUBLISHED);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        planEntity.setApi("another");
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        PlanEntity pushPlan = new PlanEntity();
        pushPlan.setStatus(PlanStatus.PUBLISHED);
        pushPlan.setApi("another");
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), "push-plan-id")).thenReturn(pushPlan);

        subscriptionService.transfer(GraviteeContext.getExecutionContext(), transferSubscription, USER_ID);
    }

    @Test(expected = TransferNotAllowedException.class)
    public void shouldNotTransferSubscription_onPushPlanFromStandardPlan() throws Exception {
        final TransferSubscriptionEntity transferSubscription = new TransferSubscriptionEntity();
        transferSubscription.setId(SUBSCRIPTION_ID);
        transferSubscription.setPlan("push-plan-id");

        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        planEntity.setStatus(PlanStatus.PUBLISHED);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        planEntity.setApi("another");
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        PlanEntity pushPlan = new PlanEntity();
        pushPlan.setStatus(PlanStatus.PUBLISHED);
        pushPlan.setApi("another");
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), "push-plan-id")).thenReturn(pushPlan);

        subscriptionService.transfer(GraviteeContext.getExecutionContext(), transferSubscription, USER_ID);
    }

    @Test(expected = TransferNotAllowedException.class)
    public void shouldNotTransferSubscription_onPlanWithDifferentSecurity() throws Exception {
        final TransferSubscriptionEntity transferSubscription = new TransferSubscriptionEntity();
        transferSubscription.setId(SUBSCRIPTION_ID);
        transferSubscription.setPlan(PLAN_ID);

        when(subscription.getPlan()).thenReturn("JWT-plan-id");
        when(subscription.getApi()).thenReturn(API_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        planEntity.setStatus(PlanStatus.PUBLISHED);
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        planEntity.setApi("another");
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        PlanEntity jwtPlan = new PlanEntity();
        jwtPlan.setStatus(PlanStatus.PUBLISHED);
        jwtPlan.setSecurity(PlanSecurityType.JWT);
        jwtPlan.setApi("another");
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), "JWT-plan-id")).thenReturn(jwtPlan);

        subscriptionService.transfer(GraviteeContext.getExecutionContext(), transferSubscription, USER_ID);
    }

    @Test(expected = PlanRestrictedException.class)
    public void shouldNotCreateBecauseRestricted() {
        // Stub
        planEntity.setExcludedGroups(List.of("excl1", "excl2"));
        planEntity.setApi("api1");

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
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
        planEntity.setExcludedGroups(List.of("excl1", "excl2"));
        planEntity.setApi("api1");
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Stub
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), "api1")).thenReturn(apiModelEntity);

        when(subscriptionRepository.create(any()))
            .thenAnswer(
                (Answer<Subscription>) invocation -> {
                    Subscription subscription = (Subscription) invocation.getArguments()[0];
                    subscription.setId(SUBSCRIPTION_ID);
                    return subscription;
                }
            );

        final SecurityContext securityContext = mock(SecurityContext.class);
        UserDetails principal = new UserDetails("toto", "pwdtoto", List.of(new SimpleGrantedAuthority(ENVIRONMENT_ADMIN)));
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
        when(apiSearchService.findGenericByEnvironmentAndIdIn(GraviteeContext.getExecutionContext(), Set.of(API_ID)))
            .thenReturn(Set.of(apiEntity));
        final SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION_ID);
        subscriptionEntity.setApplication(APPLICATION_ID);
        subscriptionEntity.setApi(API_ID);
        subscriptionEntity.setSubscribedBy(SUBSCRIBER_ID);
        subscriptionEntity.setPlan(PLAN_ID);
        SubscriptionMetadataQuery query = new SubscriptionMetadataQuery("DEFAULT", "DEFAULT", List.of(subscriptionEntity))
            .withApplications(true)
            .withPlans(true)
            .withApis(true)
            .withSubscribers(true)
            .includeDetails();

        Metadata metadata = subscriptionService.getMetadata(GraviteeContext.getExecutionContext(), query);
        assertFalse(metadata.toMap().isEmpty());

        assertNotNull(metadata);
        Mockito.verify(applicationService, times(1)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(APPLICATION_ID)));
        Mockito
            .verify(apiSearchService, times(1))
            .findGenericByEnvironmentAndIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(API_ID)));
        Mockito.verify(planSearchService, times(1)).findByIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(PLAN_ID)));
        Mockito.verify(userService, times(1)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(SUBSCRIBER_ID)));
        Mockito.verify(apiEntrypointService, times(1)).getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);
    }

    @Test
    public void shouldGetEmptyMetadataWithSubscriptions() {
        final SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION_ID);
        subscriptionEntity.setApplication(APPLICATION_ID);
        subscriptionEntity.setApi(API_ID);
        subscriptionEntity.setSubscribedBy(SUBSCRIBER_ID);
        subscriptionEntity.setPlan(PLAN_ID);
        SubscriptionMetadataQuery query = new SubscriptionMetadataQuery("DEFAULT", "DEFAULT", List.of(subscriptionEntity))
            .withApplications(false)
            .withPlans(false)
            .withApis(false)
            .withSubscribers(false)
            .excludeDetails();

        Metadata metadata = subscriptionService.getMetadata(GraviteeContext.getExecutionContext(), query);

        assertNotNull(metadata);
        Mockito.verify(applicationService, times(0)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(APPLICATION_ID)));
        Mockito
            .verify(apiSearchService, times(0))
            .findGenericByEnvironmentAndIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(API_ID)));
        Mockito.verify(planSearchService, times(0)).findByIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(PLAN_ID)));
        Mockito.verify(userService, times(0)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(SUBSCRIBER_ID)));
        Mockito.verify(apiEntrypointService, times(0)).getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);
    }

    @Test
    public void shouldFillApiMetadataAfterService() {
        when(apiEntity.getId()).thenReturn(API_ID);
        when(apiSearchService.findGenericByEnvironmentAndIdIn(GraviteeContext.getExecutionContext(), Set.of(API_ID)))
            .thenReturn(Set.of(apiEntity));
        final SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION_ID);
        subscriptionEntity.setApplication(APPLICATION_ID);
        subscriptionEntity.setApi(API_ID);
        subscriptionEntity.setSubscribedBy(SUBSCRIBER_ID);
        subscriptionEntity.setPlan(PLAN_ID);
        BiFunction<Metadata, GenericApiEntity, GenericApiEntity> delegate = mock(BiFunction.class);
        SubscriptionMetadataQuery query = new SubscriptionMetadataQuery("DEFAULT", "DEFAULT", List.of(subscriptionEntity))
            .withApis(true)
            .fillApiMetadata(delegate);

        Metadata metadata = subscriptionService.getMetadata(GraviteeContext.getExecutionContext(), query);
        assertFalse(metadata.toMap().isEmpty());

        assertNotNull(metadata);
        Mockito.verify(applicationService, times(0)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(APPLICATION_ID)));
        Mockito
            .verify(apiSearchService, times(1))
            .findGenericByEnvironmentAndIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(API_ID)));
        Mockito.verify(planSearchService, times(0)).findByIdIn(eq(GraviteeContext.getExecutionContext()), eq(Set.of(PLAN_ID)));
        Mockito.verify(userService, times(0)).findByIds(eq(GraviteeContext.getExecutionContext()), eq(Set.of(SUBSCRIBER_ID)));
        Mockito.verify(apiEntrypointService, times(0)).getApiEntrypoints(GraviteeContext.getExecutionContext(), apiEntity);
        Mockito.verify(delegate, times(1)).apply(any(Metadata.class), eq(apiEntity));
    }

    @Test
    public void search_should_not_fill_planSecurity_nor_apiKeys_if_boolean_to_false() throws TechnicalException {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApis(List.of("api-id-1"));

        Subscription subscription1 = buildTestSubscription("sub1", "api-id-1", ACCEPTED, "plan-id", null, null);
        when(subscriptionRepository.search(any(), any(), any())).thenReturn(new Page<>(List.of(subscription1), 1, 1, 1));

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
        verifyNoInteractions(planSearchService);
        verifyNoInteractions(apiKeyService);
    }

    @Test
    public void search_should_fill_plan_security_if_boolean_to_true() throws TechnicalException {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setApis(List.of("api-id-1"));

        Subscription subscription1 = buildTestSubscription("sub1", "api-id-1", ACCEPTED, "plan-id", null, null);
        when(subscriptionRepository.search(any(), any(), any())).thenReturn(new Page<>(List.of(subscription1), 1, 1, 1));

        PlanEntity foundPlan = new PlanEntity();
        foundPlan.setId("plan-id");
        foundPlan.setSecurity(PlanSecurityType.OAUTH2);
        when(planSearchService.findByIdIn(GraviteeContext.getExecutionContext(), Set.of("plan-id"))).thenReturn(Set.of(foundPlan));

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
        when(subscriptionRepository.search(any(), any(), any())).thenReturn(new Page<>(List.of(subscription1), 1, 1, 1));

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
        verifyNoInteractions(planSearchService);
    }

    @Test
    public void shouldUpdateSubscriptionWithEndingDateAndSetApiKeyExpirationIfNotSharedApikey() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        application.setApiKeyMode(ApiKeyMode.UNSPECIFIED);

        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

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
    public void shouldUpdateSubscriptionWithEndingDateAndDontSetApiKeyExpirationIfSharedApikey() throws Exception {
        UpdateSubscriptionEntity updatedSubscription = new UpdateSubscriptionEntity();
        updatedSubscription.setId(SUBSCRIPTION_ID);
        updatedSubscription.setEndingAt(new Date());

        application.setApiKeyMode(ApiKeyMode.SHARED);

        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setEndingAt(updatedSubscription.getEndingAt());

        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        List<ApiKeyEntity> apiKeys = List.of(
            buildTestApiKey("apikey-1", false, false),
            buildTestApiKey("apikey-2", true, false),
            buildTestApiKey("apikey-3", false, false),
            buildTestApiKey("apikey-4", false, true)
        );
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID)).thenReturn(apiKeys);

        // Run
        subscriptionService.update(GraviteeContext.getExecutionContext(), updatedSubscription);

        // subscription has been updated, but no API Key has been updated because they are shared with other subscriptions
        verify(subscriptionRepository, times(1)).update(subscription);
        verify(apiKeyService, times(1)).findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
        verifyNoMoreInteractions(apiKeyService);
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotUpdateSubscriptionConfigurationCauseNotFound() {
        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setSubscriptionId(SUBSCRIPTION_ID);

        subscriptionService.update(GraviteeContext.getExecutionContext(), updateSubscriptionConfigurationEntity);
    }

    @Test(expected = SubscriptionNotUpdatableException.class)
    public void shouldNotUpdateSubscriptionCauseClosed() throws TechnicalException {
        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setSubscriptionId(SUBSCRIPTION_ID);

        when(subscription.getStatus()).thenReturn(Subscription.Status.CLOSED);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        subscriptionService.update(GraviteeContext.getExecutionContext(), updateSubscriptionConfigurationEntity);
    }

    @Test
    public void shouldUpdateSubscriptionInFailure() throws TechnicalException {
        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        planV4.setValidation(AUTO);
        planV4.setMode(PlanMode.PUSH);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        Subscription subscription = new Subscription();
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setPlan(PLAN_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        subscription.setConsumerStatus(Subscription.ConsumerStatus.FAILURE);

        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setSubscriptionId(SUBSCRIPTION_ID);
        updateSubscriptionConfigurationEntity.setMetadata(Map.of("key", "value"));
        SubscriptionConfigurationEntity subscriptionConfiguration = new SubscriptionConfigurationEntity();
        subscriptionConfiguration.setEntrypointId("entrypointId");
        subscriptionConfiguration.setEntrypointConfiguration("{\"key\":\"value\"}");
        updateSubscriptionConfigurationEntity.setConfiguration(subscriptionConfiguration);

        subscriptionService.update(GraviteeContext.getExecutionContext(), updateSubscriptionConfigurationEntity);

        // verify subscription has been updated without any status change
        verify(subscriptionRepository)
            .update(
                argThat(sub ->
                    sub.getStatus() == ACCEPTED &&
                    Map.of("key", "value").equals(sub.getMetadata()) &&
                    "{\"entrypointId\":\"entrypointId\",\"channel\":null,\"entrypointConfiguration\":{\"key\":\"value\"}}".equals(
                            sub.getConfiguration()
                        ) &&
                    Subscription.ConsumerStatus.STARTED.equals(sub.getConsumerStatus())
                )
            );
    }

    @Test
    public void shouldUpdateSubscriptionConfigurationOnPlanWithAutomaticValidation() throws TechnicalException {
        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        planV4.setValidation(AUTO);
        planV4.setMode(PlanMode.PUSH);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        Subscription subscription = new Subscription();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setPlan(PLAN_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());

        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setSubscriptionId(SUBSCRIPTION_ID);
        updateSubscriptionConfigurationEntity.setMetadata(Map.of("key", "value"));
        SubscriptionConfigurationEntity subscriptionConfiguration = new SubscriptionConfigurationEntity();
        subscriptionConfiguration.setEntrypointId("entrypointId");
        subscriptionConfiguration.setEntrypointConfiguration("{\"key\":\"value\"}");
        updateSubscriptionConfigurationEntity.setConfiguration(subscriptionConfiguration);

        subscriptionService.update(GraviteeContext.getExecutionContext(), updateSubscriptionConfigurationEntity);

        // verify subscription has been updated without any status change
        verify(subscriptionRepository)
            .update(
                argThat(sub ->
                    sub.getStatus() == ACCEPTED &&
                    Map.of("key", "value").equals(sub.getMetadata()) &&
                    "{\"entrypointId\":\"entrypointId\",\"channel\":null,\"entrypointConfiguration\":{\"key\":\"value\"}}".equals(
                            sub.getConfiguration()
                        )
                )
            );
    }

    @Test
    public void shouldUpdateSubscriptionConfigurationOnPlanWithManualValidation() throws TechnicalException {
        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        planV4.setValidation(MANUAL);
        planV4.setMode(PlanMode.PUSH);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        Subscription subscription = new Subscription();
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setPlan(PLAN_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());

        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setSubscriptionId(SUBSCRIPTION_ID);
        updateSubscriptionConfigurationEntity.setMetadata(Map.of("key", "value"));
        SubscriptionConfigurationEntity subscriptionConfiguration = new SubscriptionConfigurationEntity();
        subscriptionConfiguration.setEntrypointId("entrypointId");
        subscriptionConfiguration.setEntrypointConfiguration("{\"key\":\"value\"}");
        updateSubscriptionConfigurationEntity.setConfiguration(subscriptionConfiguration);

        subscriptionService.update(GraviteeContext.getExecutionContext(), updateSubscriptionConfigurationEntity);

        // verify subscription has been updated with pending status
        verify(subscriptionRepository)
            .update(
                argThat(sub ->
                    sub.getStatus() == PENDING &&
                    Map.of("key", "value").equals(sub.getMetadata()) &&
                    "{\"entrypointId\":\"entrypointId\",\"channel\":null,\"entrypointConfiguration\":{\"key\":\"value\"}}".equals(
                            sub.getConfiguration()
                        )
                )
            );
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotPauseByConsumerBecauseDoesNoExist() throws Exception {
        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.pauseConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test(expected = SubscriptionConsumerStatusNotUpdatableException.class)
    public void shouldNotPauseByConsumerBecauseApiDefinitionNotV4() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiModelEntity.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);

        subscriptionService.pauseConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test(expected = SubscriptionConsumerStatusNotUpdatableException.class)
    public void shouldNotPauseByConsumerBecauseNoSubscriptionListener() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        io.gravitee.rest.api.model.v4.api.ApiModel apiModel = mock(io.gravitee.rest.api.model.v4.api.ApiModel.class);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModel);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiModel.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(apiModel.getListeners()).thenReturn(List.of());

        subscriptionService.pauseConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test(expected = SubscriptionNotPausableException.class)
    public void shouldNotPauseByConsumerBecauseAlreadyPaused() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setConsumerStatus(Subscription.ConsumerStatus.STOPPED);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        io.gravitee.rest.api.model.v4.api.ApiModel apiModel = mock(io.gravitee.rest.api.model.v4.api.ApiModel.class);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModel);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiModel.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(apiModel.getListeners()).thenReturn(List.of(new SubscriptionListener()));

        subscriptionService.pauseConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test
    public void shouldPauseByConsumer() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setConsumerStatus(Subscription.ConsumerStatus.STARTED);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        io.gravitee.rest.api.model.v4.api.ApiModel apiModel = mock(io.gravitee.rest.api.model.v4.api.ApiModel.class);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModel);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        when(apiModel.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(apiModel.getListeners()).thenReturn(List.of(new SubscriptionListener()));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        final ApiKeyEntity apiKey = buildTestApiKey(subscription.getId(), false, false);
        when(apiKeyService.findBySubscription(any(), any())).thenReturn(List.of(apiKey));

        subscriptionService.pauseConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);

        assertThat(subscription.getConsumerPausedAt()).isNotNull();
        assertThat(subscription.getConsumerStatus()).isEqualTo(Subscription.ConsumerStatus.STOPPED);
        verify(apiKeyService).update(GraviteeContext.getExecutionContext(), apiKey);
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                anyMap(),
                eq(Subscription.AuditEvent.SUBSCRIPTION_PAUSED_BY_CONSUMER),
                any(),
                any(),
                any()
            );
        verify(auditService)
            .createApplicationAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(APPLICATION_ID),
                anyMap(),
                eq(Subscription.AuditEvent.SUBSCRIPTION_PAUSED_BY_CONSUMER),
                any(),
                any(),
                any()
            );
    }

    @Test(expected = SubscriptionNotFoundException.class)
    public void shouldNotResumeByConsumerBecauseDoesNoExist() throws Exception {
        // Stub
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        subscriptionService.resumeConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test(expected = SubscriptionConsumerStatusNotUpdatableException.class)
    public void shouldNotResumeByConsumerBecauseApiDefinitionNotV4() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModelEntity);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(apiModelEntity.getDefinitionVersion()).thenReturn(DefinitionVersion.V2);

        subscriptionService.resumeConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test(expected = SubscriptionConsumerStatusNotUpdatableException.class)
    public void shouldNotResumeByConsumerBecauseNoSubscriptionListener() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        io.gravitee.rest.api.model.v4.api.ApiModel apiModel = mock(io.gravitee.rest.api.model.v4.api.ApiModel.class);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModel);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(apiModel.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(apiModel.getListeners()).thenReturn(List.of());

        subscriptionService.resumeConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test(expected = SubscriptionNotPausedException.class)
    public void shouldNotResumeByConsumerBecauseAlreadyActive() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setConsumerStatus(Subscription.ConsumerStatus.STARTED);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        io.gravitee.rest.api.model.v4.api.ApiModel apiModel = mock(io.gravitee.rest.api.model.v4.api.ApiModel.class);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModel);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(apiModel.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(apiModel.getListeners()).thenReturn(List.of(new SubscriptionListener()));

        subscriptionService.resumeConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);
    }

    @Test
    public void shouldResumeByConsumer() throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setConsumerStatus(Subscription.ConsumerStatus.STOPPED);

        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        io.gravitee.rest.api.model.v4.api.ApiModel apiModel = mock(io.gravitee.rest.api.model.v4.api.ApiModel.class);
        when(apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(apiModel);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);
        when(apiModel.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(apiModel.getListeners()).thenReturn(List.of(new SubscriptionListener()));
        when(subscriptionRepository.update(subscription)).thenReturn(subscription);
        final ApiKeyEntity apiKey = buildTestApiKey(subscription.getId(), false, false);
        when(apiKeyService.findBySubscription(any(), any())).thenReturn(List.of(apiKey));

        subscriptionService.resumeConsumer(GraviteeContext.getExecutionContext(), SUBSCRIPTION_ID);

        assertThat(subscription.getConsumerPausedAt()).isNull();
        assertThat(subscription.getConsumerStatus()).isEqualTo(Subscription.ConsumerStatus.STARTED);
        verify(apiKeyService).update(GraviteeContext.getExecutionContext(), apiKey);
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                anyMap(),
                eq(Subscription.AuditEvent.SUBSCRIPTION_RESUMED_BY_CONSUMER),
                any(),
                any(),
                any()
            );
        verify(auditService)
            .createApplicationAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(APPLICATION_ID),
                anyMap(),
                eq(Subscription.AuditEvent.SUBSCRIPTION_RESUMED_BY_CONSUMER),
                any(),
                any(),
                any()
            );
    }

    @Test
    public void update_should_not_override_clientId_if_not_present() throws Exception {
        testUpdateSubscriptionDependingOnClientIdSituation(null, "client-id", null);
    }

    @Test
    public void update_should_not_override_clientId_if_new_clientId_is_null() throws Exception {
        testUpdateSubscriptionDependingOnClientIdSituation("client-id", null, "client-id");
    }

    @Test
    public void update_should_override_clientId_if_present_and_new_clientId_is_not_null() throws Exception {
        testUpdateSubscriptionDependingOnClientIdSituation("client-id", "new-client-id", "new-client-id");
    }

    @Test
    public void should_search_and_exclude_apis() throws Exception {
        SubscriptionQuery query = new SubscriptionQuery();
        query.setExcludedApis(List.of(API_ID));

        Subscription subscription = buildTestSubscription(ACCEPTED);

        when(subscriptionRepository.search(any())).thenReturn(List.of(subscription));

        Collection<SubscriptionEntity> result = subscriptionService.search(GraviteeContext.getExecutionContext(), query);

        assertThat(result).hasSize(1);

        verify(subscriptionRepository).search(argThat(criteria -> criteria.getExcludedApis().contains(API_ID)));
    }

    @Test
    public void shouldBuildCorrectStringForCsvExport() {
        Date date = new Date();
        String formattedDate = dateFormatter.format(date);
        List<SubscriptionEntity> subscriptions = List.of(
            SubscriptionEntity
                .builder()
                .plan("plan")
                .application("application")
                .createdAt(date)
                .processedAt(date)
                .startingAt(date)
                .endingAt(date)
                .status(SubscriptionStatus.ACCEPTED)
                .build()
        );
        String expectedResult = String.format(
            """
                Plan;Application;Creation date;Process date;Start date;End date date;Status
                Example plan;Example application;%1$s;%1$s;%1$s;%1$s;ACCEPTED
                """,
            formattedDate
        );
        Map<String, Map<String, Object>> metadata = prepareMetadata();

        String result = subscriptionService.exportAsCsv(subscriptions, metadata);

        assertEquals(expectedResult, result);
    }

    @Test
    public void shouldBuildCorrectStringForCsvExportWhenEndingDateIsNull() {
        Date date = new Date();
        String formattedDate = dateFormatter.format(date);
        List<SubscriptionEntity> subscriptions = List.of(
            SubscriptionEntity
                .builder()
                .plan("plan")
                .application("application")
                .createdAt(date)
                .processedAt(date)
                .startingAt(date)
                .endingAt(null)
                .status(SubscriptionStatus.ACCEPTED)
                .build()
        );

        String expectedResult = String.format(
            """
                Plan;Application;Creation date;Process date;Start date;End date date;Status
                Example plan;Example application;%1$s;%1$s;%1$s;;ACCEPTED
                """,
            formattedDate
        );
        Map<String, Map<String, Object>> metadata = prepareMetadata();

        String result = subscriptionService.exportAsCsv(subscriptions, metadata);

        assertEquals(expectedResult, result);
    }

    private Map<String, Map<String, Object>> prepareMetadata() {
        return Map.of("plan", Map.of("name", "Example plan"), "application", Map.of("name", "Example application"));
    }

    private void testUpdateSubscriptionDependingOnClientIdSituation(
        String initialClientId,
        String updatedClientId,
        String expectedClientId
    ) throws Exception {
        Subscription subscription = buildTestSubscription(ACCEPTED);
        subscription.setClientId(initialClientId);

        UpdateSubscriptionEntity updateSubscription = new UpdateSubscriptionEntity();
        updateSubscription.setId(subscription.getId());
        updateSubscription.setEndingAt(new Date());

        // Stub
        when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.update(any())).thenAnswer(returnsFirstArg());
        planEntity.setSecurity(PlanSecurityType.API_KEY);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(planEntity);

        // Run
        subscriptionService.update(
            GraviteeContext.getExecutionContext(),
            updateSubscription,
            s -> {
                if (updatedClientId != null && s.getClientId() != null) {
                    s.setClientId(updatedClientId);
                }
            }
        );

        verify(subscriptionRepository, times(1)).update(argThat(sub -> Objects.equals(sub.getClientId(), expectedClientId)));
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
