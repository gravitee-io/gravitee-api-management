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
import io.gravitee.definition.model.services.healthcheck.Request;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestSerializer extends StdScalarSerializer<Request> {

    public RequestSerializer(Class<Request> t) {
        super(t);
    }

    @Override
    public void serialize(Request request, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("path", request.getPath());
        jgen.writeStringField("method", request.getMethod().name());

        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            jgen.writeArrayFieldStart("headers");

            request
                .getHeaders()
                .forEach(
                    httpHeader -> {
                        try {
                            jgen.writeStartObject();
                            jgen.writeStringField("name", httpHeader.getName());
                            jgen.writeStringField("value", httpHeader.getValue());
                            jgen.writeEndObject();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                );
            jgen.writeEndArray();
        }
        if (request.getBody() != null && !request.getBody().isEmpty()) {
            jgen.writeStringField("body", request.getBody());
        }

        jgen.writeBooleanField("fromRoot", request.isFromRoot());
        jgen.writeEndObject();
    }
}
