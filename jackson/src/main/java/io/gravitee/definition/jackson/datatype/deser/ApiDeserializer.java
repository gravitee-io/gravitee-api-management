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
package io.gravitee.definition.jackson.datatype.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Proxy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class ApiDeserializer extends StdScalarDeserializer<Api> {

    public ApiDeserializer(Class<Api> vc) {
        super(vc);
    }

    @Override
    public Api deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Api api = new Api();

        JsonNode nameNode = node.get("name");
        if (nameNode == null) {
            throw ctxt.mappingException("Name property is required");
        } else {
            api.setName(nameNode.asText());
        }

        JsonNode versionNode = node.get("version");
        if (versionNode == null) {
            api.setVersion("undefined");
        } else {
            api.setVersion(versionNode.asText());
        }

        JsonNode proxyNode = node.get("proxy");
        if (proxyNode != null) {
            api.setProxy(proxyNode.traverse(jp.getCodec()).readValueAs(Proxy.class));
        } else {
            throw ctxt.mappingException("Proxy part of API is required");
        }

        JsonNode pathsNode = node.get("paths");
        if (pathsNode != null) {
            Map<String, Path> paths = new HashMap<>();
                pathsNode.fields().forEachRemaining(jsonNode -> {
                    try {
                        Path path = jsonNode.getValue().traverse(jp.getCodec()).readValueAs(Path.class);
                        path.setPath(jsonNode.getKey());
                        paths.put(jsonNode.getKey(), path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                api.setPaths(paths);
        }

        return api;
    }
}