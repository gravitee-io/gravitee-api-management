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
package io.gravitee.apim.infra.query_service.subscription_form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormQueryServiceImplTest {

    @Mock
    SubscriptionFormRepository repository;

    SubscriptionFormQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionFormQueryServiceImpl(repository);
    }

    @Nested
    class FindByEnvironmentId {

        @Test
        void should_return_subscription_form_when_found() throws TechnicalException {
            var repoForm = SubscriptionForm.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .environmentId("environment-id")
                .gmdContent("<gmd-input name=\"company\" label=\"Company\" required=\"true\"/>")
                .enabled(true)
                .build();

            when(repository.findByEnvironmentId("environment-id")).thenReturn(Optional.of(repoForm));

            var result = service.findDefaultForEnvironmentId("environment-id");

            assertThat(result).isPresent();
            assertThat(result.get().getId().toString()).hasToString("550e8400-e29b-41d4-a716-446655440000");
            assertThat(result.get().getEnvironmentId()).isEqualTo("environment-id");
            assertThat(result.get().getGmdContent()).isEqualTo("<gmd-input name=\"company\" label=\"Company\" required=\"true\"/>");
            assertThat(result.get().isEnabled()).isTrue();
        }

        @Test
        void should_return_empty_when_form_not_found() throws TechnicalException {
            when(repository.findByEnvironmentId("environment-id")).thenReturn(Optional.empty());

            var result = service.findDefaultForEnvironmentId("environment-id");

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            when(repository.findByEnvironmentId("environment-id")).thenThrow(new TechnicalException("Database error"));

            assertThatThrownBy(() -> service.findDefaultForEnvironmentId("environment-id"))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to find a SubscriptionForm for environment: environment-id")
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }
}
