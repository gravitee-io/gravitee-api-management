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

import io.gravitee.definition.model.v4.edge.EdgeApi;
import io.gravitee.gateway.reactor.ReactableApi;
import org.junit.platform.commons.PreconditionViolationException;

public class EdgeApiDeploymentPreparer implements ApiDeploymentPreparer<EdgeApi> {

    @Override
    public ReactableApi<EdgeApi> toReactable(EdgeApi definition, String environmentId) {
        final io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi api = new io.gravitee.gateway.reactive.handlers.api.v4.EdgeApi(
            definition
        );
        api.setEnvironmentId(environmentId);
        return api;
    }

    @Override
    public void ensureMinimalRequirementForApi(EdgeApi definition) {
        if (definition.getType() == null) {
            throw new PreconditionViolationException("'type' field must be defined on a V4 Edge API Definition");
        }
    }
}
