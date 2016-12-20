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
package io.gravitee.management.services.dynamicproperties.provider.http.mapper;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.bazaarvoice.jolt.chainr.ChainrBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.gravitee.management.services.dynamicproperties.model.DynamicProperty;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JoltMapper {

    private final Logger logger = LoggerFactory.getLogger(JoltMapper.class);

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

        ArrayList transformed = (ArrayList) chainr.transform(JsonUtils.jsonToMap(source));
        jsonProperties = JsonUtils.toJsonString(transformed);

        //Now ensure current json properties is well formatted.
    //    if (validateJson(jsonProperties)) {

        List<Object> items = JsonUtils.jsonToList(jsonProperties);
        Object collect = items.stream()
                .map(item -> {
                    Map<String, String> mapItem = (Map<String, String>) item;
                    Object key = mapItem.get("key");
                    if (key instanceof Number) {
                        return new DynamicProperty(key.toString(), mapItem.get("value"));
                    } else {
                        return new DynamicProperty((String) key, mapItem.get("value"));
                    }
                })
                .collect(Collectors.toList());

        return (Collection<DynamicProperty>) collect;
    }
}
