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
package io.gravitee.apim.core.subscription_form.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.SubscriptionFormCrudServiceInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSchemaGenerator;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSubmissionValidator;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormDefinitionValidationException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormValidationException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateSubscriptionFormUseCaseTest {

    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final GraviteeMarkdownValidator gmdValidator = new GraviteeMarkdownValidator();
    private final SubscriptionFormSchemaGenerator schemaGenerator = new SubscriptionFormSchemaGeneratorImpl();

    private UpdateSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        useCase = new UpdateSubscriptionFormUseCase(crudService, queryService, gmdValidator, schemaGenerator);
    }

    @Test
    void should_update_existing_form() {
        // Given
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(existingForm));
        queryService.initWith(List.of(existingForm));

        // When
        var result = useCase.execute(
            new UpdateSubscriptionFormUseCase.Input(
                existingForm.getEnvironmentId(),
                existingForm.getId(),
                "<gmd-input name=\"updated\" fieldKey=\"updated\"/>"
            )
        );

        // Then
        assertThat(result.subscriptionForm().getGmdContent()).isEqualTo(
            GraviteeMarkdown.of("<gmd-input name=\"updated\" fieldKey=\"updated\"/>")
        );
        assertThat(result.subscriptionForm().getId()).isEqualTo(existingForm.getId());
        assertThat(result.subscriptionForm().getValidationConstraints()).isNotNull();
        assertThat(result.subscriptionForm().getValidationConstraints().byFieldKey()).containsKey("updated");
    }

    @Test
    void should_persist_empty_constraints_when_gmd_has_no_form_fields() {
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(existingForm));
        queryService.initWith(List.of(existingForm));

        var result = useCase.execute(
            new UpdateSubscriptionFormUseCase.Input(existingForm.getEnvironmentId(), existingForm.getId(), "<p>Only static content</p>")
        );

        assertThat(result.subscriptionForm().getValidationConstraints()).isNotNull();
        assertThat(result.subscriptionForm().getValidationConstraints().isEmpty()).isTrue();
    }

    @Test
    void should_throw_exception_when_form_not_exists() {
        var input = new UpdateSubscriptionFormUseCase.Input(
            "env-1",
            SubscriptionFormId.of("550e8400-e29b-41d4-a716-446655440000"),
            "<gmd-input name=\"test\" fieldKey=\"test\"/>"
        );
        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
    }

    @Test
    void should_throw_when_form_exceeds_max_field_count() {
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(existingForm));
        queryService.initWith(List.of(existingForm));

        int tooMany = SubscriptionFormSubmissionValidator.MAX_METADATA_COUNT + 1;
        StringBuilder gmd = new StringBuilder();
        for (int i = 0; i < tooMany; i++) {
            gmd.append("<gmd-input fieldKey=\"field").append(i).append("\"/>");
        }

        var input = new UpdateSubscriptionFormUseCase.Input(existingForm.getEnvironmentId(), existingForm.getId(), gmd.toString());

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
            .extracting(e -> ((SubscriptionFormDefinitionValidationException) e).getErrors())
            .satisfies(errors ->
                assertThat(errors).containsExactly(
                    "Subscription form must not exceed " + SubscriptionFormSubmissionValidator.MAX_METADATA_COUNT + " fields"
                )
            );
    }

    @Test
    void should_throw_definition_validation_exception_when_schema_generation_fails() {
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(existingForm));
        queryService.initWith(List.of(existingForm));

        var input = new UpdateSubscriptionFormUseCase.Input(
            existingForm.getEnvironmentId(),
            existingForm.getId(),
            "<gmd-input required=\"true\"/>"
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
            .extracting(e -> ((SubscriptionFormDefinitionValidationException) e).getErrors())
            .satisfies(errors ->
                assertThat(errors).containsExactly("GMD form field is missing required 'fieldkey' attribute for element: gmd-input")
            );
    }

    @Test
    void should_throw_when_content_is_empty() {
        // Given
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(existingForm));

        var input = new UpdateSubscriptionFormUseCase.Input(existingForm.getEnvironmentId(), existingForm.getId(), "");

        // When & Then
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(GraviteeMarkdownContentEmptyException.class)
            .hasMessage("Content must not be null or empty");
    }
}
