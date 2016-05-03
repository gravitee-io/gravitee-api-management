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
package io.gravitee.definition.jackson.datatype.services.healthcheck.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.services.healthcheck.HealthCheck;

import java.io.IOException;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class HealthCheckSerializer extends StdScalarSerializer<HealthCheck> {

    public HealthCheckSerializer(Class<HealthCheck> t) {
        super(t);
    }

    @Override
    public void serialize(HealthCheck healthCheck, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeNumberField("interval", healthCheck.getInterval());
        jgen.writeStringField("unit", healthCheck.getUnit().toString());
        jgen.writeBooleanField("enabled", healthCheck.isEnabled());
        jgen.writeObjectField("request", healthCheck.getRequest());
        jgen.writeObjectField("expectation", healthCheck.getExpectation());
        jgen.writeEndObject();
    }
}
