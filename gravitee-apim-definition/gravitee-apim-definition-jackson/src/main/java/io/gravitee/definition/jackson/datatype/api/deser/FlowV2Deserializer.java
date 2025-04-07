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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowV2Deserializer extends StdScalarDeserializer<FlowV2Impl> {

    public FlowV2Deserializer(Class<FlowV2Impl> vc) {
        super(vc);
    }

    @Override
    public FlowV2Impl deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        FlowV2Impl flow = new FlowV2Impl();

        JsonNode jsonId = node.path("id");

        if (!jsonId.isNull() && !jsonId.asText().isEmpty()) {
            flow.setId(jsonId.asText());
        }

        JsonNode name = node.path("name");
        if (!name.isNull() && !name.isMissingNode()) {
            flow.setName(name.asText());
        }

        flow.setEnabled(node.path("enabled").asBoolean(true));

        JsonNode jsonPathOperator = node.get("path-operator");

        if (jsonPathOperator != null) {
            flow.setPathOperator(jsonPathOperator.traverse(jp.getCodec()).readValueAs(PathOperator.class));
        }

        JsonNode condition = node.path("condition");
        if (!condition.isNull() && !condition.isMissingNode()) {
            flow.setCondition(condition.asText());
        }

        JsonNode consumersNode = node.get("consumers");
        if (consumersNode != null && consumersNode.isArray()) {
            final List<Consumer> consumers = new ArrayList<>();
            consumersNode
                .elements()
                .forEachRemaining(jsonNode -> {
                    try {
                        consumers.add(jsonNode.traverse(jp.getCodec()).readValueAs(Consumer.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            flow.setConsumers(consumers);
        }

        JsonNode methodsNode = node.get("methods");
        if (methodsNode != null && methodsNode.isArray()) {
            EnumSet<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);
            methodsNode.elements().forEachRemaining(jsonNode -> methods.add(HttpMethod.valueOf(jsonNode.asText().toUpperCase())));
            flow.setMethods(methods);
        }

        JsonNode preNode = node.get("pre");
        if (preNode != null && preNode.isArray()) {
            final List<Step> steps = new ArrayList<>();
            preNode
                .elements()
                .forEachRemaining(jsonNode -> {
                    try {
                        steps.add(jsonNode.traverse(jp.getCodec()).readValueAs(Step.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            flow.setPre(steps);
        }

        JsonNode postNode = node.get("post");
        if (postNode != null && postNode.isArray()) {
            final List<Step> steps = new ArrayList<>();
            postNode
                .elements()
                .forEachRemaining(jsonNode -> {
                    try {
                        steps.add(jsonNode.traverse(jp.getCodec()).readValueAs(Step.class));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            flow.setPost(steps);
        }

        return flow;
    }
}
