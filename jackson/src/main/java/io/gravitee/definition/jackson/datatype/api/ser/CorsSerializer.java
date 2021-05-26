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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.Cors;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CorsSerializer extends StdScalarSerializer<Cors> {

    public CorsSerializer(Class<Cors> t) {
        super(t);
    }

    @Override
    public void serialize(Cors cors, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        jgen.writeBooleanField("enabled", cors.isEnabled());
        jgen.writeBooleanField("allowCredentials", cors.isAccessControlAllowCredentials());

        jgen.writeArrayFieldStart("allowOrigin");
        if (cors.getAccessControlAllowOrigin() != null && !cors.getAccessControlAllowOrigin().isEmpty()) {
            cors
                .getAccessControlAllowOrigin()
                .forEach(
                    origin -> {
                        try {
                            // check pattern regex before to save
                            if (!"*".equals(origin) && (origin.contains("(") || origin.contains("[") || origin.contains("*"))) {
                                try {
                                    Pattern.compile(origin);
                                } catch (PatternSyntaxException pse) {
                                    throw provider.mappingException("Allow origin regex invalid: " + pse.getMessage());
                                }
                            }
                            jgen.writeString(origin);
                        } catch (IOException e) {
                            if (e instanceof JsonMappingException) {
                                throw new IllegalStateException(e.getMessage());
                            } else {
                                e.printStackTrace();
                            }
                        }
                    }
                );
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("allowHeaders");
        if (cors.getAccessControlAllowHeaders() != null && !cors.getAccessControlAllowHeaders().isEmpty()) {
            cors
                .getAccessControlAllowHeaders()
                .forEach(
                    header -> {
                        try {
                            jgen.writeString(header);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                );
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("allowMethods");
        if (cors.getAccessControlAllowMethods() != null && !cors.getAccessControlAllowMethods().isEmpty()) {
            cors
                .getAccessControlAllowMethods()
                .forEach(
                    method -> {
                        try {
                            jgen.writeString(method);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                );
        }
        jgen.writeEndArray();

        jgen.writeArrayFieldStart("exposeHeaders");
        if (cors.getAccessControlExposeHeaders() != null && !cors.getAccessControlExposeHeaders().isEmpty()) {
            cors
                .getAccessControlExposeHeaders()
                .forEach(
                    header -> {
                        try {
                            jgen.writeString(header);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                );
        }
        jgen.writeEndArray();

        jgen.writeNumberField("maxAge", cors.getAccessControlMaxAge());

        if (cors.isRunPolicies()) {
            jgen.writeBooleanField("runPolicies", cors.isRunPolicies());
        }

        jgen.writeEndObject();
    }
}
