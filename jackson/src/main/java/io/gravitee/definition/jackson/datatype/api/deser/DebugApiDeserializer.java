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
package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.HttpResponse;
import io.gravitee.definition.model.debug.DebugApi;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugApiDeserializer extends StdScalarDeserializer<DebugApi> {

    private final Logger logger = LoggerFactory.getLogger(DebugApiDeserializer.class);

    private final ApiDeserializer base;

    public DebugApiDeserializer(Class<?> vc) {
        super(vc);
        this.base = new ApiDeserializer(Api.class);
    }

    @Override
    public DebugApi deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        DebugApi debugApi = (DebugApi) this.base.deserialize(jp, ctxt, new DebugApi(), node);
        JsonNode requestNode = node.get("request");
        if (requestNode != null) {
            debugApi.setRequest(requestNode.traverse(jp.getCodec()).readValueAs(HttpRequest.class));
        } else {
            logger.error("A request property is required for {}", debugApi.getName());
            throw ctxt.mappingException("A request property is required for " + debugApi.getName());
        }

        JsonNode responseNode = node.get("response");
        if (responseNode != null) {
            debugApi.setResponse(responseNode.traverse(jp.getCodec()).readValueAs(HttpResponse.class));
        }

        return debugApi;
    }
}
