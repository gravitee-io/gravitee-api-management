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
public class ApiVariables {

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

    /**
     * The API properties, as exposed to EL by {@code #api.properties}.
     *
     * <p>Never {@code null}: an API whose definition has no properties gets an empty map, so
     * {@code #api.properties['missing']} evaluates to nothing instead of failing with
     * {@code EL1012E: Cannot index into a null value}. Expressions that relied on
     * {@code #api.properties == null} to detect an API without properties are now always false;
     * they must test the key they need instead, e.g. {@code #api.properties['key'] == null}.
     *
     * <p>Built lazily, on the first access, then reused: an API whose policies never read its
     * properties allocates nothing. The empty map is a {@link TemplatedValueHashMap}, the same type
     * as the populated one.
     */
    public Map<String, String> getProperties() {
        if (apiProperties == null) {
            if (api.getDefinition().getProperties() == null) {
                this.apiProperties = new TemplatedValueHashMap();
                return this.apiProperties;
            }
            this.apiProperties = api
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

    /** Whether the definition carries properties, without building the map {@link #getProperties()} returns. */
    boolean hasDefinitionProperties() {
        return api.getDefinition().getProperties() != null;
    }
}
