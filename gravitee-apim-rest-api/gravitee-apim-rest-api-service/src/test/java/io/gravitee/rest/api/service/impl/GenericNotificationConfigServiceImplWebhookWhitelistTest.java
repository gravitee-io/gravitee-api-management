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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.exceptions.MessageUrlForbiddenException;
import io.gravitee.rest.api.service.exceptions.NotificationConfigNotFoundException;
import io.gravitee.rest.api.service.exceptions.NotifierDisabledException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Calls the service as the application context wires it, so the {@code notifiers.webhook.*} settings reach it the
 * way they do in production.
 */
@SpringJUnitConfig(GenericNotificationConfigServiceImplWebhookWhitelistTest.TestConfig.class)
@TestPropertySource(properties = "notifiers.webhook.whitelist[0]=" + GenericNotificationConfigServiceImplWebhookWhitelistTest.ALLOWED)
class GenericNotificationConfigServiceImplWebhookWhitelistTest {

    static final String ALLOWED = "https://allowed.example.com/hooks";
    private static final String FORBIDDEN_URL = "https://forbidden.example.com/hooks/not-allowed";

    @Configuration
    @Import(GenericNotificationConfigServiceImpl.class)
    @ComponentScan(
        basePackages = "io.gravitee.rest.api.service.notifiers",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.WebhookNotifierPolicy")
    )
    static class TestConfig {

        @Bean
        GenericNotificationConfigRepository genericNotificationConfigRepository() {
            return mock(GenericNotificationConfigRepository.class);
        }
    }

    @Autowired
    private GenericNotificationConfigService service;

    @Autowired
    private GenericNotificationConfigRepository genericNotificationConfigRepository;

    @BeforeEach
    void setUp() throws Exception {
        givenARepositoryThatStoresWhatItIsGiven(genericNotificationConfigRepository);
    }

    private static void givenARepositoryThatStoresWhatItIsGiven(GenericNotificationConfigRepository repository) throws Exception {
        reset(repository);
        lenient()
            .when(repository.create(any()))
            .thenAnswer(i -> i.getArgument(0));
        lenient()
            .when(repository.update(any()))
            .thenAnswer(i -> i.getArgument(0));
    }

    private static GenericNotificationConfigEntity webhook(String url) {
        return GenericNotificationConfigEntity.builder()
            .id("config-id")
            .name("webhook")
            .referenceType("API")
            .referenceId("api-id")
            .notifier("default-webhook")
            .config(url)
            .hooks(List.of("API_STARTED"))
            .organizationId("DEFAULT")
            .build();
    }

    @Test
    void should_reject_api_webhook_outside_of_the_whitelist() throws Exception {
        assertThatThrownBy(() -> service.create(webhook(FORBIDDEN_URL))).isInstanceOf(MessageUrlForbiddenException.class);
        verify(genericNotificationConfigRepository, never()).create(any());
    }

    @Test
    void should_reject_api_webhook_updated_to_a_url_outside_of_the_whitelist() throws Exception {
        givenAnExistingApiWebhook();

        assertThatThrownBy(() -> service.update(webhook(FORBIDDEN_URL))).isInstanceOf(MessageUrlForbiddenException.class);
        verify(genericNotificationConfigRepository, never()).update(any());
    }

    @Test
    void should_reject_a_host_that_only_shares_the_whitelisted_prefix() {
        assertThatThrownBy(() -> service.create(webhook(ALLOWED + "-evil/x"))).isInstanceOf(MessageUrlForbiddenException.class);
    }

    @Test
    void should_accept_api_webhook_inside_the_whitelist() {
        GenericNotificationConfigEntity created = service.create(webhook(ALLOWED + "/notify"));

        assertThat(created.getConfig()).isEqualTo(ALLOWED + "/notify");
    }

    @Test
    void should_accept_an_allowed_update() throws Exception {
        givenAnExistingApiWebhook();

        GenericNotificationConfigEntity updated = service.update(webhook(ALLOWED + "/notify"));

        assertThat(updated.getConfig()).isEqualTo(ALLOWED + "/notify");
    }

    @Test
    void should_accept_an_update_that_keeps_a_stored_webhook_url_no_longer_whitelisted() throws Exception {
        givenAnExistingApiWebhook(storedWebhook(FORBIDDEN_URL));
        GenericNotificationConfigEntity hooksOnlyUpdate = webhook(FORBIDDEN_URL);
        hooksOnlyUpdate.setHooks(List.of("API_STOPPED"));

        GenericNotificationConfigEntity updated = service.update(hooksOnlyUpdate);

        assertThat(updated.getHooks()).containsExactly("API_STOPPED");
    }

    @Test
    void should_reject_switching_a_stored_notifier_to_a_webhook_outside_of_the_whitelist() throws Exception {
        GenericNotificationConfig storedEmail = storedWebhook(FORBIDDEN_URL);
        storedEmail.setNotifier("default-email");
        givenAnExistingApiWebhook(storedEmail);

        assertThatThrownBy(() -> service.update(webhook(FORBIDDEN_URL))).isInstanceOf(MessageUrlForbiddenException.class);
        verify(genericNotificationConfigRepository, never()).update(any());
    }

    @Test
    void should_report_a_webhook_of_another_reference_as_not_found_before_checking_its_url() throws Exception {
        givenAnExistingApiWebhook();
        GenericNotificationConfigEntity otherReference = webhook(FORBIDDEN_URL);
        otherReference.setReferenceId("other-api-id");

        assertThatThrownBy(() -> service.update(otherReference)).isInstanceOf(NotificationConfigNotFoundException.class);
        verify(genericNotificationConfigRepository, never()).update(any());
    }

    @Test
    void should_not_apply_the_whitelist_to_email_notifiers() {
        GenericNotificationConfigEntity email = webhook("someone@example.com");
        email.setNotifier("default-email");

        GenericNotificationConfigEntity created = service.create(email);

        assertThat(created.getConfig()).isEqualTo("someone@example.com");
    }

    private void givenAnExistingApiWebhook() throws Exception {
        givenAnExistingApiWebhook(storedWebhook(ALLOWED));
    }

    private void givenAnExistingApiWebhook(GenericNotificationConfig stored) throws Exception {
        lenient().when(genericNotificationConfigRepository.findById("config-id")).thenReturn(Optional.of(stored));
    }

    private static GenericNotificationConfig storedWebhook(String url) {
        GenericNotificationConfig stored = new GenericNotificationConfig();
        stored.setId("config-id");
        stored.setReferenceType(NotificationReferenceType.API);
        stored.setReferenceId("api-id");
        stored.setNotifier("default-webhook");
        stored.setConfig(url);
        return stored;
    }

    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
    @SpringJUnitConfig(TestConfig.class)
    class WithAnEmptyWhitelist {

        @Autowired
        private GenericNotificationConfigService service;

        @Autowired
        private GenericNotificationConfigRepository genericNotificationConfigRepository;

        @BeforeEach
        void setUp() throws Exception {
            givenARepositoryThatStoresWhatItIsGiven(genericNotificationConfigRepository);
        }

        @Test
        void should_accept_any_webhook_url() {
            GenericNotificationConfigEntity created = service.create(webhook(FORBIDDEN_URL));

            assertThat(created.getConfig()).isEqualTo(FORBIDDEN_URL);
        }
    }

    @Nested
    @NestedTestConfiguration(NestedTestConfiguration.EnclosingConfiguration.OVERRIDE)
    @SpringJUnitConfig(TestConfig.class)
    @TestPropertySource(properties = "notifiers.webhook.enabled=false")
    class WithWebhookNotifiersDisabled {

        @Autowired
        private GenericNotificationConfigService service;

        @Autowired
        private GenericNotificationConfigRepository genericNotificationConfigRepository;

        @BeforeEach
        void setUp() throws Exception {
            givenARepositoryThatStoresWhatItIsGiven(genericNotificationConfigRepository);
        }

        @Test
        void should_reject_api_webhook() throws Exception {
            assertThatThrownBy(() -> service.create(webhook(ALLOWED))).isInstanceOf(NotifierDisabledException.class);
            verify(genericNotificationConfigRepository, never()).create(any());
        }

        @Test
        void should_accept_an_update_that_keeps_the_stored_webhook() throws Exception {
            lenient().when(genericNotificationConfigRepository.findById("config-id")).thenReturn(Optional.of(storedWebhook(ALLOWED)));
            GenericNotificationConfigEntity hooksOnlyUpdate = webhook(ALLOWED);
            hooksOnlyUpdate.setHooks(List.of("API_STOPPED"));

            GenericNotificationConfigEntity updated = service.update(hooksOnlyUpdate);

            assertThat(updated.getHooks()).containsExactly("API_STOPPED");
        }

        @Test
        void should_reject_an_update_that_changes_the_webhook_url() throws Exception {
            lenient().when(genericNotificationConfigRepository.findById("config-id")).thenReturn(Optional.of(storedWebhook(ALLOWED)));

            assertThatThrownBy(() -> service.update(webhook(ALLOWED + "/other"))).isInstanceOf(NotifierDisabledException.class);
            verify(genericNotificationConfigRepository, never()).update(any());
        }
    }
}
