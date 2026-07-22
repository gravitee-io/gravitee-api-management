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
import inmemory.SubscriptionFormCrudServiceInMemory;
import inmemory.SubscriptionFormQueryServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormConstraintsFactory;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.infra.domain_service.subscription_form.SubscriptionFormSchemaGeneratorImpl;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateDefaultSubscriptionFormUseCaseTest {

    private static final String ENVIRONMENT_ID = "environment-id";

    private final SubscriptionFormCrudServiceInMemory crudService = new SubscriptionFormCrudServiceInMemory();
    private final SubscriptionFormQueryServiceInMemory queryService = new SubscriptionFormQueryServiceInMemory();
    private final SubscriptionFormSchemaGeneratorImpl schemaGenerator = new SubscriptionFormSchemaGeneratorImpl();
    private CreateDefaultSubscriptionFormUseCase useCase;

    @BeforeEach
    void setUp() {
        crudService.reset();
        queryService.reset();
        useCase = new CreateDefaultSubscriptionFormUseCase(crudService, queryService, schemaGenerator);
    }

    @Test
    void should_create_default_subscription_form_when_missing() throws Exception {
        var defaultContent = new ClassPathResource("templates/default-subscription-form.md").getContentAsString(StandardCharsets.UTF_8);
        var expectedConstraints = SubscriptionFormConstraintsFactory.fromSchema(
            schemaGenerator.generate(GraviteeMarkdown.of(defaultContent))
        );

        useCase.execute(ENVIRONMENT_ID);

        assertThat(crudService.storage())
            .hasSize(1)
            .first()
            .satisfies(created -> {
                assertThat(created.getId()).isNotNull();
                assertThat(created.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
                assertThat(created.getGmdContent().value()).isEqualTo(defaultContent);
                assertThat(created.isEnabled()).isFalse();
                assertThat(created.getValidationConstraints().isEmpty()).isFalse();
                assertThat(created.getValidationConstraints()).isEqualTo(expectedConstraints);
            });
    }

    @Test
    void should_do_nothing_when_form_already_exists() {
        SubscriptionForm existing = SubscriptionFormFixtures.aSubscriptionForm();
        queryService.initWith(List.of(existing));

        useCase.execute(ENVIRONMENT_ID);

        assertThat(crudService.storage()).isEmpty();
    }
}
