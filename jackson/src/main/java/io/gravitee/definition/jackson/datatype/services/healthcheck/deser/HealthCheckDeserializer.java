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
import io.gravitee.definition.model.services.healthcheck.HealthCheck;
import io.gravitee.definition.model.services.healthcheck.Expectation;
import io.gravitee.definition.model.services.healthcheck.Request;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckDeserializer extends StdScalarDeserializer<HealthCheck> {

    public HealthCheckDeserializer(Class<HealthCheck> vc) {
        super(vc);
    }

    @Override
    public HealthCheck deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        HealthCheck healthCheck = new HealthCheck();

        final JsonNode intervalNode = node.get("interval");
        if (intervalNode != null) {
            healthCheck.setInterval(intervalNode.asLong());
        } else {
            throw ctxt.mappingException("[healthcheck] Interval is required");
        }

        final JsonNode unitNode = node.get("unit");
        if (unitNode != null) {
            healthCheck.setUnit(TimeUnit.valueOf(unitNode.asText().toUpperCase()));
        } else {
            throw ctxt.mappingException("[healthcheck] Unit is required");
        }

        final JsonNode requestNode = node.get("request");
        if (requestNode != null) {
            healthCheck.setRequest(requestNode.traverse(jp.getCodec()).readValueAs(Request.class));
        } else {
            throw ctxt.mappingException("[healthcheck] Request is required");
        }

        final JsonNode expectationNode = node.get("expectation");
        if (expectationNode != null) {
            healthCheck.setExpectation(expectationNode.traverse(jp.getCodec()).readValueAs(Expectation.class));
        } else {
            Expectation expectation = new Expectation();
            expectation.setAssertions(Collections.singletonList(Expectation.DEFAULT_ASSERTION));
            healthCheck.setExpectation(expectation);
        }

        final JsonNode healthCheckEnabledNode = node.get("enabled");
        if (healthCheckEnabledNode != null) {
            healthCheck.setEnabled(healthCheckEnabledNode.asBoolean(true));
        } else {
            healthCheck.setEnabled(true);
        }

        return healthCheck;
    }
}