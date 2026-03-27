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
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetSubscriptionFormForEnvironmentUseCaseTest {

    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final SubscriptionFormElResolverInMemory elResolver = new SubscriptionFormElResolverInMemory();
    private final SubscriptionFormSchemaGeneratorImpl schemaGenerator = new SubscriptionFormSchemaGeneratorImpl();
    private GetSubscriptionFormForEnvironmentUseCase useCase;

    @BeforeEach
    void setUp() {
        queryService.reset();
        elResolver.reset();
        useCase = new GetSubscriptionFormForEnvironmentUseCase(queryService, schemaGenerator, elResolver);
    }

    @Test
    void should_return_subscription_form_for_environment() {
        // Given
        SubscriptionForm expectedForm = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(expectedForm));

        // When
        var result = useCase.execute(
            GetSubscriptionFormForEnvironmentUseCase.Input.builder()
                .environmentId(expectedForm.getEnvironmentId())
                .onlyEnabled(false)
                .build()
        );

        // Then
        assertThat(result.subscriptionForm()).isEqualTo(expectedForm);
        assertThat(result.resolvedOptions()).isEmpty();
    }

    @Test
    void should_return_disabled_form_when_onlyEnabled_false() {
        SubscriptionForm disabledForm = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(disabledForm));

        var result = useCase.execute(
            GetSubscriptionFormForEnvironmentUseCase.Input.builder()
                .environmentId(disabledForm.getEnvironmentId())
                .onlyEnabled(false)
                .build()
        );

        assertThat(result.subscriptionForm()).isEqualTo(disabledForm);
    }

    @Test
    void should_throw_when_onlyEnabled_true_and_form_disabled() {
        SubscriptionForm disabledForm = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(disabledForm));

        var input = GetSubscriptionFormForEnvironmentUseCase.Input.builder()
            .environmentId(disabledForm.getEnvironmentId())
            .onlyEnabled(true)
            .build();
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(SubscriptionFormNotFoundException.class)
            .hasMessageContaining(disabledForm.getEnvironmentId());
    }

    @Test
    void should_throw_exception_when_subscription_form_not_found() {
        var input = GetSubscriptionFormForEnvironmentUseCase.Input.builder()
            .environmentId("unknown-environment")
            .onlyEnabled(false)
            .build();
        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(SubscriptionFormNotFoundException.class)
            .hasMessageContaining("unknown-environment");
    }

    @Test
    void should_return_resolved_options_when_apiId_is_present() {
        // Given a form with an EL select field
        var form = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .gmdContent(
                io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown.of(
                    "<gmd-select fieldKey=\"env\" options=\"${api.metadata['envs']}:Prod,Test\"/>"
                )
            )
            .build();
        queryService.initWith(List.of(form));
        elResolver.withResolved(Map.of("api.metadata['envs']", List.of("Dev", "Staging", "Prod")));

        // When retrieving with apiId
        var result = useCase.execute(
            GetSubscriptionFormForEnvironmentUseCase.Input.builder()
                .environmentId(form.getEnvironmentId())
                .onlyEnabled(false)
                .apiId("my-api-id")
                .build()
        );

        // Then resolved options are returned
        assertThat(result.resolvedOptions()).containsEntry("env", List.of("Dev", "Staging", "Prod"));
    }

    @Test
    void should_return_fallback_options_when_no_resolved_options_configured() {
        // Given a form with an EL select field
        var form = SubscriptionFormFixtures.aSubscriptionFormBuilder()
            .gmdContent(
                io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown.of(
                    "<gmd-select fieldKey=\"env\" options=\"${api.metadata['envs']}:Prod,Test\"/>"
                )
            )
            .build();
        queryService.initWith(List.of(form));

        // When retrieving without apiId
        var result = useCase.execute(
            GetSubscriptionFormForEnvironmentUseCase.Input.builder().environmentId(form.getEnvironmentId()).onlyEnabled(false).build()
        );

        // Then fallback options from the expression are returned
        assertThat(result.resolvedOptions()).containsEntry("env", List.of("Prod", "Test"));
    }
}
