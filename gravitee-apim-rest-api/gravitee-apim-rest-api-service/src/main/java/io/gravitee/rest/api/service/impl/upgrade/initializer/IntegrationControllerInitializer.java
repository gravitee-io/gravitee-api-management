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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import io.gravitee.exchange.api.configuration.IdentifyConfiguration;
import io.gravitee.exchange.api.controller.ExchangeController;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.initializer.Initializer;
import io.gravitee.node.api.license.LicenseManager;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IntegrationControllerInitializer implements Initializer {

    private final ExchangeController integrationExchangeController;
    private final IdentifyConfiguration identifyConfiguration;

    public IntegrationControllerInitializer(
        @Qualifier("integrationExchangeController") Optional<ExchangeController> integrationExchangeController,
        @Qualifier("integrationIdentifyConfiguration") Optional<IdentifyConfiguration> identifyConfiguration
    ) {
        this.integrationExchangeController = integrationExchangeController.orElse(null);
        this.identifyConfiguration = identifyConfiguration.orElse(null);
    }

    @Override
    public boolean initialize() {
        if (shouldStart()) {
            try {
                integrationExchangeController.start();
                log.info("Integrations started.");
            } catch (Exception e) {
                log.error("Fail to start Integration Controller", e);
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    private boolean shouldStart() {
        return identifyConfiguration != null && identifyConfiguration.getProperty("enabled", Boolean.class, false);
    }

    @Override
    public int getOrder() {
        return InitializerOrder.INTEGRATION_CONTROLLER_INITIALIZER;
    }
}
