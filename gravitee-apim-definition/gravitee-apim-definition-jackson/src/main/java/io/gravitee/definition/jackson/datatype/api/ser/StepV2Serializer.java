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
package io.gravitee.definition.jackson.datatype.api.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.flow.StepV2;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StepV2Serializer extends StdScalarSerializer<StepV2> {

    public StepV2Serializer(Class<StepV2> vc) {
        super(vc);
    }

    @Override
    public void serialize(StepV2 step, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("name", step.getName());
        if (step.getDescription() != null) {
            jgen.writeStringField("description", step.getDescription());
        }
        jgen.writeBooleanField("enabled", step.isEnabled());
        jgen.writeStringField("policy", step.getPolicy());
        if (step.getCondition() != null && !step.getCondition().isBlank()) {
            jgen.writeStringField("condition", step.getCondition());
        }
        jgen.writeFieldName("configuration");
        jgen.writeRawValue(step.getConfiguration());
        jgen.writeEndObject();
    }
}
