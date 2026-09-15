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
import inmemory.SubscriptionFormElResolverInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetSubscriptionFormUseCaseTest {

    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final SubscriptionFormElResolverInMemory elResolver = new SubscriptionFormElResolverInMemory();
    private GetSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        queryService.reset();
        elResolver.reset();
        useCase = new GetSubscriptionFormUseCase(queryService, new SubscriptionFormSchemaGeneratorImpl(), elResolver);
    }

    @Test
    void should_return_the_form_whether_enabled_or_not() {
        var disabledForm = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(disabledForm));

        var result = useCase.execute(new GetSubscriptionFormUseCase.Input(disabledForm.getEnvironmentId(), disabledForm.getId()));

        assertThat(result.subscriptionForm()).isEqualTo(disabledForm);
        assertThat(result.resolvedOptions()).isEmpty();
    }

    @Test
    void should_throw_when_the_form_belongs_to_another_environment() {
        var form = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(form));

        var input = new GetSubscriptionFormUseCase.Input("other-env", form.getId());

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(SubscriptionFormNotFoundException.class)
            .hasMessageContaining(form.getId().toString());
    }

    @Test
    void should_throw_when_the_form_does_not_exist() {
        var input = new GetSubscriptionFormUseCase.Input(SubscriptionFormFixtures.ENVIRONMENT_ID, SubscriptionFormId.random());

        assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(SubscriptionFormNotFoundException.class);
    }

    @Test
    void should_return_fallback_options_when_no_resolved_options_configured() {
        var form = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .gmdContent(GraviteeMarkdown.of("<gmd-select fieldKey=\"env\" options=\"{#api.metadata['envs']}:Prod,Test\"/>"))
            .build();
        queryService.initWith(List.of(form));

        var result = useCase.execute(new GetSubscriptionFormUseCase.Input(form.getEnvironmentId(), form.getId()));

        assertThat(result.resolvedOptions()).containsEntry("env", List.of("Prod", "Test"));
    }
}
