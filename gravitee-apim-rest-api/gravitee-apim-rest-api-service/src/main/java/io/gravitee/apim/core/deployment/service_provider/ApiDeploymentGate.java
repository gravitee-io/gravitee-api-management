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
package io.gravitee.apim.core.deployment.service_provider;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;

/**
 * Asks every {@link ApiDeploymentValidator} the platform holds, so the core gates a deployment on what each module
 * owns inside a definition without knowing which modules are installed.
 *
 * @author GraviteeSource Team
 */
public interface ApiDeploymentGate {
    /**
     * @see ApiDeploymentValidator#validateDeployable(Api)
     * @throws ApiNotDeployableException the refusal of the first validator that refuses.
     */
    void validateDeployable(Api api);

    /**
     * @see ApiDeploymentValidator#validateInternalFlowDeployable(Api)
     * @throws ApiNotDeployableException the refusal of the first validator that refuses.
     */
    void validateInternalFlowDeployable(Api api);
}
