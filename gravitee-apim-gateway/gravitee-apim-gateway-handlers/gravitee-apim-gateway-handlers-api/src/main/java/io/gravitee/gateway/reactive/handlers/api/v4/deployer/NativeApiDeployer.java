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
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.handlers.api.v4.NativeApi;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NativeApiDeployer extends AbstractApiDeployer<NativeApi> {

    public NativeApiDeployer(final GatewayConfiguration gatewayConfiguration, final DataEncryptor dataEncryptor) {
        super(gatewayConfiguration, dataEncryptor);
    }

    @Override
    public void initialize(final NativeApi nativeApi) {
        io.gravitee.definition.model.v4.nativeapi.NativeApi apiDefinition = nativeApi.getDefinition();

        if (apiDefinition.getPlans() == null) {
            apiDefinition.setPlans(List.of());
        }
        if (apiDefinition.getProperties() != null) {
            decryptProperties(apiDefinition.getProperties());
        }
    }

    @Override
    public List<String> getPlans(final NativeApi nativeApi) {
        return nativeApi.getDefinition().getPlans().stream().map(NativePlan::getName).collect(Collectors.toList());
    }
}
