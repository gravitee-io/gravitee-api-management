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
package io.gravitee.gateway.reactive.handlers.api.el;

import io.gravitee.common.util.TemplatedValueHashMap;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.gateway.reactor.ReactableApi;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ApiVariables {

    private final ReactableApi<? extends AbstractApi> api;

    private Map<String, String> apiProperties;

    ApiVariables(final ReactableApi<? extends AbstractApi> api) {
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

    public Map<String, String> getProperties() {
        if (apiProperties == null && api.getDefinition().getProperties() != null) {
            this.apiProperties =
                api
                    .getDefinition()
                    .getProperties()
                    .stream()
                    .collect(
                        Collectors.toMap(
                            io.gravitee.definition.model.v4.property.Property::getKey,
                            io.gravitee.definition.model.v4.property.Property::getValue,
                            (v1, v2) -> {
                                throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
                            },
                            TemplatedValueHashMap::new
                        )
                    );
        }

        return this.apiProperties;
    }
}
