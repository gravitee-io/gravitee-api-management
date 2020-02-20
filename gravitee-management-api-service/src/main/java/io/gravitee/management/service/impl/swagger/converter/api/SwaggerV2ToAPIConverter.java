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
package io.gravitee.management.service.impl.swagger.converter.api;

import io.gravitee.management.model.api.NewSwaggerApiEntity;
import io.gravitee.management.model.api.SwaggerPath;
import io.gravitee.management.model.api.SwaggerVerb;
import io.gravitee.management.service.swagger.SwaggerV2Descriptor;
import io.swagger.models.*;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SwaggerV2ToAPIConverter implements SwaggerToApiConverter<SwaggerV2Descriptor> {

    private final static String DEFAULT_HTTPS_SCHEME = "https";

    private final boolean includePolicies;

    private final String defaultScheme;

    public SwaggerV2ToAPIConverter(boolean includePolicies) {
        this(includePolicies, DEFAULT_HTTPS_SCHEME);
    }

    public SwaggerV2ToAPIConverter(boolean includePolicies, String defaultScheme) {
        this.includePolicies = includePolicies;
        this.defaultScheme = defaultScheme;
    }

    @Override
    public NewSwaggerApiEntity convert(SwaggerV2Descriptor descriptor) {
        if (descriptor == null || descriptor.getSpecification() == null || descriptor.getSpecification().getInfo() == null) {
            return null;
        }

        Swagger swagger = descriptor.getSpecification();
        final NewSwaggerApiEntity apiEntity = new NewSwaggerApiEntity();
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

        apiEntity.setEndpoint(Arrays.asList(scheme + "://" + swagger.getHost() + swagger.getBasePath()));
        apiEntity.setPaths(swagger.getPaths().entrySet().stream()
                .map(entry -> {
                    final SwaggerPath swaggerPath = new SwaggerPath();
                    swaggerPath.setPath(entry.getKey().replaceAll("\\{(.[^/]*)\\}", ":$1"));
                    if (includePolicies) {
                        final ArrayList<SwaggerVerb> verbs = new ArrayList<>();
                        entry.getValue().getOperationMap().forEach((key, operation) -> {
                            final SwaggerVerb swaggerVerb = new SwaggerVerb();
                            swaggerVerb.setVerb(key.name());
                            swaggerVerb.setDescription(operation.getSummary() == null ?
                                    (operation.getOperationId() == null ? operation.getDescription() : operation.getOperationId()) :
                                    operation.getSummary());
                            final Map.Entry<String, Response> responseEntry = operation.getResponses().entrySet().iterator().next();
                            swaggerVerb.setResponseStatus(responseEntry.getKey());
                            if (operation.getProduces() != null && !operation.getProduces().isEmpty()) {
                                swaggerVerb.setContentType(operation.getProduces().get(0));
                            }
                            final Model responseSchema = responseEntry.getValue().getResponseSchema();
                            if (responseSchema != null) {
                                if (responseSchema instanceof ArrayModel) {
                                    final ArrayModel arrayModel = (ArrayModel) responseSchema;
                                    swaggerVerb.setArray(true);
                                    if (arrayModel.getItems() instanceof RefProperty) {
                                        final String simpleRef = ((RefProperty) arrayModel.getItems()).getSimpleRef();
                                        swaggerVerb.setResponseProperties(getResponseFromSimpleRef(swagger, simpleRef));
                                    } else if (arrayModel.getItems() instanceof ObjectProperty) {
                                        swaggerVerb.setResponseProperties(getResponseProperties(swagger, ((ObjectProperty) arrayModel.getItems()).getProperties()));
                                    }
                                } else if (responseSchema instanceof RefModel) {
                                    final String simpleRef = ((RefModel) responseSchema).getSimpleRef();
                                    swaggerVerb.setResponseProperties(getResponseFromSimpleRef(swagger, simpleRef));
                                } else if (responseSchema instanceof ModelImpl) {
                                    final ModelImpl model = (ModelImpl) responseSchema;
                                    swaggerVerb.setArray("array".equals(model.getType()));
                                    if ("object".equals(model.getType())) {
                                        if (model.getAdditionalProperties() != null) {
                                            swaggerVerb.setResponseProperties(Collections.singletonMap("additionalProperty", model.getAdditionalProperties().getType()));
                                        }
                                    }
                                }
                            }
                            verbs.add(swaggerVerb);
                        });
                        swaggerPath.setVerbs(verbs);
                    }
                    return swaggerPath;
                })
                .collect(toCollection(ArrayList::new)));
        return apiEntity;
    }

    private Map<String, Object> getResponseFromSimpleRef(Swagger swagger, String simpleRef) {
        final Map<String, Property> properties = swagger.getDefinitions().get(simpleRef).getProperties();
        if (properties == null) {
            return emptyMap();
        }
        return getResponseProperties(swagger, properties);
    }

    private Map<String, Object> getResponseProperties(Swagger swagger, Map<String, Property> properties) {
        return properties.entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> {
                    final Property property = e.getValue();
                    if (property instanceof RefProperty) {
                        return this.getResponseFromSimpleRef(swagger, ((RefProperty) property).getSimpleRef());
                    }
                    return property.getType();
                }));
    }
}
