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
import io.gravitee.common.util.TemplatedValueHashMap;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class ApiDeserializer extends StdScalarDeserializer<Api> {

    private final Logger logger = LoggerFactory.getLogger(ApiDeserializer.class);

    public ApiDeserializer(Class<Api> vc) {
        super(vc);
    }

    @Override
    public Api deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Api api = new Api();

        JsonNode idNode = node.get("id");
        if (idNode == null) {
            throw ctxt.mappingException("ID property is required");
        } else {
            api.setId(idNode.asText());
        }

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
            logger.error("A proxy property is required for {}", api.getName());
            throw ctxt.mappingException("A proxy property is required for " + api.getName());
        }

        JsonNode servicesNode = node.get("services");
        if (servicesNode != null) {
            Services services = servicesNode.traverse(jp.getCodec()).readValueAs(Services.class);
            api.getServices().set(services.getAll());
        }

        JsonNode resourcesNode = node.get("resources");
        if (resourcesNode != null && resourcesNode.isArray()) {
            resourcesNode.elements().forEachRemaining(resourceNode -> {
                try {
                    Resource resource = resourceNode.traverse(jp.getCodec()).readValueAs(Resource.class);
                    if (! api.getResources().contains(resource)) {
                        api.getResources().add(resource);
                    } else {
                        logger.error("A resource already exists with name {}", resource.getName());
                        throw ctxt.mappingException("A resource already exists with name " + resource.getName());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        JsonNode pathsNode = node.get("paths");
        if (pathsNode != null) {
            final Map<String, Path> paths = new TreeMap<>((Comparator<String>) (path1, path2) -> path2.compareTo(path1));
            pathsNode.fields().forEachRemaining(jsonNode -> {
                try {
                    Path path = jsonNode.getValue().traverse(jp.getCodec()).readValueAs(Path.class);
                    path.setPath(jsonNode.getKey());
                    paths.put(jsonNode.getKey(), path);
                } catch (IOException e) {
                    logger.error("Path {} can not be de-serialized", jsonNode.getKey());
                }
            });

            api.setPaths(paths);
        }

        JsonNode propertiesNode = node.get("properties");
        if (propertiesNode != null) {
            Map<String, String> properties = new TemplatedValueHashMap();
            propertiesNode.fields().forEachRemaining(jsonNode ->
                properties.put(jsonNode.getKey(), jsonNode.getValue().textValue())
            );

            api.setProperties(properties);
        }

        JsonNode tagsNode = node.get("tags");

        if (tagsNode != null && tagsNode.isArray()) {
            tagsNode.elements().forEachRemaining(jsonNode -> api.getTags().add(jsonNode.asText()));
        }

        JsonNode viewsNode = node.get("views");

        if (viewsNode != null && viewsNode.isArray()) {
            viewsNode.elements().forEachRemaining(jsonNode -> api.getViews().add(jsonNode.asText()));
        }

        return api;
    }
}
