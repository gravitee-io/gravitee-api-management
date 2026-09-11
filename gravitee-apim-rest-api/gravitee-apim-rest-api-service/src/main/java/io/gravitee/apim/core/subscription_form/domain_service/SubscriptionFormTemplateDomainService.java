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
package io.gravitee.apim.core.subscription_form.domain_service;

import io.gravitee.apim.core.DomainService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Holds the GMD definition every subscription form starts from: what the environment default form is seeded
 * with, and what the Console pre-fills when a form is created.
 *
 * @author Gravitee.io Team
 */
@DomainService
public class SubscriptionFormTemplateDomainService {

    private static final String TEMPLATE_PATH = "templates/default-subscription-form.md";

    public String gmdContent() {
        try (var is = getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Could not load default subscription form template: " + TEMPLATE_PATH);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load default subscription form template", e);
        }
    }
}
