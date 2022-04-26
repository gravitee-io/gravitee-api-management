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

import static java.util.Comparator.reverseOrder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.FlowStage;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDeserializer extends StdScalarDeserializer<Api> {

    private final Logger logger = LoggerFactory.getLogger(ApiDeserializer.class);

    public ApiDeserializer(Class<Api> vc) {
        super(vc);
    }

    @Override
    public Api deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return this.deserialize(jp, ctxt, new Api(), node);
    }

    public Api deserialize(JsonParser jp, DeserializationContext ctxt, Api api, JsonNode node) throws IOException {
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
        api.setExecutionMode(ExecutionMode.fromLabel(node.path("execution_mode").asText()));

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

        // Add compatibility with Definition 1.22
        if (api.getServices() != null) {
            EndpointDiscoveryService discoveryService = api.getServices().get(EndpointDiscoveryService.class);
            if (discoveryService != null) {
                api.getServices().remove(EndpointDiscoveryService.class);
                Set<EndpointGroup> endpointGroups = api.getProxy().getGroups();
                if (endpointGroups != null && !endpointGroups.isEmpty()) {
                    EndpointGroup defaultGroup = endpointGroups.iterator().next();
                    defaultGroup.getServices().put(EndpointDiscoveryService.class, discoveryService);
                }
            }
        }

        JsonNode resourcesNode = node.get("resources");
        if (resourcesNode != null && resourcesNode.isArray()) {
            resourcesNode
                .elements()
                .forEachRemaining(
                    resourceNode -> {
                        try {
                            Resource resource = resourceNode.traverse(jp.getCodec()).readValueAs(Resource.class);
                            if (!api.getResources().contains(resource)) {
                                api.getResources().add(resource);
                            } else {
                                throw ctxt.mappingException("A resource already exists with name " + resource.getName());
                            }
                        } catch (IOException e) {
                            logger.error("An error occurred during api deserialization", e);
                        }
                    }
                );
        }

        // If no version provided, defaults to 1.0.0
        api.setDefinitionVersion(DefinitionVersion.valueOfLabel(node.path("gravitee").asText(DefinitionVersion.V1.getLabel())));

        // If no flow mode provided, defaults to "default"
        api.setFlowMode(FlowMode.valueOf(node.path("flow_mode").asText(FlowMode.DEFAULT.name())));

        if (api.getDefinitionVersion() == DefinitionVersion.V1) {
            if (node.get("flows") != null) {
                throw ctxt.mappingException("Flows are only available for definition >= 2.x.x ");
            }

            JsonNode pathsNode = node.get("paths");
            if (pathsNode != null) {
                final Map<String, List<Rule>> paths = new TreeMap<>(reverseOrder());
                pathsNode
                    .fields()
                    .forEachRemaining(
                        jsonNode -> {
                            try {
                                List<Rule> rules = jsonNode
                                    .getValue()
                                    .traverse(jp.getCodec())
                                    .readValueAs(new TypeReference<List<Rule>>() {});
                                paths.put(jsonNode.getKey(), rules);
                            } catch (IOException e) {
                                logger.error("Path {} can not be de-serialized", jsonNode.getKey());
                            }
                        }
                    );

                api.setPaths(paths);
            }
        }

        if (api.getDefinitionVersion() == DefinitionVersion.V2) {
            if (node.get("paths") != null) {
                throw ctxt.mappingException("Paths are only available for definition 1.x.x ");
            }

            JsonNode flowsNode = node.get("flows");
            if (flowsNode != null) {
                final List<Flow> flows = new ArrayList<>();
                flowsNode
                    .elements()
                    .forEachRemaining(
                        jsonNode -> {
                            try {
                                Flow flow = jsonNode.traverse(jp.getCodec()).readValueAs(Flow.class);
                                flow.setStage(FlowStage.API);
                                flows.add(flow);
                            } catch (IOException e) {
                                logger.error("Flow {} can not be de-serialized", jsonNode.asText());
                            }
                        }
                    );
                api.setFlows(flows);
            }

            JsonNode plansNode = node.get("plans");
            if (plansNode != null) {
                final List<Plan> plans = new ArrayList<>();
                plansNode
                    .elements()
                    .forEachRemaining(
                        jsonNode -> {
                            try {
                                Plan plan = jsonNode.traverse(jp.getCodec()).readValueAs(Plan.class);
                                plan.getFlows().forEach(flow -> flow.setStage(FlowStage.PLAN));
                                plans.add(plan);
                            } catch (IOException e) {
                                logger.error("Plan {} can not be de-serialized", jsonNode.asText());
                            }
                        }
                    );
                api.setPlans(plans);
            }
        }

        JsonNode propertiesNode = node.get("properties");
        if (propertiesNode != null) {
            Properties properties = propertiesNode.traverse(jp.getCodec()).readValueAs(Properties.class);
            api.setProperties(properties);
        }

        JsonNode tagsNode = node.get("tags");

        if (tagsNode != null && tagsNode.isArray()) {
            tagsNode.elements().forEachRemaining(jsonNode -> api.getTags().add(jsonNode.asText()));
        }

        JsonNode pathMappingsNode = node.get("path_mappings");
        if (pathMappingsNode != null) {
            pathMappingsNode
                .elements()
                .forEachRemaining(
                    jsonNode -> {
                        final String pathMapping = jsonNode.asText();
                        api.getPathMappings().put(pathMapping, Pattern.compile(pathMapping.replaceAll(":\\w*", "[^\\/]*") + "/*"));
                    }
                );
        }

        JsonNode responseTemplatesNode = node.get("response_templates");
        if (responseTemplatesNode != null) {
            final Map<String, Map<String, ResponseTemplate>> responseTemplates = new HashMap<>();
            responseTemplatesNode
                .fields()
                .forEachRemaining(
                    jsonNode -> {
                        try {
                            Map<String, ResponseTemplate> templates = jsonNode
                                .getValue()
                                .traverse(jp.getCodec())
                                .readValueAs(new TypeReference<Map<String, ResponseTemplate>>() {});
                            responseTemplates.put(jsonNode.getKey(), templates);
                        } catch (IOException e) {
                            logger.error("Response templates {} can not be de-serialized", jsonNode.getKey());
                        }
                    }
                );

            api.setResponseTemplates(responseTemplates);
        }

        return api;
    }
}
