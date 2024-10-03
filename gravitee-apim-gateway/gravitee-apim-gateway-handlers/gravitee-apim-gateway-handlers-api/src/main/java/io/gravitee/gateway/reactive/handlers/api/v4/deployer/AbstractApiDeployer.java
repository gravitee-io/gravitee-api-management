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
package io.gravitee.gateway.reactive.handlers.api.v4.deployer;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.Deployer;
import io.gravitee.gateway.reactor.ReactableApi;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractApiDeployer<T extends ReactableApi<?>> implements Deployer<T> {

    private final Logger logger = LoggerFactory.getLogger(AbstractApiDeployer.class);

    private final GatewayConfiguration gatewayConfiguration;
    private final DataEncryptor dataEncryptor;

    protected AbstractApiDeployer(final GatewayConfiguration gatewayConfiguration, final DataEncryptor dataEncryptor) {
        this.gatewayConfiguration = gatewayConfiguration;
        this.dataEncryptor = dataEncryptor;
    }

    protected boolean filterPlanStatus(final String planStatus) {
        return (
            PlanStatus.PUBLISHED.getLabel().equalsIgnoreCase(planStatus) || PlanStatus.DEPRECATED.getLabel().equalsIgnoreCase(planStatus)
        );
    }

    protected boolean filterShardingTag(final String planName, final String apiName, final Set<String> tags) {
        if (tags != null && !tags.isEmpty()) {
            boolean hasMatchingTags = gatewayConfiguration.hasMatchingTags(tags);
            if (!hasMatchingTags) {
                logger.debug("Plan name[{}] api[{}] has been ignored because not in configured sharding tags", planName, apiName);
            }
            return hasMatchingTags;
        }
        return true;
    }

    protected void decryptProperties(final List<Property> properties) {
        for (Property property : properties) {
            if (property.isEncrypted()) {
                try {
                    property.setValue(dataEncryptor.decrypt(property.getValue()));
                    property.setEncrypted(false);
                } catch (GeneralSecurityException e) {
                    logger.error("Error decrypting API property value for key {}", property.getKey(), e);
                }
            }
        }
    }
}
