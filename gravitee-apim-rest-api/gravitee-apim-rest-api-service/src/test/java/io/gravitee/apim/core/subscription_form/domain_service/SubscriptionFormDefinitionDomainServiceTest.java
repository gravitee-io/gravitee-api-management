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
package io.gravitee.apim.core.subscription_form.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.SubscriptionFormFixtures;
import fixtures.core.model.SubscriptionFormSchemaFixtures;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormDefinitionValidationException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNameAlreadyExistsException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionFormDefinitionDomainServiceTest {

    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final SubscriptionFormSchemaGenerator schemaGenerator = mock(SubscriptionFormSchemaGenerator.class);
    private SubscriptionFormDefinitionDomainService service;

    @BeforeEach
    void setUp() {
        queryService.reset();
        service = new SubscriptionFormDefinitionDomainService(new GraviteeMarkdownValidator(), schemaGenerator, queryService);
    }

    @Nested
    class Compile {

        @Test
        void should_derive_constraints_from_the_definition() {
            when(schemaGenerator.generate(any())).thenReturn(SubscriptionFormSchemaFixtures.schema(anInput("company", true)));

            var definition = service.compile("<gmd-input fieldKey=\"company\" required=\"true\"/>");

            assertThat(definition.gmdContent()).isEqualTo(GraviteeMarkdown.of("<gmd-input fieldKey=\"company\" required=\"true\"/>"));
            assertThat(definition.constraints().byFieldKey()).containsOnlyKeys("company");
        }

        @Test
        void should_reject_an_empty_definition() {
            assertThatThrownBy(() -> service.compile("  ")).isInstanceOf(GraviteeMarkdownContentEmptyException.class);
        }

        @Test
        void should_reject_a_definition_with_too_many_fields() {
            var fields = IntStream.rangeClosed(0, SubscriptionFormSubmissionValidator.MAX_METADATA_COUNT)
                .mapToObj(i -> anInput("field" + i, false))
                .toArray(SubscriptionFormSchema.Field[]::new);
            when(schemaGenerator.generate(any())).thenReturn(SubscriptionFormSchemaFixtures.schema(fields));

            assertThatThrownBy(() -> service.compile("<gmd-input fieldKey=\"too-many\"/>"))
                .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
                .hasMessageContaining("must not exceed");
        }
    }

    @Nested
    class ValidateName {

        @Test
        void should_accept_a_name_unused_in_the_environment() {
            queryService.initWith(List.of(SubscriptionFormFixtures.aSubscriptionForm()));

            assertThatCode(() ->
                service.validateName(SubscriptionFormFixtures.ENVIRONMENT_ID, "Partners", null)
            ).doesNotThrowAnyException();
        }

        @Test
        void should_accept_the_current_name_of_the_form_being_updated() {
            var form = SubscriptionFormFixtures.aSubscriptionForm();
            queryService.initWith(List.of(form));

            assertThatCode(() -> service.validateName(form.getEnvironmentId(), form.getName(), form.getId())).doesNotThrowAnyException();
        }

        @Test
        void should_reject_a_name_used_by_another_form_ignoring_case_and_spaces() {
            queryService.initWith(List.of(SubscriptionFormFixtures.aSubscriptionForm()));

            assertThatThrownBy(() ->
                service.validateName(SubscriptionFormFixtures.ENVIRONMENT_ID, " default ", SubscriptionFormId.random())
            ).isInstanceOf(SubscriptionFormNameAlreadyExistsException.class);
        }

        @Test
        void should_reject_a_name_longer_than_the_column() {
            var tooLong = "n".repeat(SubscriptionFormDefinitionDomainService.MAX_NAME_LENGTH + 1);

            assertThatThrownBy(() -> service.validateName(SubscriptionFormFixtures.ENVIRONMENT_ID, tooLong, null))
                .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
                .hasMessageContaining("255");
        }

        @Test
        void should_reject_a_blank_name() {
            assertThatThrownBy(() -> service.validateName(SubscriptionFormFixtures.ENVIRONMENT_ID, " ", null)).isInstanceOf(
                SubscriptionFormDefinitionValidationException.class
            );
        }
    }

    private static SubscriptionFormSchema.InputField anInput(String fieldKey, boolean required) {
        return new SubscriptionFormSchema.InputField(fieldKey, required, null, null, null, null);
    }
}
