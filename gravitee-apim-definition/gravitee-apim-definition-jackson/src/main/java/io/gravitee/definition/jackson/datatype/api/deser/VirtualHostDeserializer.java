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
package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.VirtualHost;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VirtualHostDeserializer extends StdScalarDeserializer<VirtualHost> {

    private static final String URI_PATH_SEPARATOR = "/";

    public VirtualHostDeserializer(Class<VirtualHost> vc) {
        super(vc);
    }

    @Override
    public VirtualHost deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        VirtualHost vhost = new VirtualHost();

        JsonNode hostNode = node.get("host");
        if (hostNode != null) {
            vhost.setHost(hostNode.asText());
        }

        JsonNode pathNode = node.get("path");
        if (pathNode != null) {
            vhost.setPath(formatContextPath(pathNode.asText()));
        } else {
            vhost.setPath(URI_PATH_SEPARATOR);
        }

        vhost.setOverrideEntrypoint(node.path("override_entrypoint").asBoolean(false));

        return vhost;
    }

    private String formatContextPath(String contextPath) {
        String[] parts = contextPath.split(URI_PATH_SEPARATOR);
        StringBuilder finalPath = new StringBuilder();

        if (parts.length > 0) {
            for (String part : parts) {
                if (!part.isEmpty()) {
                    finalPath.append(URI_PATH_SEPARATOR).append(part);
                }
            }
            return finalPath.toString();
        }

        return URI_PATH_SEPARATOR;
    }
}
