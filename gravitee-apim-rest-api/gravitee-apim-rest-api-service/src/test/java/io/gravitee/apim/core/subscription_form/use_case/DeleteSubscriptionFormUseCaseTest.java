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
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormIsDefaultException;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteSubscriptionFormUseCaseTest {

    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private DeleteSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        useCase = new DeleteSubscriptionFormUseCase(crudService, queryService);
    }

    @Test
    void should_delete_a_non_default_form() {
        var defaultForm = SubscriptionFormFixtures.aSubscriptionForm();
        var partnerForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .id(SubscriptionFormId.random())
            .name("Partners")
            .defaultForm(false)
            .build();
        crudService.initWith(List.of(defaultForm, partnerForm));
        queryService.initWith(List.of(defaultForm, partnerForm));

        useCase.execute(new DeleteSubscriptionFormUseCase.Input(SubscriptionFormFixtures.ENVIRONMENT_ID, partnerForm.getId()));

        assertThat(crudService.storage()).extracting(SubscriptionForm::getId).containsExactly(defaultForm.getId());
    }

    @Test
    void should_refuse_to_delete_the_default_form() {
        var defaultForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(defaultForm));
        queryService.initWith(List.of(defaultForm));

        var input = new DeleteSubscriptionFormUseCase.Input(SubscriptionFormFixtures.ENVIRONMENT_ID, defaultForm.getId());

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormIsDefaultException.class);
        assertThat(crudService.storage()).hasSize(1);
    }

    @Test
    void should_throw_when_the_form_does_not_exist_in_the_environment() {
        var form = SubscriptionFormFixtures.aSubscriptionFormBuilder().defaultForm(false).build();
        queryService.initWith(List.of(form));

        var input = new DeleteSubscriptionFormUseCase.Input("other-env", form.getId());

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
    }
}
