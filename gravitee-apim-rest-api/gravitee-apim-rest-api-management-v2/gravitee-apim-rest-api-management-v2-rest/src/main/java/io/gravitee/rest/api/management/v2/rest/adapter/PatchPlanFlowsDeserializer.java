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
package io.gravitee.rest.api.management.v2.rest.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.use_case.PatchPlanUseCase.PlanFlowsConverter;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.management.v2.rest.mapper.FlowMapper;
import io.gravitee.rest.api.management.v2.rest.model.FlowV4;
import java.io.IOException;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
public class PatchPlanFlowsDeserializer implements PlanFlowsConverter {

    private final ObjectMapper objectMapper;

    public PatchPlanFlowsDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode toCurrentFlowsNode(List<Flow> flows) {
        return objectMapper.valueToTree(FlowMapper.INSTANCE.mapFromHttpV4(flows));
    }

    @Override
    public List<Flow> fromPatchedFlowsNode(JsonNode flowsNode) {
        try {
            var restFlows = objectMapper.readerForListOf(FlowV4.class).<List<FlowV4>>readValue(flowsNode);
            return FlowMapper.INSTANCE.mapToHttpV4(restFlows);
        } catch (IOException e) {
            throw new ValidationDomainException("Invalid value for field 'flows': " + e.getMessage(), e);
        }
    }
}
