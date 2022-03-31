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
import io.gravitee.definition.model.debug.DebugMetrics;
import java.io.IOException;

public class DebugMetricsDeserializer extends StdScalarDeserializer<DebugMetrics> {

    public DebugMetricsDeserializer(Class<DebugMetrics> vc) {
        super(vc);
    }

    @Override
    public DebugMetrics deserialize(JsonParser jp, DeserializationContext deserializationContext)
        throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        DebugMetrics debugMetrics = new DebugMetrics();
        if (node.has("proxyLatencyMs")) {
            debugMetrics.setProxyLatencyMs(node.get("proxyLatencyMs").asLong());
        }
        if (node.has("apiResponseTimeMs")) {
            debugMetrics.setApiResponseTimeMs(node.get("apiResponseTimeMs").asLong());
        }
        if (node.has("proxyResponseTimeMs")) {
            debugMetrics.setProxyResponseTimeMs(node.get("proxyResponseTimeMs").asLong());
        }
        return debugMetrics;
    }
}
