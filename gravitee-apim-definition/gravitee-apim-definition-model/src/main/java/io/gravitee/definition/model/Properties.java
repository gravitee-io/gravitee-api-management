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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.util.TemplatedValueHashMap;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Properties implements Serializable {

    @JsonProperty("properties")
    private List<Property> propertiesList = List.of();

    @JsonIgnore
    private Map<String, String> entries = Map.of();

    public Properties() {}

    @Builder
    public Properties(List<Property> propertiesList) {
        setProperties(propertiesList);
    }

    public void setProperties(List<Property> properties) {
        this.propertiesList = properties == null ? List.of() : properties;

        this.entries = this.propertiesList.stream().collect(
            Collectors.toMap(
                Property::getKey,
                Property::getValue,
                (v1, v2) -> {
                    throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
                },
                TemplatedValueHashMap::new
            )
        );
    }

    public List<Property> getProperties() {
        return propertiesList;
    }

    @JsonIgnore
    public Map<String, String> getValues() {
        return this.entries;
    }
}
