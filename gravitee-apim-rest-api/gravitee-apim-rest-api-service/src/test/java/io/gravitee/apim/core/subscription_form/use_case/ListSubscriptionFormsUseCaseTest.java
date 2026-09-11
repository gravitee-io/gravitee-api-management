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

import fixtures.core.model.SubscriptionFormFixtures;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListSubscriptionFormsUseCaseTest {

    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private ListSubscriptionFormsUseCase useCase;

    @BeforeEach
    void setUp() {
        queryService.reset();
        useCase = new ListSubscriptionFormsUseCase(queryService);
    }

    @Test
    void should_list_the_forms_of_the_environment_only() {
        var defaultForm = SubscriptionFormFixtures.aSubscriptionForm();
        var partnerForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .id(SubscriptionFormId.random())
            .name("Partners")
            .defaultForm(false)
            .build();
        var otherEnvironmentForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .id(SubscriptionFormId.random())
            .environmentId("other-env")
            .build();
        queryService.initWith(List.of(defaultForm, partnerForm, otherEnvironmentForm));

        var result = useCase.execute(new ListSubscriptionFormsUseCase.Input(SubscriptionFormFixtures.ENVIRONMENT_ID));

        assertThat(result.subscriptionForms()).containsExactly(defaultForm, partnerForm);
    }

    @Test
    void should_return_an_empty_list_when_the_environment_has_no_form() {
        var result = useCase.execute(new ListSubscriptionFormsUseCase.Input("unknown-env"));

        assertThat(result.subscriptionForms()).isEmpty();
    }
}
