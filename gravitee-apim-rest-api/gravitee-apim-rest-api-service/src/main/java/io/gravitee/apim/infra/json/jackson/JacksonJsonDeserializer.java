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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JacksonJsonDeserializer implements JsonDeserializer {

    private final JsonMapper mapper;

    private final JsonMapper mapperNonStrict;

    public JacksonJsonDeserializer() {
        this(JsonMapperFactory.build());
    }

    public JacksonJsonDeserializer(JsonMapper mapper) {
        this.mapper = mapper;
        this.mapperNonStrict = mapper.copy();
        this.mapperNonStrict.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public <T> T deserialize(String json, Class<T> clazz) throws JsonProcessingException {
        return deserialize(mapper, json, clazz);
    }

    @Override
    public <T> T deserializeNonStrict(String json, Class<T> clazz) throws JsonProcessingException {
        return deserialize(mapperNonStrict, json, clazz);
    }

    private <T> T deserialize(JsonMapper mapper, String json, Class<T> clazz) throws JsonProcessingException {
        try {
            return mapper.readValue(json, clazz);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.debug("Fail to deserialize json: {}", json, e);
            throw new JsonProcessingException("Fail to deserialize json", e);
        }
    }
}
