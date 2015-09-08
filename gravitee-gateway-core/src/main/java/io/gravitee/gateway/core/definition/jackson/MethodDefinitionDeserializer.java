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
package io.gravitee.gateway.core.definition.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.core.definition.MethodDefinition;
import io.gravitee.gateway.core.definition.PolicyDefinition;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class MethodDefinitionDeserializer extends JsonDeserializer<MethodDefinition> {

    @Override
    public MethodDefinition deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        MethodDefinition methodDefinition = new MethodDefinition();

        node.fieldNames().forEachRemaining(field -> {
            JsonNode subNode = node.findValue(field);

            switch(field) {
                case "methods":
                    if (subNode != null && subNode.isArray()) {
                        HttpMethod [] methods = new HttpMethod[subNode.size()];

                        final int[] idx = {0};
                        subNode.elements().forEachRemaining(jsonNode -> {
                            methods[idx[0]++] = HttpMethod.valueOf(jsonNode.asText());
                        });

                        methodDefinition.setMethods(methods);
                    }
                    break;
                default:
                    // We are in the case of a policy
                    PolicyDefinition policy = new PolicyDefinition();

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

        //    System.out.println(nmode);


        /*
        System.out.println("-----------------");
        System.out.println(node.toString());
        */

        return methodDefinition;
    }
}
