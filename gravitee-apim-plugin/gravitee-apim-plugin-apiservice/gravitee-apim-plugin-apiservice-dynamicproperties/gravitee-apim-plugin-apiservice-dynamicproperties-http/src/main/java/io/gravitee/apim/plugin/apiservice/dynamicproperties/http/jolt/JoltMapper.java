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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http.jolt;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.bazaarvoice.jolt.chainr.ChainrBuilder;
import io.gravitee.definition.model.v4.property.Property;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JoltMapper {

    private final Chainr joltReader;

    public JoltMapper(String specification) {
        Objects.requireNonNull(specification, "JOLT specification must not be null");
        this.joltReader = new ChainrBuilder(JsonUtils.jsonToList(specification)).build();
    }

    public List<Property> map(String source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        ArrayList<Object> transformedProperties = (ArrayList<Object>) (isJsonArray(source)
                ? joltReader.transform(JsonUtils.jsonToList(source))
                : joltReader.transform(JsonUtils.jsonToMap(source)));
        final String newProperties = JsonUtils.toJsonString(transformedProperties);

        final List<Object> extractObjects = JsonUtils.jsonToList(newProperties);
        return extractObjects == null
            ? List.of()
            : extractObjects
                .stream()
                .map(property -> {
                    Map<Object, Object> mapItem = (Map<Object, Object>) property;
                    String key = String.valueOf(mapItem.get("key"));
                    String value = String.valueOf(mapItem.get("value"));
                    return new Property(key, value, false, true);
                })
                .toList();
    }

    private boolean isJsonArray(String source) {
        return source.trim().charAt(0) == '[';
    }
}
