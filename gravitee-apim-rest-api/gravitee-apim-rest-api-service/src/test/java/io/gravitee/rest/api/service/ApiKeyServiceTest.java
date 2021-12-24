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

import static org.junit.Assert.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.ApiKeyServiceImpl;
import io.gravitee.rest.api.service.notification.ApiHook;
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

    @InjectMocks
    private ApiKeyService apiKeyService = new ApiKeyServiceImpl();

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private PlanService planService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private EmailService emailService;

    @Mock
    private ApiService apiService;

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
        when(subscription.getEndingAt()).thenReturn(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);

        // Stub API Key creation
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());

        // Run
        final ApiKeyEntity apiKey = apiKeyService.generate(SUBSCRIPTION_ID);

        // Verify
        verify(apiKeyRepository, times(1)).create(any());
        assertEquals(API_KEY, apiKey.getKey());
        assertFalse(apiKey.isRevoked());
        assertEquals(subscription.getEndingAt(), apiKey.getExpireAt());
        assertEquals(subscription.getId(), apiKey.getSubscription());

        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        verify(auditService).createApiAuditLog(any(), argument.capture(), any(), any(), any(), any());
        Map<Audit.AuditProperties, String> properties = argument.getValue();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey(Audit.AuditProperties.API));
        assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
        assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));
    }

    @Test
    public void shouldGenerateWithCustomApiKey() throws TechnicalException {
        final String customApiKey = "customApiKey";

        // Prepare subscription
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getEndingAt()).thenReturn(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);

        // Stub API Key creation
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());

        // Run
        final ApiKeyEntity apiKey = apiKeyService.generate(SUBSCRIPTION_ID, customApiKey);

        // Verify
        verify(apiKeyRepository, times(1)).create(any());
        assertFalse(apiKey.isRevoked());
        assertEquals(subscription.getEndingAt(), apiKey.getExpireAt());
        assertEquals(subscription.getId(), apiKey.getSubscription());

        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        verify(auditService).createApiAuditLog(any(), argument.capture(), any(), any(), any(), any());
        Map<Audit.AuditProperties, String> properties = argument.getValue();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey(Audit.AuditProperties.API));
        assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
        assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));

        verify(apiKeyGenerator, times(0)).generate();
        assertEquals(customApiKey, apiKey.getKey());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotGenerateBecauseTechnicalException() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(TechnicalManagementException.class);

        apiKeyService.generate(SUBSCRIPTION_ID);
    }

    @Test(expected = ApiKeyAlreadyExistingException.class)
    public void shouldNotGenerateBecauseApiKeyAlreadyExistsForAnotherApp() throws TechnicalException {
        ApiKey existingKey = new ApiKey();
        existingKey.setApplication("another Application");
        existingKey.setApi("another Api");

        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);
        when(apiKeyRepository.findByKey("alreadyExistingApiKey")).thenReturn(List.of(existingKey));

        apiKeyService.generate(SUBSCRIPTION_ID, "alreadyExistingApiKey");
    }

    @Test
    public void shouldRevoke() throws Exception {
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscription(SUBSCRIPTION_ID);
        apiKey.setCreatedAt(new Date());
        apiKey.setPlan(PLAN_ID);
        apiKey.setApplication(APPLICATION_ID);
        final ApiModelEntity api = mock(ApiModelEntity.class);
        when(api.getId()).thenReturn("123");

        // Prepare data
        when(subscription.getApplication()).thenReturn(APPLICATION_ID);
        when(subscription.getPlan()).thenReturn(PLAN_ID);

        // Stub
        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenReturn(Optional.of(apiKey));
        when(applicationService.findById(GraviteeContext.getCurrentEnvironment(), subscription.getApplication())).thenReturn(application);
        when(planService.findById(subscription.getPlan())).thenReturn(plan);
        when(apiService.findByIdForTemplates(any())).thenReturn(api);

        // Run
        apiKeyService.revoke(API_KEY, API_ID, true);

        // Verify
        verify(apiKeyRepository, times(1)).update(any());

        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        verify(auditService).createApiAuditLog(any(), argument.capture(), any(), any(), any(), any());
        Map<Audit.AuditProperties, String> properties = argument.getValue();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey(Audit.AuditProperties.API));
        assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
        assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));
    }

    @Test(expected = ApiKeyAlreadyExpiredException.class)
    public void shouldNotRevokeBecauseAlreadyRevoked() throws Exception {
        apiKey = new ApiKey();
        apiKey.setRevoked(true);

        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenReturn(Optional.of(apiKey));

        apiKeyService.revoke(API_KEY, API_ID, true);
    }

    @Test(expected = ApiKeyAlreadyExpiredException.class)
    public void shouldNotRevokeBecauseAlreadyExpired() throws Exception {
        apiKey = new ApiKey();
        apiKey.setExpireAt(Date.from(new Date().toInstant().minus(1, ChronoUnit.DAYS)));

        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenReturn(Optional.of(apiKey));

        apiKeyService.revoke(API_KEY, API_ID, true);
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void shouldNotRevokeBecauseNotFound() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenReturn(Optional.empty());

        apiKeyService.revoke(API_KEY, API_ID, true);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotRevokeBecauseTechnicalException() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenThrow(TechnicalException.class);

        apiKeyService.revoke(API_KEY, API_ID, true);
    }

    @Test
    public void shouldReactivateRevoked() throws Exception {
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscription(SUBSCRIPTION_ID);
        apiKey.setCreatedAt(new Date());
        apiKey.setPlan(PLAN_ID);
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setRevoked(true);
        final ApiModelEntity api = new ApiModelEntity();
        api.setId("123");

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setApi(api.getId());
        subscription.setStatus(SubscriptionStatus.PAUSED);

        // Stub
        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenReturn(Optional.of(apiKey));
        when(subscriptionService.findById(apiKey.getSubscription())).thenReturn(subscription);
        when(apiKeyRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        // Run
        apiKeyService.reactivate(API_KEY, API_ID);

        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        verify(auditService).createApiAuditLog(any(), argument.capture(), any(), any(), any(), any());
        Map<Audit.AuditProperties, String> properties = argument.getValue();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey(Audit.AuditProperties.API));
        assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
        assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));
    }

    @Test
    public void shouldReactivateExpired() throws Exception {
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscription(SUBSCRIPTION_ID);
        apiKey.setCreatedAt(new Date());
        apiKey.setPlan(PLAN_ID);
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setExpireAt(new Date(System.currentTimeMillis() - 10000));
        final ApiModelEntity api = new ApiModelEntity();
        api.setId("123");

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setApi(api.getId());
        subscription.setStatus(SubscriptionStatus.PAUSED);

        // Stub
        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenReturn(Optional.of(apiKey));
        when(subscriptionService.findById(apiKey.getSubscription())).thenReturn(subscription);
        when(apiKeyRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        // Run
        apiKeyService.reactivate(API_KEY, API_ID);

        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        verify(auditService).createApiAuditLog(any(), argument.capture(), any(), any(), any(), any());
        Map<Audit.AuditProperties, String> properties = argument.getValue();
        assertEquals(3, properties.size());
        assertTrue(properties.containsKey(Audit.AuditProperties.API));
        assertTrue(properties.containsKey(Audit.AuditProperties.API_KEY));
        assertTrue(properties.containsKey(Audit.AuditProperties.APPLICATION));
    }

    @Test(expected = ApiKeyAlreadyActivatedException.class)
    public void shouldNotReactivateBecauseOfAlreadyActivated() throws Exception {
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscription(SUBSCRIPTION_ID);
        apiKey.setCreatedAt(new Date());
        apiKey.setPlan(PLAN_ID);
        apiKey.setApplication(APPLICATION_ID);

        // Stub
        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenReturn(Optional.of(apiKey));

        // Run
        apiKeyService.reactivate(API_KEY, API_ID);
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void shouldNotReactivateBecauseOfApiKeyNotFound() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenReturn(Optional.empty());

        apiKeyService.reactivate(API_KEY, API_ID);
    }

    @Test(expected = SubscriptionNotActiveException.class)
    public void shouldNotReactivateBecauseOfNotActiveSubscription() throws TechnicalException {
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscription(SUBSCRIPTION_ID);
        apiKey.setCreatedAt(new Date());
        apiKey.setPlan(PLAN_ID);
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setExpireAt(new Date(System.currentTimeMillis() - 10000));

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setStatus(SubscriptionStatus.CLOSED);
        // Stub
        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenReturn(Optional.of(apiKey));
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscriptionEntity);

        apiKeyService.reactivate(API_KEY, API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotReactivate_technicalException() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi(API_KEY, API_ID)).thenThrow(TechnicalException.class);

        apiKeyService.revoke(API_KEY, API_ID, true);
    }

    @Test
    public void shouldRenew() throws TechnicalException {
        // Prepare data
        // apiKey object is not a mock since its state is updated by the call to apiKeyService.renew()
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscription(SUBSCRIPTION_ID);
        apiKey.setCreatedAt(new Date());
        apiKey.setPlan(PLAN_ID);
        apiKey.setApplication(APPLICATION_ID);
        final ApiModelEntity api = mock(ApiModelEntity.class);
        when(api.getId()).thenReturn("123");

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getEndingAt()).thenReturn(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        when(subscription.getApplication()).thenReturn(APPLICATION_ID);
        when(subscription.getPlan()).thenReturn(PLAN_ID);

        // Stub
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(subscriptionService.findById(subscription.getId())).thenReturn(subscription);
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());
        when(apiKeyRepository.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKey));
        when(applicationService.findById(GraviteeContext.getCurrentEnvironment(), subscription.getApplication())).thenReturn(application);
        when(planService.findById(subscription.getPlan())).thenReturn(plan);
        when(apiService.findByIdForTemplates(any())).thenReturn(api);

        // Run
        final ApiKeyEntity apiKeyEntity = apiKeyService.renew(SUBSCRIPTION_ID);

        // Verify
        // A new API Key has been created
        verify(apiKeyRepository, times(1)).create(any());
        assertEquals(API_KEY, apiKeyEntity.getKey());

        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        verify(auditService, times(2)).createApiAuditLog(any(), argument.capture(), any(), any(), any(), any());
        for (Map<Audit.AuditProperties, String> properties : argument.getAllValues()) {
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
    public void shouldRenewWithoutRenewingExpiredKeys() throws TechnicalException {
        // Prepare data
        // apiKey object is not a mock since its state is updated by the call to apiKeyService.renew()
        apiKey = new ApiKey();
        apiKey.setKey("123-456-789");
        apiKey.setSubscription(SUBSCRIPTION_ID);
        apiKey.setCreatedAt(new Date());
        apiKey.setPlan(PLAN_ID);
        apiKey.setApplication(APPLICATION_ID);
        apiKey.setExpireAt(Date.from(new Date().toInstant().minus(1, ChronoUnit.DAYS)));
        final ApiModelEntity api = mock(ApiModelEntity.class);

        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getEndingAt()).thenReturn(Date.from(new Date().toInstant().plus(1, ChronoUnit.DAYS)));
        when(subscription.getApplication()).thenReturn(APPLICATION_ID);
        when(subscription.getPlan()).thenReturn(PLAN_ID);

        // Stub
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(subscriptionService.findById(subscription.getId())).thenReturn(subscription);
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());
        when(apiKeyRepository.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKey));
        when(applicationService.findById(GraviteeContext.getCurrentEnvironment(), subscription.getApplication())).thenReturn(application);
        when(planService.findById(subscription.getPlan())).thenReturn(plan);
        when(apiService.findByIdForTemplates(any())).thenReturn(api);

        // Run
        final ApiKeyEntity apiKeyEntity = apiKeyService.renew(SUBSCRIPTION_ID);

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
        ApiKey existingKey = new ApiKey();
        existingKey.setApplication("another Application");
        existingKey.setApi("another Api");

        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);
        when(apiKeyRepository.findByKey("alreadyExistingApiKey")).thenReturn(List.of(existingKey));

        apiKeyService.renew(SUBSCRIPTION_ID, "alreadyExistingApiKey");
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void shouldNotUpdate() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi(any(), any())).thenReturn(Optional.empty());
        apiKeyService.update(new ApiKeyEntity());
        fail("It should throws ApiKeyNotFoundException");
    }

    @Test
    public void shouldUpdateNotExpired() throws TechnicalException {
        ApiKey existingApiKey = new ApiKey();
        when(apiKeyRepository.findByKeyAndApi("ABC", "api12")).thenReturn(Optional.of(existingApiKey));
        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey("ABC");
        apiKeyEntity.setApi("api12");
        apiKeyEntity.setRevoked(true);
        apiKeyEntity.setPaused(true);

        apiKeyService.update(apiKeyEntity);

        verify(apiKeyRepository, times(1)).update(existingApiKey);
        //must be manage by the revoke method
        assertFalse("isRevoked", existingApiKey.isRevoked());
        assertTrue("isPaused", existingApiKey.isPaused());
    }

    @Test
    public void shouldUpdateExpired() throws TechnicalException {
        ApiKey existingApiKey = new ApiKey();
        when(apiKeyRepository.findByKeyAndApi("ABC", "api12")).thenReturn(Optional.of(existingApiKey));
        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey("ABC");
        apiKeyEntity.setApi("api12");
        apiKeyEntity.setPaused(true);
        apiKeyEntity.setExpireAt(new Date());
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setEndingAt(new Date());
        when(subscriptionService.findById(any())).thenReturn(subscriptionEntity);
        //notification mocks
        when(applicationService.findById(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(mock(ApplicationEntity.class));
        PlanEntity mockedPlan = mock(PlanEntity.class);
        when(planService.findById(any())).thenReturn(mockedPlan);
        when(apiService.findByIdForTemplates(any())).thenReturn(mock(ApiModelEntity.class));

        apiKeyService.update(apiKeyEntity);

        verify(apiKeyRepository, times(1)).update(existingApiKey);
        //must be manage by the revoke method
        assertFalse("isRevoked", existingApiKey.isRevoked());
        assertTrue("isPaused", existingApiKey.isPaused());
        verify(notifierService, times(1)).trigger(eq(ApiHook.APIKEY_EXPIRED), any(), any());
        verify(auditService, times(1)).createApiAuditLog(any(), any(), eq(ApiKey.AuditEvent.APIKEY_EXPIRED), any(), any(), any());
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

        List<ApiKeyEntity> bySubscription = apiKeyService.findBySubscription(subscriptionId).stream().collect(Collectors.toList());

        assertEquals(3, bySubscription.size());
        assertEquals("first", bySubscription.get(0).getKey());
        assertEquals("second", bySubscription.get(1).getKey());
        assertEquals("last", bySubscription.get(2).getKey());
    }

    @Test
    public void canCreate_should_return_true_cause_key_doesnt_exists_yet() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "my-api-id";
        String applicationId = "my-application-id";

        when(apiKeyRepository.findByKey(apiKeyToCreate)).thenReturn(Collections.emptyList());

        boolean canCreate = apiKeyService.canCreate(apiKeyToCreate, apiId, applicationId);

        assertTrue(canCreate);
    }

    @Test
    public void canCreate_should_return_true_cause_key_already_exists_for_same_application_on_other_api() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "my-api-id";
        String applicationId = "my-application-id";

        ApiKey existingApiKey1 = new ApiKey();
        existingApiKey1.setApi("anotherApi-1");
        existingApiKey1.setApplication(applicationId);
        existingApiKey1.setKey(apiKeyToCreate);

        ApiKey existingApiKey2 = new ApiKey();
        existingApiKey2.setApi("anotherApi-2");
        existingApiKey2.setApplication(applicationId);
        existingApiKey2.setKey(apiKeyToCreate);

        when(apiKeyRepository.findByKey(apiKeyToCreate)).thenReturn(List.of(existingApiKey1, existingApiKey2));

        boolean canCreate = apiKeyService.canCreate(apiKeyToCreate, apiId, applicationId);

        assertTrue(canCreate);
    }

    @Test
    public void canCreate_should_return_false_cause_key_already_exists_for_same_application_on_same_api() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "my-api-id";
        String applicationId = "my-application-id";

        ApiKey existingApiKey1 = new ApiKey();
        existingApiKey1.setApi("anotherApi-1");
        existingApiKey1.setApplication(applicationId);
        existingApiKey1.setKey(apiKeyToCreate);

        ApiKey existingApiKey2 = new ApiKey();
        existingApiKey2.setApi(apiId);
        existingApiKey2.setApplication(applicationId);
        existingApiKey2.setKey(apiKeyToCreate);

        when(apiKeyRepository.findByKey(apiKeyToCreate)).thenReturn(List.of(existingApiKey1, existingApiKey2));

        boolean canCreate = apiKeyService.canCreate(apiKeyToCreate, apiId, applicationId);

        assertFalse(canCreate);
    }

    @Test
    public void canCreate_should_return_false_cause_key_already_exists_for_another_application() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "my-api-id";
        String applicationId = "my-application-id";

        ApiKey existingApiKey = new ApiKey();
        existingApiKey.setApi("anotherApi-1");
        existingApiKey.setApplication("anotherApp");
        existingApiKey.setKey(apiKeyToCreate);

        when(apiKeyRepository.findByKey(apiKeyToCreate)).thenReturn(List.of(existingApiKey));

        boolean canCreate = apiKeyService.canCreate(apiKeyToCreate, apiId, applicationId);

        assertFalse(canCreate);
    }

    @Test(expected = TechnicalManagementException.class)
    public void canCreate_should_throw_TechnicalManagementException_cause_key_search_thrown_exception() throws Exception {
        String apiKeyToCreate = "apikey-i-want-to-create";
        String apiId = "my-api-id";
        String applicationId = "my-application-id";

        when(apiKeyRepository.findByKey(apiKeyToCreate)).thenThrow(TechnicalException.class);

        apiKeyService.canCreate(apiKeyToCreate, apiId, applicationId);
    }

    @Test
    public void revokeById_should_read_key_by_id_and_update_it() throws TechnicalException {
        ApiKey apiKey = new ApiKey();

        when(apiKeyRepository.findById("apiKeyId")).thenReturn(Optional.of(apiKey));
        when(planService.findById(any())).thenReturn(new PlanEntity());
        when(applicationService.findById(any())).thenReturn(new ApplicationEntity());
        when(apiService.findByIdForTemplates(any())).thenReturn(new ApiModelEntity());

        apiKeyService.revoke("apiKeyId", true);

        verify(apiKeyRepository, times(1)).update(apiKey);
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void revokeById_should_throw_apiKeyNotFoundException_when_key_not_found() throws TechnicalException {
        when(apiKeyRepository.findById("apiKeyId")).thenReturn(Optional.empty());

        apiKeyService.revoke("apiKeyId", true);
    }

    @Test(expected = TechnicalManagementException.class)
    public void revokeById_should_throw_technicalManagementException_when_exception_thrown() throws TechnicalException {
        when(apiKeyRepository.findById("apiKeyId")).thenThrow(TechnicalException.class);

        apiKeyService.revoke("apiKeyId", true);
    }

    @Test(expected = TechnicalManagementException.class)
    public void findByKey_should_throw_technicalManagementException_when_exception_thrown() throws TechnicalException {
        when(apiKeyRepository.findByKey("apiKey")).thenThrow(TechnicalException.class);

        apiKeyService.findByKey("apiKey");
    }

    @Test
    public void findByKey_should_convert_to_entities_and_return_list() throws TechnicalException {
        ApiKey apiKey1 = new ApiKey();
        apiKey1.setId("api-key-1-id");
        ApiKey apiKey2 = new ApiKey();
        apiKey2.setId("api-key-2-id");
        when(apiKeyRepository.findByKey("apiKey")).thenReturn(List.of(apiKey1, apiKey2));

        List<ApiKeyEntity> apiKeyEntities = apiKeyService.findByKey("apiKey");

        assertEquals(2, apiKeyEntities.size());
        assertEquals("api-key-1-id", apiKeyEntities.get(0).getId());
        assertEquals("api-key-2-id", apiKeyEntities.get(1).getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void findById_should_throw_technicalManagementException_when_exception_thrown() throws TechnicalException {
        when(apiKeyRepository.findById("apiKey")).thenThrow(TechnicalException.class);

        apiKeyService.findById("apiKey");
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void findById_should_throw_ApiKeyNotFoundException_when_not_found() throws TechnicalException {
        when(apiKeyRepository.findById("apiKey")).thenReturn(Optional.empty());

        apiKeyService.findById("apiKey");
    }

    @Test
    public void findById_should_convert_to_entity_and_return() throws TechnicalException {
        ApiKey apiKey1 = new ApiKey();
        apiKey1.setId("api-key-1-id");
        when(apiKeyRepository.findById("apiKey")).thenReturn(Optional.of(apiKey1));

        ApiKeyEntity apiKeyEntity = apiKeyService.findById("apiKey");

        assertEquals("api-key-1-id", apiKeyEntity.getId());
    }

    @Test(expected = TechnicalManagementException.class)
    public void findByKeyAndApi_should_throw_technicalManagementException_when_exception_thrown() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi("apiKey", "apiId")).thenThrow(TechnicalException.class);

        apiKeyService.findByKeyAndApi("apiKey", "apiId");
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void findByKeyAndApi_should_throw_apiKeyNotFoundException_when_not_found() throws TechnicalException {
        when(apiKeyRepository.findByKeyAndApi("apiKey", "apiId")).thenReturn(Optional.empty());

        apiKeyService.findByKeyAndApi("apiKey", "apiId");
    }

    @Test
    public void findByKeyAndApi_should_convert_found_api_to_entity_and_return_it() throws TechnicalException {
        ApiKey apiKey = new ApiKey();
        apiKey.setId("api-key-1-id");
        when(apiKeyRepository.findByKeyAndApi("apiKey", "apiId")).thenReturn(Optional.of(apiKey));

        ApiKeyEntity apiKeyEntity = apiKeyService.findByKeyAndApi("apiKey", "apiId");
        assertNotNull(apiKeyEntity);
        assertEquals("api-key-1-id", apiKeyEntity.getId());
    }
}
