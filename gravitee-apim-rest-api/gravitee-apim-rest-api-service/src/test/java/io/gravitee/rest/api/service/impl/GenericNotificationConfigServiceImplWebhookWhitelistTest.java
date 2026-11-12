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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.service.exceptions.MessageUrlForbiddenException;
import io.gravitee.rest.api.service.exceptions.NotifierDisabledException;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierPolicy;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class GenericNotificationConfigServiceImplWebhookWhitelistTest {

    private static final String ALLOWED = "https://allowed.example.com/hooks";
    private static final String FORBIDDEN_URL = "https://forbidden.example.com/hooks/not-allowed";

    @Mock
    private GenericNotificationConfigRepository genericNotificationConfigRepository;

    private GenericNotificationConfigServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        lenient()
            .when(genericNotificationConfigRepository.create(any()))
            .thenAnswer(i -> i.getArgument(0));
        lenient()
            .when(genericNotificationConfigRepository.update(any()))
            .thenAnswer(i -> i.getArgument(0));
    }

    private void givenEnvironment(MockEnvironment environment) {
        service = new GenericNotificationConfigServiceImpl();
        service.genericNotificationConfigRepository = genericNotificationConfigRepository;
        service.webhookNotifierPolicy = new WebhookNotifierPolicy(environment);
    }

    private static MockEnvironment whitelisting(String url) {
        return new MockEnvironment().withProperty("notifiers.webhook.whitelist[0]", url);
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
        givenEnvironment(whitelisting(ALLOWED));

        assertThatThrownBy(() -> service.create(webhook(FORBIDDEN_URL))).isInstanceOf(MessageUrlForbiddenException.class);
        verify(genericNotificationConfigRepository, never()).create(any());
    }

    @Test
    void should_reject_api_webhook_updated_to_a_url_outside_of_the_whitelist() throws Exception {
        givenEnvironment(whitelisting(ALLOWED));

        assertThatThrownBy(() -> service.update(webhook(FORBIDDEN_URL))).isInstanceOf(MessageUrlForbiddenException.class);
        verify(genericNotificationConfigRepository, never()).update(any());
    }

    @Test
    void should_reject_a_host_that_only_shares_the_whitelisted_prefix() {
        givenEnvironment(whitelisting(ALLOWED));

        assertThatThrownBy(() -> service.create(webhook(ALLOWED + "-evil/x"))).isInstanceOf(MessageUrlForbiddenException.class);
    }

    @Test
    void should_accept_api_webhook_inside_the_whitelist() {
        givenEnvironment(whitelisting(ALLOWED));

        GenericNotificationConfigEntity created = service.create(webhook(ALLOWED + "/notify"));

        assertThat(created.getConfig()).isEqualTo(ALLOWED + "/notify");
    }

    @Test
    void should_accept_any_webhook_url_when_the_whitelist_is_empty() {
        givenEnvironment(new MockEnvironment());

        GenericNotificationConfigEntity created = service.create(webhook(FORBIDDEN_URL));

        assertThat(created.getConfig()).isEqualTo(FORBIDDEN_URL);
    }

    @Test
    void should_accept_an_allowed_update() throws Exception {
        givenEnvironment(whitelisting(ALLOWED));
        GenericNotificationConfig existing = new GenericNotificationConfig();
        existing.setId("config-id");
        existing.setReferenceType(NotificationReferenceType.API);
        existing.setReferenceId("api-id");
        lenient().when(genericNotificationConfigRepository.findById("config-id")).thenReturn(Optional.of(existing));

        GenericNotificationConfigEntity updated = service.update(webhook(ALLOWED + "/notify"));

        assertThat(updated.getConfig()).isEqualTo(ALLOWED + "/notify");
    }

    @Test
    void should_reject_api_webhook_when_webhook_notifiers_are_disabled() {
        givenEnvironment(new MockEnvironment().withProperty("notifiers.webhook.enabled", "false"));

        assertThatThrownBy(() -> service.create(webhook(ALLOWED))).isInstanceOf(NotifierDisabledException.class);
    }

    @Test
    void should_not_apply_the_whitelist_to_email_notifiers() {
        givenEnvironment(whitelisting(ALLOWED));
        GenericNotificationConfigEntity email = webhook("someone@example.com");
        email.setNotifier("default-email");

        GenericNotificationConfigEntity created = service.create(email);

        assertThat(created.getConfig()).isEqualTo("someone@example.com");
    }
}
