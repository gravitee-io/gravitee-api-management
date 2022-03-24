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
package io.gravitee.definition.model;

import io.gravitee.common.util.TemplatedValueHashMap;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Properties implements Serializable {

    private List<Property> properties;
    private Map<String, String> entries;

    public void setProperties(List<Property> properties) {
        this.properties = properties;

        if (properties != null) {
            this.entries =
                properties
                    .stream()
                    .collect(
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
    }

    public List<Property> getProperties() {
        return properties;
    }

    public Map<String, String> getValues() {
        return this.entries;
    }
}
