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
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.Proxy;
import java.io.IOException;
import java.util.Set;
import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ProxySerializer extends StdScalarSerializer<Proxy> {

    private static final Logger log = LoggerFactory.getLogger(ProxySerializer.class);

    public ProxySerializer(Class<Proxy> t) {
        super(t);
    }

    @Override
    public void serialize(Proxy proxy, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        if (proxy.getVirtualHosts() != null) {
            writeArrayField(jgen, "virtual_hosts", proxy.getVirtualHosts(), "An error occurred while serializing api virtual hosts.");
        }

        jgen.writeBooleanField("strip_context_path", proxy.isStripContextPath());

        jgen.writeBooleanField("preserve_host", proxy.isPreserveHost());

        if (proxy.getLogging() != null) {
            jgen.writeObjectField("logging", proxy.getLogging());
        }

        final Set<EndpointGroup> groups = proxy.getGroups();

        if (groups != null) {
            writeArrayField(jgen, "groups", groups, "An error occurred while serializing api proxy groups.");
        }

        if (proxy.getFailover() != null) {
            jgen.writeObjectField("failover", proxy.getFailover());
        }

        if (proxy.getCors() != null && proxy.getCors().isEnabled()) {
            jgen.writeObjectField("cors", proxy.getCors());
        }

        if (proxy.getServers() != null && !proxy.getServers().isEmpty()) {
            writeArrayField(jgen, "servers", proxy.getServers(), "An error occurred while serializing api proxy servers.");
        }

        jgen.writeEndObject();
    }

    private void writeArrayField(JsonGenerator jgen, String fieldName, Iterable<?> values, String errorMessage) throws IOException {
        jgen.writeArrayFieldStart(fieldName);
        for (Object value : values) {
            try {
                jgen.writeObject(value);
            } catch (IOException e) {
                log.warn(errorMessage, e);
            }
        }
        jgen.writeEndArray();
    }
}
