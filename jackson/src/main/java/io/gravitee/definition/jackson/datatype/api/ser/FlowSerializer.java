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
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;

import java.io.IOException;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowSerializer extends StdScalarSerializer<Flow> {

    public FlowSerializer() {
        super(Flow.class);
    }

    @Override
    public void serialize(Flow flow, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        jgen.writeStringField("name", flow.getName());

        jgen.writeObjectFieldStart("path-operator");
        jgen.writeStringField("operator", flow.getOperator().toString().toUpperCase());
        jgen.writeStringField("path", flow.getPath());
        jgen.writeEndObject();

        jgen.writeStringField("condition", flow.getCondition());

        jgen.writeArrayFieldStart("methods");
        if (flow.getMethods() != null) {
            for (HttpMethod method : flow.getMethods()) {
                jgen.writeString(method.toString().toUpperCase());
            }
        }

        jgen.writeEndArray();

        jgen.writeArrayFieldStart("pre");
        for (Step step : flow.getPre()) {
            jgen.writeObject(step);
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("post");
        for (Step step : flow.getPost()) {
            jgen.writeObject(step);
        }
        jgen.writeEndArray();

        jgen.writeBooleanField("enabled", flow.isEnabled());

        jgen.writeEndObject();

    }

}
