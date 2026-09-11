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
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormDefinitionValidationException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNameAlreadyExistsException;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateSubscriptionFormUseCaseTest {

    private static final String ENVIRONMENT_ID = SubscriptionFormFixtures.ENVIRONMENT_ID;
    private static final String GMD = "<gmd-input name=\"company\" fieldKey=\"company\" required=\"true\"/>";

    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private CreateSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        var definitionDomainService = new SubscriptionFormDefinitionDomainService(
            new GraviteeMarkdownValidator(),
            new SubscriptionFormSchemaGeneratorImpl(),
            queryService
        );
        useCase = new CreateSubscriptionFormUseCase(crudService, definitionDomainService);
    }

    @Test
    void should_create_a_disabled_non_default_form_with_derived_constraints() {
        var result = useCase.execute(new CreateSubscriptionFormUseCase.Input(ENVIRONMENT_ID, " Partners ", GMD));

        var created = result.subscriptionForm();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(created.getName()).isEqualTo("Partners");
        assertThat(created.getGmdContent()).isEqualTo(GraviteeMarkdown.of(GMD));
        assertThat(created.isEnabled()).isFalse();
        assertThat(created.isDefaultForm()).isFalse();
        assertThat(created.getValidationConstraints().byFieldKey()).containsOnlyKeys("company");
        assertThat(crudService.storage()).containsExactly(created);
    }

    @Test
    void should_throw_when_a_form_with_the_same_name_exists_in_the_environment() {
        queryService.initWith(List.of(SubscriptionFormFixtures.aSubscriptionFormBuilder().name("Partners").build()));

        var input = new CreateSubscriptionFormUseCase.Input(ENVIRONMENT_ID, "partners", GMD);

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNameAlreadyExistsException.class);
        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_allow_the_same_name_in_another_environment() {
        queryService.initWith(List.of(SubscriptionFormFixtures.aSubscriptionFormBuilder().name("Partners").build()));

        var result = useCase.execute(new CreateSubscriptionFormUseCase.Input("other-env", "Partners", GMD));

        assertThat(result.subscriptionForm().getEnvironmentId()).isEqualTo("other-env");
    }

    @Test
    void should_throw_when_name_is_blank() {
        var input = new CreateSubscriptionFormUseCase.Input(ENVIRONMENT_ID, "   ", GMD);

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(SubscriptionFormDefinitionValidationException.class)
            .extracting(e -> ((SubscriptionFormDefinitionValidationException) e).getErrors())
            .satisfies(errors -> assertThat(errors).containsExactly("Subscription form name must not be empty"));
    }

    @Test
    void should_throw_when_content_is_empty() {
        var input = new CreateSubscriptionFormUseCase.Input(ENVIRONMENT_ID, "Partners", " ");

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(GraviteeMarkdownContentEmptyException.class);
        assertThat(crudService.storage()).isEmpty();
    }

    @Test
    void should_throw_definition_validation_exception_when_schema_generation_fails() {
        var input = new CreateSubscriptionFormUseCase.Input(ENVIRONMENT_ID, "Partners", "<gmd-input required=\"true\"/>");

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormDefinitionValidationException.class);
        assertThat(crudService.storage()).isEmpty();
    }
}
