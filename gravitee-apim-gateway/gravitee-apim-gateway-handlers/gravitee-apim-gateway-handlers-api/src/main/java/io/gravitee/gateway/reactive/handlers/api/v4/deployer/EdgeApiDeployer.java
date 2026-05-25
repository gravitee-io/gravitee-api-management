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
import io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi;
import java.util.List;
import java.util.stream.Collectors;

public class EdgeApiDeployer extends AbstractApiDeployer<EdgeApi> {

    public EdgeApiDeployer(final GatewayConfiguration gatewayConfiguration, final DataEncryptor dataEncryptor) {
        super(gatewayConfiguration, dataEncryptor);
    }

    @Override
    public void initialize(final EdgeApi edgeApi) {
        io.gravitee.definition.model.v4.edge.EdgeApi apiDefinition = edgeApi.getDefinition();

        if (apiDefinition.getProperties() != null) {
            decryptProperties(apiDefinition.getProperties());
        }
    }

    @Override
    public List<String> getPlans(final EdgeApi edgeApi) {
        var plans = edgeApi.getDefinition().getPlans();
        return plans != null ? plans.stream().map(Plan::getName).collect(Collectors.toList()) : List.of();
    }
}
