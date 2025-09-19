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
package io.gravitee.apim.gateway.tests.sdk;

import static io.gravitee.apim.gateway.tests.sdk.GatewayTestingExtension.GATEWAY_DYNAMIC_CONFIG_KEY;

import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayTestParameterResolver;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

public class GatewayDynamicConfigParameterResolver implements GatewayTestParameterResolver {

    private static final Collection<Class<?>> SUPPORTED_TYPES = Set.of(
        GatewayDynamicConfig.HttpConfig.class,
        GatewayDynamicConfig.TcpConfig.class,
        GatewayDynamicConfig.Config.class
    );

    @Override
    public boolean supports(ParameterContext parameterContext) {
        return SUPPORTED_TYPES.contains(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolve(ExtensionContext extensionContext, ParameterContext parameterContext, AbstractGatewayTest gatewayTest) {
        if (
            extensionContext.getStore(ExtensionContext.Namespace.GLOBAL).get(GATEWAY_DYNAMIC_CONFIG_KEY) instanceof
                GatewayDynamicConfig.GatewayDynamicConfigImpl config
        ) {
            return config;
        }
        throw new IllegalArgumentException("Gateway seems not deployed");
    }
}
