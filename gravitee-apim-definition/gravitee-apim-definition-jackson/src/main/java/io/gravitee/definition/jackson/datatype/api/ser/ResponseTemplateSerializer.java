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
import io.gravitee.definition.model.ResponseTemplate;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTemplateSerializer extends StdScalarSerializer<ResponseTemplate> {

    public ResponseTemplateSerializer(Class<ResponseTemplate> t) {
        super(t);
    }

    @Override
    public void serialize(ResponseTemplate responseTemplate, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeNumberField("status", responseTemplate.getStatusCode());
        if (responseTemplate.getHeaders() != null && !responseTemplate.getHeaders().isEmpty()) {
            jgen.writeObjectField("headers", responseTemplate.getHeaders());
        }
        jgen.writeStringField("body", responseTemplate.getBody());
        jgen.writeBooleanField("propagateErrorKeyToLogs", responseTemplate.isPropagateErrorKeyToLogs());
        jgen.writeEndObject();
    }
}
