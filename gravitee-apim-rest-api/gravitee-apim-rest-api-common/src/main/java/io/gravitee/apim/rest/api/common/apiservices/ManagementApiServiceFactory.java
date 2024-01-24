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
package io.gravitee.apim.rest.api.common.apiservices;

import com.google.errorprone.annotations.DoNotCall;
import io.gravitee.gateway.reactive.api.apiservice.ApiServiceFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;

public interface ManagementApiServiceFactory<T extends ManagementApiService> extends ApiServiceFactory<T> {
    String DEPLOYMENT_CONTEXT_MESSAGE = "ManagementApiService can only be created with a ManagementDeploymentContext";

    @Override
    @DoNotCall(DEPLOYMENT_CONTEXT_MESSAGE)
    default T createService(DeploymentContext deploymentContext) {
        throw new UnsupportedOperationException(DEPLOYMENT_CONTEXT_MESSAGE);
    }

    T createService(DefaultManagementDeploymentContext deploymentContext);
}
