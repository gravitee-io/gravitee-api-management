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
import io.gravitee.definition.model.services.healthcheck.Expectation;
import io.gravitee.definition.model.services.healthcheck.HealthCheck;
import io.gravitee.definition.model.services.healthcheck.Request;

import java.io.IOException;
import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckDeserializer extends ScheduledServiceDeserializer<HealthCheck> {

    public HealthCheckDeserializer(Class<HealthCheck> vc) {
        super(vc);
    }

    @Override
    protected void deserialize(HealthCheck service, JsonParser jsonParser, JsonNode node, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        super.deserialize(service, jsonParser, node, ctxt);

        final JsonNode requestNode = node.get("request");
        if (requestNode != null) {
            service.setRequest(requestNode.traverse(jsonParser.getCodec()).readValueAs(Request.class));
        } else {
            throw ctxt.mappingException("[healthcheck] Request is required");
        }

        final JsonNode expectationNode = node.get("expectation");
        if (expectationNode != null) {
            service.setExpectation(expectationNode.traverse(jsonParser.getCodec()).readValueAs(Expectation.class));
        } else {
            Expectation expectation = new Expectation();
            expectation.setAssertions(Collections.singletonList(Expectation.DEFAULT_ASSERTION));
            service.setExpectation(expectation);
        }
    }
}