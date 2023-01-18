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
package io.gravitee.definition.model.v4;

import io.gravitee.definition.model.v4.property.Property;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiBuilder {

    private String apiId;
    private String name;
    private String apiVersion;
    private Map<String, String> properties;
    private Set<String> tags;

    public static ApiBuilder anApiV4() {
        return new ApiBuilder();
    }

    public ApiBuilder id(String id) {
        this.apiId = id;
        return this;
    }

    public ApiBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ApiBuilder apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public ApiBuilder properties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public ApiBuilder tags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public Api build() {
        var api = new Api();
        api.setId(apiId);
        api.setName(name);
        api.setApiVersion(apiVersion);
        api.setTags(tags);

        if (properties != null) {
            api.setProperties(
                properties.entrySet().stream().map(p -> new Property(p.getKey(), p.getValue(), false, false)).collect(Collectors.toList())
            );
        }

        return api;
    }
}
