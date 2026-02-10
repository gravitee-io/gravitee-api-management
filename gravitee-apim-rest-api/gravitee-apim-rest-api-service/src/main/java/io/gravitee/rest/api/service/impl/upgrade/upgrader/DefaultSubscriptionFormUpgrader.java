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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.ENVIRONMENTS_DEFAULT_SUBSCRIPTION_FORM_UPGRADER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.SubscriptionFormRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.SubscriptionForm;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Upgrader that creates a default subscription form for each environment.
 * The form is disabled by default and contains a boilerplate template.
 *
 * @author Gravitee.io Team
 */
@Component
@CustomLog
public class DefaultSubscriptionFormUpgrader implements Upgrader {

    private String defaultFormContent;

    private final EnvironmentRepository environmentRepository;
    private final SubscriptionFormRepository subscriptionFormRepository;

    public DefaultSubscriptionFormUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy SubscriptionFormRepository subscriptionFormRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.subscriptionFormRepository = subscriptionFormRepository;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        var environments = environmentRepository.findAll();
        int created = 0;

        for (Environment environment : environments) {
            var existingForm = subscriptionFormRepository.findByEnvironmentId(environment.getId());

            if (existingForm.isEmpty()) {
                SubscriptionForm defaultForm = SubscriptionForm.builder()
                    .id(UuidString.generateRandom())
                    .environmentId(environment.getId())
                    .gmdContent(getDefaultFormContent())
                    .enabled(false) // Disabled by default
                    .build();

                subscriptionFormRepository.create(defaultForm);
                created++;
                log.info("Created default subscription form for environment [{}]", environment.getId());
            }
        }

        log.info("Default subscription form upgrader completed. Created {} forms for {} environments", created, environments.size());
        return true;
    }

    private String getDefaultFormContent() {
        if (defaultFormContent == null) {
            try {
                var resource = new ClassPathResource("templates/default-subscription-form.md");
                defaultFormContent = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new TechnicalManagementException("Failed to load default subscription form template", e);
            }
        }
        return defaultFormContent;
    }

    @Override
    public int getOrder() {
        return ENVIRONMENTS_DEFAULT_SUBSCRIPTION_FORM_UPGRADER;
    }
}
