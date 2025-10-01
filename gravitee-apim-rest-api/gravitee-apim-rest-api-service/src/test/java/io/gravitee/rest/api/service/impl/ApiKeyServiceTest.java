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

import static io.gravitee.repository.management.model.ApiKey.AuditEvent.APIKEY_EXPIRED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApiModel;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApiKeyGenerator;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyActivatedException;
import io.gravitee.rest.api.service.exceptions.ApiKeyAlreadyExistingException;
import io.gravitee.rest.api.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotActiveException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeyServiceTest {

    private static final String API_ID = "myAPI";
    private static final String APPLICATION_ID = "myApplication";
    private static final String PLAN_ID = "myPlan";
    private static final String API_KEY = "ef02ecd0-71bb-11e5-9d70-feff819cdc9f";
    private static final String SUBSCRIPTION_ID = "subscription-1";
    private static final String CUSTOM_API_KEY = "an-api-key";
    private static final String ENVIRONMENT_ID = "DEFAULT";

    @InjectMocks
    private ApiKeyService apiKeyService = new ApiKeyServiceImpl();

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private EmailService emailService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiTemplateService apiTemplateService;

    @Mock
    private ApiKeyGenerator apiKeyGenerator;

    @Mock
    private ApplicationEntity application;

    @Mock
    private ApiKey apiKey;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private PlanEntity plan;

    @Mock
    private AuditService auditService;

    @Mock
    private NotifierService notifierService;

    @Test
    public void shouldGenerate() throws TechnicalException {
        // Generated API Key
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);

        // Prepare subscription
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getApplication()).thenReturn(APPLICATION_ID);
        when(subscription.getEndingAt()).thenReturn(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        when(subscriptionService.findByIdIn(List.of(SUBSCRIPTION_ID))).thenReturn(Set.of(subscription));
        // Stub API Key creation
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());

        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(APPLICATION_ID))).thenReturn(application);
        // Run
        final ApiKeyEntity apiKey = apiKeyService.generate(GraviteeContext.getExecutionContext(), application, subscription, null);

        // Verify
        verify(apiKeyRepository, times(1)).create(any());
        assertEquals(API_KEY, apiKey.getKey());
        assertFalse(apiKey.isRevoked());
        assertEquals(subscription.getEndingAt(), apiKey.getExpireAt());
        assertEquals(subscription.getId(), apiKey.getSubscriptions().iterator().next().getId());

        ArgumentCaptor<AuditService.AuditLogData> argument = ArgumentCaptor.forClass(AuditService.AuditLogData.class);
        verify(auditService).createApiAuditLog(eq(GraviteeContext.getExecutionContext()), argument.capture(), any());
        Map<Audit.AuditProperties, String> properties = argument.getValue().getProperties();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey(Audit.AuditProperties.API));
        assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
        assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));
    }

    @Test
    public void shouldGenerateReusingSharedKey() throws TechnicalException {
        // Existing api key
        String sharedApiKeyValue = "shared-api-key-value";
        String sharedApiKeyId = "shared-api-key-id";
        String sharedSubscriptionId = "shared-subscription-id";

        ApiKey sharedKey = new ApiKey();
        sharedKey.setId(sharedApiKeyId);
        sharedKey.setKey(sharedApiKeyValue);
        sharedKey.setSubscriptions(List.of(sharedSubscriptionId));
        when(apiKeyRepository.findByApplication(APPLICATION_ID)).thenReturn(List.of(sharedKey));

        // Existing subscription
        SubscriptionEntity sharedSubscription = new SubscriptionEntity();
        sharedSubscription.setId(sharedSubscriptionId);
        when(subscriptionService.findByIdIn(List.of(sharedSubscriptionId))).thenReturn(Set.of(sharedSubscription));

        // Existing application
        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION_ID);
        application.setApiKeyMode(ApiKeyMode.SHARED);

        // Subscription to add
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);

        // Updated api key
        ApiKey updatedSharedKey = new ApiKey();
        updatedSharedKey.setId(sharedApiKeyId);
        updatedSharedKey.setKey(sharedApiKeyValue);
        updatedSharedKey.setSubscriptions(List.of(sharedSubscriptionId, SUBSCRIPTION_ID));
        when(apiKeyRepository.addSubscription(sharedApiKeyId, SUBSCRIPTION_ID)).thenReturn(Optional.of(updatedSharedKey));
        when(subscriptionService.findByIdIn(List.of(sharedSubscriptionId, SUBSCRIPTION_ID))).thenReturn(
            Set.of(sharedSubscription, subscription)
        );

        // Call
        ApiKeyEntity newKey = apiKeyService.generate(GraviteeContext.getExecutionContext(), application, subscription, null);

        // Verify
        assertEquals(sharedApiKeyValue, newKey.getKey());
        assertEquals(sharedApiKeyId, newKey.getId());
        assertEquals(Set.of(sharedSubscription, subscription), newKey.getSubscriptions());
    }

    @Test
    public void shouldGenerateWithCustomApiKey() throws TechnicalException {
        // Prepare subscription
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getEndingAt()).thenReturn(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        when(subscription.getApplication()).thenReturn(APPLICATION_ID);
        when(subscriptionService.findByIdIn(List.of(SUBSCRIPTION_ID))).thenReturn(Set.of(subscription));
        // Stub API Key creation
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());

        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), anyString())).thenReturn(application);

        // Run
        final ApiKeyEntity apiKey = apiKeyService.generate(
            GraviteeContext.getExecutionContext(),
            application,
            subscription,
            CUSTOM_API_KEY
        );

        // Verify
        verify(apiKeyRepository, times(1)).create(any());
        assertFalse(apiKey.isRevoked());
        assertEquals(subscription.getEndingAt(), apiKey.getExpireAt());
        assertEquals(subscription.getId(), apiKey.getSubscriptions().iterator().next().getId());

        ArgumentCaptor<AuditService.AuditLogData> argument = ArgumentCaptor.forClass(AuditService.AuditLogData.class);
        verify(auditService).createApiAuditLog(eq(GraviteeContext.getExecutionContext()), argument.capture(), any());
        Map<Audit.AuditProperties, String> properties = argument.getValue().getProperties();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey(Audit.AuditProperties.API));
        assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
        assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));

        verify(apiKeyGenerator, times(0)).generate();
        assertEquals(CUSTOM_API_KEY, apiKey.getKey());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotGenerateBecauseTechnicalException() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndEnvironmentId(any(), any())).thenThrow(TechnicalManagementException.class);
        apiKeyService.generate(GraviteeContext.getExecutionContext(), application, subscription, "a-custom-key");
    }

    @Test(expected = ApiKeyAlreadyExistingException.class)
    public void shouldNotGenerateBecauseApiKeyAlreadyExistsForAnotherApp() throws TechnicalException {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApi(API_ID);
        subscription.setApplication(APPLICATION_ID);

        SubscriptionEntity conflictingSubscription = new SubscriptionEntity();
        conflictingSubscription.setId("conflicting-subscription-id");
        conflictingSubscription.setApi(API_ID);
        conflictingSubscription.setApplication("other-app-id");

        ApplicationEntity conflictingApplication = new ApplicationEntity();
        conflictingApplication.setId("conflicting-application-id");

        ApiKey conflictingKey = new ApiKey();
        conflictingKey.setApplication(conflictingApplication.getId());
        conflictingKey.setSubscriptions(List.of(conflictingSubscription.getId()));

        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION_ID);

        when(subscriptionService.findByIdIn(List.of(conflictingSubscription.getId()))).thenReturn(Set.of(conflictingSubscription));
        when(apiKeyRepository.findByKeyAndEnvironmentId("alreadyExistingApiKey", ENVIRONMENT_ID)).thenReturn(List.of(conflictingKey));
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), anyString())).thenReturn(application);

        apiKeyService.generate(GraviteeContext.getExecutionContext(), application, subscription, "alreadyExistingApiKey");
    }

    @Test(expected = SubscriptionNotActiveException.class)
    public void shouldNotGenerateBecauseSubscriptionNotActive() {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApi(API_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setStatus(SubscriptionStatus.CLOSED);
        apiKeyService.renew(GraviteeContext.getExecutionContext(), subscription, "apiKey");
    }

    @Test
    public void shouldReactivateRevoked() throws Exception {
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscriptions(List.of(SUBSCRIPTION_ID));
        apiKey.setCreatedAt(new Date());
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setRevoked(true);
        final ApiModel api = new ApiModel();
        api.setId("123");

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApi(api.getId());
        subscription.setStatus(SubscriptionStatus.PAUSED);

        ApplicationEntity application = mock(ApplicationEntity.class);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), anyString())).thenReturn(application);

        ApiKeyEntity apiKeyEntity = mock(ApiKeyEntity.class);
        when(apiKeyEntity.getId()).thenReturn(API_KEY);
        when(apiKeyEntity.getApplication()).thenReturn(application);

        // Stub
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);
        when(subscriptionService.findByIdIn(apiKey.getSubscriptions())).thenReturn(Set.of(subscription));
        when(apiKeyRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        // Run
        apiKeyService.reactivate(GraviteeContext.getExecutionContext(), apiKeyEntity);

        ArgumentCaptor<AuditService.AuditLogData> argument = ArgumentCaptor.forClass(AuditService.AuditLogData.class);
        verify(auditService).createApiAuditLog(eq(GraviteeContext.getExecutionContext()), argument.capture(), any());
        Map<Audit.AuditProperties, String> properties = argument.getValue().getProperties();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey(Audit.AuditProperties.API));
        assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
        assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));
    }

    @Test
    public void shouldReactivateExpired() throws Exception {
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscriptions(List.of(SUBSCRIPTION_ID));
        apiKey.setCreatedAt(new Date());
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setExpireAt(new Date(System.currentTimeMillis() - 10000));
        final ApiModel api = new ApiModel();
        api.setId("123");

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApi(api.getId());
        subscription.setStatus(SubscriptionStatus.PAUSED);
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);

        ApplicationEntity application = mock(ApplicationEntity.class);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), anyString())).thenReturn(application);

        ApiKeyEntity apiKeyEntity = mock(ApiKeyEntity.class);
        when(apiKeyEntity.getApplication()).thenReturn(application);
        when(apiKeyEntity.getId()).thenReturn("api-key-id");

        // Stub
        when(apiKeyRepository.findById("api-key-id")).thenReturn(Optional.of(apiKey));
        when(subscriptionService.findByIdIn(apiKey.getSubscriptions())).thenReturn(Set.of(subscription));
        when(apiKeyRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        // Run
        apiKeyService.reactivate(GraviteeContext.getExecutionContext(), apiKeyEntity);

        ArgumentCaptor<AuditService.AuditLogData> argument = ArgumentCaptor.forClass(AuditService.AuditLogData.class);
        verify(auditService).createApiAuditLog(eq(GraviteeContext.getExecutionContext()), argument.capture(), any());
        Map<Audit.AuditProperties, String> properties = argument.getValue().getProperties();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey(Audit.AuditProperties.API));
        assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
        assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));
    }

    @Test(expected = ApiKeyAlreadyActivatedException.class)
    public void shouldNotReactivateBecauseOfAlreadyActivated() throws Exception {
        ApiKeyEntity apiKeyEntity = mock(ApiKeyEntity.class);
        when(apiKeyEntity.getId()).thenReturn(API_KEY);

        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscriptions(List.of(SUBSCRIPTION_ID));
        apiKey.setCreatedAt(new Date());
        apiKey.setApplication(APPLICATION_ID);

        // Stub
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));

        // Run
        apiKeyService.reactivate(GraviteeContext.getExecutionContext(), apiKeyEntity);
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void shouldNotReactivateBecauseOfApiKeyNotFound() throws TechnicalException {
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.empty());

        ApiKeyEntity apiKeyEntity = mock(ApiKeyEntity.class);
        when(apiKeyEntity.getId()).thenReturn(API_KEY);

        apiKeyService.reactivate(GraviteeContext.getExecutionContext(), apiKeyEntity);
    }

    @Test(expected = SubscriptionNotActiveException.class)
    public void shouldNotReactivateBecauseOfNotActiveSubscription() throws TechnicalException {
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscriptions(List.of(SUBSCRIPTION_ID));
        apiKey.setCreatedAt(new Date());
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setExpireAt(new Date(System.currentTimeMillis() - 10000));

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApi(API_ID);
        subscription.setStatus(SubscriptionStatus.CLOSED);

        ApplicationEntity application = mock(ApplicationEntity.class);

        ApiKeyEntity apiKeyEntity = mock(ApiKeyEntity.class);
        when(apiKeyEntity.getId()).thenReturn(API_KEY);
        when(apiKeyEntity.getApplication()).thenReturn(application);

        // Stub
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscriptionEntity);

        apiKeyService.reactivate(GraviteeContext.getExecutionContext(), apiKeyEntity);
    }

    @Test
    public void shouldRenew() throws TechnicalException {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(API_KEY);
        apiKey.setKey("123-456-789");
        apiKey.setCreatedAt(new Date());
        apiKey.setApplication(APPLICATION_ID);

        ApiModel api = new ApiModel();
        api.setId("123");

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setEndingAt(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        subscription.setPlan(PLAN_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setApi(api.getId());
        subscription.setStatus(SubscriptionStatus.PAUSED);

        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION_ID);
        application.setApiKeyMode(ApiKeyMode.UNSPECIFIED);

        List<String> subscriptionIds = List.of(SUBSCRIPTION_ID);
        Set<SubscriptionEntity> subscriptions = Set.of(subscription);
        apiKey.setSubscriptions(subscriptionIds);

        PlanEntity plan = new PlanEntity();
        plan.setSecurity(PlanSecurityType.API_KEY);

        // Stub
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(subscriptionService.findById(subscription.getId())).thenReturn(subscription);
        when(subscriptionService.findByIdIn(argThat(subscriptionIds::containsAll))).thenReturn(subscriptions);
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());
        when(apiKeyRepository.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKey));
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(subscription.getApplication()))).thenReturn(
            application
        );
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), subscription.getPlan())).thenReturn(plan);
        when(apiTemplateService.findByIdForTemplates(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(api);

        // Run
        final ApiKeyEntity apiKeyEntity = apiKeyService.renew(GraviteeContext.getExecutionContext(), subscription);

        // Verify
        // A new API Key has been created
        verify(apiKeyRepository, times(1)).create(any());
        assertEquals(API_KEY, apiKeyEntity.getKey());

        ArgumentCaptor<AuditService.AuditLogData> argument = ArgumentCaptor.forClass(AuditService.AuditLogData.class);
        verify(auditService, times(2)).createApiAuditLog(eq(GraviteeContext.getExecutionContext()), argument.capture(), any());
        for (AuditService.AuditLogData auditLogData : argument.getAllValues()) {
            Map<Audit.AuditProperties, String> properties = auditLogData.getProperties();
            assertEquals(3, properties.size());
            assertTrue(properties.containsKey(Audit.AuditProperties.API));
            assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
            assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));
        }

        // Old API Key has been revoked
        verify(apiKeyRepository, times(1)).update(apiKey);
        assertFalse(apiKey.isRevoked());
        assertNotNull(apiKey.getExpireAt());
    }

    @Test
    public void shouldRenewAndKeepSharedSubscriptions() throws TechnicalException {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(API_KEY);
        apiKey.setKey("123-456-789");
        apiKey.setCreatedAt(new Date());
        apiKey.setApplication(APPLICATION_ID);

        ApiModel api = new ApiModel();
        api.setId("123");

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);

        String sharedSubscriptionId = "shared-subscription-id";
        SubscriptionEntity sharedSubscription = new SubscriptionEntity();
        sharedSubscription.setId(sharedSubscriptionId);

        subscription.setEndingAt(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        subscription.setPlan(PLAN_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setApi(api.getId());
        subscription.setStatus(SubscriptionStatus.PAUSED);

        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION_ID);
        application.setApiKeyMode(ApiKeyMode.UNSPECIFIED);

        List<String> subscriptionIds = List.of(SUBSCRIPTION_ID, sharedSubscriptionId);
        Set<SubscriptionEntity> subscriptions = Set.of(subscription, sharedSubscription);
        apiKey.setSubscriptions(subscriptionIds);

        PlanEntity plan = new PlanEntity();
        plan.setSecurity(PlanSecurityType.API_KEY);

        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(subscriptionService.findById(subscription.getId())).thenReturn(subscription);
        when(subscriptionService.findByIdIn(argThat(subscriptionIds::containsAll))).thenReturn(subscriptions);
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());
        when(apiKeyRepository.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Set.of(apiKey));
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(subscription.getApplication()))).thenReturn(
            application
        );
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), subscription.getPlan())).thenReturn(plan);
        when(apiTemplateService.findByIdForTemplates(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(api);

        final ApiKeyEntity apiKeyEntity = apiKeyService.renew(GraviteeContext.getExecutionContext(), subscription);

        assertNotNull(apiKeyEntity.getSubscriptions());
        assertEquals(2, apiKeyEntity.getSubscriptions().size());
        assertEquals(apiKeyEntity.getSubscriptions(), subscriptions);
    }

    @Test
    public void shouldRenewWithoutRenewingExpiredKeys() throws TechnicalException {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(API_KEY);
        apiKey.setKey("123-456-789");
        apiKey.setCreatedAt(new Date());
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setExpireAt(Date.from(new Date().toInstant().minus(1, ChronoUnit.DAYS)));

        ApiModel api = new ApiModel();
        api.setId("123");

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setEndingAt(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        subscription.setPlan(PLAN_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setApi(api.getId());
        subscription.setStatus(SubscriptionStatus.PAUSED);

        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION_ID);
        application.setApiKeyMode(ApiKeyMode.EXCLUSIVE);

        List<String> subscriptionIds = List.of(SUBSCRIPTION_ID);
        Set<SubscriptionEntity> subscriptions = Set.of(subscription);
        apiKey.setSubscriptions(subscriptionIds);

        PlanEntity plan = new PlanEntity();
        plan.setSecurity(PlanSecurityType.API_KEY);

        // Stub
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(subscriptionService.findByIdIn(argThat(subscriptionIds::containsAll))).thenReturn(subscriptions);
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());
        when(apiKeyRepository.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKey));
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(subscription.getApplication()))).thenReturn(
            application
        );
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), subscription.getPlan())).thenReturn(plan);
        when(apiTemplateService.findByIdForTemplates(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(api);

        // Run
        final ApiKeyEntity apiKeyEntity = apiKeyService.renew(GraviteeContext.getExecutionContext(), subscription);

        // Verify
        // A new API Key has been created
        verify(apiKeyRepository, times(1)).create(any());
        assertEquals(API_KEY, apiKeyEntity.getKey());

        // Old API Key has been revoked
        verify(apiKeyRepository, times(0)).update(apiKey);
        assertFalse(apiKey.isRevoked());
        assertNotNull(apiKey.getExpireAt());
    }

    @Test(expected = ApiKeyAlreadyExistingException.class)
    public void shouldNotRenewBecauseApiKeyAlreadyExistsForAnotherApp() throws TechnicalException {
        String conflictingApplicationId = "conflicting-application-id";

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApi(API_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setStatus(SubscriptionStatus.PAUSED);

        SubscriptionEntity conflictingSubscription = new SubscriptionEntity();
        conflictingSubscription.setId("conflicting-subscription-id");
        conflictingSubscription.setApi(API_ID);
        conflictingSubscription.setApplication(conflictingApplicationId);

        ApplicationEntity conflictingApplication = new ApplicationEntity();
        conflictingApplication.setId(conflictingApplicationId);

        ApiKey conflictingKey = new ApiKey();
        conflictingKey.setApplication(conflictingApplicationId);
        conflictingKey.setSubscriptions(List.of(conflictingSubscription.getId()));

        PlanEntity plan = new PlanEntity();
        plan.setSecurity(PlanSecurityType.API_KEY);

        when(applicationService.findById(any(), eq(conflictingApplicationId))).thenReturn(conflictingApplication);
        when(apiKeyRepository.findByKeyAndEnvironmentId("alreadyExistingApiKey", ENVIRONMENT_ID)).thenReturn(List.of(conflictingKey));
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        apiKeyService.renew(GraviteeContext.getExecutionContext(), subscription, "alreadyExistingApiKey");
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotRenewSubscriptionWithJwtPlan() {
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setPlan(PLAN_ID);
        subscriptionEntity.setStatus(SubscriptionStatus.PAUSED);

        PlanEntity plan = new PlanEntity();
        plan.setSecurity(PlanSecurityType.JWT);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        apiKeyService.renew(GraviteeContext.getExecutionContext(), subscriptionEntity, CUSTOM_API_KEY);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotRenewSubscriptionWithoutSecurity() {
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setPlan(PLAN_ID);
        subscriptionEntity.setStatus(SubscriptionStatus.PAUSED);

        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(new PlanEntity());

        apiKeyService.renew(GraviteeContext.getExecutionContext(), subscriptionEntity, CUSTOM_API_KEY);
    }

    @Test
    public void shouldRenewForCustomApiKeyAssociatedToClosedSubscription() throws TechnicalException {
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscription(SUBSCRIPTION_ID);
        apiKey.setCreatedAt(new Date());
        apiKey.setPlan(PLAN_ID);
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setExpireAt(Date.from(new Date().toInstant().minus(1, ChronoUnit.DAYS)));

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setPlan(PLAN_ID);
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setStatus(SubscriptionStatus.PAUSED);
        subscription.setEndingAt(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        subscription.setApplication(APPLICATION_ID);

        PlanEntity plan = new PlanEntity();
        plan.setSecurity(PlanSecurityType.API_KEY);
        plan.setApi(API_ID);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(plan);

        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());
        when(apiKeyRepository.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKey));
        when(applicationService.findById(GraviteeContext.getExecutionContext(), subscription.getApplication())).thenReturn(application);

        final ApiModel api = mock(ApiModel.class);

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setPlan(PLAN_ID);
        subscriptionEntity.setId(SUBSCRIPTION_ID);
        subscriptionEntity.setStatus(SubscriptionStatus.PAUSED);

        apiKeyService.renew(GraviteeContext.getExecutionContext(), subscriptionEntity, CUSTOM_API_KEY);

        verify(apiKeyRepository, times(1)).create(any());
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void shouldNotUpdate() throws TechnicalException {
        apiKeyService.update(GraviteeContext.getExecutionContext(), new ApiKeyEntity());
        fail("It should throws ApiKeyNotFoundException");
    }

    @Test
    public void shouldUpdateNotExpired() throws TechnicalException {
        ApiKey existingApiKey = new ApiKey();
        when(apiKeyRepository.findById("api-key-id")).thenReturn(Optional.of(existingApiKey));

        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setId("api-key-id");
        apiKeyEntity.setKey("ABC");
        apiKeyEntity.setSubscriptions(Set.of());
        apiKeyEntity.setRevoked(true);
        apiKeyEntity.setPaused(true);

        apiKeyService.update(GraviteeContext.getExecutionContext(), apiKeyEntity);

        verify(apiKeyRepository, times(1)).update(existingApiKey);
        //must be manage by the revoke method
        assertFalse("isRevoked", existingApiKey.isRevoked());
        assertTrue("isPaused", existingApiKey.isPaused());
    }

    @Test
    public void shouldUpdateExpired() throws TechnicalException {
        ApiKey existingApiKey = new ApiKey();
        existingApiKey.setApplication(APPLICATION_ID);
        when(apiKeyRepository.findById("api-key-id")).thenReturn(Optional.of(existingApiKey));

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId("subscription-id");

        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setId("api-key-id");
        apiKeyEntity.setApplication(application);
        apiKeyEntity.setKey("ABC");
        apiKeyEntity.setPaused(true);
        apiKeyEntity.setSubscriptions(Set.of(subscription));
        apiKeyEntity.setApplication(application);
        apiKeyEntity.setExpireAt(new Date());

        when(subscriptionService.findById(any())).thenReturn(subscription);

        //notification mocks
        PlanEntity mockedPlan = mock(PlanEntity.class);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), anyString())).thenReturn(application);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mockedPlan);
        when(apiTemplateService.findByIdForTemplates(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(ApiModel.class));
        when(subscriptionService.findByIdIn(any())).thenReturn(Set.of(subscription));
        apiKeyService.update(GraviteeContext.getExecutionContext(), apiKeyEntity);

        verify(apiKeyRepository, times(1)).update(existingApiKey);
        //must be manage by the revoke method
        assertFalse("isRevoked", existingApiKey.isRevoked());
        assertTrue("isPaused", existingApiKey.isPaused());
        verify(notifierService, times(1)).trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.APIKEY_EXPIRED), any(), any());
        verify(auditService, times(1)).createApiAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(auditLogData -> auditLogData.getEvent().equals(APIKEY_EXPIRED)),
            any()
        );
    }

    @Test
    public void shouldFindBySubscription() throws TechnicalException {
        String subscriptionId = "my-subscription";
        SubscriptionEntity subscriptionEntity = mock(SubscriptionEntity.class);
        when(subscriptionEntity.getId()).thenReturn(subscriptionId);
        when(subscriptionService.findById(subscriptionId)).thenReturn(subscriptionEntity);
        ApiKey firstKey = mock(ApiKey.class);
        when(firstKey.getCreatedAt()).thenReturn(new Date(Instant.now().toEpochMilli()));
        when(firstKey.getKey()).thenReturn("first");
        ApiKey secondkey = mock(ApiKey.class);
        when(secondkey.getKey()).thenReturn("second");
        when(secondkey.getCreatedAt()).thenReturn(new Date(Instant.now().minus(5, ChronoUnit.DAYS).toEpochMilli()));
        ApiKey lastKey = mock(ApiKey.class);
        when(lastKey.getKey()).thenReturn("last");
        when(lastKey.getCreatedAt()).thenReturn(new Date(Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli()));
        Set<ApiKey> keys = new HashSet<>(Arrays.asList(lastKey, firstKey, secondkey));
        when(apiKeyRepository.findBySubscription(subscriptionId)).thenReturn(keys);

        List<ApiKeyEntity> bySubscription = apiKeyService
            .findBySubscription(GraviteeContext.getExecutionContext(), subscriptionId)
            .stream()
            .collect(Collectors.toList());

        assertEquals(3, bySubscription.size());
        assertEquals("first", bySubscription.get(0).getKey());
        assertEquals("second", bySubscription.get(1).getKey());
        assertEquals("last", bySubscription.get(2).getKey());
    }

    @Test
    public void shouldRemoveExpirationDate() throws TechnicalException {
        ApiKey existingApiKey = new ApiKey();
        existingApiKey.setExpireAt(Date.from(Instant.now().plusSeconds(60)));
        when(apiKeyRepository.findById("api-key-id")).thenReturn(Optional.of(existingApiKey));

        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setId("api-key-id");
        apiKeyEntity.setKey("ABC");
        apiKeyEntity.setSubscriptions(Set.of());
        apiKeyEntity.setRevoked(true);
        apiKeyEntity.setExpireAt(null);

        apiKeyService.update(GraviteeContext.getExecutionContext(), apiKeyEntity);

        verify(apiKeyRepository, times(1)).update(existingApiKey);
        verifyNoInteractions(notifierService);
    }

    @Test
    public void canCreate_should_return_true_cause_key_doesnt_exists_yet() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "my-api-id";
        String applicationId = "my-application-id";

        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApi(apiId);
        subscriptionQuery.setApplication(applicationId);

        when(apiKeyRepository.findByKeyAndEnvironmentId(apiKeyToCreate, ENVIRONMENT_ID)).thenReturn(Collections.emptyList());

        boolean canCreate = apiKeyService.canCreate(GraviteeContext.getExecutionContext(), apiKeyToCreate, apiId, applicationId);

        assertTrue(canCreate);
    }

    @Test
    public void canCreate_should_return_true_cause_key_already_exists_for_same_application_on_other_api() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "my-api-id";
        String applicationId = "my-application-id";

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId("subscription-1");
        subscriptionEntity.setApplication(applicationId);
        subscriptionEntity.setApi("another-api-1");

        ApplicationEntity application = new ApplicationEntity();
        application.setId(applicationId);

        ApiKey existingApiKey = new ApiKey();
        existingApiKey.setSubscriptions(List.of("subscription-1"));
        existingApiKey.setApplication(applicationId);
        existingApiKey.setKey(apiKeyToCreate);

        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(applicationId))).thenReturn(application);
        when(subscriptionService.findByIdIn(List.of("subscription-1"))).thenReturn(Set.of(subscriptionEntity));
        when(apiKeyRepository.findByKeyAndEnvironmentId(apiKeyToCreate, ENVIRONMENT_ID)).thenReturn(List.of(existingApiKey));

        boolean canCreate = apiKeyService.canCreate(GraviteeContext.getExecutionContext(), apiKeyToCreate, apiId, applicationId);

        assertTrue(canCreate);
    }

    @Test
    public void canCreate_should_return_false_cause_key_already_exists_for_same_application_on_same_api() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "my-api-id";
        String applicationId = "my-application-id";

        ApplicationEntity application = new ApplicationEntity();
        application.setId(applicationId);

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId("subscription-1");
        subscriptionEntity.setApplication(applicationId);
        subscriptionEntity.setApi(apiId);

        List<String> subscriptionIds = List.of("subscription-1", "subscription-2");
        Set<SubscriptionEntity> subscriptions = Set.of(subscriptionEntity);

        ApiKey existingApiKey = new ApiKey();
        existingApiKey.setSubscriptions(subscriptionIds);
        existingApiKey.setApplication(applicationId);
        existingApiKey.setKey(apiKeyToCreate);

        when(apiKeyRepository.findByKeyAndEnvironmentId(apiKeyToCreate, ENVIRONMENT_ID)).thenReturn(List.of(existingApiKey));
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(applicationId))).thenReturn(application);
        when(subscriptionService.findByIdIn(argThat(subscriptionIds::containsAll))).thenReturn(subscriptions);

        boolean canCreate = apiKeyService.canCreate(GraviteeContext.getExecutionContext(), apiKeyToCreate, apiId, applicationId);

        assertFalse(canCreate);
    }

    @Test
    public void canCreate_should_return_false_cause_key_already_exists_for_another_application() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "api-id";
        String subscriptionId = "subscription";
        String applicationId = "application-id";
        String conflictingApplicationId = "another-app";
        List<String> subscriptionIds = List.of(subscriptionId);

        ApplicationEntity conflictingApplication = new ApplicationEntity();
        conflictingApplication.setId(conflictingApplicationId);

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(subscriptionId);
        subscriptionEntity.setApplication(conflictingApplicationId);
        subscriptionEntity.setApi(apiId);

        ApiKey existingApiKey = new ApiKey();
        existingApiKey.setSubscriptions(List.of(subscriptionId));
        existingApiKey.setApplication(conflictingApplicationId);
        existingApiKey.setKey(apiKeyToCreate);

        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), eq(conflictingApplicationId))).thenReturn(
            conflictingApplication
        );
        when(subscriptionService.findByIdIn(argThat(subscriptionIds::containsAll))).thenReturn(Set.of(subscriptionEntity));
        when(apiKeyRepository.findByKeyAndEnvironmentId(apiKeyToCreate, ENVIRONMENT_ID)).thenReturn(List.of(existingApiKey));

        boolean canCreate = apiKeyService.canCreate(GraviteeContext.getExecutionContext(), apiKeyToCreate, apiId, applicationId);

        assertFalse(canCreate);
    }

    @Test(expected = TechnicalManagementException.class)
    public void canCreate_should_throw_TechnicalManagementException_cause_key_search_thrown_exception() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "my-api-id";
        String applicationId = "my-application-id";
        when(apiKeyRepository.findByKeyAndEnvironmentId(apiKeyToCreate, ENVIRONMENT_ID)).thenThrow(TechnicalManagementException.class);
        apiKeyService.canCreate(GraviteeContext.getExecutionContext(), apiKeyToCreate, apiId, applicationId);
    }

    @Test(expected = TechnicalManagementException.class)
    public void findByKey_should_throw_technicalManagementException_when_exception_thrown() throws TechnicalException {
        when(apiKeyRepository.findByKey("apiKey")).thenThrow(TechnicalException.class);

        apiKeyService.findByKey(GraviteeContext.getExecutionContext(), "apiKey");
    }

    @Test
    public void findByKey_should_convert_to_entities_and_return_list() throws TechnicalException {
        ApiKey apiKey1 = new ApiKey();
        apiKey1.setId("api-key-1-id");
        ApiKey apiKey2 = new ApiKey();
        apiKey2.setId("api-key-2-id");
        when(apiKeyRepository.findByKey("apiKey")).thenReturn(List.of(apiKey1, apiKey2));

        List<ApiKeyEntity> apiKeyEntities = apiKeyService.findByKey(GraviteeContext.getExecutionContext(), "apiKey");

        assertEquals(2, apiKeyEntities.size());
        assertEquals("api-key-1-id", apiKeyEntities.get(0).getId());
        assertEquals("api-key-2-id", apiKeyEntities.get(1).getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void findById_should_throw_technicalManagementException_when_exception_thrown() throws TechnicalException {
        when(apiKeyRepository.findById("apiKey")).thenThrow(TechnicalException.class);

        apiKeyService.findById(GraviteeContext.getExecutionContext(), "apiKey");
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void findById_should_throw_ApiKeyNotFoundException_when_not_found() throws TechnicalException {
        when(apiKeyRepository.findById("apiKey")).thenReturn(Optional.empty());

        apiKeyService.findById(GraviteeContext.getExecutionContext(), "apiKey");
    }

    @Test
    public void findById_should_convert_to_entity_and_return() throws TechnicalException {
        ApiKey apiKey1 = new ApiKey();
        apiKey1.setId("api-key-1-id");
        when(apiKeyRepository.findById("apiKey")).thenReturn(Optional.of(apiKey1));

        ApiKeyEntity apiKeyEntity = apiKeyService.findById(GraviteeContext.getExecutionContext(), "apiKey");

        assertEquals("api-key-1-id", apiKeyEntity.getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void findByKeyAndApi_should_throw_technicalManagementException_when_exception_thrown() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi("apiKey", "apiId")).thenThrow(TechnicalException.class);

        apiKeyService.findByKeyAndApi(GraviteeContext.getExecutionContext(), "apiKey", "apiId");
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void findByKeyAndApi_should_throw_apiKeyNotFoundException_when_not_found() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi("apiKey", "apiId")).thenReturn(Optional.empty());

        apiKeyService.findByKeyAndApi(GraviteeContext.getExecutionContext(), "apiKey", "apiId");
    }

    @Test
    public void findByKeyAndApi_should_convert_found_api_to_entity_and_return_it() throws TechnicalException {
        ApiKey apiKey = new ApiKey();
        apiKey.setId("api-key-1-id");
        when(apiKeyRepository.findByKeyAndApi("apiKey", "apiId")).thenReturn(Optional.of(apiKey));

        ApiKeyEntity apiKeyEntity = apiKeyService.findByKeyAndApi(GraviteeContext.getExecutionContext(), "apiKey", "apiId");
        assertNotNull(apiKeyEntity);
        assertEquals("api-key-1-id", apiKeyEntity.getId());
    }

    @Test
    public void findByKeyAndApi_should_hash_key() throws TechnicalException {
        ApiKey apiKey = new ApiKey();
        apiKey.setId("api-key-1-id");
        apiKey.setKey("not-hashed");
        when(apiKeyRepository.findByKeyAndApi("apiKey", "apiId")).thenReturn(Optional.of(apiKey));

        ApiKeyEntity apiKeyEntity = apiKeyService.findByKeyAndApi(GraviteeContext.getExecutionContext(), "apiKey", "apiId");
        assertNotNull(apiKeyEntity);
        assertEquals("api-key-1-id", apiKeyEntity.getId());
        assertEquals("not-hashed", apiKeyEntity.getKey());
        assertEquals("803cd1142626848853291b8b5e894041", apiKeyEntity.getHash());
    }

    @Test(expected = InvalidApplicationApiKeyModeException.class)
    public void renew_for_application_should_throw_exception_if_not_shared_apikey_mode() {
        ApplicationEntity application = new ApplicationEntity();
        application.setApiKeyMode(ApiKeyMode.UNSPECIFIED);

        apiKeyService.renew(GraviteeContext.getExecutionContext(), application);
    }

    @Test
    public void renew_for_application_should_expire_previous_keys() throws TechnicalException {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-test-app");
        application.setApiKeyMode(ApiKeyMode.SHARED);

        ApiKey newApiKey = buildTestApiKey("apiKey-X");
        List<ApiKey> allApiKeys = List.of(buildTestApiKey("apiKey-2"), newApiKey, buildTestApiKey("apiKey-3"), buildTestApiKey("apiKey-4"));
        when(apiKeyRepository.findByApplication("my-test-app")).thenReturn(allApiKeys);
        when(apiKeyRepository.create(any())).thenReturn(newApiKey);

        apiKeyService.renew(GraviteeContext.getExecutionContext(), application);

        // 3 old keys have been expired, but not the new one
        verify(apiKeyRepository, times(1)).update(argThat(apiKey -> apiKey.getId().equals("apiKey-2") && apiKey.getExpireAt() != null));
        verify(apiKeyRepository, times(1)).update(argThat(apiKey -> apiKey.getId().equals("apiKey-3") && apiKey.getExpireAt() != null));
        verify(apiKeyRepository, times(1)).update(argThat(apiKey -> apiKey.getId().equals("apiKey-4") && apiKey.getExpireAt() != null));
        verify(apiKeyRepository, never()).update(argThat(apiKey -> apiKey.getId().equals("apiKey-X") && apiKey.getExpireAt() != null));
    }

    @Test
    public void renew_for_application_should_copy_previous_keys_subscriptions() throws TechnicalException {
        ApplicationEntity application = new ApplicationEntity();
        application.setId("my-test-app");
        application.setApiKeyMode(ApiKeyMode.SHARED);

        ApiKey newApiKey = buildTestApiKey("apiKey-X");
        ApiKey oldApiKey2 = buildTestApiKey("apiKey-2");
        ApiKey oldApiKey4 = buildTestApiKey("apiKey-4");
        List<ApiKey> allApiKeys = List.of(oldApiKey2, newApiKey, buildTestApiKey("apiKey-3"), oldApiKey4);
        when(apiKeyRepository.findByApplication("my-test-app")).thenReturn(allApiKeys);
        when(apiKeyRepository.create(any())).thenReturn(newApiKey);

        oldApiKey2.setSubscriptions(List.of("sub1", "sub2"));
        oldApiKey4.setSubscriptions(List.of("sub3"));

        apiKeyService.renew(GraviteeContext.getExecutionContext(), application);

        // the new key has been updated with 3 subscriptions from expired keys
        verify(apiKeyRepository, times(1)).update(
            argThat(
                apiKey ->
                    apiKey.getId().equals("apiKey-X") &&
                    apiKey.getSubscriptions().contains("sub1") &&
                    apiKey.getSubscriptions().contains("sub2") &&
                    apiKey.getSubscriptions().contains("sub3")
            )
        );
    }

    @Test
    public void should_not_duplicate_subscription_and_update_updatedAt_when_adding_existing_subscription() throws TechnicalException {
        ApiKey existingApiKey = new ApiKey();
        existingApiKey.setId("id-of-apikey-2");
        existingApiKey.setKey("dummy-key");

        Date originalUpdatedAt = new Date();
        existingApiKey.setUpdatedAt(originalUpdatedAt);
        existingApiKey.setSubscriptions(new ArrayList<>(Arrays.asList("subscription2", "subscriptionX")));

        ApiKey updatedApiKey = new ApiKey();
        updatedApiKey.setId("id-of-apikey-2");
        updatedApiKey.setKey("dummy-key");

        Date newUpdatedAt = new Date(originalUpdatedAt.getTime() + 1000);
        updatedApiKey.setUpdatedAt(newUpdatedAt);
        updatedApiKey.setSubscriptions(new ArrayList<>(Arrays.asList("subscription2", "subscriptionX")));

        when(apiKeyRepository.findById("id-of-apikey-2")).thenReturn(Optional.of(existingApiKey));
        when(apiKeyRepository.addSubscription("id-of-apikey-2", "subscription2")).thenReturn(Optional.of(updatedApiKey));

        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById("id-of-apikey-2");
        assertTrue("API key should exist", apiKeyOpt.isPresent());
        ApiKey apiKey = apiKeyOpt.get();
        List<String> initialSubscriptions = apiKey.getSubscriptions();
        assertEquals("Initial subscriptions count should be 2", 2, initialSubscriptions.size());

        Optional<ApiKey> resultOpt = apiKeyRepository.addSubscription("id-of-apikey-2", "subscription2");
        assertTrue("Updated API key should be returned", resultOpt.isPresent());
        ApiKey resultApiKey = resultOpt.get();
        List<String> updatedSubscriptions = resultApiKey.getSubscriptions();

        long count = updatedSubscriptions
            .stream()
            .filter(s -> s.equals("subscription2"))
            .count();
        assertEquals("Subscription 'subscription2' should appear only once", 1, count);

        assertEquals("Subscription count should remain unchanged", 2, updatedSubscriptions.size());

        assertTrue("updatedAt should be updated to a later time", resultApiKey.getUpdatedAt().after(existingApiKey.getUpdatedAt()));
    }

    private ApiKey buildTestApiKey(String id) {
        ApiKey newApiKey = new ApiKey();
        newApiKey.setId(id);
        newApiKey.setCreatedAt(new Date());
        return newApiKey;
    }

    private SubscriptionEntity buildTestSubscription(String id) {
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(id);
        return subscriptionEntity;
    }
}
