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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.services.healthcheck.HealthCheckRequest;
import io.gravitee.definition.model.services.healthcheck.HealthCheckResponse;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import java.io.IOException;
import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StepDeserializer extends StdScalarDeserializer<HealthCheckStep> {

    public StepDeserializer(Class<HealthCheckStep> vc) {
        super(vc);
    }

    @Override
    public HealthCheckStep deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        HealthCheckStep step = new HealthCheckStep();

        final JsonNode requestNode = node.get("request");
        if (requestNode != null) {
            step.setRequest(requestNode.traverse(jsonParser.getCodec()).readValueAs(HealthCheckRequest.class));
        } else {
            throw ctxt.mappingException("[health-check] Step request is required");
        }

        final JsonNode responseNode = node.get("response");
        if (responseNode != null) {
            step.setResponse(responseNode.traverse(jsonParser.getCodec()).readValueAs(HealthCheckResponse.class));
        } else {
            HealthCheckResponse response = new HealthCheckResponse();
            response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
            step.setResponse(response);
        }

        final JsonNode nameNode = node.get("name");
        if (nameNode != null) {
            step.setName(nameNode.asText());
        } else {
            step.setName("default-step");
        }

        return step;
    }
}
