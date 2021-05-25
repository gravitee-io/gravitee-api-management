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
package io.gravitee.gateway.handlers.api.path;

import io.gravitee.gateway.api.Request;

/**
 * This resolver is used to determine the configured {@link Path} from
 * the API definition {@link io.gravitee.definition.model.Api} for the given
 * {@link Request}. By getting this path, the gateway will be able to determine
 * the {@link io.gravitee.definition.model.Rule} to apply for policy chains (request and response).
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PathResolver {
    /**
     * The "resolved" path.
     *
     * @param request The incoming request.
     * @return The "resolved" path for current request.
     */
    Path resolve(Request request);
}
