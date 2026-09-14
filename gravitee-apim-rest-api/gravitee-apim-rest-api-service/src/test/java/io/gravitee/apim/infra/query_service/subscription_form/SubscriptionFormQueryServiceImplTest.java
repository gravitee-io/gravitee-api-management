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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.List;
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

    private static final String FORM_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String GMD = "<gmd-input name=\"company\" label=\"Company\" required=\"true\"/>";

    @Mock
    SubscriptionFormRepository repository;

    SubscriptionFormQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionFormQueryServiceImpl(repository);
    }

    @Nested
    class FindDefaultForEnvironmentId {

        @Test
        void should_return_the_default_form_of_the_environment() throws TechnicalException {
            when(repository.findDefaultByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.of(aRow()));

            var result = service.findDefaultForEnvironmentId(ENVIRONMENT_ID);

            assertThat(result).isPresent();
            var form = result.get();
            assertThat(form.getId()).hasToString(FORM_ID);
            assertThat(form.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
            assertThat(form.getName()).isEqualTo("Default");
            assertThat(form.isDefaultForm()).isTrue();
            assertThat(form.getGmdContent()).isEqualTo(GraviteeMarkdown.of(GMD));
            assertThat(form.isEnabled()).isTrue();
            assertThat(form.getValidationConstraints().byFieldKey()).containsOnlyKeys("company");
        }

        @Test
        void should_return_empty_when_form_not_found() throws TechnicalException {
            when(repository.findDefaultByEnvironmentId(ENVIRONMENT_ID)).thenReturn(Optional.empty());

            var result = service.findDefaultForEnvironmentId(ENVIRONMENT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            when(repository.findDefaultByEnvironmentId(ENVIRONMENT_ID)).thenThrow(new TechnicalException("Database error"));

            assertThatThrownBy(() -> service.findDefaultForEnvironmentId(ENVIRONMENT_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to find the default SubscriptionForm of environment: " + ENVIRONMENT_ID)
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }

    @Nested
    class FindAllByEnvironmentId {

        @Test
        void should_return_every_form_of_the_environment_with_its_definition() throws TechnicalException {
            var otherRow = aRow()
                .toBuilder()
                .id("0d0c2d1e-5f4a-4c3b-9a8e-7f6d5c4b3a21")
                .name("Partner onboarding")
                .defaultForm(false)
                .build();
            when(repository.findAllByEnvironmentId(ENVIRONMENT_ID)).thenReturn(List.of(aRow(), otherRow));

            var result = service.findAllByEnvironmentId(ENVIRONMENT_ID);

            assertThat(result)
                .extracting(form -> form.getName())
                .containsExactly("Default", "Partner onboarding");
            assertThat(result)
                .extracting(form -> form.getGmdContent())
                .containsOnly(GraviteeMarkdown.of(GMD));
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            when(repository.findAllByEnvironmentId(ENVIRONMENT_ID)).thenThrow(new TechnicalException("Database error"));

            assertThatThrownBy(() -> service.findAllByEnvironmentId(ENVIRONMENT_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to list the SubscriptionForms of environment: " + ENVIRONMENT_ID);
        }
    }

    @Nested
    class FindByIdAndEnvironmentId {

        @Test
        void should_return_the_form_with_its_definition() throws TechnicalException {
            when(repository.findByIdAndEnvironmentId(FORM_ID, ENVIRONMENT_ID)).thenReturn(Optional.of(aRow()));

            var result = service.findByIdAndEnvironmentId(ENVIRONMENT_ID, SubscriptionFormId.of(FORM_ID));

            assertThat(result).isPresent();
            assertThat(result.get().getId()).hasToString(FORM_ID);
            assertThat(result.get().getGmdContent()).isEqualTo(GraviteeMarkdown.of(GMD));
        }

        @Test
        void should_return_empty_when_form_not_found() throws TechnicalException {
            when(repository.findByIdAndEnvironmentId(FORM_ID, ENVIRONMENT_ID)).thenReturn(Optional.empty());

            var result = service.findByIdAndEnvironmentId(ENVIRONMENT_ID, SubscriptionFormId.of(FORM_ID));

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_technical_domain_exception_when_repository_throws_technical_exception() throws TechnicalException {
            when(repository.findByIdAndEnvironmentId(any(), any())).thenThrow(new TechnicalException("Database error"));

            assertThatThrownBy(() -> service.findByIdAndEnvironmentId(ENVIRONMENT_ID, SubscriptionFormId.of(FORM_ID)))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage(
                    "An error occurred while trying to find a SubscriptionForm with id: " + FORM_ID + " in environment: " + ENVIRONMENT_ID
                )
                .hasCauseInstanceOf(TechnicalException.class);
        }
    }

    private static SubscriptionForm aRow() {
        return SubscriptionForm.builder()
            .id(FORM_ID)
            .environmentId(ENVIRONMENT_ID)
            .name("Default")
            .defaultForm(true)
            .gmdContent(GMD)
            .enabled(true)
            .validationConstraints("{\"company\":[{\"type\":\"required\"}]}")
            .build();
    }
}
