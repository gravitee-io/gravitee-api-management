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
package io.gravitee.gateway.handlers.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.security.core.SubscriptionTrustStoreLoaderManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionCacheServiceTest {

    private static final String PLAN_ID = "my-test-plan-id";
    private static final String API_ID = "my-test-api-id";
    private static final String SUB_ID = "my-test-subscription-id";
    private static final String SUB_ID_2 = "my-test-subscription-id-2";
    private static final String API_KEY = "my-test-api-key";
    private static final String CLIENT_ID = "my-test-client-id";
    private static final String CLIENT_CERTIFICATE = "my-test-client-certificate";

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;

    @Mock
    private ApiManager apiManager;

    private SubscriptionCacheService subscriptionService;
    private Map<String, Subscription> cacheByApiClientId;
    private Map<String, Subscription> cacheByApiClientCertificate;
    private Map<String, Subscription> cacheBySubscriptionId;
    private Map<String, Set<String>> cacheByApiId;

    @BeforeEach
    public void setup() throws Exception {
        subscriptionService = new SubscriptionCacheService(apiKeyService, subscriptionTrustStoreLoaderManager, apiManager);
        cacheByApiClientId = (Map<String, Subscription>) ReflectionTestUtils.getField(subscriptionService, "cacheByApiClientId");
        cacheByApiClientCertificate =
            (Map<String, Subscription>) ReflectionTestUtils.getField(subscriptionService, "cacheByApiClientCertificate");
        cacheBySubscriptionId = (Map<String, Subscription>) ReflectionTestUtils.getField(subscriptionService, "cacheBySubscriptionId");
        cacheByApiId = (Map<String, Set<String>>) ReflectionTestUtils.getField(subscriptionService, "cacheByApiId");
    }

    @Nested
    class RegisterTest {

        @Test
        void should_register_subscription_with_client_certificate() {
            Subscription subscription = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, PLAN_ID);
            subscriptionService.register(subscription);

            Subscription byId = cacheBySubscriptionId.get(SUB_ID);
            assertThat(byId).isNotNull().isEqualTo(subscription);

            // With plan key
            String cacheKeyWithPlan = subscriptionService.buildClientCertificateCacheKey(subscription);
            Subscription byClientIdWithPlan = cacheByApiClientCertificate.get(cacheKeyWithPlan);
            assertThat(byClientIdWithPlan).isNotNull().isEqualTo(subscription);

            // Without plan key
            String cacheKeyWithoutPlan = subscriptionService.buildCacheKeyFromClientInfo(
                subscription.getApi(),
                subscription.getClientCertificate(),
                null
            );
            Subscription byClientIdWithoutPlan = cacheByApiClientCertificate.get(cacheKeyWithoutPlan);
            assertThat(byClientIdWithoutPlan).isNotNull().isEqualTo(subscription);

            // By api
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).hasSize(3).contains(cacheKeyWithPlan, cacheKeyWithoutPlan, SUB_ID);

            ArgumentCaptor<Set<String>> serversListCaptor = ArgumentCaptor.forClass(Set.class);
            verify(subscriptionTrustStoreLoaderManager).registerSubscription(eq(subscription), serversListCaptor.capture());
            assertThat(serversListCaptor.getValue()).isEmpty();
        }

        @Test
        void should_register_subscription_with_client_certificate_for_api_servers() {
            final Api api = new Api(new io.gravitee.definition.model.v4.Api());
            final HttpListener httpListener = new HttpListener();
            api.getDefinition().setListeners(List.of(httpListener));
            httpListener.setServers(List.of("server1", "server3"));
            when(apiManager.get(API_ID)).thenReturn((ReactableApi) api);
            Subscription subscription = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, PLAN_ID);
            subscriptionService.register(subscription);

            Subscription byId = cacheBySubscriptionId.get(SUB_ID);
            assertThat(byId).isNotNull().isEqualTo(subscription);

            // With plan key
            String cacheKeyWithPlan = subscriptionService.buildClientCertificateCacheKey(subscription);
            Subscription byClientIdWithPlan = cacheByApiClientCertificate.get(cacheKeyWithPlan);
            assertThat(byClientIdWithPlan).isNotNull().isEqualTo(subscription);

            // Without plan key
            String cacheKeyWithoutPlan = subscriptionService.buildCacheKeyFromClientInfo(
                subscription.getApi(),
                subscription.getClientCertificate(),
                null
            );
            Subscription byClientIdWithoutPlan = cacheByApiClientCertificate.get(cacheKeyWithoutPlan);
            assertThat(byClientIdWithoutPlan).isNotNull().isEqualTo(subscription);

            // By api
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).hasSize(3).contains(cacheKeyWithPlan, cacheKeyWithoutPlan, SUB_ID);

            ArgumentCaptor<Set<String>> serversListCaptor = ArgumentCaptor.forClass(Set.class);
            verify(subscriptionTrustStoreLoaderManager).registerSubscription(eq(subscription), serversListCaptor.capture());
            assertThat(serversListCaptor.getValue()).hasSize(2).contains("server1", "server3");
        }

        @Test
        void should_register_subscription_with_new_client_certificate_when_already_registered() {
            Subscription subscription = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, PLAN_ID);
            subscriptionService.register(subscription);

            Subscription originalSub = cacheBySubscriptionId.get(SUB_ID);
            assertThat(originalSub).isNotNull().isEqualTo(subscription);

            Subscription subscriptionUpdated = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, "client_id_updated", PLAN_ID);
            subscriptionService.register(subscriptionUpdated);

            Subscription byId = cacheBySubscriptionId.get(SUB_ID);
            assertThat(byId).isNotNull().isEqualTo(subscriptionUpdated);

            // With plan key
            String cacheKeyWithPlan = subscriptionService.buildClientCertificateCacheKey(subscriptionUpdated);
            Subscription byClientIdWithPlan = cacheByApiClientCertificate.get(cacheKeyWithPlan);
            assertThat(byClientIdWithPlan).isNotNull().isEqualTo(subscriptionUpdated);

            // Without plan key
            String cacheKeyWithoutPlan = subscriptionService.buildCacheKeyFromClientInfo(
                subscriptionUpdated.getApi(),
                subscriptionUpdated.getClientCertificate(),
                null
            );
            Subscription byClientIdWithoutPlan = cacheByApiClientCertificate.get(cacheKeyWithoutPlan);
            assertThat(byClientIdWithoutPlan).isNotNull().isEqualTo(subscriptionUpdated);

            // By api
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).hasSize(3).contains(cacheKeyWithPlan, cacheKeyWithoutPlan, SUB_ID);
        }

        @Test
        void should_register_subscription_with_client_id() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);

            Subscription byId = cacheBySubscriptionId.get(SUB_ID);
            assertThat(byId).isNotNull().isEqualTo(subscription);

            // With plan key
            String cacheKeyWithPlan = subscriptionService.buildClientIdCacheKey(subscription);
            Subscription byClientIdWithPlan = cacheByApiClientId.get(cacheKeyWithPlan);
            assertThat(byClientIdWithPlan).isNotNull().isEqualTo(subscription);

            // Without plan key
            String cacheKeyWithoutPlan = subscriptionService.buildCacheKeyFromClientInfo(
                subscription.getApi(),
                subscription.getClientId(),
                null
            );
            Subscription byClientIdWithoutPlan = cacheByApiClientId.get(cacheKeyWithoutPlan);
            assertThat(byClientIdWithoutPlan).isNotNull().isEqualTo(subscription);

            // By api
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).hasSize(3).contains(cacheKeyWithPlan, cacheKeyWithoutPlan, SUB_ID);
        }

        @Test
        void should_register_subscription_with_new_client_id_when_already_registered() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);

            Subscription originalSub = cacheBySubscriptionId.get(SUB_ID);
            assertThat(originalSub).isNotNull().isEqualTo(subscription);

            Subscription subscriptionUpdated = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, "client_id_updated", PLAN_ID);
            subscriptionService.register(subscriptionUpdated);

            Subscription byId = cacheBySubscriptionId.get(SUB_ID);
            assertThat(byId).isNotNull().isEqualTo(subscriptionUpdated);

            // With plan key
            String cacheKeyWithPlan = subscriptionService.buildClientIdCacheKey(subscriptionUpdated);
            Subscription byClientIdWithPlan = cacheByApiClientId.get(cacheKeyWithPlan);
            assertThat(byClientIdWithPlan).isNotNull().isEqualTo(subscriptionUpdated);

            // Without plan key
            String cacheKeyWithoutPlan = subscriptionService.buildCacheKeyFromClientInfo(
                subscriptionUpdated.getApi(),
                subscriptionUpdated.getClientId(),
                null
            );
            Subscription byClientIdWithoutPlan = cacheByApiClientId.get(cacheKeyWithoutPlan);
            assertThat(byClientIdWithoutPlan).isNotNull().isEqualTo(subscriptionUpdated);

            // By api
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).hasSize(3).contains(cacheKeyWithPlan, cacheKeyWithoutPlan, SUB_ID);
        }

        @Test
        void should_register_subscription_without_client_id() {
            Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID);
            subscriptionService.register(subscription);

            Subscription byId = cacheBySubscriptionId.get(SUB_ID);
            assertThat(byId).isNotNull().isEqualTo(subscription);
            assertThat(cacheByApiClientId).isEmpty();
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).hasSize(1).contains(SUB_ID);
        }

        @Test
        void should_not_register_subscription_when_subscription_is_not_accepted() {
            Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID);
            subscription.setStatus(io.gravitee.repository.management.model.Subscription.Status.CLOSED.name());

            subscriptionService.register(subscription);

            assertThat(cacheBySubscriptionId).doesNotContainKey(SUB_ID);

            // With plan key
            String cacheKeyWithPlan = subscriptionService.buildClientIdCacheKey(subscription);
            assertThat(cacheByApiClientId).doesNotContainKey(cacheKeyWithPlan);

            // Without plan key
            String cacheKeyWithoutPlan = subscriptionService.buildCacheKeyFromClientInfo(
                subscription.getApi(),
                subscription.getClientId(),
                null
            );
            assertThat(cacheByApiClientId).doesNotContainKey(cacheKeyWithoutPlan);

            // By api
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).isNull();
        }
    }

    @Nested
    class UnregisterTest {

        @Test
        void should_unregister_subscription_with_client_certificate() {
            Subscription subscription = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, PLAN_ID);
            subscriptionService.register(subscription);

            Subscription byId = cacheBySubscriptionId.get(SUB_ID);
            assertThat(byId).isNotNull().isEqualTo(subscription);

            subscriptionService.unregister(subscription);

            assertThat(cacheBySubscriptionId).doesNotContainKey(SUB_ID);

            // With plan key
            String cacheKeyWithPlan = subscriptionService.buildClientCertificateCacheKey(subscription);
            assertThat(cacheByApiClientId).doesNotContainKey(cacheKeyWithPlan);
            assertThat(cacheByApiClientCertificate).doesNotContainKey(cacheKeyWithPlan);

            // Without plan key
            String cacheKeyWithoutPlan = subscriptionService.buildCacheKeyFromClientInfo(
                subscription.getApi(),
                subscription.getClientId(),
                null
            );
            assertThat(cacheByApiClientId).doesNotContainKey(cacheKeyWithoutPlan);
            assertThat(cacheByApiClientCertificate).doesNotContainKey(cacheKeyWithoutPlan);

            // By api
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).isNull();
        }

        @Test
        void should_unregister_subscription_with_client_id() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);

            Subscription byId = cacheBySubscriptionId.get(SUB_ID);
            assertThat(byId).isNotNull().isEqualTo(subscription);

            subscriptionService.unregister(subscription);

            assertThat(cacheBySubscriptionId).doesNotContainKey(SUB_ID);

            // With plan key
            String cacheKeyWithPlan = subscriptionService.buildClientIdCacheKey(subscription);
            assertThat(cacheByApiClientId).doesNotContainKey(cacheKeyWithPlan);

            // Without plan key
            String cacheKeyWithoutPlan = subscriptionService.buildCacheKeyFromClientInfo(
                subscription.getApi(),
                subscription.getClientId(),
                null
            );
            assertThat(cacheByApiClientId).doesNotContainKey(cacheKeyWithoutPlan);

            // By api
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).isNull();
        }

        @Test
        void should_do_nothing_when_unregistered_an_non_registered_subscription() {
            Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID);

            subscriptionService.unregister(subscription);

            assertThat(cacheBySubscriptionId).doesNotContainKey(SUB_ID);

            // With plan key
            String cacheKeyWithPlan = subscriptionService.buildClientIdCacheKey(subscription);
            assertThat(cacheByApiClientId).doesNotContainKey(cacheKeyWithPlan);

            // Without plan key
            String cacheKeyWithoutPlan = subscriptionService.buildCacheKeyFromClientInfo(
                subscription.getApi(),
                subscription.getClientId(),
                null
            );
            assertThat(cacheByApiClientId).doesNotContainKey(cacheKeyWithoutPlan);

            // By api
            Set<String> byApiId = cacheByApiId.get(API_ID);
            assertThat(byApiId).isNull();
        }

        @Test
        void should_unregister_all_subscriptions_by_api() {
            for (int i = 0; i < 5; i++) {
                Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID + i, API_ID, CLIENT_ID, PLAN_ID);
                subscriptionService.register(subscription);
            }
            subscriptionService.unregisterByApiId(API_ID);

            assertThat(cacheByApiClientId).isEmpty();
            assertThat(cacheByApiClientCertificate).isEmpty();

            // By api
            assertThat(cacheByApiId).isEmpty();
        }
    }

    @Nested
    class GetTest {

        @Test
        void should_get_subscriptions_by_id() {
            Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID);
            subscriptionService.register(subscription);

            Optional<Subscription> subscriptionOpt = subscriptionService.getById(SUB_ID);
            assertThat(subscriptionOpt).contains(subscription);
        }

        @Test
        void should_get_subscriptions_by_client_id() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);

            Optional<Subscription> subscriptionOpt = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
            assertThat(subscriptionOpt).contains(subscription);
        }

        @Test
        void should_get_subscription_by_api_id_and_apiKey() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);
            SecurityToken securityToken = SecurityToken.forApiKey("apiKeyValue");
            ApiKey apiKey = new ApiKey();
            apiKey.setSubscription(SUB_ID);
            when(apiKeyService.getByApiAndKey(API_ID, "apiKeyValue")).thenReturn(Optional.of(apiKey));

            Optional<Subscription> subscriptionOpt = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
            assertThat(subscriptionOpt).contains(subscription);
        }

        @Test
        void should_not_get_subscription_by_api_id_without_apiKey() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);
            SecurityToken securityToken = SecurityToken.forApiKey("apiKeyValue");
            when(apiKeyService.getByApiAndKey(API_ID, "apiKeyValue")).thenReturn(Optional.empty());

            Optional<Subscription> subscriptionOpt = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
            assertThat(subscriptionOpt).isEmpty();
        }

        @Test
        void should_get_subscription_by_api_id_and_clientId() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);
            SecurityToken securityToken = SecurityToken.forClientId(CLIENT_ID);
            Optional<Subscription> subscriptionOpt = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
            assertThat(subscriptionOpt).contains(subscription);
        }

        @Test
        void should_not_get_subscription_by_api_id_and_unknown_security_token() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);
            SecurityToken securityToken = SecurityToken.builder().tokenValue("unknown").tokenType("unknown").build();
            Optional<Subscription> subscriptionOpt = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
            assertThat(subscriptionOpt).isEmpty();
        }
    }

    private Subscription buildAcceptedSubscription(String id, String apiId) {
        return buildSubscription(id, apiId, s -> {}, null, io.gravitee.repository.management.model.Subscription.Status.ACCEPTED);
    }

    private Subscription buildAcceptedSubscriptionWithClientId(String id, String apiId, String clientId, String plan) {
        return buildSubscription(
            id,
            apiId,
            s -> s.setClientId(clientId),
            plan,
            io.gravitee.repository.management.model.Subscription.Status.ACCEPTED
        );
    }

    private Subscription buildAcceptedSubscriptionWithClientCertificate(String id, String apiId, String clientCertificate, String plan) {
        return buildSubscription(
            id,
            apiId,
            s -> s.setClientCertificate(clientCertificate),
            plan,
            io.gravitee.repository.management.model.Subscription.Status.ACCEPTED
        );
    }

    private Subscription buildSubscription(
        String id,
        String api,
        Consumer<Subscription> subscriptionModifier,
        String plan,
        final io.gravitee.repository.management.model.Subscription.Status status
    ) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setApi(api);
        subscription.setPlan(plan);
        subscriptionModifier.accept(subscription);
        subscription.setStatus(status.name());
        return subscription;
    }
}
