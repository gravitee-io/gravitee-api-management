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
package io.gravitee.rest.api.services.dictionary.provider.http.mapper;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.bazaarvoice.jolt.chainr.ChainrBuilder;
import io.gravitee.rest.api.services.dictionary.model.DynamicProperty;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class JoltMapper {

    private final Chainr chainr;

    public JoltMapper(String specification) {
        Objects.requireNonNull(specification, "Specification must not be null");
        chainr = new ChainrBuilder(JsonUtils.jsonToList(specification)).build();
    }

    public JoltMapper(InputStream specification) {
        Objects.requireNonNull(specification, "Specification must not be null");
        chainr = new ChainrBuilder(JsonUtils.jsonToList(specification)).build();
    }

    public Collection<DynamicProperty> map(String source) {
        //Default value is equal to the input json value (in case empty jolt specs)
        String jsonProperties = source;
        ArrayList transformed;
        if (jsonProperties != null && jsonProperties.charAt(0) == '[') {
            transformed = (ArrayList) chainr.transform(JsonUtils.jsonToList(source));
        } else {
            transformed = (ArrayList) chainr.transform(JsonUtils.jsonToMap(source));
        }
        jsonProperties = JsonUtils.toJsonString(transformed);

        //Now ensure current json properties is well formatted.
        //    if (validateJson(jsonProperties)) {

        List<Object> items = JsonUtils.jsonToList(jsonProperties);
        Object collect = items
            .stream()
            .map(item -> {
                Map<Object, Object> mapItem = (Map<Object, Object>) item;
                String key = String.valueOf(mapItem.get("key"));
                String value = String.valueOf(mapItem.get("value"));
                return new DynamicProperty(key, value);
            })
            .collect(Collectors.toList());

        return (Collection<DynamicProperty>) collect;
    }
}
