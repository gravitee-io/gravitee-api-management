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
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import java.io.IOException;
import java.util.EnumSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RuleDeserializer extends StdScalarDeserializer<Rule> {

    public RuleDeserializer(Class<Rule> vc) {
        super(vc);
    }

    @Override
    public Rule deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Rule rule = new Rule();
        EnumSet<HttpMethod> methods = EnumSet.noneOf(HttpMethod.class);

        node
            .fieldNames()
            .forEachRemaining(
                field -> {
                    JsonNode subNode = node.findValue(field);

                    switch (field) {
                        case "methods":
                            if (subNode != null && subNode.isArray()) {
                                subNode
                                    .elements()
                                    .forEachRemaining(
                                        jsonNode -> {
                                            methods.add(HttpMethod.valueOf(jsonNode.asText().toUpperCase()));
                                        }
                                    );
                            }
                            break;
                        case "description":
                            if (subNode != null) {
                                rule.setDescription(subNode.asText());
                            }
                            break;
                        case "enabled":
                            if (subNode != null) {
                                rule.setEnabled(subNode.asBoolean(true));
                            }
                            break;
                        default:
                            // We are in the case of a policy
                            Policy policy = new Policy();

                            policy.setName(field);
                            policy.setConfiguration(subNode.toString());

                            rule.setPolicy(policy);

                            break;
                    }
                }
            );

        rule.setMethods((methods.isEmpty()) ? EnumSet.allOf(HttpMethod.class) : methods);

        return rule;
    }
}
