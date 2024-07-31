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
package io.gravitee.apim.gateway.tests.sdk.converters;

import io.gravitee.gateway.reactor.ReactableApi;

/**
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiDeploymentPreparer<D> {
    /**
     * Convert an API Definition to a {@link ReactableApi}
     * @param definition the definition to transform
     * @return the ReactableApi>
     */
    ReactableApi<D> toReactable(D definition, String environmentId);

    /**
     * Ensure minimal requirements for the API.
     * @param definition the api definition to complete
     */
    void ensureMinimalRequirementForApi(D definition);
}
