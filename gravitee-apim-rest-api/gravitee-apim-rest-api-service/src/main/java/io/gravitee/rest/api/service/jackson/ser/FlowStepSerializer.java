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
package io.gravitee.rest.api.service.jackson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.repository.management.model.flow.FlowStep;
import java.io.IOException;

public class FlowStepSerializer extends StdScalarSerializer<FlowStep> {

    public FlowStepSerializer(Class<FlowStep> vc) {
        super(vc);
    }

    @Override
    public void serialize(FlowStep step, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("name", step.getName());
        jsonGenerator.writeStringField("description", step.getDescription());
        jsonGenerator.writeBooleanField("enabled", step.isEnabled());
        jsonGenerator.writeStringField("policy", step.getPolicy());
        jsonGenerator.writeNumberField("order", step.getOrder());
        if (step.getCondition() != null) {
            jsonGenerator.writeStringField("condition", step.getCondition());
        }
        jsonGenerator.writeFieldName("configuration");
        jsonGenerator.writeRawValue(step.getConfiguration());
        jsonGenerator.writeEndObject();
    }
}
