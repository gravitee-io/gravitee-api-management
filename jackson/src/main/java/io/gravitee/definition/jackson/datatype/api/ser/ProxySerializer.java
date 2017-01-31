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
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.Proxy;

import java.io.IOException;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProxySerializer extends StdScalarSerializer<Proxy> {

    public ProxySerializer(Class<Proxy> t) {
        super(t);
    }

    @Override
    public void serialize(Proxy proxy, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("context_path", proxy.getContextPath());
        jgen.writeBooleanField("strip_context_path", proxy.isStripContextPath());
        jgen.writeBooleanField("dumpRequest", proxy.isDumpRequest());
        jgen.writeBooleanField("multiTenant", proxy.isMultiTenant());

        final List<Endpoint> endpoints = proxy.getEndpoints();

        jgen.writeArrayFieldStart("endpoints");
        endpoints.forEach(endpoint -> {
            try {
                jgen.writeObject(endpoint);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        jgen.writeEndArray();

        if (proxy.getLoadBalancer() != null) {
            jgen.writeObjectField("load_balancing", proxy.getLoadBalancer());
        }

        if (proxy.getFailover() != null) {
            jgen.writeObjectField("failover", proxy.getFailover());
        }

        jgen.writeEndObject();
    }
}
