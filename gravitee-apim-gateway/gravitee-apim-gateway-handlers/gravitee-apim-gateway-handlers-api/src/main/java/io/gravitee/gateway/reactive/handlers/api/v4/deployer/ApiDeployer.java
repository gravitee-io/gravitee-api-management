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
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeployer extends AbstractApiDeployer<Api> {

    public ApiDeployer(final GatewayConfiguration gatewayConfiguration, final DataEncryptor dataEncryptor) {
        super(gatewayConfiguration, dataEncryptor);
    }

    @Override
    public void initialize(final Api api) {
        var apiDefinition = api.getDefinition();

        // Keep the plan filtering for io.gravitee.gateway.services.localregistry.LocalApiDefinitionRegistry
        apiDefinition.setPlans(filterPlans(apiDefinition.getPlans()));
        if (apiDefinition.getProperties() != null) {
            decryptProperties(apiDefinition.getProperties());
        }
    }

    @Override
    public List<String> getPlans(final Api api) {
        return api.getDefinition().getPlans() != null
            ? api.getDefinition().getPlans().stream().map(Plan::getName).collect(Collectors.toList())
            : List.of();
    }

    private List<Plan> filterPlans(final List<Plan> plans) {
        if (plans == null) {
            return List.of();
        }
        return plans
            .stream()
            .filter(plan -> plan.getStatus() != null)
            .filter(plan -> filterPlanStatus(plan.getStatus().getLabel()))
            .filter(plan -> filterShardingTag(plan.getName(), plan.getId(), plan.getTags()))
            .collect(Collectors.toList());
    }
}
