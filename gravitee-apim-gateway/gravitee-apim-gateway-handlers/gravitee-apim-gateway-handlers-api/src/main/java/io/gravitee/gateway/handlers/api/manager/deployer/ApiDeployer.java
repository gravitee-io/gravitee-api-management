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
package io.gravitee.gateway.handlers.api.manager.deployer;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactive.handlers.api.v4.deployer.AbstractApiDeployer;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeployer extends AbstractApiDeployer<Api> {

    private final Logger logger = LoggerFactory.getLogger(ApiDeployer.class);
    private final DataEncryptor dataEncryptor;

    public ApiDeployer(final GatewayConfiguration gatewayConfiguration, final DataEncryptor dataEncryptor) {
        super(gatewayConfiguration, dataEncryptor);
        this.dataEncryptor = dataEncryptor;
    }

    @Override
    public void initialize(final Api api) {
        // Filter plans according to the sharding tags and the plan status
        io.gravitee.definition.model.Api apiDefinition = api.getDefinition();

        // Keep the plan filtering for io.gravitee.gateway.services.localregistry.LocalApiDefinitionRegistry
        if (apiDefinition.getPlans() != null) {
            apiDefinition.setPlans(filterPlan(api));
        } else {
            apiDefinition.setPlans(List.of());
        }

        if (apiDefinition.getProperties() != null) {
            decryptProperties(apiDefinition.getProperties());
        }
    }

    @Override
    public List<String> getPlans(final Api api) {
        return api.getDefinition().getPlans().stream().map(Plan::getName).collect(Collectors.toList());
    }

    private List<Plan> filterPlan(final Api api) {
        return api
            .getDefinition()
            .getPlans()
            .stream()
            .filter(plan -> filterPlanStatus(plan.getStatus()))
            .filter(plan -> filterShardingTag(plan.getName(), plan.getId(), plan.getTags()))
            .collect(Collectors.toList());
    }

    private void decryptProperties(final Properties properties) {
        for (Property property : properties.getProperties()) {
            if (property.isEncrypted()) {
                try {
                    property.setValue(dataEncryptor.decrypt(property.getValue()));
                    property.setEncrypted(false);
                    properties.getValues().put(property.getKey(), property.getValue());
                } catch (GeneralSecurityException e) {
                    logger.error("Error decrypting API property value for key {}", property.getKey(), e);
                }
            }
        }
    }
}
