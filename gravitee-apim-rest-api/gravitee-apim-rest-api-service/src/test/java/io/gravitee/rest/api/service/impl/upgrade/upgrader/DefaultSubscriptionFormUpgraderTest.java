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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
import org.springframework.core.io.ClassPathResource;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultSubscriptionFormUpgraderTest {

    private static final Environment ANOTHER_ENVIRONMENT = Environment.builder()
        .id("ANOTHER_ENVIRONMENT")
        .hrids(List.of("another environment"))
        .name("another environment")
        .organizationId("DEFAULT")
        .build();

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    SubscriptionFormRepository subscriptionFormRepository;

    DefaultSubscriptionFormUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new DefaultSubscriptionFormUpgrader(environmentRepository, subscriptionFormRepository);
    }

    @Test
    void should_do_nothing_when_there_is_no_environment() throws TechnicalException, UpgraderException {
        when(environmentRepository.findAll()).thenReturn(Collections.emptySet());

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(subscriptionFormRepository);
    }

    @Test
    void should_throw_UpgraderException_when_repository_findAll_throws() throws TechnicalException {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("this is a test exception"));

        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }

    @Test
    void should_throw_UpgraderException_when_repository_create_throws() throws TechnicalException {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));
        when(subscriptionFormRepository.findByEnvironmentId("DEFAULT")).thenReturn(Optional.empty());
        when(subscriptionFormRepository.create(org.mockito.ArgumentMatchers.any())).thenThrow(new TechnicalException("create failed"));

        assertThatThrownBy(() -> upgrader.upgrade()).isInstanceOf(UpgraderException.class);
    }

    @Test
    void should_create_default_subscription_form_for_environments_without_form() throws Exception {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT, ANOTHER_ENVIRONMENT));
        when(subscriptionFormRepository.findByEnvironmentId("DEFAULT")).thenReturn(Optional.empty());
        when(subscriptionFormRepository.findByEnvironmentId("ANOTHER_ENVIRONMENT")).thenReturn(
            Optional.of(SubscriptionForm.builder().id("existing").environmentId("ANOTHER_ENVIRONMENT").build())
        );

        var resource = new ClassPathResource("templates/default-subscription-form.md");
        var defaultContent = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<SubscriptionForm> captor = ArgumentCaptor.forClass(SubscriptionForm.class);
        verify(subscriptionFormRepository, times(1)).create(captor.capture());

        var createdForm = captor.getValue();
        assertThat(createdForm.getEnvironmentId()).isEqualTo("DEFAULT");
        assertThat(createdForm.getGmdContent()).isEqualTo(defaultContent);
        assertThat(createdForm.isEnabled()).isFalse();
        assertThat(createdForm.getId()).isNotBlank();
    }
}
