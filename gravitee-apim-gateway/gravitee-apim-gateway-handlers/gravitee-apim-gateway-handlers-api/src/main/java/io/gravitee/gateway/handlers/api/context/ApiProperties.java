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
package io.gravitee.gateway.handlers.api.context;

import io.gravitee.gateway.handlers.api.definition.Api;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class ApiProperties {

    private final Api api;

    ApiProperties(final Api api) {
        this.api = api;
    }

    public String getId() {
        return this.api.getId();
    }

    public String getName() {
        return this.api.getName();
    }

    public String getVersion() {
        return this.api.getApiVersion();
    }

    public io.gravitee.definition.model.Properties getProperties() {
        return this.api.getDefinition().getProperties();
    }
}
