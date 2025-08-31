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
package io.gravitee.rest.api.service.v4;

import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiStateService {
    /**
     * Deploys an API
     * @param executionContext the execution context containing organization and environment information
     * @param apiToDeploy is the API to deploy
     * @param authenticatedUser user to reference in deployment properties
     * @param apiDeploymentEntity additional information about the deployment
     * @return the deployed API
     */
    GenericApiEntity deploy(
        ExecutionContext executionContext,
        Api apiToDeploy,
        String authenticatedUser,
        ApiDeploymentEntity apiDeploymentEntity
    );

    /**
     * Deploys an API fetched from database thanks to its id.
     * @param executionContext the execution context containing organization and environment information
     * @param apiId to get the last version of the API from database before deploying it
     * @param authenticatedUser user to reference in deployment properties
     * @param apiDeploymentEntity additional information about the deployment
     * @return the deployed API
     */
    GenericApiEntity deploy(
        ExecutionContext executionContext,
        String apiId,
        String authenticatedUser,
        ApiDeploymentEntity apiDeploymentEntity
    );

    GenericApiEntity start(ExecutionContext executionContext, String apiId, String userId);

    boolean startV2DynamicProperties(String apiId);

    boolean startV4DynamicProperties(String apiId);

    GenericApiEntity stop(ExecutionContext executionContext, String apiId, String userId);

    boolean stopV2DynamicProperties(String apiId);

    boolean stopV4DynamicProperties(String apiId);

    boolean isSynchronized(ExecutionContext executionContext, GenericApiEntity apiEntity);
}
