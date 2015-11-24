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
package io.gravitee.definition.jackson.datatype.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.Monitoring;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class MonitoringDeserializer extends StdScalarDeserializer<Monitoring> {

    public MonitoringDeserializer(Class<Monitoring> vc) {
        super(vc);
    }

    @Override
    public Monitoring deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Monitoring monitoring = new Monitoring();

        final JsonNode endpoint = node.get("endpoint");
        if (endpoint != null) {
            monitoring.setEndpoint(endpoint.asText());
        }

        JsonNode monitoringEnabledNode = node.get("enabled");
        if (monitoringEnabledNode != null) {
            monitoring.setEnabled(monitoringEnabledNode.asBoolean(true));
        }

        final JsonNode interval = node.get("interval");
        if (endpoint != null) {
            monitoring.setInterval(interval.asLong());
        }

        JsonNode unit = node.get("unit");
        if (unit != null) {
            monitoring.setUnit(TimeUnit.valueOf(unit.asText().toUpperCase()));
        }

        return monitoring;
    }
}