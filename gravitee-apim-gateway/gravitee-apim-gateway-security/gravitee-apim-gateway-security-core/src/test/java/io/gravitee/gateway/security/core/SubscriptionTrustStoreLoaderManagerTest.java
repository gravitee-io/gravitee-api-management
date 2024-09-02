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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.server.DefaultServerManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.server.VertxServer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        cut.registerSubscription(Subscription.builder().id("subscriptionId").build(), Set.of());

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
        cut.registerSubscription(Subscription.builder().id("subscriptionId").build(), Set.of("server1", "server3"));

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
        cut.registerSubscription(Subscription.builder().id("subscriptionId").build(), Set.of());

        final List<ILoggingEvent> logList = listAppender.list;
        assertThat(logList)
            .hasSize(1)
            .element(0)
            .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
            .containsExactly("Registering TrustStoreLoader for subscription subscriptionId", Level.DEBUG);

        cut.registerSubscription(Subscription.builder().id("subscriptionId").build(), Set.of());
        assertThat(logList)
            .hasSize(2)
            .element(1)
            .extracting(ILoggingEvent::getFormattedMessage, ILoggingEvent::getLevel)
            .containsExactly("A TrustStoreLoader for subscription subscriptionId is already registered", Level.DEBUG);
    }

    @Test
    void should_unregister_subscription() {
        final Subscription subscription = Subscription.builder().id("subscriptionId").build();
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
        assertThat(logList).hasSize(0);
    }
}
