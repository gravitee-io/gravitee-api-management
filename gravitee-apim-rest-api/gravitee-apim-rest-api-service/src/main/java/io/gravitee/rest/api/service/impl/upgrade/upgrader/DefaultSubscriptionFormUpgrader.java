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

import io.gravitee.apim.core.subscription_form.use_case.CreateDefaultSubscriptionFormUseCase;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
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

    private final EnvironmentRepository environmentRepository;
    private final CreateDefaultSubscriptionFormUseCase createDefaultSubscriptionFormUseCase;

    public DefaultSubscriptionFormUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        CreateDefaultSubscriptionFormUseCase createDefaultSubscriptionFormUseCase
    ) {
        this.environmentRepository = environmentRepository;
        this.createDefaultSubscriptionFormUseCase = createDefaultSubscriptionFormUseCase;
    }

    @Override
    public String version() {
        return "v2";
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        for (final var environment : environmentRepository.findAll()) {
            createDefaultSubscriptionFormUseCase.execute(environment.getId());
        }
        return true;
    }

    @Override
    public int getOrder() {
        return ENVIRONMENTS_DEFAULT_SUBSCRIPTION_FORM_UPGRADER;
    }
}
