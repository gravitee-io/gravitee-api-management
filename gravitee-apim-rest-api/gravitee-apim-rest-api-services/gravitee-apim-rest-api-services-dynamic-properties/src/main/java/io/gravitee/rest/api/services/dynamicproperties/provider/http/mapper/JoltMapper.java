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
package io.gravitee.rest.api.services.dynamicproperties.provider.http.mapper;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.bazaarvoice.jolt.chainr.ChainrBuilder;
import com.google.common.base.Strings;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JoltMapper {

    private Chainr chainr;

    public JoltMapper(String specification) {
        if (!Strings.isNullOrEmpty(specification)) {
            chainr = new ChainrBuilder(JsonUtils.jsonToList(specification)).build();
        }
    }

    public List<DynamicProperty> map(final String source) {
        if (Strings.isNullOrEmpty(source)) {
            return List.of();
        }

        //Default value is equal to the input json value (in case empty jolt specs)
        String jsonProperties = source;
        if (chainr != null) {
            List<?> transformed;
            if (source.charAt(0) == '[') {
                transformed = (List<?>) chainr.transform(JsonUtils.jsonToList(source));
            } else {
                transformed = (List<?>) chainr.transform(JsonUtils.jsonToMap(source));
            }
            jsonProperties = JsonUtils.toJsonString(transformed);
        }

        List<Object> items = JsonUtils.jsonToList(jsonProperties);
        return items
            .stream()
            .map(item -> {
                Map<?, ?> mapItem = (Map<?, ?>) item;
                String key = String.valueOf(mapItem.get("key"));
                String value = String.valueOf(mapItem.get("value"));
                return new DynamicProperty(key, value);
            })
            .collect(Collectors.toList());
    }
}
