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
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormDefaultDomainService;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SetDefaultSubscriptionFormUseCaseTest {

    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private SetDefaultSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        useCase = new SetDefaultSubscriptionFormUseCase(queryService, new SubscriptionFormDefaultDomainService(crudService, queryService));
    }

    @Test
    void should_promote_the_form_and_demote_the_previous_default() {
        var previousDefault = SubscriptionFormFixtures.aSubscriptionForm();
        var partnerForm = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .id(SubscriptionFormId.random())
            .name("Partners")
            .defaultForm(false)
            .build();
        crudService.initWith(List.of(previousDefault, partnerForm));
        queryService.initWith(List.of(previousDefault, partnerForm));

        var result = useCase.execute(
            new SetDefaultSubscriptionFormUseCase.Input(SubscriptionFormFixtures.ENVIRONMENT_ID, partnerForm.getId())
        );

        assertThat(result.subscriptionForm().isDefaultForm()).isTrue();
        assertThat(crudService.storage())
            .filteredOn(SubscriptionForm::isDefaultForm)
            .extracting(SubscriptionForm::getId)
            .containsExactly(partnerForm.getId());
        assertThat(previousDefault.isDefaultForm()).isFalse();
    }

    @Test
    void should_do_nothing_when_the_form_already_is_the_default() {
        var defaultForm = SubscriptionFormFixtures.aSubscriptionForm();
        crudService.initWith(List.of(defaultForm));
        queryService.initWith(List.of(defaultForm));

        var result = useCase.execute(
            new SetDefaultSubscriptionFormUseCase.Input(SubscriptionFormFixtures.ENVIRONMENT_ID, defaultForm.getId())
        );

        assertThat(result.subscriptionForm()).isSameAs(defaultForm);
        assertThat(defaultForm.isDefaultForm()).isTrue();
    }

    @Test
    void should_promote_the_form_when_the_environment_has_no_default_yet() {
        var partnerForm = SubscriptionFormFixtures.aSubscriptionFormBuilder().defaultForm(false).build();
        crudService.initWith(List.of(partnerForm));
        queryService.initWith(List.of(partnerForm));

        var result = useCase.execute(
            new SetDefaultSubscriptionFormUseCase.Input(SubscriptionFormFixtures.ENVIRONMENT_ID, partnerForm.getId())
        );

        assertThat(result.subscriptionForm().isDefaultForm()).isTrue();
    }

    @Test
    void should_throw_when_the_form_does_not_exist_in_the_environment() {
        var input = new SetDefaultSubscriptionFormUseCase.Input("other-env", SubscriptionFormId.random());

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
    }
}
