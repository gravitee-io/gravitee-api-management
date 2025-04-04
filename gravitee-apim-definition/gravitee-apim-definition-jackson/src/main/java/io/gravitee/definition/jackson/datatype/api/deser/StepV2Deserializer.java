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
import io.gravitee.definition.model.flow.StepV2;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StepV2Deserializer extends StdScalarDeserializer<StepV2> {

    public StepV2Deserializer(Class<StepV2> vc) {
        super(vc);
    }

    @Override
    public StepV2 deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        StepV2 step = new StepV2();
        step.setName(node.path("name").asText());

        JsonNode description = node.path("description");
        if (!description.isMissingNode() && !description.isNull()) {
            step.setDescription(description.asText());
        }
        step.setPolicy(node.get("policy").asText());
        final JsonNode condition = node.get("condition");
        if (condition != null && !condition.isMissingNode() && !condition.isNull()) {
            final String conditionStr = condition.asText();
            if (!conditionStr.isBlank()) {
                step.setCondition(conditionStr);
            }
        }
        step.setConfiguration(node.get("configuration").toString());
        step.setEnabled(node.path("enabled").asBoolean(true));

        return step;
    }
}
