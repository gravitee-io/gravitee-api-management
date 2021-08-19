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
package io.gravitee.rest.api.model;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
public class PropertiesEntity {

    @JsonProperty("properties")
    private List<PropertyEntity> properties = new ArrayList<>();

    public PropertiesEntity() {}

    public PropertiesEntity(List<PropertyEntity> properties) {
        this.properties = properties;
    }

    public PropertiesEntity(Properties properties) {
        if (properties != null && properties.getProperties() != null) {
            this.properties = properties.getProperties().stream().map(PropertyEntity::new).collect(toList());
        }
    }

    public List<PropertyEntity> getProperties() {
        return properties;
    }

    public void setProperties(List<PropertyEntity> properties) {
        this.properties = properties;
    }

    public Properties toDefinition() {
        Properties definitionProperties = new Properties();
        definitionProperties.setProperties(properties.stream().map(property -> (Property) property).collect(toList()));
        return definitionProperties;
    }
}
