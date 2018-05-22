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
package io.gravitee.gateway.core.endpoint.ref;

import io.gravitee.gateway.api.endpoint.Endpoint;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointReference extends AbstractReference {

    public static final String REFERENCE_PREFIX = "endpoint:";

    private final Endpoint endpoint;
    private final String key;

    public EndpointReference(final Endpoint endpoint) {
        this.endpoint = endpoint;
        this.key = REFERENCE_PREFIX + endpoint.name();
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String name() {
        return endpoint.name();
    }

    @Override
    public Endpoint endpoint() {
        return endpoint;
    }
}
