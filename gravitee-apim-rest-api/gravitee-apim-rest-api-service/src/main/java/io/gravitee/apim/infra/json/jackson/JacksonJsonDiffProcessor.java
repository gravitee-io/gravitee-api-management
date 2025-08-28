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
package io.gravitee.apim.infra.json.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.json.JsonDiffProcessor;
import java.util.Arrays;

public class JacksonJsonDiffProcessor implements JsonDiffProcessor {

    private final ObjectMapper mapper;

    public JacksonJsonDiffProcessor() {
        this.mapper = JsonMapperFactory.build();
    }

    public JacksonJsonDiffProcessor(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String diff(Object object1, Object object2) {
        // Hack to be able to compute diff for objects using @JsonRawValue annotation like PlanSecurity.
        // The json-patch library is not able to compute diff for such objects because it does not handle the token type
        // VALUE_EMBEDDED_OBJECT

        String oldJson = convertToString(object1);
        String newJson = convertToString(object2);

        try {
            return JsonDiff.asJson(mapper.readTree(oldJson), mapper.readTree(newJson)).toString();
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Error while computing JSON diff", e);
        }
    }

    private String convertToString(Object obj) {
        if (obj == null) {
            return "{}";
        }

        if (obj.getClass().isArray() || obj instanceof Iterable) {
            return mapper.convertValue(obj, ArrayNode.class).toString();
        }

        return mapper.convertValue(obj, ObjectNode.class).remove(Arrays.asList("updatedAt", "createdAt")).toString();
    }
}
