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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http;

import io.gravitee.apim.plugin.apiservice.dynamicproperties.http.helper.HttpDynamicPropertiesHelper;
import io.gravitee.apim.rest.api.common.apiservices.DefaultManagementDeploymentContext;
import io.gravitee.apim.rest.api.common.apiservices.ManagementApiServiceFactory;
import io.gravitee.definition.model.v4.Api;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Slf4j
public class HttpDynamicPropertiesServiceFactory implements ManagementApiServiceFactory<HttpDynamicPropertiesService> {

    @Override
    public HttpDynamicPropertiesService createService(DefaultManagementDeploymentContext deploymentContext) {
        final Api api = deploymentContext.getComponent(Api.class);

        if (HttpDynamicPropertiesHelper.canHandle(api)) {
            return new HttpDynamicPropertiesService(deploymentContext);
        }
        return null;
    }
}
