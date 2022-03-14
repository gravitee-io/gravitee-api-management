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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.PolicyScope;
import io.gravitee.definition.model.debug.DebugStep;
import io.gravitee.definition.model.debug.DebugStepStatus;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugStepDeserializer extends StdScalarDeserializer<DebugStep> {

    public DebugStepDeserializer(Class<DebugStep> vc) {
        super(vc);
    }

    @Override
    public DebugStep deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        DebugStep debugStep = new DebugStep();
        debugStep.setPolicyInstanceId(readStringValue(node, "policyInstanceId"));
        debugStep.setPolicyId(readStringValue(node, "policyId"));
        debugStep.setScope(PolicyScope.valueOf(readStringValue(node, "scope")));
        debugStep.setStatus(DebugStepStatus.valueOf(readStringValue(node, "status")));
        debugStep.setCondition(readStringValue(node, "condition"));
        debugStep.setDuration(node.get("duration").asLong());
        JsonNode resultNode = node.get("result");
        if (resultNode != null && !resultNode.isEmpty(null)) {
            Map<String, Object> result = resultNode.traverse(jp.getCodec()).readValueAs(new TypeReference<HashMap<String, Object>>() {});
            debugStep.setResult(result);
        }
        debugStep.setStage(readStringValue(node, "stage"));
        return debugStep;
    }

    private String readStringValue(JsonNode rootNode, String fieldName) {
        JsonNode fieldNode = rootNode.get(fieldName);
        if (fieldNode != null) {
            return fieldNode.asText();
        }
        return null;
    }
}
