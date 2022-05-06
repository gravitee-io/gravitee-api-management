/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.api.manager;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiDeploymentPreProcessor {

    public static final Logger logger = LoggerFactory.getLogger(ApiDeploymentPreProcessor.class);

    private final DataEncryptor dataEncryptor;
    private final GatewayConfiguration gatewayConfiguration;

    public ApiDeploymentPreProcessor(DataEncryptor dataEncryptor, GatewayConfiguration gatewayConfiguration) {
        this.dataEncryptor = dataEncryptor;
        this.gatewayConfiguration = gatewayConfiguration;
    }

    public void prepareApi(Api api) {
        api.setPlans(getPlansMatchingShardingTag(api));
        decryptProperties(api.getProperties());
    }

    private List<Plan> getPlansMatchingShardingTag(io.gravitee.gateway.handlers.api.definition.Api api) {
        return api
            .getPlans()
            .stream()
            .filter(
                plan -> {
                    if (plan.getTags() != null && !plan.getTags().isEmpty()) {
                        boolean hasMatchingTags = gatewayConfiguration.hasMatchingTags(plan.getTags());
                        if (!hasMatchingTags) {
                            logger.debug(
                                "Plan name[{}] api[{}] has been ignored because not in configured sharding tags",
                                plan.getName(),
                                api.getName()
                            );
                        }
                        return hasMatchingTags;
                    }
                    return true;
                }
            )
            .collect(Collectors.toList());
    }

    private void decryptProperties(Properties properties) {
        if (properties != null) {
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
}
