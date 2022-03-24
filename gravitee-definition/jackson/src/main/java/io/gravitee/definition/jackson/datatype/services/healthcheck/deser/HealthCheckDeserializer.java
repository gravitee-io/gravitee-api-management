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
package io.gravitee.definition.jackson.datatype.services.healthcheck.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.datatype.services.core.deser.ScheduledServiceDeserializer;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.services.healthcheck.Request;
import io.gravitee.definition.model.services.healthcheck.Response;
import io.gravitee.definition.model.services.healthcheck.Step;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckDeserializer<T extends HealthCheckService> extends ScheduledServiceDeserializer<T> {

    public HealthCheckDeserializer(Class<T> vc) {
        super(vc);
    }

    @Override
    protected void deserialize(T service, JsonParser jsonParser, JsonNode node, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        super.deserialize(service, jsonParser, node, ctxt);

        if (service.isEnabled()) {
            // New version of health-check service
            final JsonNode stepsNode = node.get("steps");
            if (stepsNode != null && stepsNode.isArray()) {
                List<Step> steps = new ArrayList<>(stepsNode.size());

                for (JsonNode stepNode : stepsNode) {
                    Step step = stepNode.traverse(jsonParser.getCodec()).readValueAs(Step.class);
                    steps.add(step);
                }

                service.setSteps(steps);
            } else {
                // Ensure backward compatibility
                Step step = new Step();

                final JsonNode requestNode = node.get("request");
                if (requestNode != null) {
                    step.setRequest(requestNode.traverse(jsonParser.getCodec()).readValueAs(Request.class));
                } else {
                    throw ctxt.mappingException("[health-check] Request is required");
                }

                final JsonNode expectationNode = node.get("expectation");
                if (expectationNode != null) {
                    step.setResponse(expectationNode.traverse(jsonParser.getCodec()).readValueAs(Response.class));
                } else {
                    Response response = new Response();
                    response.setAssertions(Collections.singletonList(Response.DEFAULT_ASSERTION));
                    step.setResponse(response);
                }

                service.setSteps(Collections.singletonList(step));
            }
        }
    }
}