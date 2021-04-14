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

        PlanEntity planEntity = mock(PlanEntity.class);
        when(planEntity.getApi()).thenReturn("apiId");
        when(planService.findById(any())).thenReturn(planEntity);

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

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotGenerateBecauseTechnicalException() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(TechnicalManagementException.class);

        apiKeyService.generate(SUBSCRIPTION_ID);
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
        when(plan.getApi()).thenReturn(API_ID);

        // Stub
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));
        when(applicationService.findById(subscription.getApplication())).thenReturn(application);
        when(planService.findById(subscription.getPlan())).thenReturn(plan);
        when(apiService.findByIdForTemplates(any())).thenReturn(api);

        // Run
        apiKeyService.revoke(API_KEY, true);

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

        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));

        apiKeyService.revoke(API_KEY, true);
    }

    @Test(expected = ApiKeyAlreadyExpiredException.class)
    public void shouldNotRevokeBecauseAlreadyExpired() throws Exception {
        apiKey = new ApiKey();
        apiKey.setExpireAt(Date.from(new Date().toInstant().minus(1, ChronoUnit.DAYS)));

        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));

        apiKeyService.revoke(API_KEY, true);
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void shouldNotRevokeBecauseNotFound() throws TechnicalException {
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.empty());

        apiKeyService.revoke(API_KEY, true);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotRevokeBecauseTechnicalException() throws TechnicalException {
        when(apiKeyRepository.findById(API_KEY)).thenThrow(TechnicalException.class);

        apiKeyService.revoke(API_KEY, true);
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
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));
        when(subscriptionService.findById(apiKey.getSubscription())).thenReturn(subscription);
        when(apiKeyRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        // Run
        apiKeyService.reactivate(API_KEY);

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
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));
        when(subscriptionService.findById(apiKey.getSubscription())).thenReturn(subscription);
        when(apiKeyRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        // Run
        apiKeyService.reactivate(API_KEY);

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
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));

        // Run
        apiKeyService.reactivate(API_KEY);
    }

    @Test(expected = ApiKeyNotFoundException.class)
    public void shouldNotReactivateBecauseOfApiKeyNotFound() throws TechnicalException {
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.empty());

        apiKeyService.reactivate(API_KEY);
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
        when(apiKeyRepository.findById(API_KEY)).thenReturn(Optional.of(apiKey));
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscriptionEntity);

        apiKeyService.reactivate(API_KEY);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotReactivate_technicalException() throws TechnicalException {
        when(apiKeyRepository.findById(API_KEY)).thenThrow(TechnicalException.class);

        apiKeyService.revoke(API_KEY, true);
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
        when(plan.getApi()).thenReturn(API_ID);

        // Stub
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(subscriptionService.findById(subscription.getId())).thenReturn(subscription);
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());
        when(apiKeyRepository.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKey));
        when(applicationService.findById(subscription.getApplication())).thenReturn(application);
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
        when(plan.getApi()).thenReturn(API_ID);

        // Stub
        when(apiKeyGenerator.generate()).thenReturn(API_KEY);
        when(subscriptionService.findById(subscription.getId())).thenReturn(subscription);
        when(apiKeyRepository.create(any())).thenAnswer(returnsFirstArg());
        when(apiKeyRepository.findBySubscription(SUBSCRIPTION_ID)).thenReturn(Collections.singleton(apiKey));
        when(applicationService.findById(subscription.getApplication())).thenReturn(application);
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

    @Test(expected = ApiKeyNotFoundException.class)
    public void shouldNotUpdate() throws TechnicalException {
        when(apiKeyRepository.findById(any())).thenReturn(Optional.empty());
        apiKeyService.update(new ApiKeyEntity());
        fail("It should throws ApiKeyNotFoundException");
    }

    @Test
    public void shouldUpdateNotExpired() throws TechnicalException {
        ApiKey existingApiKey = new ApiKey();
        when(apiKeyRepository.findById(any())).thenReturn(Optional.of(existingApiKey));
        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey("ABC");
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
        when(apiKeyRepository.findById(any())).thenReturn(Optional.of(existingApiKey));
        ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey("ABC");
        apiKeyEntity.setPaused(true);
        apiKeyEntity.setExpireAt(new Date());
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setEndingAt(new Date());
        when(subscriptionService.findById(any())).thenReturn(subscriptionEntity);
        //notification mocks
        when(applicationService.findById(any())).thenReturn(mock(ApplicationEntity.class));
        PlanEntity mockedPlan = mock(PlanEntity.class);
        when(mockedPlan.getApi()).thenReturn("api");
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
}
