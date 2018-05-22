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
package io.gravitee.gateway.core.endpoint.resolver;

import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.endpoint.Endpoint;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface EndpointResolver {

    /**
     * Returns an endpoint according to the incoming HTTP request. If not endpoint corresponds to the request,
     * or if the selected endpoint is not available, the method returns <code>null</code>.
     * @param request
     * @param executionContext
     * @return
     */
    ResolvedEndpoint resolve(Request request, ExecutionContext executionContext);

    interface ResolvedEndpoint {

        String getUri();

        Connector getConnector();

        Endpoint getEndpoint();
    }
}
