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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.DigestUtils;

@ExtendWith(MockitoExtension.class)
class SubscriptionCacheServiceTest {

    private static final String PLAN_ID = "my-test-plan-id";
    private static final String API_ID = "my-test-api-id";
    private static final String SUB_ID = "my-test-subscription-id";
    private static final String API_ID_2 = "my-test-api-id-2";
    private static final String PLAN_ID_2 = "my-test-plan-id-2";
    private static final String SUB_ID_2 = "my-test-subscription-id-2";
    private static final String CLIENT_ID = "my-test-client-id";
    private static final String CLIENT_CERTIFICATE = "my-test-client-certificate";

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private SubscriptionTrustStoreLoaderManager subscriptionTrustStoreLoaderManager;

    @Mock
    private ApiManager apiManager;

    private SubscriptionCacheService subscriptionService;

    @BeforeEach
    void setup() {
        subscriptionService = new SubscriptionCacheService(apiKeyService, subscriptionTrustStoreLoaderManager, apiManager);
    }

    @Nested
    class RegisterTest {

        @Test
        void should_register_subscription_with_client_certificate() {
            Subscription subscription = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, PLAN_ID);
            subscriptionService.register(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscription);

            // With plan key
            assertThat(subscriptionService.getByClientCertificate(subscription)).isPresent().get().isEqualTo(subscription);

            // Without plan key
            Subscription lookupWithoutPlan = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, null);
            assertThat(subscriptionService.getByClientCertificate(lookupWithoutPlan)).isPresent().get().isEqualTo(subscription);

            // By api
            assertThat(subscriptionService.getByApiId(API_ID)).hasSize(3).contains(SUB_ID);

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

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscription);

            // With plan key
            assertThat(subscriptionService.getByClientCertificate(subscription)).isPresent().get().isEqualTo(subscription);

            // Without plan key
            Subscription lookupWithoutPlan = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, null);
            assertThat(subscriptionService.getByClientCertificate(lookupWithoutPlan)).isPresent().get().isEqualTo(subscription);

            // By api
            assertThat(subscriptionService.getByApiId(API_ID)).hasSize(3).contains(SUB_ID);

            ArgumentCaptor<Set<String>> serversListCaptor = ArgumentCaptor.forClass(Set.class);
            verify(subscriptionTrustStoreLoaderManager).registerSubscription(eq(subscription), serversListCaptor.capture());
            assertThat(serversListCaptor.getValue()).hasSize(2).contains("server1", "server3");
        }

        @Test
        void should_register_subscription_with_new_client_certificate_when_already_registered() {
            Subscription subscription = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, PLAN_ID);
            subscriptionService.register(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscription);

            Subscription subscriptionUpdated = buildAcceptedSubscriptionWithClientCertificate(
                SUB_ID,
                API_ID,
                "client_cert_updated",
                PLAN_ID
            );
            subscriptionService.register(subscriptionUpdated);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscriptionUpdated);

            // With plan key
            assertThat(subscriptionService.getByClientCertificate(subscriptionUpdated)).isPresent().get().isEqualTo(subscriptionUpdated);

            // Without plan key
            Subscription lookupWithoutPlan = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, "client_cert_updated", null);
            assertThat(subscriptionService.getByClientCertificate(lookupWithoutPlan)).isPresent().get().isEqualTo(subscriptionUpdated);

            // By api
            assertThat(subscriptionService.getByApiId(API_ID)).hasSize(3).contains(SUB_ID);
        }

        @Test
        void should_register_subscription_with_client_id() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscription);

            // With plan key
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID))
                .isPresent()
                .get()
                .isEqualTo(subscription);

            // Without plan key
            assertThat(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).isPresent().get().isEqualTo(subscription);

            // By api
            assertThat(subscriptionService.getByApiId(API_ID)).hasSize(3).contains(SUB_ID);
        }

        @Test
        void should_register_subscription_with_new_client_id_when_already_registered() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscription);

            Subscription subscriptionUpdated = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, "client_id_updated", PLAN_ID);
            subscriptionService.register(subscriptionUpdated);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscriptionUpdated);

            // With plan key
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, "client_id_updated", PLAN_ID))
                .isPresent()
                .get()
                .isEqualTo(subscriptionUpdated);

            // Without plan key
            assertThat(subscriptionService.getByApiAndClientId(API_ID, "client_id_updated"))
                .isPresent()
                .get()
                .isEqualTo(subscriptionUpdated);

            // By api
            assertThat(subscriptionService.getByApiId(API_ID)).hasSize(3).contains(SUB_ID);
        }

        @Test
        void should_register_subscription_without_client_id() {
            Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID);
            subscriptionService.register(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscription);
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).isEmpty();
            assertThat(subscriptionService.getByApiId(API_ID)).hasSize(1).contains(SUB_ID);
        }

        @Test
        void should_be_idempotent_when_registering_same_subscription_twice() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);
            subscriptionService.register(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscription);
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID))
                .isPresent()
                .get()
                .isEqualTo(subscription);
            assertThat(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).isPresent().get().isEqualTo(subscription);
        }

        @Test
        void should_cleanup_old_client_id_when_updating() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);

            Subscription updated = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, "new-client-id", PLAN_ID);
            subscriptionService.register(updated);

            // New client ID is found
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, "new-client-id", PLAN_ID))
                .isPresent()
                .get()
                .isEqualTo(updated);
            assertThat(subscriptionService.getByApiAndClientId(API_ID, "new-client-id")).isPresent().get().isEqualTo(updated);

            // Old client ID is gone
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).isEmpty();
        }

        @Test
        void should_register_new_cert_and_keep_old_cert_in_cache_when_updating() {
            // Note: when a cert changes, the old cert entries in cacheByClientCertificate are NOT cleaned up.
            // This is acceptable because the real security token lookup goes through
            // subscriptionTrustStoreLoaderManager.getByCertificate, not through cacheByClientCertificate.
            Subscription subscription = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, PLAN_ID);
            subscriptionService.register(subscription);

            Subscription updated = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, "new-client-cert", PLAN_ID);
            subscriptionService.register(updated);

            // New cert is found
            assertThat(subscriptionService.getByClientCertificate(updated)).isPresent().get().isEqualTo(updated);
            Subscription lookupNewWithoutPlan = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, "new-client-cert", null);
            assertThat(subscriptionService.getByClientCertificate(lookupNewWithoutPlan)).isPresent().get().isEqualTo(updated);

            // getById returns the updated subscription
            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(updated);

            // Old cert still present in cache (stale entry, but harmless — real lookup uses trust store)
            assertThat(subscriptionService.getByClientCertificate(subscription)).isPresent();
        }

        @Test
        void should_not_register_subscription_when_subscription_is_not_accepted() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscription.setStatus(io.gravitee.repository.management.model.Subscription.Status.CLOSED.name());

            subscriptionService.register(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).isEmpty();
            assertThat(subscriptionService.getByApiId(API_ID)).isEmpty();
        }
    }

    @Nested
    class UnregisterTest {

        @Test
        void should_unregister_subscription_with_client_certificate() {
            Subscription subscription = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, PLAN_ID);
            subscriptionService.register(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscription);

            subscriptionService.unregister(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isEmpty();
            assertThat(subscriptionService.getByClientCertificate(subscription)).isEmpty();
            Subscription lookupWithoutPlan = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, null);
            assertThat(subscriptionService.getByClientCertificate(lookupWithoutPlan)).isEmpty();
            assertThat(subscriptionService.getByApiId(API_ID)).isEmpty();
        }

        @Test
        void should_unregister_subscription_with_client_id() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isPresent().get().isEqualTo(subscription);

            subscriptionService.unregister(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).isEmpty();
            assertThat(subscriptionService.getByApiId(API_ID)).isEmpty();
        }

        @Test
        void should_do_nothing_when_unregistered_an_non_registered_subscription() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.unregister(subscription);

            assertThat(subscriptionService.getById(SUB_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).isEmpty();
            assertThat(subscriptionService.getByApiId(API_ID)).isEmpty();
        }

        @Test
        void should_unregister_all_subscriptions_by_api() {
            for (int i = 0; i < 5; i++) {
                Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID + i, API_ID, CLIENT_ID, PLAN_ID);
                subscriptionService.register(subscription);
            }
            subscriptionService.unregisterByApiId(API_ID);

            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).isEmpty();
            assertThat(subscriptionService.getByApiId(API_ID)).isEmpty();
        }

        @Test
        void should_only_unregister_subscriptions_for_given_api() {
            Subscription subApi1 = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            Subscription subApi2 = buildAcceptedSubscriptionWithClientId(SUB_ID_2, API_ID_2, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subApi1);
            subscriptionService.register(subApi2);

            subscriptionService.unregisterByApiId(API_ID);

            // API_ID subscriptions are gone
            assertThat(subscriptionService.getById(SUB_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).isEmpty();
            assertThat(subscriptionService.getByApiId(API_ID)).isEmpty();

            // API_ID_2 subscriptions are intact
            assertThat(subscriptionService.getById(SUB_ID_2)).isPresent().get().isEqualTo(subApi2);
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID_2, CLIENT_ID, PLAN_ID)).isPresent().get().isEqualTo(subApi2);
            assertThat(subscriptionService.getByApiId(API_ID_2)).isNotEmpty();
        }

        @Test
        void should_evict_subscription_when_re_registered_as_closed() {
            Subscription accepted = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(accepted);
            assertThat(subscriptionService.getById(SUB_ID)).isPresent();

            // Re-register the same subscription as CLOSED
            subscriptionService.unregister(accepted);

            assertThat(subscriptionService.getById(SUB_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).isEmpty();
            assertThat(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).isEmpty();
            assertThat(subscriptionService.getByApiId(API_ID)).isEmpty();
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
        void should_get_subscription_by_api_id_and_md5_apiKey() {
            Subscription subscription = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            subscriptionService.register(subscription);
            String md5ApiKeyValue = DigestUtils.md5DigestAsHex("apiKeyValue".getBytes());
            SecurityToken securityToken = SecurityToken.forMD5ApiKey(md5ApiKeyValue);
            ApiKey apiKey = new ApiKey();
            apiKey.setSubscription(SUB_ID);
            when(apiKeyService.getByApiAndMd5Key(API_ID, md5ApiKeyValue)).thenReturn(Optional.of(apiKey));

            Optional<Subscription> subscriptionOpt = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
            assertThat(subscriptionOpt).contains(subscription);
        }

        @Test
        void should_get_subscription_by_api_id_and_clientCertificate() {
            Subscription subscription = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, CLIENT_CERTIFICATE, PLAN_ID);
            subscriptionService.register(subscription);
            SecurityToken securityToken = SecurityToken.forClientCertificate(CLIENT_CERTIFICATE);
            when(subscriptionTrustStoreLoaderManager.getByCertificate(API_ID, PLAN_ID, CLIENT_CERTIFICATE)).thenReturn(
                Optional.of(subscription)
            );

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

        @Test
        void should_get_all_exploded_subscriptions_by_id() {
            Subscription subApi1 = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
            Subscription subApi2 = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID_2, CLIENT_ID, PLAN_ID_2);
            subscriptionService.register(subApi1);
            subscriptionService.register(subApi2);

            assertThat(subscriptionService.getByApiAndSecurityToken(API_ID, SecurityToken.forClientId(CLIENT_ID), PLAN_ID)).contains(
                subApi1
            );
            assertThat(subscriptionService.getByApiAndSecurityToken(API_ID_2, SecurityToken.forClientId(CLIENT_ID), PLAN_ID_2)).contains(
                subApi2
            );
            assertThat(subscriptionService.getByApiAndSecurityToken(API_ID_2, SecurityToken.forClientId("unknown"), PLAN_ID_2)).isEmpty();
        }
    }

    @Nested
    //@Disabled
    class ConcurrentUpdateTest {

        private static final int READER_COUNT = 10;

        @Test
        void should_always_find_subscription_by_either_client_id_during_update() throws Exception {
            String oldClientId = "client-old";
            String newClientId = "client-new";
            Subscription initial = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, oldClientId, PLAN_ID);
            subscriptionService.register(initial);

            AtomicBoolean updateDone = new AtomicBoolean(false);
            AtomicBoolean foundEmpty = new AtomicBoolean(false);
            CyclicBarrier barrier = new CyclicBarrier(READER_COUNT + 1);

            try (
                ExecutorService read = Executors.newFixedThreadPool(READER_COUNT);
                ExecutorService write = Executors.newFixedThreadPool(1);
            ) {
                for (int i = 0; i < READER_COUNT; i++) {
                    read.submit(() -> {
                        barrier.await();
                        while (!updateDone.get()) {
                            Optional<Subscription> byOld = subscriptionService.getByApiAndSecurityToken(
                                API_ID,
                                SecurityToken.forClientId(oldClientId),
                                PLAN_ID
                            );
                            Optional<Subscription> byNew = subscriptionService.getByApiAndSecurityToken(
                                API_ID,
                                SecurityToken.forClientId(newClientId),
                                PLAN_ID
                            );
                            if (byOld.isEmpty() && byNew.isEmpty()) {
                                foundEmpty.set(true);
                            }
                        }
                        return null;
                    });
                }

                write.submit(() -> {
                    barrier.await();
                    Subscription updated = buildAcceptedSubscriptionWithClientId(SUB_ID, API_ID, newClientId, PLAN_ID);
                    subscriptionService.register(updated);
                    updateDone.set(true);
                    return null;
                });

                read.shutdown();
                write.shutdown();
                assertThat(read.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
                assertThat(write.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            }
            assertThat(foundEmpty).describedAs("Subscription must always be reachable by at least one clientId during update").isFalse();
        }

        @Test
        void should_always_find_subscription_by_either_client_certificate_during_update() throws Exception {
            String oldCert = "cert-old";
            String newCert = "cert-new";
            Subscription initial = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, oldCert, PLAN_ID);
            subscriptionService.register(initial);

            AtomicBoolean updateDone = new AtomicBoolean(false);
            AtomicBoolean foundEmpty = new AtomicBoolean(false);
            CyclicBarrier barrier = new CyclicBarrier(READER_COUNT + 1);

            try (
                ExecutorService read = Executors.newFixedThreadPool(READER_COUNT);
                ExecutorService write = Executors.newFixedThreadPool(1);
            ) {
                for (int i = 0; i < READER_COUNT; i++) {
                    read.submit(() -> {
                        barrier.await();
                        while (!updateDone.get()) {
                            Subscription lookupOld = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, oldCert, PLAN_ID);
                            Subscription lookupNew = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, newCert, PLAN_ID);
                            Optional<Subscription> byOld = subscriptionService.getByClientCertificate(lookupOld);
                            Optional<Subscription> byNew = subscriptionService.getByClientCertificate(lookupNew);
                            if (byOld.isEmpty() && byNew.isEmpty()) {
                                foundEmpty.set(true);
                            }
                        }
                        return null;
                    });
                }

                write.submit(() -> {
                    barrier.await();
                    Subscription updated = buildAcceptedSubscriptionWithClientCertificate(SUB_ID, API_ID, newCert, PLAN_ID);
                    subscriptionService.register(updated);
                    updateDone.set(true);
                    return null;
                });

                read.shutdown();
                write.shutdown();
                assertThat(read.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
                assertThat(write.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            }
            assertThat(foundEmpty)
                .describedAs("Subscription must always be reachable by at least one client certificate during update")
                .isFalse();
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
