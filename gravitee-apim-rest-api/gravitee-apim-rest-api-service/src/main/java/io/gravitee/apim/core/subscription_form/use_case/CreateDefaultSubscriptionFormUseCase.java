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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormConstraintsFactory;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSchemaGenerator;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import java.nio.charset.StandardCharsets;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Creates the default (disabled) subscription form for an environment when none exists.
 * Idempotent: no-op if a form is already present for the environment.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
@CustomLog
public class CreateDefaultSubscriptionFormUseCase {

    private static final String DEFAULT_FORM_TEMPLATE_PATH = "templates/default-subscription-form.md";

    private final SubscriptionFormCrudService subscriptionFormCrudService;
    private final SubscriptionFormQueryService subscriptionFormQueryService;
    private final SubscriptionFormSchemaGenerator schemaGenerator;

    public void execute(String environmentId) {
        if (subscriptionFormQueryService.findDefaultForEnvironmentId(environmentId).isPresent()) {
            return;
        }

        var gmd = GraviteeMarkdown.of(loadDefaultFormContent());
        var constraints = SubscriptionFormConstraintsFactory.fromSchema(schemaGenerator.generate(gmd));

        var defaultForm = SubscriptionForm.builder()
            .id(null)
            .environmentId(environmentId)
            .gmdContent(gmd)
            .enabled(false)
            .validationConstraints(constraints)
            .build();

        subscriptionFormCrudService.create(defaultForm);
        log.info("Created default subscription form for environment [{}]", environmentId);
    }

    private String loadDefaultFormContent() {
        try (final var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_FORM_TEMPLATE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Could not load default subscription form template: " + DEFAULT_FORM_TEMPLATE_PATH);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (e instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Could not load default subscription form template", e);
        }
    }
}
