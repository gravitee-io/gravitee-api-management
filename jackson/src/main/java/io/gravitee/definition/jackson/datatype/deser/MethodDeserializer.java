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
package io.gravitee.definition.jackson.datatype.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Method;
import io.gravitee.definition.model.Policy;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class MethodDeserializer extends StdScalarDeserializer<Method> {

    public MethodDeserializer(Class<Method> vc) {
        super(vc);
    }

    @Override
    public Method deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        Method methodDefinition = new Method();

        node.fieldNames().forEachRemaining(field -> {
            JsonNode subNode = node.findValue(field);

            switch(field) {
                case "methods":
                    if (subNode != null && subNode.isArray()) {
                        HttpMethod [] methods = new HttpMethod[subNode.size()];

                        final int[] idx = {0};
                        subNode.elements().forEachRemaining(jsonNode -> {
                            methods[idx[0]++] = HttpMethod.valueOf(jsonNode.asText().toUpperCase());
                        });

                        methodDefinition.setMethods(methods);
                    }
                    break;
                default:
                    // We are in the case of a policy
                    Policy policy = new Policy();

                    JsonNode enabledNode = subNode.get("enabled");
                    if (enabledNode != null && enabledNode.isBoolean()) {
                        policy.setEnabled(enabledNode.asBoolean());
                    }

                    policy.setName(field);
                    policy.setConfiguration(subNode.toString());

                    methodDefinition.getPolicies().add(policy);

                    break;
            }
        });

        return methodDefinition;
    }
}

