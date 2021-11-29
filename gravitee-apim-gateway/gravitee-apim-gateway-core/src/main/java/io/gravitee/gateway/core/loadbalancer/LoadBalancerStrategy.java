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
package io.gravitee.gateway.core.loadbalancer;

import io.gravitee.gateway.api.endpoint.Endpoint;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@FunctionalInterface
public interface LoadBalancerStrategy {
    /**
     * Select next endpoint and return the endpoint name.
     * If no endpoint can be selected (no endpoint with the UP state), the value
     * <code>null</code> is returned.
     *
     * @return Endpoint name or <code>null</code> if none can be selected.
     */
    Endpoint next();
}
