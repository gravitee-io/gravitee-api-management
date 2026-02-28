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
package io.gravitee.gateway.security.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.Mockito.lenient;

import io.gravitee.common.security.PKCS7Utils;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.node.api.certificate.AbstractStoreLoaderOptions;
import io.gravitee.node.api.server.DefaultServerManager;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import io.gravitee.node.certificates.AbstractKeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.server.VertxServer;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class SubscriptionTrustStoreLoaderManagerTest {

    public static final String PEM_CERTIFICATE_1 = """
        -----BEGIN CERTIFICATE-----
        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
        cKhxaqcpye8zKJ8+IDsJC2+lMQ==
        -----END CERTIFICATE-----
        """;

    private static final String PEM_CERTIFICATE_2 = """
        -----BEGIN CERTIFICATE-----
        MIIDCTCCAfGgAwIBAgIUYh2NFpteTomLlWoO7O5HKI7fg10wDQYJKoZIhvcNAQEL
        BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTI2MDEyOTE0MTYyMVoXDTI3MDEy
        OTE0MTYyMVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF
        AAOCAQ8AMIIBCgKCAQEAwquFLM7wi+EBt5JL1Q6c/qzqickBKhlymOf1a18jYsWO
        UrRRqANMvEub5zks44qdYbdm7Kj9EruDD3hfrS6YemQhIMeT+SgY3MU5cY12yB1e
        fn+0bkEg6CJdIfgvcuccqY9pu0hgFdlgK9YXEXYZzb+ai7b4qPHN2BR6toBdjf56
        ReNygcrT0igPQC9P/MsmpFuJzD69i5Z8fJcLU2V7RwXW9MKyej8CN/zrcqftG+ck
        egt5cpB2OyIt6ajQBeXarGYvOlm975RKWOD5mHpt87GgcEvMEnhFTSs47iA7X91o
        01+rhFDfPxgvhur2AG6oiB7/zS4lqHWQHRTBwurVJQIDAQABo1MwUTAdBgNVHQ4E
        FgQUHP3c1CNhl6RZHn3g/HkbSssfPhMwHwYDVR0jBBgwFoAUHP3c1CNhl6RZHn3g
        /HkbSssfPhMwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAgTa/
        lgY6fEbt88yQcLxQDseam2lox0sK6sYOwpIQV7/RiXbeM6KrXmCy59HBrJMjSpZY
        LEp9RPDf8Awg50iv6oFXXn+ZJ5Cmq2WXMpCvKxAjQWnmNs99SGfXyQsiLMxe3HlL
        CKqM8O7LZrVdOxWbNW/0ZMJl4d4vCf0LhVrbfMGLeQfqtKVmygjJM1rycKiFazM4
        cTHphvWA9/5XRFC+yD3V3ZTFE9LDeMoSF0soigR0NnCqFc5E7S9OQvuB5h5eBu9s
        T1dLBVOY8zcIYu6LDjeMr6MpiZyk+/O7ewue2kQURmDBuVrwiSSQF4AhsmbMDUuB
        iyr26LfMYtituDZy7w==
        -----END CERTIFICATE-----
        """;

    private static final String BASE_64_CERTIFICATE_1 = Base64.getEncoder().encodeToString(PEM_CERTIFICATE_1.getBytes());
    private static final String BASE_64_CERTIFICATE_2 = Base64.getEncoder().encodeToString(PEM_CERTIFICATE_2.getBytes());

    private static final String CERTIFICATE_1_DIGEST = "u21dNKud2YsKNJn3HQTTon1_qSoZi8IrBTsLiZCFQLg";
    private static final String CERTIFICATE_1_DIGEST_HEX = "bb6d5d34ab9dd98b0a3499f71d04d3a27d6a4a8662f08ac14ec2e26421502e";
    private static final String CERTIFICATE_2_DIGEST = "wS8z98uyY4COjon55dsZ4xl1NDh4Hf_mkr3p0mRLv6E";
    private static final String CERTIFICATE_2_DIGEST_HEX = "c12f33f7cbb263808e8e89f9e5db19e319753438781df9a4af7a749912efe8";

    TrustStoreLoaderManager trustStoreLoaderManager1 = new TrustStoreLoaderManager("server1", new NoOpKeyStoreLoader(new NoOpOptions()));
    TrustStoreLoaderManager trustStoreLoaderManager2 = new TrustStoreLoaderManager("server2", new NoOpKeyStoreLoader(new NoOpOptions()));
    TrustStoreLoaderManager trustStoreLoaderManager3 = new TrustStoreLoaderManager("server3", new NoOpKeyStoreLoader(new NoOpOptions()));

    @Mock
    VertxServer server1;

    @Mock
    VertxServer server2;

    @Mock
    VertxServer server3;

    DefaultServerManager serverManager;

    SubscriptionTrustStoreLoaderManager cut;

    @BeforeEach
    void setUp() throws Exception {
        serverManager = new DefaultServerManager();
        lenient().when(server1.trustStoreLoaderManager()).thenReturn(trustStoreLoaderManager1);
        lenient().when(server1.id()).thenReturn("server1");
        lenient().when(server2.trustStoreLoaderManager()).thenReturn(trustStoreLoaderManager2);
        lenient().when(server2.id()).thenReturn("server2");
        lenient().when(server3.trustStoreLoaderManager()).thenReturn(trustStoreLoaderManager3);
        lenient().when(server3.id()).thenReturn("server3");

        trustStoreLoaderManager1.start();
        trustStoreLoaderManager2.start();
        trustStoreLoaderManager3.start();

        serverManager.register(server1);
        serverManager.register(server2);
        serverManager.register(server3);

        cut = new SubscriptionTrustStoreLoaderManager(serverManager);
    }

    @Test
    void should_get_a_subscription_from_cache_without_plan() {
        final Subscription subscription = Subscription.builder()
            .id("subscriptionId")
            .api("api")
            .clientCertificate(BASE_64_CERTIFICATE_1)
            .build();

        cut.registerSubscription(subscription, Set.of());

        assertThat(cut.getByCertificate("api", null, CERTIFICATE_1_DIGEST)).contains(subscription);

        // unregister
        cut.unregisterSubscription(subscription);

        assertThat(cut.getByCertificate("api", null, CERTIFICATE_1_DIGEST)).isEmpty();
    }

    @Test
    void should_get_a_subscription_from_cache_without_plan_several_certs() {
        final Subscription subscription = Subscription.builder().id("subscriptionId").api("api").clientCertificate(getPKCS7()).build();

        cut.registerSubscription(subscription, Set.of());

        assertThat(cut.getByCertificate("api", null, CERTIFICATE_1_DIGEST)).contains(subscription);
        assertThat(cut.getByCertificate("api", null, CERTIFICATE_2_DIGEST)).contains(subscription);

        // unregister
        cut.unregisterSubscription(subscription);

        assertThat(cut.getByCertificate("api", null, CERTIFICATE_1_DIGEST)).isEmpty();
        assertThat(cut.getByCertificate("api", null, CERTIFICATE_2_DIGEST)).isEmpty();
    }

    @Test
    void should_get_subscription_from_single_and_multiple_certs() {
        final Subscription subscriptionCert1 = Subscription.builder()
            .id("subscriptionId")
            .api("api")
            .plan("plan")
            .clientCertificate(BASE_64_CERTIFICATE_1)
            .build();

        cut.registerSubscription(subscriptionCert1, Set.of());

        assertAllServers(1);

        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST)).contains(subscriptionCert1);

        final Subscription updatedCert1And2 = Subscription.builder()
            .id("subscriptionId")
            .api("api")
            .plan("plan")
            .clientCertificate(getPKCS7())
            .build();
        cut.registerSubscription(updatedCert1And2, Set.of());

        assertAllServers(2);

        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST)).contains(updatedCert1And2);
        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_2_DIGEST)).contains(updatedCert1And2);

        final Subscription updatedCert2 = Subscription.builder()
            .id("subscriptionId")
            .api("api")
            .plan("plan")
            .clientCertificate(BASE_64_CERTIFICATE_2)
            .build();
        cut.registerSubscription(updatedCert2, Set.of());

        assertAllServers(1);

        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST)).isEmpty();
        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_2_DIGEST)).contains(updatedCert2);

        // unregister
        cut.unregisterSubscription(updatedCert2);

        assertAllServers(0);

        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST)).isEmpty();
        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_2_DIGEST)).isEmpty();
    }

    @Test
    void should_register_subscription_on_all_servers() {
        final Subscription subscription = Subscription.builder()
            .id("subscriptionId")
            .api("api")
            .plan("plan")
            .clientCertificate(BASE_64_CERTIFICATE_1)
            .build();
        cut.registerSubscription(subscription, Set.of());
        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST)).contains(subscription);

        assertAllServers(1);

        // unregister
        cut.unregisterSubscription(subscription);

        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST)).isEmpty();

        assertAllServers(0);
    }

    @Test
    void should_register_subscription_on_selected_servers() {
        final Subscription subscription = Subscription.builder()
            .id("subscriptionId")
            .api("api")
            .plan("plan")
            .clientCertificate(BASE_64_CERTIFICATE_1)
            .build();
        cut.registerSubscription(subscription, Set.of("server1", "server3"));
        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST)).contains(subscription);

        assertThat(truststoreAliases(trustStoreLoaderManager1)).hasSize(1);
        assertThat(truststoreAliases(trustStoreLoaderManager2)).isEmpty();
        assertThat(truststoreAliases(trustStoreLoaderManager3)).hasSize(1);

        // unregister
        cut.unregisterSubscription(subscription);

        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST)).isEmpty();

        assertThat(truststoreAliases(trustStoreLoaderManager1)).isEmpty();
        assertThat(truststoreAliases(trustStoreLoaderManager3)).isEmpty();
    }

    @Test
    void should_not_register_already_registered_subscription() {
        cut.registerSubscription(
            Subscription.builder().id("subscriptionId").api("apiId").clientCertificate(BASE_64_CERTIFICATE_1).build(),
            Set.of()
        );
        assertAllServers(1);

        cut.registerSubscription(Subscription.builder().id("subscriptionId").clientCertificate(BASE_64_CERTIFICATE_1).build(), Set.of());
        assertAllServers(1);
    }

    @Test
    void should_register_subscription_with_multiple_certs_on_all_servers() {
        final Subscription subscription = Subscription.builder()
            .id("subscriptionId")
            .api("api")
            .plan("plan")
            .clientCertificate(getPKCS7())
            .build();
        cut.registerSubscription(subscription, Set.of());
        final Optional<Subscription> result1 = cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST);
        assertThat(result1).contains(subscription);
        final Optional<Subscription> result2 = cut.getByCertificate("api", "plan", CERTIFICATE_2_DIGEST);
        assertThat(result2).contains(subscription);
        assertThat(result1).get().isEqualTo(result2.get());

        assertAllServers(2);

        // unregister
        cut.unregisterSubscription(subscription);

        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_1_DIGEST)).isEmpty();
        assertThat(cut.getByCertificate("api", "plan", CERTIFICATE_2_DIGEST)).isEmpty();

        assertAllServers(0);
    }

    @Test
    void should_unregister_subscription_with_multiple_certs() {
        final Subscription subscription = Subscription.builder()
            .id("subscriptionId")
            .plan("planId")
            .api("apiId")
            .clientCertificate(getPKCS7())
            .build();
        cut.registerSubscription(subscription, Set.of());

        assertAllServers(2);

        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_1_DIGEST)).isPresent();
        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_2_DIGEST)).isPresent();

        cut.unregisterSubscription(subscription);

        assertAllServers(0);

        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_1_DIGEST)).isEmpty();
        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_2_DIGEST)).isEmpty();
    }

    @Test
    void should_register_subscription_with_multiple_certs_and_remove_one() {
        cut.registerSubscription(
            Subscription.builder().id("subscriptionId").api("apiId").plan("planId").clientCertificate(getPKCS7()).build(),
            Set.of()
        );

        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_1_DIGEST)).isPresent();
        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_2_DIGEST)).isPresent();

        assertAllServers(2);
        // cert 2 is no longer there... no new registration
        cut.registerSubscription(
            Subscription.builder().id("subscriptionId").api("apiId").plan("planId").clientCertificate(BASE_64_CERTIFICATE_1).build(),
            Set.of()
        );

        assertAllServers(1);

        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_1_DIGEST)).isPresent();
        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_2_DIGEST)).isEmpty();
    }

    @Test
    void should_register_subscription_one_certs_replace_it() {
        cut.registerSubscription(
            Subscription.builder().id("subscriptionId").api("apiId").plan("planId").clientCertificate(BASE_64_CERTIFICATE_1).build(),
            Set.of()
        );

        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_1_DIGEST)).isPresent();
        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_2_DIGEST)).isEmpty();

        assertAllServers(1);
        assertThat(truststoreAliases(trustStoreLoaderManager1)).first().asInstanceOf(STRING).contains(CERTIFICATE_1_DIGEST_HEX);

        // Verify there is only one loader instance shared across servers
        cut.registerSubscription(
            Subscription.builder().id("subscriptionId").api("apiId").plan("planId").clientCertificate(BASE_64_CERTIFICATE_2).build(),
            Set.of()
        );

        assertThat(truststoreAliases(trustStoreLoaderManager1)).first().asInstanceOf(STRING).contains(CERTIFICATE_2_DIGEST_HEX);

        assertAllServers(1);

        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_1_DIGEST)).isEmpty();
        assertThat(cut.getByCertificate("apiId", "planId", CERTIFICATE_2_DIGEST)).isPresent();
    }

    private void assertAllServers(int expected) {
        assertThat(truststoreAliases(trustStoreLoaderManager1)).hasSize(expected);
        assertThat(truststoreAliases(trustStoreLoaderManager2)).hasSize(expected);
        assertThat(truststoreAliases(trustStoreLoaderManager3)).hasSize(expected);
    }

    private static String getPKCS7() {
        return Base64.getEncoder().encodeToString(PKCS7Utils.createBundle(List.of(PEM_CERTIFICATE_1, PEM_CERTIFICATE_2)));
    }

    List<String> truststoreAliases(TrustStoreLoaderManager trustStoreLoaderManager) {
        try {
            var method = AbstractKeyStoreLoaderManager.class.getDeclaredMethod("aliases");
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> aliases = (List<String>) method.invoke(trustStoreLoaderManager);
            return aliases;
        } catch (Exception e) {
            throw new RuntimeException("Failed to access aliases method", e);
        }
    }

    @Getter
    @SuperBuilder
    @ToString
    static class NoOpOptions extends AbstractStoreLoaderOptions {

        protected NoOpOptions() {
            super(NoOpOptions.builder());
        }
    }

    static class NoOpKeyStoreLoader extends AbstractKeyStoreLoader<NoOpOptions> {

        protected NoOpKeyStoreLoader(NoOpOptions o) {
            super(o);
        }

        @Override
        public void start() {
            // no op
        }

        @Override
        public void stop() {
            // no op
        }

        @Override
        public String id() {
            return "test";
        }
    }
}
