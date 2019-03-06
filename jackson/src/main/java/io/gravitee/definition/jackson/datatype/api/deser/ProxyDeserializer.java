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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
            createEndpointGroups(node, jp.getCodec(), proxy, ctxt);
        }

        //check that endpoint groups and endpoints don't have the same name
        //deser have already check that group names are unique
        // and endpoint names too (in the same group)
        if (proxy.getGroups() != null && !proxy.getGroups().isEmpty()) {
            Set<String> endpointNames = proxy.getGroups().stream()
                    .map(EndpointGroup::getName)
                    .collect(Collectors.toSet());
            for (EndpointGroup group : proxy.getGroups()) {
                if (group.getEndpoints() != null) {
                    for (Endpoint endpoint : group.getEndpoints()) {
                        if (endpointNames.contains(endpoint.getName())) {
                            throw ctxt.mappingException("[api] API endpoint names and group names must be unique");
                        }
                        endpointNames.add(endpoint.getName());
                    }
                }
            }
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

        // Keep it for backward compatibility
        JsonNode loggingModeNode = node.get("loggingMode");
        if (loggingModeNode != null) {
            Logging logging = new Logging();
            logging.setMode(LoggingMode.valueOf(loggingModeNode.asText().toUpperCase()));
            proxy.setLogging(logging);
        }

        JsonNode loggingNode = node.get("logging");
        if (loggingNode != null) {
            Logging logging = loggingNode.traverse(jp.getCodec()).readValueAs(Logging.class);
            proxy.setLogging(logging);
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

    private void createEndpointGroups(JsonNode node, ObjectCodec codec, Proxy proxy, DeserializationContext ctxt) throws IOException {
        final JsonNode nodeGroups = node.get("groups");

        Set<EndpointGroup> groups = new LinkedHashSet<>(nodeGroups.size());

        for (JsonNode jsonNode : nodeGroups) {
            EndpointGroup group = jsonNode.traverse(codec).readValueAs(EndpointGroup.class);
            boolean added = groups.add(group);
            if (!added) {
                throw ctxt.mappingException("[api] API must have single endpoint group names");
            }
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