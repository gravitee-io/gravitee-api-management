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
package io.gravitee.rest.api.management.v2.rest.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import java.io.IOException;
import java.util.Arrays;

/**
 * The ApiV4Definition CRD and the Automation API write the flow mode as the enum name (DEFAULT, BEST_MATCH), not as
 * FlowMode's JSON label (default, best-match). The CRD export writes the name, and the CRD import reads both.
 */
public final class FlowExecutionCRDJson {

    private FlowExecutionCRDJson() {}

    public static class Serializer extends StdSerializer<FlowExecution> {

        public Serializer() {
            super(FlowExecution.class);
        }

        @Override
        public void serialize(FlowExecution flowExecution, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            if (flowExecution.getMode() != null) {
                gen.writeStringField("mode", flowExecution.getMode().name());
            }
            gen.writeBooleanField("matchRequired", flowExecution.isMatchRequired());
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends StdDeserializer<FlowExecution> {

        public Deserializer() {
            super(FlowExecution.class);
        }

        @Override
        public FlowExecution deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.readValueAsTree();
            if (node instanceof ObjectNode object) {
                var mode = object.path("mode").asText();
                Arrays.stream(FlowMode.values())
                    .filter(flowMode -> flowMode.name().equalsIgnoreCase(mode))
                    .findFirst()
                    .ifPresent(flowMode -> object.put("mode", flowMode.getLabel()));
            }
            return parser.getCodec().treeToValue(node, FlowExecution.class);
        }
    }
}
