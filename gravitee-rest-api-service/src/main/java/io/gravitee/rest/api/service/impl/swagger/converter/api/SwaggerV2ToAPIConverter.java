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
import io.gravitee.rest.api.service.impl.swagger.visitor.v2.SwaggerDescriptorVisitor;
import io.gravitee.rest.api.service.impl.swagger.visitor.v2.SwaggerOperationVisitor;
import io.gravitee.rest.api.service.swagger.SwaggerV2Descriptor;
import io.gravitee.policy.api.swagger.Policy;
import io.swagger.models.Swagger;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toMap;

import static io.gravitee.rest.api.service.validator.PolicyCleaner.clearNullValues;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SwaggerV2ToAPIConverter implements SwaggerToApiConverter<SwaggerV2Descriptor>, SwaggerDescriptorVisitor<SwaggerApiEntity> {

    private final static String DEFAULT_HTTPS_SCHEME = "https";

    private final Collection<? extends SwaggerOperationVisitor> visitors;

    private final String defaultScheme;

    public SwaggerV2ToAPIConverter(Collection<? extends SwaggerOperationVisitor> visitors) {
        this(visitors, DEFAULT_HTTPS_SCHEME);
    }

    public SwaggerV2ToAPIConverter(Collection<? extends SwaggerOperationVisitor> visitors, String defaultScheme) {
        this.visitors = visitors;
        this.defaultScheme = defaultScheme;
    }

    @Override
    public SwaggerApiEntity visit(Swagger swagger) {
        final SwaggerApiEntity apiEntity = new SwaggerApiEntity();
        apiEntity.setName(swagger.getInfo().getTitle());

        if (swagger.getBasePath() != null && !swagger.getBasePath().isEmpty()) {
            apiEntity.setContextPath(swagger.getBasePath());
        } else {
            apiEntity.setContextPath(apiEntity.getName().replaceAll("\\s+", "").toLowerCase());
        }

        apiEntity.setDescription(swagger.getInfo().getDescription() == null ? "Description of " + apiEntity.getName() :
                swagger.getInfo().getDescription());
        apiEntity.setVersion(swagger.getInfo().getVersion());

        String scheme = (swagger.getSchemes() == null || swagger.getSchemes().isEmpty()) ? defaultScheme :
                swagger.getSchemes().iterator().next().toValue();

        apiEntity.setEndpoint(Collections.singletonList(scheme + "://" + swagger.getHost() + swagger.getBasePath()));
        apiEntity.setPaths(swagger.getPaths().entrySet().stream()
                .map(entry -> {
                    final io.gravitee.definition.model.Path path = new Path();
                    path.setPath(entry.getKey().replaceAll("\\{(.[^/]*)\\}", ":$1"));
                    List<Rule> rules = new ArrayList<>();

                    entry.getValue().getOperationMap().forEach(new BiConsumer<io.swagger.models.HttpMethod, io.swagger.models.Operation>() {

                        @Override
                        public void accept(io.swagger.models.HttpMethod httpMethod, io.swagger.models.Operation operation) {
                            visitors.forEach(new Consumer<SwaggerOperationVisitor>() {
                                @Override
                                public void accept(SwaggerOperationVisitor operationVisitor) {
                                    // Consider only policy visitor for now
                                    Optional<Policy> policy = (Optional<Policy>) operationVisitor.visit(swagger, operation);

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

        if (apiEntity.getPaths() != null) {
            apiEntity.setPathMappings(apiEntity.getPaths().keySet());
        }

        return apiEntity;
    }

    @Override
    public SwaggerApiEntity convert(SwaggerV2Descriptor descriptor) {
        if (descriptor == null || descriptor.getSpecification() == null || descriptor.getSpecification().getInfo() == null) {
            return null;
        }

        return visit(descriptor.getSpecification());
    }
}
