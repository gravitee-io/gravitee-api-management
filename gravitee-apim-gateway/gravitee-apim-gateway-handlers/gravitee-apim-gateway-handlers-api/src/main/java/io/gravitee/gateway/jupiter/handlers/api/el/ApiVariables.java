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
package io.gravitee.gateway.jupiter.handlers.api.el;

import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.gateway.jupiter.handlers.api.v4.Api;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ApiVariables {

    private final Api api;

    ApiVariables(final Api api) {
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

    public List<Property> getProperties() {
        return this.api.getDefinition().getProperties();
    }
}
