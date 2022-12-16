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

import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.v4.property.Property;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiBuilder {

    private final Api api;

    public ApiBuilder() {
        this.api = new Api();
    }

    public ApiBuilder id(String id) {
        api.setId(id);
        return this;
    }

    public ApiBuilder name(String name) {
        api.setName(name);
        return this;
    }

    public ApiBuilder apiVersion(String apiVersion) {
        api.setApiVersion(apiVersion);
        return this;
    }

    public ApiBuilder properties(Map<String, String> properties) {
        api.setProperties(
            properties.entrySet().stream().map(p -> new Property(p.getKey(), p.getValue(), false, false)).collect(Collectors.toList())
        );
        return this;
    }

    public Api build() {
        return api;
    }

    public io.gravitee.definition.model.Api buildv2() {
        var apiv2 = new io.gravitee.definition.model.Api();
        apiv2.setId(api.getId());
        apiv2.setName(api.getName());
        apiv2.setVersion(api.getApiVersion());

        if (api.getProperties() != null) {
            var props = new Properties();
            props.setProperties(
                api
                    .getProperties()
                    .stream()
                    .map(p -> new io.gravitee.definition.model.Property(p.getKey(), p.getValue(), p.isEncrypted()))
                    .collect(Collectors.toList())
            );
            apiv2.setProperties(props);
        }
        return apiv2;
    }
}
