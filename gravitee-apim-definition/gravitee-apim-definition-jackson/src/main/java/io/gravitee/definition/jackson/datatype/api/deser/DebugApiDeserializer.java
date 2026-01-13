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
package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.HttpResponse;
import io.gravitee.definition.model.debug.DebugApiV2;
import io.gravitee.definition.model.debug.DebugMetrics;
import io.gravitee.definition.model.debug.DebugStep;
import io.gravitee.definition.model.debug.PreprocessorStep;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CustomLog
public class DebugApiDeserializer extends StdScalarDeserializer<DebugApiV2> {

    private static final Logger log = LoggerFactory.getLogger(DebugApiDeserializer.class);

    private final ApiDeserializer base;

    public DebugApiDeserializer(Class<?> vc) {
        super(vc);
        this.base = new ApiDeserializer(Api.class);
    }

    @Override
    public DebugApiV2 deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = jp.getCodec();
        JsonNode node = codec.readTree(jp);
        DebugApiV2 debugApi = (DebugApiV2) this.base.deserialize(jp, ctxt, new DebugApiV2(), node);
        JsonNode requestNode = node.get("request");
        if (requestNode != null) {
            debugApi.setRequest(requestNode.traverse(codec).readValueAs(HttpRequest.class));
        } else {
            log.error("A request property is required for {}", debugApi.getName());
            throw JsonMappingException.from(ctxt, "A request property is required for " + debugApi.getName());
        }

        readOptional(node, "response", codec, HttpResponse.class, debugApi::setResponse);
        readOptional(node, "debugSteps", codec, new TypeReference<List<DebugStep>>() {}, debugApi::setDebugSteps);
        readOptional(node, "preprocessorStep", codec, PreprocessorStep.class, debugApi::setPreprocessorStep);
        readOptional(node, "backendResponse", codec, HttpResponse.class, debugApi::setBackendResponse);
        readOptional(node, "metrics", codec, DebugMetrics.class, debugApi::setMetrics);

        return debugApi;
    }

    private static <T> void readOptional(JsonNode node, String field, ObjectCodec codec, Class<T> type, Consumer<T> setter)
        throws IOException {
        JsonNode valueNode = node.get(field);
        if (valueNode != null) {
            setter.accept(valueNode.traverse(codec).readValueAs(type));
        }
    }

    private static <T> void readOptional(JsonNode node, String field, ObjectCodec codec, TypeReference<T> type, Consumer<T> setter)
        throws IOException {
        JsonNode valueNode = node.get(field);
        if (valueNode != null) {
            setter.accept(valueNode.traverse(codec).readValueAs(type));
        }
    }
}
