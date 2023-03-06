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
package io.gravitee.gateway.tests.grpc.v3;

import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.tests.grpc.jupiter.GrpcBidirectionalStreamingJupiterIntegrationTest;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
class GrpcBidirectionalStreamingV3IntegrationTest extends GrpcBidirectionalStreamingJupiterIntegrationTest {

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        super.configureGateway(gatewayConfigurationBuilder);
        gatewayConfigurationBuilder.jupiterModeEnabled(false);
    }

    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        super.configureApi(api, definitionClass);
        if (isLegacyApi(definitionClass)) {
            final Api definition = (Api) api.getDefinition();
            definition.setExecutionMode(ExecutionMode.V3);
        }
    }
}
