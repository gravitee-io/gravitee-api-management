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
package io.gravitee.definition.jackson.datatype.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import io.gravitee.definition.model.Proxy;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class ProxySerializer extends StdScalarSerializer<Proxy> {

    public ProxySerializer(Class<Proxy> t) {
        super(t);
    }

    @Override
    public void serialize(Proxy proxy, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("context_path", proxy.getContextPath());
        jgen.writeObjectField("target", proxy.getTarget().toString());
        jgen.writeBooleanField("strip_context_path", proxy.isStripContextPath());
        jgen.writeEndObject();

        if (proxy.getHttpClient() != null) {
            jgen.writeObjectField("http_client", proxy.getHttpClient());
        }
    }
}
