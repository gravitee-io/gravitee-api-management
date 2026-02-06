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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.common.security.PKCS7Utils;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.server.DefaultServerManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.server.VertxServer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class SubscriptionTrustStoreLoaderManagerTest {

    private SubscriptionTrustStoreLoaderManager cut;
    private ListAppender<ILoggingEvent> listAppender;

    @Mock
    private TrustStoreLoaderManager trustStoreLoaderManager;

    @Mock
    private VertxServer server1;

    @Mock
    private VertxServer server2;

    @Mock
    private VertxServer server3;

    private DefaultServerManager serverManager;

    @BeforeEach
    void setUp() {
        // get Logback Logger
        Logger logger = (Logger) LoggerFactory.getLogger(SubscriptionTrustStoreLoaderManager.class);

        // create and start a ListAppender
        listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        // addAppender is outdated now
        logger.addAppender(listAppender);

        serverManager = new DefaultServerManager();
        lenient().when(server1.trustStoreLoaderManager()).thenReturn(trustStoreLoaderManager);
        lenient().when(server1.id()).thenReturn("server1");
        lenient().when(server2.trustStoreLoaderManager()).thenReturn(trustStoreLoaderManager);
        lenient().when(server2.id()).thenReturn("server2");
        lenient().when(server3.trustStoreLoaderManager()).thenReturn(trustStoreLoaderManager);
        lenient().when(server3.id()).thenReturn("server3");

        serverManager.register(server1);
        serverManager.register(server2);
        serverManager.register(server3);
        cut = new SubscriptionTrustStoreLoaderManager(serverManager);
    }

    @Test
    void should_register_subscription_on_all_servers() {
        cut.registerSubscription(Subscription.builder().id("subscriptionId").clientCertificate(BASE_64_CERTIFICATE_1).build(), Set.of());

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList)
            .hasSize(1)
            .element(0)
            .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
            .containsExactly("Registering TrustStoreLoader for subscription subscriptionId", Level.DEBUG);

        ArgumentCaptor<SubscriptionTrustStoreLoader> captor = ArgumentCaptor.forClass(SubscriptionTrustStoreLoader.class);
        verify(trustStoreLoaderManager, times(serverManager.servers().size())).registerLoader(captor.capture());
        // Verify there is only one loader instance shared accross servers
        assertThat(new HashSet<>(captor.getAllValues()))
            .hasSize(1)
            .first()
            .satisfies(loader -> assertThat(loader.id()).contains("subscriptionId"));
    }

    @Test
    void should_register_subscription_on_selected_servers() {
        cut.registerSubscription(
            Subscription.builder().id("subscriptionId").clientCertificate(BASE_64_CERTIFICATE_1).build(),
            Set.of("server1", "server3")
        );

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList)
            .hasSize(1)
            .element(0)
            .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
            .containsExactly("Registering TrustStoreLoader for subscription subscriptionId", Level.DEBUG);

        ArgumentCaptor<SubscriptionTrustStoreLoader> captor = ArgumentCaptor.forClass(SubscriptionTrustStoreLoader.class);
        verify(server1).trustStoreLoaderManager();
        verify(server2, never()).trustStoreLoaderManager();
        verify(server3).trustStoreLoaderManager();
        verify(trustStoreLoaderManager, times(2)).registerLoader(captor.capture());
        // Verify there is only one loader instance shared accross servers
        assertThat(new HashSet<>(captor.getAllValues()))
            .hasSize(1)
            .first()
            .satisfies(loader -> assertThat(loader.id()).contains("subscriptionId"));
    }

    @Test
    void should_not_register_already_registered_subscription() {
        cut.registerSubscription(Subscription.builder().id("subscriptionId").clientCertificate(BASE_64_CERTIFICATE_1).build(), Set.of());

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList)
            .hasSize(1)
            .element(0)
            .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
            .containsExactly("Registering TrustStoreLoader for subscription subscriptionId", Level.DEBUG);

        cut.registerSubscription(Subscription.builder().id("subscriptionId").clientCertificate(BASE_64_CERTIFICATE_1).build(), Set.of());
        assertThat(logList)
            .hasSize(2)
            .element(1)
            .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
            .containsExactly("A TrustStoreLoader for subscription subscriptionId is already registered", Level.DEBUG);
    }

    @Test
    void should_unregister_subscription() {
        final Subscription subscription = Subscription.builder().id("subscriptionId").clientCertificate(BASE_64_CERTIFICATE_1).build();
        cut.registerSubscription(subscription, Set.of());

        ArgumentCaptor<SubscriptionTrustStoreLoader> captor = ArgumentCaptor.forClass(SubscriptionTrustStoreLoader.class);
        verify(trustStoreLoaderManager, times(serverManager.servers().size())).registerLoader(captor.capture());
        final List<KeyStoreEvent> keyStoreEvents = new ArrayList<>();
        captor.getAllValues().forEach(loader -> loader.setEventHandler(keyStoreEvents::add));

        cut.unregisterSubscription(subscription);

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList)
            .hasSize(2)
            .element(1)
            .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
            .containsExactly("Stopping TrustStoreLoader for subscription subscriptionId", Level.DEBUG);

        assertThat(keyStoreEvents)
            .hasSize(1)
            .first()
            .satisfies(event -> {
                assertThat(event).isInstanceOf(KeyStoreEvent.UnloadEvent.class);
                assertThat(event.loaderId()).contains("subscriptionId");
            });
    }

    @Test
    void should_not_unregister_absent_subscription() {
        final Subscription subscription = Subscription.builder().id("subscriptionId").build();
        cut.unregisterSubscription(subscription);

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList).isEmpty();
    }

    @Test
    void should_get_a_subscription() {
        final Subscription subscription = Subscription.builder()
            .id("subscriptionId")
            .api("api")
            .plan("plan")
            .clientCertificate(BASE_64_CERTIFICATE_1)
            .build();
        cut.registerSubscription(subscription, Set.of("server1", "server3"));
        final Optional<Subscription> result = cut.getByCertificate("api", CERTIFICATE_DIGEST, "plan");
        assertThat(result).contains(subscription);
    }

    @Test
    void should_get_a_subscription_with_two_certs() {
        final Subscription subscription = Subscription.builder()
            .id("subscriptionId")
            .api("api")
            .plan("plan")
            .clientCertificate(Base64.getEncoder().encodeToString(PKCS7Utils.createBundle(List.of(PEM_CERTIFICATE_1, PEM_CERTIFICATE_2))))
            .build();
        cut.registerSubscription(subscription, Set.of("server1", "server3"));
        final Optional<Subscription> result = cut.getByCertificate("api", CERTIFICATE_DIGEST, "plan");
        assertThat(result).contains(subscription);
    }

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

    private static final String CERTIFICATE_DIGEST = "u21dNKud2YsKNJn3HQTTon1_qSoZi8IrBTsLiZCFQLg";
}
