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
package io.gravitee.rest.api.model.api;

import io.gravitee.common.util.TemplatedValueHashMap;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serialization of an API to be used with the TemplateEngine
 * An equivalent to ApiProperties and ApiVariables classes in the Gateway.
 */
public class ApiTemplateVariables {

    private final GenericApiEntity api;

    public ApiTemplateVariables(final GenericApiEntity api) {
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
        if (this.api != null) {
            if (DefinitionVersion.V4.equals(this.api.getDefinitionVersion())) {
                var v4Api = (io.gravitee.rest.api.model.v4.api.ApiEntity) api;
                if (v4Api.getProperties() != null) {
                    return v4Api
                        .getProperties()
                        .stream()
                        .collect(
                            Collectors.toMap(
                                io.gravitee.definition.model.v4.property.Property::getKey,
                                io.gravitee.definition.model.v4.property.Property::getValue,
                                (v1, v2) -> {
                                    throw new IllegalArgumentException(String.format("Duplicate key for values %s and %s", v1, v2));
                                },
                                TemplatedValueHashMap::new
                            )
                        );
                }
            } else {
                var v2Api = (io.gravitee.rest.api.model.api.ApiEntity) api;
                if (v2Api.getProperties() != null) {
                    return v2Api.getProperties().getValues();
                }
            }
        }

        return Map.of();
    }
}
