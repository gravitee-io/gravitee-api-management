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
package io.gravitee.management.model.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.management.model.PolicyConfigurationEntity;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyConfigurationDeserializer extends JsonDeserializer<PolicyConfigurationEntity> {

    @Override
    public PolicyConfigurationEntity deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        PolicyConfigurationEntity policy = new PolicyConfigurationEntity();
        policy.setPolicy(node.get("policy").asText());
        policy.setConfiguration(node.get("configuration").toString());

        return policy;
    }
}