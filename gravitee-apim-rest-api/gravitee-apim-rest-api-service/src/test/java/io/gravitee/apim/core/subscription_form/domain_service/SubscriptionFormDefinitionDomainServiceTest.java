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

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.SubscriptionFormFixtures;
import fixtures.core.model.SubscriptionFormSchemaFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormApiAlreadyMappedException;
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
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private SubscriptionFormDefinitionDomainService service;

    @BeforeEach
    void setUp() {
        queryService.reset();
        apiCrudService.reset();
        service = new SubscriptionFormDefinitionDomainService(
            new GraviteeMarkdownValidator(),
            schemaGenerator,
            queryService,
            apiCrudService
        );
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

    @Nested
    class ValidateApiIds {

        @Test
        void should_accept_no_apis() {
            assertThat(service.validateApiIds(SubscriptionFormFixtures.ENVIRONMENT_ID, null, null)).isEmpty();
            assertThat(service.validateApiIds(SubscriptionFormFixtures.ENVIRONMENT_ID, List.of(), null)).isEmpty();
            assertThat(service.validateApiIds(SubscriptionFormFixtures.ENVIRONMENT_ID, List.of(" ", ""), null)).isEmpty();
        }

        @Test
        void should_return_the_distinct_apis_of_the_environment() {
            apiCrudService.initWith(List.of(anApi("api-1"), anApi("api-2")));

            var result = service.validateApiIds(SubscriptionFormFixtures.ENVIRONMENT_ID, List.of("api-1", " api-2 ", "api-1"), null);

            assertThat(result).containsExactly("api-1", "api-2");
        }

        @Test
        void should_reject_an_api_that_does_not_exist() {
            apiCrudService.initWith(List.of(anApi("api-1")));

            assertThatThrownBy(() -> service.validateApiIds(SubscriptionFormFixtures.ENVIRONMENT_ID, List.of("api-1", "ghost"), null))
                .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
                .hasMessageContaining("ghost");
        }

        @Test
        void should_reject_an_api_of_another_environment() {
            apiCrudService.initWith(List.of(anApi("api-1").toBuilder().environmentId("other-env").build()));

            assertThatThrownBy(() -> service.validateApiIds(SubscriptionFormFixtures.ENVIRONMENT_ID, List.of("api-1"), null)).isInstanceOf(
                SubscriptionFormDefinitionValidationException.class
            );
        }

        @Test
        void should_reject_an_api_already_mapped_to_another_form() {
            apiCrudService.initWith(List.of(anApi("api-1")));
            queryService.initWith(
                List.of(SubscriptionFormFixtures.aSubscriptionFormBuilder().name("Partners").apiIds(List.of("api-1")).build())
            );

            assertThatThrownBy(() ->
                service.validateApiIds(SubscriptionFormFixtures.ENVIRONMENT_ID, List.of("api-1"), SubscriptionFormId.random())
            )
                .isInstanceOf(SubscriptionFormApiAlreadyMappedException.class)
                .hasMessageContaining("Partners");
        }

        @Test
        void should_accept_an_api_already_mapped_to_the_form_being_updated() {
            apiCrudService.initWith(List.of(anApi("api-1")));
            var form = SubscriptionFormFixtures.aSubscriptionFormBuilder().apiIds(List.of("api-1")).build();
            queryService.initWith(List.of(form));

            assertThat(service.validateApiIds(form.getEnvironmentId(), List.of("api-1"), form.getId())).containsExactly("api-1");
        }

        private static io.gravitee.apim.core.api.model.Api anApi(String id) {
            return ApiFixtures.aProxyApiV4().toBuilder().id(id).environmentId(SubscriptionFormFixtures.ENVIRONMENT_ID).build();
        }
    }
}
