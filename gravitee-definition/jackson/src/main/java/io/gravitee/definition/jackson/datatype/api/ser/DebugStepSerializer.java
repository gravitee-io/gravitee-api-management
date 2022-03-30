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
package io.gravitee.definition.jackson.datatype.api.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.debug.DebugStep;
import java.io.IOException;
import java.util.Map;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugStepSerializer extends StdScalarSerializer<DebugStep> {

    public DebugStepSerializer(Class<DebugStep> t) {
        super(t);
    }

    @Override
    public void serialize(DebugStep debugStep, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        if (debugStep.getPolicyInstanceId() != null) {
            jgen.writeStringField("policyInstanceId", debugStep.getPolicyInstanceId());
        }

        if (debugStep.getPolicyId() != null) {
            jgen.writeStringField("policyId", debugStep.getPolicyId());
        }

        if (debugStep.getScope() != null) {
            jgen.writeStringField("scope", debugStep.getScope().name());
        }

        if (debugStep.getStatus() != null) {
            jgen.writeStringField("status", debugStep.getStatus().name());
        }

        if (debugStep.getDuration() != null) {
            jgen.writeNumberField("duration", debugStep.getDuration());
        }

        if (debugStep.getResult() != null) {
            jgen.writeObjectFieldStart("result");
            for (Map.Entry<String, Object> entry : debugStep.getResult().entrySet()) {
                jgen.writeObjectField(entry.getKey(), entry.getValue());
            }
            jgen.writeEndObject();
        }
        jgen.writeEndObject();
    }
}
