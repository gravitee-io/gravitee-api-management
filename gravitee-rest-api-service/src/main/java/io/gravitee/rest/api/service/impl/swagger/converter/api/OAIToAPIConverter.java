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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Rule;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.model.api.SwaggerPath;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIDescriptorVisitor;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.gravitee.policy.api.swagger.Policy;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAIToAPIConverter implements SwaggerToApiConverter<OAIDescriptor>, OAIDescriptorVisitor<SwaggerApiEntity> {

    private final Collection<? extends OAIOperationVisitor> visitors;

    public OAIToAPIConverter(Collection<? extends OAIOperationVisitor> visitors) {
        this.visitors = visitors;
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
        apiEntity.setName(oai.getInfo().getTitle());

        String contextPath = null;
        if (!oai.getServers().isEmpty()) {
            List<String> evaluatedServerUrl = mapServersToEndpoint(oai.getServers());
            apiEntity.setEndpoint(evaluatedServerUrl);
            contextPath = evaluatedServerUrl.get(0);
            contextPath = URI.create(contextPath).getPath();
        }

        if (contextPath == null || contextPath.equals("/")) {
            contextPath = apiEntity.getName().replaceAll("\\s+", "").toLowerCase();
        }

        apiEntity.setContextPath(contextPath);

        apiEntity.setDescription(oai.getInfo().getDescription() == null ? "Description of " + apiEntity.getName() :
                oai.getInfo().getDescription());
        apiEntity.setVersion(oai.getInfo().getVersion());

        apiEntity.setPaths(oai.getPaths().entrySet().stream()
                .map(entry -> {
                    final io.gravitee.definition.model.Path path = new Path();
                    path.setPath(entry.getKey().replaceAll("\\{(.[^/]*)\\}", ":$1"));

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
                                        defPolicy.setConfiguration(policy.get().getConfiguration());
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

        if (apiEntity.getPaths() != null) {
            apiEntity.setPathMappings(apiEntity.getPaths().keySet());
        }

        return apiEntity;
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
