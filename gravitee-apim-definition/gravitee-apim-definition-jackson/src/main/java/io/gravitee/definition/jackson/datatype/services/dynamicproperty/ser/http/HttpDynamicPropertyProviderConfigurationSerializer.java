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
package io.gravitee.definition.jackson.datatype.services.dynamicproperty.ser.http;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpDynamicPropertyProviderConfigurationSerializer extends StdScalarSerializer<HttpDynamicPropertyProviderConfiguration> {

    public HttpDynamicPropertyProviderConfigurationSerializer(Class<HttpDynamicPropertyProviderConfiguration> t) {
        super(t);
    }

    @Override
    public void serialize(HttpDynamicPropertyProviderConfiguration configuration, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("url", configuration.getUrl());
        jgen.writeStringField("specification", configuration.getSpecification());
        jgen.writeBooleanField("useSystemProxy", configuration.isUseSystemProxy());
        jgen.writeStringField("method", configuration.getMethod().name());
        if (configuration.getHeaders() != null) {
            jgen.writeFieldName("headers");
            jgen.writeStartArray();
            for (HttpHeader header : configuration.getHeaders()) {
                jgen.writeStartObject();
                jgen.writeStringField("name", header.getName());
                jgen.writeStringField("value", header.getValue());
                jgen.writeEndObject();
            }
            jgen.writeEndArray();
        }

        if (configuration.getBody() != null) {
            jgen.writeStringField("body", configuration.getBody());
        }
        jgen.writeEndObject();
    }
}
