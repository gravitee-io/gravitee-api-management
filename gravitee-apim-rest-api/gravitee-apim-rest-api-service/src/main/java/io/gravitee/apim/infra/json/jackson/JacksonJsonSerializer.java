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
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.json.JsonSerializer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JacksonJsonSerializer implements JsonSerializer {

    private final JsonMapper mapper;

    public JacksonJsonSerializer() {
        this(JsonMapperFactory.build());
    }

    public JacksonJsonSerializer(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String serialize(Object object) throws JsonProcessingException {
        try {
            return mapper.writeValueAsString(object);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.debug("Fail to serialize json: {}", object, e);
            throw new JsonProcessingException("Fail to serialize object", e);
        }
    }
}
