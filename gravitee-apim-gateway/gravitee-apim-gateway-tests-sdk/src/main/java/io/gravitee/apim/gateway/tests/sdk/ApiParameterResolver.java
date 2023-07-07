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

import io.gravitee.apim.gateway.tests.sdk.annotations.InjectApi;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayTestParameterResolver;
import io.gravitee.gateway.reactor.ReactableApi;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiParameterResolver implements GatewayTestParameterResolver {

    @Override
    public boolean supports(ParameterContext parameterContext) {
        return (
            parameterContext.getParameter().getType() == ReactableApi.class &&
            parameterContext.getParameter().isAnnotationPresent(InjectApi.class)
        );
    }

    @Override
    public Object resolve(ExtensionContext extensionContext, ParameterContext parameterContext, AbstractGatewayTest gatewayTest) {
        final InjectApi annotation = parameterContext.getParameter().getAnnotation(InjectApi.class);
        final ReactableApi<?> reactableApi = gatewayTest.deployedApis.get(annotation.apiId());
        if (reactableApi == null) {
            throw new PreconditionViolationException("Not api found for [" + annotation.apiId() + "] deployed at method level");
        }
        return reactableApi;
    }
}
