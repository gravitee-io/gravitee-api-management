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
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetSubscriptionFormForEnvironmentUseCaseTest {

    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private GetSubscriptionFormForEnvironmentUseCase useCase;

    @BeforeEach
    void setUp() {
        queryService.reset();
        useCase = new GetSubscriptionFormForEnvironmentUseCase(queryService);
    }

    @Test
    void should_return_subscription_form_for_environment() {
        // Given
        SubscriptionForm expectedForm = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(expectedForm));

        // When
        var result = useCase.execute(new GetSubscriptionFormForEnvironmentUseCase.Input(expectedForm.getEnvironmentId(), false));

        // Then
        assertThat(result.subscriptionForm()).isEqualTo(expectedForm);
    }

    @Test
    void should_return_disabled_form_when_onlyEnabled_false() {
        SubscriptionForm disabledForm = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(disabledForm));

        var result = useCase.execute(new GetSubscriptionFormForEnvironmentUseCase.Input(disabledForm.getEnvironmentId(), false));

        assertThat(result.subscriptionForm()).isEqualTo(disabledForm);
    }

    @Test
    void should_throw_when_onlyEnabled_true_and_form_disabled() {
        SubscriptionForm disabledForm = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(disabledForm));

        var input = new GetSubscriptionFormForEnvironmentUseCase.Input(disabledForm.getEnvironmentId(), true);
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(SubscriptionFormNotFoundException.class)
            .hasMessageContaining(disabledForm.getEnvironmentId());
    }

    @Test
    void should_throw_exception_when_subscription_form_not_found() {
        var input = new GetSubscriptionFormForEnvironmentUseCase.Input("unknown-environment", false);
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(SubscriptionFormNotFoundException.class)
            .hasMessageContaining("unknown-environment");
    }
}
