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
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DisableSubscriptionFormUseCaseTest {

    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private DisableSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        useCase = new DisableSubscriptionFormUseCase(crudService, queryService);
    }

    @Test
    void should_disable_enabled_form() {
        // Given
        SubscriptionForm enabledForm = SubscriptionFormFixtures.anEnabledSubscriptionForm();
        crudService.initWith(List.of(enabledForm));
        queryService.initWith(List.of(enabledForm));

        // When
        var result = useCase.execute(new DisableSubscriptionFormUseCase.Input(enabledForm.getEnvironmentId(), enabledForm.getId()));

        // Then
        assertThat(result.subscriptionForm().isEnabled()).isFalse();
    }

    @Test
    void should_be_idempotent_when_already_disabled() {
        // Given
        SubscriptionForm disabledForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(disabledForm));
        queryService.initWith(List.of(disabledForm));

        // When
        var result = useCase.execute(new DisableSubscriptionFormUseCase.Input(disabledForm.getEnvironmentId(), disabledForm.getId()));

        // Then
        assertThat(result.subscriptionForm().isEnabled()).isFalse();
    }

    @Test
    void should_throw_exception_when_form_not_found() {
        var input = new DisableSubscriptionFormUseCase.Input(
            "unknown-environment",
            SubscriptionFormId.of("550e8400-e29b-41d4-a716-446655440000")
        );
        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
    }
}
