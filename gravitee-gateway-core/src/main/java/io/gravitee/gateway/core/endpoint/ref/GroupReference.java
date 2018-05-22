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
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupReference extends AbstractReference {

    public static final String REFERENCE_PREFIX = "group:";

    private final LoadBalancedEndpointGroup group;
    private final String key;

    public GroupReference(final LoadBalancedEndpointGroup group) {
        this.group = group;
        this.key = REFERENCE_PREFIX + group.getName();
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String name() {
        return group.getName();
    }

    @Override
    public Endpoint endpoint() {
        return group.next();
    }
}
