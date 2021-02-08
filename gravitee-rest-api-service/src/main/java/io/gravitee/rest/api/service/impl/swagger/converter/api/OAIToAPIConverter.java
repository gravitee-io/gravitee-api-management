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
package io.gravitee.rest.api.service.impl.swagger.converter.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.policy.api.swagger.Policy;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIDescriptorVisitor;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.gravitee.rest.api.service.swagger.converter.extension.XGraviteeIODefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.service.validator.PolicyHelper.clearNullValues;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAIToAPIConverter implements SwaggerToApiConverter<OAIDescriptor>, OAIDescriptorVisitor<SwaggerApiEntity> {

    public final static String X_GRAVITEEIO_DEFINITION_VENDOR_EXTENSION = "x-graviteeio-definition";

    private final static String PICTURE_REGEX = "^data:image/[\\w]+;base64,.*$";

    protected final Collection<? extends OAIOperationVisitor> visitors;

    private GroupService groupService;

    private TagService tagService;

    public OAIToAPIConverter(Collection<? extends OAIOperationVisitor> visitors, GroupService groupService, TagService tagService) {
        this.visitors = visitors;
        this.groupService = groupService;
        this.tagService = tagService;
    }

    @Override
    public SwaggerApiEntity convert(OAIDescriptor descriptor) {
        if (descriptor == null || descriptor.getSpecification() == null) {
            return null;
        }

        return visit(descriptor.getSpecification());
    }

    @Override
    public SwaggerApiEntity visit(OpenAPI oai) {
        final SwaggerApiEntity apiEntity = new SwaggerApiEntity();

        // Name
        apiEntity.setName(oai.getInfo().getTitle());

        // Description
        apiEntity.setDescription(oai.getInfo().getDescription() == null ? "Description of " + apiEntity.getName() : oai.getInfo().getDescription());

        // Version
        apiEntity.setVersion(oai.getInfo().getVersion());

        fill(apiEntity, oai);

        // Use X-Gravitee to add information in API
        XGraviteeIODefinition xGraviteeIODefinition = null;
        if (oai.getExtensions() != null && oai.getExtensions().get(X_GRAVITEEIO_DEFINITION_VENDOR_EXTENSION) != null) {
            xGraviteeIODefinition = new ObjectMapper().convertValue(oai.getExtensions().get(X_GRAVITEEIO_DEFINITION_VENDOR_EXTENSION), XGraviteeIODefinition.class);
        }

        // Proxy
        Proxy proxy = new Proxy();
        String defaultEndpoint = null;

        // Proxy - Endpoints
        if (!oai.getServers().isEmpty()) {
            List<String> evaluatedServerUrl = mapServersToEndpoint(oai.getServers());

            EndpointGroup defaultGroup = new EndpointGroup();
            defaultGroup.setName("default-group");

            if (evaluatedServerUrl == null) {
                defaultGroup.setEndpoints(singleton(new HttpEndpoint("default", defaultEndpoint)));
            } else if (evaluatedServerUrl.size() == 1) {
                defaultEndpoint = evaluatedServerUrl.get(0);
                defaultGroup.setEndpoints(singleton(new HttpEndpoint("default", defaultEndpoint)));
            } else {
                defaultEndpoint = evaluatedServerUrl.get(0);
                defaultGroup.setEndpoints(new HashSet<>());
                for (int i = 0; i < evaluatedServerUrl.size(); i++) {
                    defaultGroup.getEndpoints().add(new HttpEndpoint("server" + (i + 1), evaluatedServerUrl.get(i)));
                }
            }
            proxy.setGroups(singleton(defaultGroup));

            apiEntity.setProxy(proxy);
        }

        // Proxy - Context Path / Virtual Host
        if (xGraviteeIODefinition != null && xGraviteeIODefinition.getVirtualHosts() != null) {
            proxy.setVirtualHosts(xGraviteeIODefinition.getVirtualHosts().stream()
                    .map(vHost -> new VirtualHost(vHost.getHost(), vHost.getPath(), (vHost.getOverrideEntrypoint() != null ? vHost.getOverrideEntrypoint() : false)))
                    .collect(Collectors.toList()));
        } else {
            String contextPath = null;
            if (defaultEndpoint != null) {
                contextPath = URI.create(defaultEndpoint).getPath();
            }
            if (contextPath == null || contextPath.equals("/")) {
                contextPath = apiEntity.getName().replaceAll("\\s+", "").toLowerCase();
            }
            proxy.setVirtualHosts(singletonList(new VirtualHost(contextPath)));
        }
        apiEntity.setProxy(proxy);

        // Add xGraviteeIODefinition config
        if (xGraviteeIODefinition != null) {
            // Categories
            if (xGraviteeIODefinition.getCategories() != null && !xGraviteeIODefinition.getCategories().isEmpty()) {
                apiEntity.setCategories(new HashSet<>(xGraviteeIODefinition.getCategories()));
            }

            // Groups
            if (xGraviteeIODefinition.getGroups() != null && !xGraviteeIODefinition.getGroups().isEmpty()) {
                // Groups in schema are group name. Replace them by id
                Set<String> groupIdsToImport= xGraviteeIODefinition.getGroups().stream()
                        .flatMap(group -> groupService.findByName(group).stream())
                        .map(GroupEntity::getId)
                        .collect(Collectors.toSet());
                apiEntity.setGroups(groupIdsToImport);
            }

            // Labels
            if (xGraviteeIODefinition.getLabels() != null && !xGraviteeIODefinition.getLabels().isEmpty()) {
                apiEntity.setLabels(xGraviteeIODefinition.getLabels());
            }

            // Metadata
            if (xGraviteeIODefinition.getMetadata() != null && !xGraviteeIODefinition.getMetadata().isEmpty()) {
                final List<ApiMetadataEntity> apiMetadataEntities = xGraviteeIODefinition.getMetadata().stream()
                        .map(metadata -> {
                            ApiMetadataEntity apiMetadata = new ApiMetadataEntity();
                            apiMetadata.setKey(IdGenerator.generate(metadata.getName()));
                            apiMetadata.setName(metadata.getName());
                            apiMetadata.setValue(metadata.getValue());
                            apiMetadata.setFormat(metadata.getFormat() != null ? MetadataFormat.valueOf(metadata.getFormat().name()) : MetadataFormat.STRING);
                            return apiMetadata;
                        })
                        .collect(Collectors.toList());
                apiEntity.setMetadata(apiMetadataEntities);
            }

            // Picture
            if (xGraviteeIODefinition.getPicture() != null && !StringUtils.isEmpty(xGraviteeIODefinition.getPicture())) {
                if (xGraviteeIODefinition.getPicture().matches(PICTURE_REGEX)) {
                    apiEntity.setPicture(xGraviteeIODefinition.getPicture());
                }
            }

            // Properties
            if (xGraviteeIODefinition.getProperties() != null && !xGraviteeIODefinition.getProperties().isEmpty()) {
                Properties properties = new Properties();
                properties.setProperties(
                        xGraviteeIODefinition.getProperties().stream()
                                .map(prop -> new Property(prop.getKey(), prop.getValue()))
                                .collect(Collectors.toList())
                );

                apiEntity.setProperties(properties);
            }

            // Tags
            if (xGraviteeIODefinition.getTags() != null && !xGraviteeIODefinition.getTags().isEmpty()) {
                final Map<String, String> tagMap = tagService.findAll().stream().collect(toMap(TagEntity::getId, TagEntity::getName));
                final Set<String> tagIdToAdd = xGraviteeIODefinition.getTags().stream()
                        .map(tag -> findTagIdByName(tagMap, tag))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                if (tagIdToAdd != null && !tagIdToAdd.isEmpty()) {
                    apiEntity.setTags(tagIdToAdd);
                }
            }

            // Visibility
            if (xGraviteeIODefinition.getVisibility() != null) {
                apiEntity.setVisibility(Visibility.valueOf(xGraviteeIODefinition.getVisibility().name()));
            }
        }
        return apiEntity;
    }

    protected SwaggerApiEntity fill(final SwaggerApiEntity apiEntity, OpenAPI oai) {
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V1.getLabel());

        // Paths
        apiEntity.setPaths(oai.getPaths().entrySet().stream()
            .map(entry -> {
                final io.gravitee.definition.model.Path path = new Path();
                path.setPath(entry.getKey().replaceAll("\\{(.[^/\\}]*)\\}", ":$1"));

                Map<PathItem.HttpMethod, Operation> operations = entry.getValue().readOperationsMap();
                List<Rule> rules = new ArrayList<>();

                operations.forEach(new BiConsumer<PathItem.HttpMethod, Operation>() {
                    @Override
                    public void accept(PathItem.HttpMethod httpMethod, Operation operation) {
                        visitors.forEach(new Consumer<OAIOperationVisitor>() {
                            @Override
                            public void accept(OAIOperationVisitor oaiOperationVisitor) {
                                // Consider only policy visitor for now
                                Optional<Policy> policy = (Optional<Policy>) oaiOperationVisitor.visit(oai, operation);

                                if (policy.isPresent()) {
                                    final Rule rule = new Rule();
                                    rule.setEnabled(true);
                                    rule.setDescription(operation.getSummary() == null ?
                                        (operation.getOperationId() == null ? operation.getDescription() : operation.getOperationId()) :
                                        operation.getSummary());
                                    rule.setMethods(singleton(HttpMethod.valueOf(httpMethod.name())));

                                    io.gravitee.definition.model.Policy defPolicy = new io.gravitee.definition.model.Policy();
                                    defPolicy.setName(policy.get().getName());
                                    defPolicy.setConfiguration(clearNullValues(policy.get().getConfiguration()));
                                    rule.setPolicy(defPolicy);
                                    rules.add(rule);
                                }
                            }
                        });
                    }
                });

                path.setRules(rules);

                return path;
            })
            .collect(toMap(Path::getPath, path -> path)));

        // Path Mappings
        if (apiEntity.getPaths() != null) {
            apiEntity.setPathMappings(apiEntity.getPaths().keySet());
        }

        return apiEntity;
    }

    private String findTagIdByName(Map<String, String> tagMap, String tag) {
        for (Map.Entry<String, String> entry : tagMap.entrySet()) {
            if (entry.getValue().equals(tag)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private List<String> mapServersToEndpoint(List<Server> servers) {
        List<String> endpoints = new ArrayList<>();
        for (Server server : servers) {
            ServerVariables serverVariables = server.getVariables();
            String serverUrl = server.getUrl();
            if (CollectionUtils.isEmpty(serverVariables)) {
                endpoints.add(serverUrl);
            } else {
                List<String> evaluatedUrls = Collections.singletonList(serverUrl);
                for (Map.Entry<String, ServerVariable> serverVar : serverVariables.entrySet()) {
                    evaluatedUrls = evaluateServerUrlsForOneVar(serverVar.getKey(), serverVar.getValue(),
                            evaluatedUrls);
                }
                endpoints.addAll(evaluatedUrls);
            }
        }
        return endpoints;
    }

    private List<String> evaluateServerUrlsForOneVar(String varName, ServerVariable serverVar,
                                                     List<String> templateUrls) {
        List<String> evaluatedUrls = new ArrayList<>();
        for (String templateUrl : templateUrls) {
            Matcher matcher = Pattern.compile("\\{" + varName + "\\}").matcher(templateUrl);
            if (matcher.find()) {
                if (CollectionUtils.isEmpty(serverVar.getEnum()) && serverVar.getDefault() != null) {
                    evaluatedUrls.add(templateUrl.replace(matcher.group(0), serverVar.getDefault()));
                } else {
                    for (String enumValue : serverVar.getEnum()) {
                        evaluatedUrls.add(templateUrl.replace(matcher.group(0), enumValue));
                    }
                }
            }
        }
        return evaluatedUrls;
    }
}
