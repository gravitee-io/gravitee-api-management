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
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.gravitee_markdown.exception.GraviteeMarkdownContentEmptyException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateSubscriptionFormUseCaseTest {

    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final GraviteeMarkdownValidator gmdValidator = new GraviteeMarkdownValidator();
    private UpdateSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        useCase = new UpdateSubscriptionFormUseCase(crudService, queryService, gmdValidator);
    }

    @Test
    void should_update_existing_form() {
        // Given
        SubscriptionForm existingForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(existingForm));
        queryService.initWith(List.of(existingForm));

        // When
        var result = useCase.execute(
            new UpdateSubscriptionFormUseCase.Input(existingForm.getEnvironmentId(), existingForm.getId(), "<gmd-input name=\"updated\"/>")
        );

        // Then
        assertThat(result.subscriptionForm().getGmdContent()).isEqualTo("<gmd-input name=\"updated\"/>");
        assertThat(result.subscriptionForm().getId()).isEqualTo(existingForm.getId());
    }

    @Test
    void should_throw_exception_when_form_not_exists() {
        var input = new UpdateSubscriptionFormUseCase.Input(
            "env-1",
            SubscriptionFormId.of("550e8400-e29b-41d4-a716-446655440000"),
            "<gmd-input name=\"test\"/>"
        );
        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
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
