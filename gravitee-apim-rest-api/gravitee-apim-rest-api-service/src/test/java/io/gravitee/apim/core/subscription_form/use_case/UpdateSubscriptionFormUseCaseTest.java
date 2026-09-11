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
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormDefinitionDomainService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSubmissionValidator;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormDefinitionValidationException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNameAlreadyExistsException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateSubscriptionFormUseCaseTest {

    private static final String GMD = "<gmd-input name=\"updated\" fieldKey=\"updated\"/>";

    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private UpdateSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        var definitionDomainService = new SubscriptionFormDefinitionDomainService(
            new GraviteeMarkdownValidator(),
            new SubscriptionFormSchemaGeneratorImpl(),
            queryService
        );
        useCase = new UpdateSubscriptionFormUseCase(crudService, queryService, definitionDomainService);
    }

    @Test
    void should_update_name_and_definition_of_existing_form() {
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(existingForm));
        queryService.initWith(List.of(existingForm));

        var result = useCase.execute(
            new UpdateSubscriptionFormUseCase.Input(existingForm.getEnvironmentId(), existingForm.getId(), "  Partners  ", GMD)
        );

        assertThat(result.subscriptionForm().getId()).isEqualTo(existingForm.getId());
        assertThat(result.subscriptionForm().getName()).isEqualTo("Partners");
        assertThat(result.subscriptionForm().getGmdContent()).isEqualTo(GraviteeMarkdown.of(GMD));
        assertThat(result.subscriptionForm().getValidationConstraints().byFieldKey()).containsKey("updated");
        assertThat(result.subscriptionForm().isDefaultForm()).isEqualTo(existingForm.isDefaultForm());
        assertThat(result.subscriptionForm().isEnabled()).isEqualTo(existingForm.isEnabled());
    }

    @Test
    void should_keep_the_current_name() {
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(existingForm));
        queryService.initWith(List.of(existingForm));

        var result = useCase.execute(
            new UpdateSubscriptionFormUseCase.Input(existingForm.getEnvironmentId(), existingForm.getId(), existingForm.getName(), GMD)
        );

        assertThat(result.subscriptionForm().getName()).isEqualTo(SubscriptionFormFixtures.FORM_NAME);
    }

    @Test
    void should_persist_empty_constraints_when_gmd_has_no_form_fields() {
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(existingForm));
        queryService.initWith(List.of(existingForm));

        var result = useCase.execute(
            new UpdateSubscriptionFormUseCase.Input(
                existingForm.getEnvironmentId(),
                existingForm.getId(),
                existingForm.getName(),
                "<p>Only static content</p>"
            )
        );

        assertThat(result.subscriptionForm().getValidationConstraints().isEmpty()).isTrue();
    }

    @Test
    void should_throw_exception_when_form_not_exists() {
        var input = new UpdateSubscriptionFormUseCase.Input(
            "env-1",
            SubscriptionFormId.of("550e8400-e29b-41d4-a716-446655440000"),
            "Any",
            "<gmd-input name=\"test\" fieldKey=\"test\"/>"
        );

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
    }

    @Test
    void should_throw_when_renaming_to_the_name_of_another_form() {
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        SubscriptionForm otherForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .id(SubscriptionFormId.random())
            .name("Partners")
            .defaultForm(false)
            .build();
        crudService.initWith(List.of(existingForm, otherForm));
        queryService.initWith(List.of(existingForm, otherForm));

        var input = new UpdateSubscriptionFormUseCase.Input(existingForm.getEnvironmentId(), existingForm.getId(), "partners", GMD);

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNameAlreadyExistsException.class);
        assertThat(crudService.storage()).extracting(SubscriptionForm::getGmdContent).doesNotContain(GraviteeMarkdown.of(GMD));
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
        var input = new UpdateSubscriptionFormUseCase.Input(
            existingForm.getEnvironmentId(),
            existingForm.getId(),
            existingForm.getName(),
            gmd.toString()
        );

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
            existingForm.getName(),
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
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(existingForm));
        var input = new UpdateSubscriptionFormUseCase.Input(
            existingForm.getEnvironmentId(),
            existingForm.getId(),
            existingForm.getName(),
            ""
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(GraviteeMarkdownContentEmptyException.class)
            .hasMessage("Content must not be null or empty");
    }
}
