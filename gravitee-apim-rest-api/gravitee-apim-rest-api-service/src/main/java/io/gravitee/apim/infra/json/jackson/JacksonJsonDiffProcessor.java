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

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.gravitee.apim.core.json.JsonDiffProcessor;
import java.util.Arrays;

public class JacksonJsonDiffProcessor implements JsonDiffProcessor {

    private final JsonMapper mapper;

    public JacksonJsonDiffProcessor() {
        this.mapper = JsonMapperFactory.build();
    }

    public JacksonJsonDiffProcessor(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String diff(Object object1, Object object2) {
        ObjectNode oldNode = object1 == null
            ? mapper.createObjectNode()
            : mapper.convertValue(object1, ObjectNode.class).remove(Arrays.asList("updatedAt", "createdAt"));
        ObjectNode newNode = object2 == null
            ? mapper.createObjectNode()
            : mapper.convertValue(object2, ObjectNode.class).remove(Arrays.asList("updatedAt", "createdAt"));
        return JsonDiff.asJson(oldNode, newNode).toString();
    }
}
