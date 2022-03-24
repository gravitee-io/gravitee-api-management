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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Failover;
import io.gravitee.definition.model.FailoverCase;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.Step;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowDeserializer extends StdScalarDeserializer<Flow> {

    public FlowDeserializer() {
        super(Flow.class);
    }

    @Override
    public Flow deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Flow flow = new Flow();

        flow.setName(node.path("name").asText());
        flow.setEnabled(node.path("enabled").asBoolean(true));
        JsonNode pathOperator = node.path("path-operator");
        flow.setPath(pathOperator.path("path").asText());
        flow.setOperator(Operator.valueOf(pathOperator.path("operator").asText(Operator.STARTS_WITH.name())));
        flow.setCondition(node.path("condition").asText());


        JsonNode methodsNode = node.get("methods");
        if (methodsNode != null && methodsNode.isArray()) {
            EnumSet<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);
            methodsNode.elements().forEachRemaining(jsonNode -> methods.add(HttpMethod.valueOf(jsonNode.asText().toUpperCase())));
            flow.setMethods(methods);
        }

        JsonNode preNode = node.get("pre");
        if (preNode != null && preNode.isArray()) {
            final List<Step> steps = new ArrayList<>();
            preNode.elements().forEachRemaining(jsonNode -> {
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
            postNode.elements().forEachRemaining(jsonNode -> {
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
