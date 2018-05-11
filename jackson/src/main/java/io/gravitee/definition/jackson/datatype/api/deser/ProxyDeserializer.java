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
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.*;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProxyDeserializer extends StdScalarDeserializer<Proxy> {

    public ProxyDeserializer(Class<Proxy> vc) {
        super(vc);
    }

    @Override
    public Proxy deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Proxy proxy = new Proxy();
        final JsonNode contextPath = node.get("context_path");
        if (contextPath != null) {
            String sContextPath = formatContextPath(contextPath.asText());
            proxy.setContextPath(sContextPath);
        } else {
            throw ctxt.mappingException("[api] API must have a valid context path");
        }

        final JsonNode nodeEndpoints = node.get("endpoints");
        final JsonNode nodeGroups = node.get("groups");

        if (nodeEndpoints != null && nodeEndpoints.isArray()) {
            createDefaultEndpointGroup(node, jp.getCodec(), proxy);
        } else if (nodeGroups != null && nodeGroups.isArray()) {
            createEndpointGroups(node, jp.getCodec(), proxy);
        }

        JsonNode stripContextNode = node.get("strip_context_path");
        if (stripContextNode != null) {
            proxy.setStripContextPath(stripContextNode.asBoolean(false));
        }

        JsonNode failoverNode = node.get("failover");
        if (failoverNode != null) {
            Failover failover = failoverNode.traverse(jp.getCodec()).readValueAs(Failover.class);
            proxy.setFailover(failover);
        }

        JsonNode loggingNode = node.get("loggingMode");
        if (loggingNode != null) {
            proxy.setLoggingMode(LoggingMode.valueOf(loggingNode.asText().toUpperCase()));
        } else {
            proxy.setLoggingMode(Proxy.DEFAULT_LOGGING_MODE);
        }

        JsonNode corsNode = node.get("cors");
        if (corsNode != null) {
            Cors cors = corsNode.traverse(jp.getCodec()).readValueAs(Cors.class);
            proxy.setCors(cors);
        }

        return proxy;
    }

    private void createDefaultEndpointGroup(JsonNode node, ObjectCodec codec, Proxy proxy) throws IOException {
        final EndpointGroup group = node.traverse(codec).readValueAs(EndpointGroup.class);
        group.setName("default-group");
        proxy.setGroups(Collections.singleton(group));
    }

    private void createEndpointGroups(JsonNode node, ObjectCodec codec, Proxy proxy) throws IOException {
        final JsonNode nodeGroups = node.get("groups");

        Set<EndpointGroup> groups = new LinkedHashSet<>(nodeGroups.size());

        for (JsonNode jsonNode : nodeGroups) {
            EndpointGroup group = jsonNode.traverse(codec).readValueAs(EndpointGroup.class);
            groups.add(group);
        }

        proxy.setGroups(groups);
    }

    private String formatContextPath(String contextPath) {
        String [] parts = contextPath.split("/");
        StringBuilder finalPath = new StringBuilder("/");

        for(String part : parts) {
            if (! part.isEmpty()) {
                finalPath.append(part).append('/');
            }
        }

        return finalPath.deleteCharAt(finalPath.length() - 1).toString();
    }
}