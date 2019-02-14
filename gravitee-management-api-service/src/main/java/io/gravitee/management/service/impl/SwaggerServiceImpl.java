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
package io.gravitee.management.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.ImportSwaggerDescriptorEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.api.NewSwaggerApiEntity;
import io.gravitee.management.model.api.SwaggerPath;
import io.gravitee.management.model.api.SwaggerVerb;
import io.gravitee.management.service.SwaggerService;
import io.gravitee.management.service.exceptions.SwaggerDescriptorException;
import io.swagger.models.*;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SwaggerServiceImpl implements SwaggerService {

    private final Logger logger = LoggerFactory.getLogger(SwaggerServiceImpl.class);

    @Value("${swagger.scheme:https}")
    private String defaultScheme;
    @Inject
    private ObjectMapper mapper;

    static {
        System.setProperty(String.format("%s.trustAll", RemoteUrl.class.getName()), Boolean.TRUE.toString());
        System.setProperty(String.format("%s.trustAll", io.swagger.v3.parser.util.RemoteUrl.class.getName()), Boolean.TRUE.toString());
    }

    @Override
    public NewSwaggerApiEntity prepare(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        NewSwaggerApiEntity apiEntity;

        // try to read swagger in version 2
        apiEntity = prepareV2(swaggerDescriptor);

        // try to read swagger in version 3 (openAPI)
        if (apiEntity == null) {
            apiEntity = prepareV3(swaggerDescriptor);
        }

        // try to read swagger in version 1
        if (apiEntity == null) {
            apiEntity = prepareV1(swaggerDescriptor);
        }

        if (apiEntity == null) {
            throw new SwaggerDescriptorException();
        }

        return apiEntity;
    }

    private NewSwaggerApiEntity prepareV1(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        NewSwaggerApiEntity apiEntity;
        try {
            logger.info("Loading an old Swagger descriptor from {}", swaggerDescriptor.getPayload());
            if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.INLINE) {
                File temp = null;
                try {
                    temp = createTmpSwagger1File(swaggerDescriptor.getPayload());
                    apiEntity = mapSwagger12ToNewApi(new SwaggerCompatConverter().read(temp.getAbsolutePath()), swaggerDescriptor.isWithPolicyMocks());
                } finally {
                    if (temp != null) {
                        temp.delete();
                    }
                }
            } else {
                apiEntity = mapSwagger12ToNewApi(new SwaggerCompatConverter().read(swaggerDescriptor.getPayload()), swaggerDescriptor.isWithPolicyMocks());
            }
        } catch (IOException ioe) {
            logger.error("Can not read old Swagger specification", ioe);
            throw new SwaggerDescriptorException();
        }
        return apiEntity;
    }

    private NewSwaggerApiEntity prepareV2(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        NewSwaggerApiEntity apiEntity;
        logger.info("Trying to loading a Swagger descriptor in v2");
        if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.INLINE) {
            apiEntity = mapSwagger12ToNewApi(new SwaggerParser().parse(swaggerDescriptor.getPayload()), swaggerDescriptor.isWithPolicyMocks());
        } else {
            apiEntity = mapSwagger12ToNewApi(new SwaggerParser().read(swaggerDescriptor.getPayload()), swaggerDescriptor.isWithPolicyMocks());
        }
        return apiEntity;
    }

    private NewSwaggerApiEntity prepareV3(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        NewSwaggerApiEntity apiEntity;
        logger.info("Trying to loading an OpenAPI descriptor");
        if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.INLINE) {
            apiEntity = mapOpenApiToNewApi(new OpenAPIV3Parser().readContents(swaggerDescriptor.getPayload()), swaggerDescriptor.isWithPolicyMocks());
        } else {
            apiEntity = mapOpenApiToNewApi(new OpenAPIV3Parser().readWithInfo(swaggerDescriptor.getPayload(), null), swaggerDescriptor.isWithPolicyMocks());
        }
        return apiEntity;
    }

    @Override
    public void transform(final PageEntity page) {
        if (page.getContent() != null
                && page.getConfiguration() != null
                && page.getConfiguration().get("tryItURL") != null
                && !page.getConfiguration().get("tryItURL").isEmpty()) {

            Object swagger = transformV2(page.getContent(), page.getConfiguration());

            if (swagger == null) {
                swagger = transformV1(page.getContent(), page.getConfiguration());
            }

            if (swagger == null) {
                swagger = transformV3(page.getContent(), page.getConfiguration());
            }

            if (swagger == null) {
                throw new SwaggerDescriptorException();
            }

            if (page.getContentType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
                try {
                    page.setContent(Json.pretty().writeValueAsString(swagger));
                } catch (JsonProcessingException e) {
                    logger.error("Unexpected error", e);
                }
            } else {
                try {
                    page.setContent(Yaml.pretty().writeValueAsString(swagger));
                } catch (JsonProcessingException e) {
                    logger.error("Unexpected error", e);
                }
            }
        }
    }

    private File createTmpSwagger1File(String content) {
        // Create temporary file for Swagger parser (only for descriptor version < 2.x)
        File temp = null;
        String fileName = "gio_swagger_" + System.currentTimeMillis();
        BufferedWriter bw = null;
        FileWriter out = null;
        Swagger swagger = null;
        try {
            temp = File.createTempFile(fileName, ".tmp");
            out = new FileWriter(temp);
            bw = new BufferedWriter(out);
            bw.write(content);
            bw.close();
        } catch (IOException ioe) {
            // Fallback to the new parser
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
        return temp;
    }

    private Swagger transformV1(String content, Map<String, String> config) {
        // Create temporary file for Swagger parser (only for descriptor version < 2.x)
        File temp = null;
        Swagger swagger = null;
        try {
            temp = createTmpSwagger1File(content);
            swagger = new SwaggerCompatConverter().read(temp.getAbsolutePath());
            if (swagger != null && config != null && config.get("tryItURL") != null) {
                URI newURI = URI.create(config.get("tryItURL"));
                swagger.setSchemes(Collections.singletonList(Scheme.forValue(newURI.getScheme())));
                swagger.setHost((newURI.getPort() != -1) ? newURI.getHost() + ':' + newURI.getPort() : newURI.getHost());
                swagger.setBasePath((newURI.getRawPath().isEmpty()) ? "/" : newURI.getRawPath());
            }
        } catch (IOException ioe) {
            // Fallback to the new parser
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
        return swagger;
    }

    private Swagger transformV2(String content, Map<String, String> config) {
        Swagger swagger = new SwaggerParser().parse(content);
        if (swagger != null && config != null && config.get("tryItURL") != null) {
            URI newURI = URI.create(config.get("tryItURL"));
            swagger.setSchemes(Collections.singletonList(Scheme.forValue(newURI.getScheme())));
            swagger.setHost((newURI.getPort() != -1) ? newURI.getHost() + ':' + newURI.getPort() : newURI.getHost());
            swagger.setBasePath((newURI.getRawPath().isEmpty()) ? "/" : newURI.getRawPath());
        }
        return swagger;
    }

    private OpenAPI transformV3(String content, Map<String, String> config) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, null);
        if (result != null && config != null && config.get("tryItURL") != null) {
            URI newURI = URI.create(config.get("tryItURL"));
            result.getOpenAPI().getServers().forEach(server -> {
                try {
                    server.setUrl(new URI(newURI.getScheme(),
                            newURI.getUserInfo(),
                            newURI.getHost(),
                            newURI.getPort(),
                            newURI.getPath(),
                            newURI.getQuery(),
                            newURI.getFragment()).toString());
                } catch (URISyntaxException e) {
                    logger.error(e.getMessage(), e);
                }
            });
        }
        if (result != null) {
            return result.getOpenAPI();
        } else {
            return null;
        }
    }

    private NewSwaggerApiEntity mapSwagger12ToNewApi(final Swagger swagger, final boolean isWithPolicyMocks) {
        if (swagger == null || swagger.getInfo() == null) {
            return null;
        }
        final NewSwaggerApiEntity apiEntity = new NewSwaggerApiEntity();
        apiEntity.setName(swagger.getInfo().getTitle());
        apiEntity.setContextPath(apiEntity.getName().replaceAll("\\s+", "").toLowerCase());
        apiEntity.setDescription(swagger.getInfo().getDescription() == null ? "Description of " + apiEntity.getName() :
                swagger.getInfo().getDescription());
        apiEntity.setVersion(swagger.getInfo().getVersion());

        String scheme = (swagger.getSchemes() == null || swagger.getSchemes().isEmpty()) ? defaultScheme :
                swagger.getSchemes().iterator().next().toValue();

        apiEntity.setEndpoint(scheme + "://" + swagger.getHost() + swagger.getBasePath());
        apiEntity.setPaths(swagger.getPaths().entrySet().stream()
                .map(entry -> {
                    final SwaggerPath swaggerPath = new SwaggerPath();
                    swaggerPath.setPath(entry.getKey().replaceAll("\\{(.[^/]*)\\}", ":$1"));
                    if (isWithPolicyMocks) {
                        final ArrayList<SwaggerVerb> verbs = new ArrayList<>();
                        entry.getValue().getOperationMap().forEach((key, operation) -> {
                            final SwaggerVerb swaggerVerb = new SwaggerVerb();
                            swaggerVerb.setVerb(key.name());
                            swaggerVerb.setDescription(operation.getSummary() == null ?
                                    (operation.getOperationId() == null ? operation.getDescription() : operation.getOperationId()) :
                                    operation.getSummary());
                            final Map.Entry<String, Response> responseEntry = operation.getResponses().entrySet().iterator().next();
                            swaggerVerb.setResponseStatus(responseEntry.getKey());
                            final Model responseSchema = responseEntry.getValue().getResponseSchema();
                            if (responseSchema != null) {
                                if (responseSchema instanceof ArrayModel) {
                                    final ArrayModel arrayModel = (ArrayModel) responseSchema;
                                    swaggerVerb.setResponseType(arrayModel.getType());
                                    if (arrayModel.getItems() instanceof RefProperty) {
                                        final String simpleRef = ((RefProperty) arrayModel.getItems()).getSimpleRef();
                                        swaggerVerb.setResponseProperties(getResponseFromSimpleRef(swagger, simpleRef));
                                    } else if (arrayModel.getItems() instanceof ObjectProperty) {
                                        swaggerVerb.setResponseProperties(getResponseProperties(swagger, ((ObjectProperty) arrayModel.getItems()).getProperties()));
                                    }
                                } else if (responseSchema instanceof RefModel) {
                                    swaggerVerb.setResponseType("object");
                                    final String simpleRef = ((RefModel) responseSchema).getSimpleRef();
                                    swaggerVerb.setResponseProperties(getResponseFromSimpleRef(swagger, simpleRef));
                                } else if (responseSchema instanceof ModelImpl) {
                                    final ModelImpl model = (ModelImpl) responseSchema;
                                    swaggerVerb.setResponseType(model.getType());
                                    if ("object".equals(model.getType())) {
                                        if (model.getAdditionalProperties() != null) {
                                            swaggerVerb.setResponseProperties(Collections.singletonMap("additionalProperty", model.getAdditionalProperties().getType()));
                                        }
                                    } else {
                                        swaggerVerb.setResponseType(model.getType());
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

    private NewSwaggerApiEntity mapOpenApiToNewApi(final SwaggerParseResult swagger, final boolean isWithPolicyMocks) {
        if (swagger == null || swagger.getOpenAPI() == null || swagger.getOpenAPI().getInfo() == null) {
            return null;
        }
        final NewSwaggerApiEntity apiEntity = new NewSwaggerApiEntity();
        apiEntity.setName(swagger.getOpenAPI().getInfo().getTitle());
        apiEntity.setContextPath(apiEntity.getName().replaceAll("\\s+", "").toLowerCase());
        apiEntity.setDescription(swagger.getOpenAPI().getInfo().getDescription() == null ? "Description of " + apiEntity.getName() :
                swagger.getOpenAPI().getInfo().getDescription());
        apiEntity.setVersion(swagger.getOpenAPI().getInfo().getVersion());

        if (!swagger.getOpenAPI().getServers().isEmpty()) {
            apiEntity.setEndpoint(swagger.getOpenAPI().getServers().get(0).getUrl());
        }
        apiEntity.setPaths(swagger.getOpenAPI().getPaths().entrySet().stream()
                .map(entry -> {
                    final SwaggerPath swaggerPath = new SwaggerPath();
                    swaggerPath.setPath(entry.getKey().replaceAll("\\{(.[^/]*)\\}", ":$1"));
                    if (isWithPolicyMocks) {
                        final ArrayList<SwaggerVerb> verbs = new ArrayList<>();
                        final PathItem pathItem = entry.getValue();
                        if (pathItem.getGet() != null)
                            verbs.add(getSwaggerVerb(swagger, pathItem.getGet(), "GET"));
                        if (pathItem.getPut() != null)
                            verbs.add(getSwaggerVerb(swagger, pathItem.getPut(), "PUT"));
                        if (pathItem.getPost() != null)
                            verbs.add(getSwaggerVerb(swagger, pathItem.getPost(), "POST"));
                        if (pathItem.getDelete() != null)
                            verbs.add(getSwaggerVerb(swagger, pathItem.getDelete(), "DELETE"));
                        if (pathItem.getPatch() != null)
                            verbs.add(getSwaggerVerb(swagger, pathItem.getPatch(), "PATCH"));
                        if (pathItem.getHead() != null)
                            verbs.add(getSwaggerVerb(swagger, pathItem.getHead(), "HEAD"));
                        if (pathItem.getOptions() != null)
                            verbs.add(getSwaggerVerb(swagger, pathItem.getOptions(), "OPTIONS"));
                        if (pathItem.getTrace() != null)
                            verbs.add(getSwaggerVerb(swagger, pathItem.getTrace(), "TRACE"));
                        swaggerPath.setVerbs(verbs);
                    }
                    return swaggerPath;
                })
                .collect(toCollection(ArrayList::new)));
        return apiEntity;
    }

    private SwaggerVerb getSwaggerVerb(final SwaggerParseResult swagger, final Operation operation, final String verb) {
        final SwaggerVerb swaggerVerb = new SwaggerVerb();
        swaggerVerb.setVerb(verb);
        swaggerVerb.setDescription(operation.getSummary() == null ?
                (operation.getOperationId() == null ? operation.getDescription() : operation.getOperationId()) :
                operation.getSummary());
        final Map.Entry<String, ApiResponse> responseEntry = operation.getResponses().entrySet().iterator().next();
        swaggerVerb.setResponseStatus(responseEntry.getKey());
        if (responseEntry.getValue().getContent() != null) {
            final io.swagger.v3.oas.models.media.MediaType mediaType =
                    responseEntry.getValue().getContent().entrySet().iterator().next().getValue();
            if (mediaType.getExample() != null) {
                swaggerVerb.setResponseExample(mapper.convertValue(mediaType.getExample(), Map.class));
            } else if (mediaType.getExamples() != null) {
                swaggerVerb.setResponseExample(mediaType.getExamples().entrySet().iterator().next().getValue().getValue());
            } else {
                final Schema responseSchema = mediaType.getSchema();
                if (responseSchema != null) {
                    if (responseSchema instanceof ArraySchema) {
                        final ArraySchema arraySchema = (ArraySchema) responseSchema;
                        processResponseSchema(swagger, swaggerVerb, "array", arraySchema.getItems());
                    } else {
                        processResponseSchema(swagger, swaggerVerb, responseSchema.getType() == null ? "object" :
                                responseSchema.getType(), responseSchema);
                    }
                }
            }
        }
        return swaggerVerb;
    }

    private void processResponseSchema(SwaggerParseResult swagger, SwaggerVerb swaggerVerb, String type, Schema responseSchema) {
        if (responseSchema.getProperties() == null) {
            swaggerVerb.setResponseType(type);
            if (responseSchema.getAdditionalProperties() != null) {
                swaggerVerb.setResponseProperties(Collections.singletonMap("additionalProperty", ((ObjectSchema) responseSchema.getAdditionalProperties()).getType()));
            } else if (responseSchema.get$ref() != null) {
                if (!"array".equals(type)) {
                    swaggerVerb.setResponseType(getTypeByRef(swagger, responseSchema.get$ref()));
                }
                swaggerVerb.setResponseProperties(getResponseFromSimpleRef(swagger, responseSchema.get$ref()));
            }
        } else {
            swaggerVerb.setResponseExample(getResponseExample(responseSchema.getProperties()));
        }
    }

    private String getTypeByRef(SwaggerParseResult swagger, String ref) {
        final String simpleRef = ref.substring(ref.lastIndexOf('/') + 1);
        return swagger.getOpenAPI().getComponents().getSchemas().get(simpleRef).getType();
    }

    private Map<String, Object> getResponseFromSimpleRef(SwaggerParseResult swagger, String ref) {
        final String simpleRef = ref.substring(ref.lastIndexOf('/') + 1);
        final Schema schema = swagger.getOpenAPI().getComponents().getSchemas().get(simpleRef);

        if (schema instanceof ArraySchema) {
            return getResponseFromSimpleRef(swagger, ((ArraySchema) schema).getItems().get$ref());
        } else if (schema instanceof ComposedSchema) {
            final Map<String, Object> response = new HashMap<>();
            ((ComposedSchema) schema).getAllOf().forEach(composedSchema -> {
                if (composedSchema.get$ref() != null) {
                    response.putAll(getResponseFromSimpleRef(swagger, composedSchema.get$ref()));
                }
                if (composedSchema.getProperties() != null) {
                    response.putAll(getResponseProperties(swagger, composedSchema.getProperties()));
                }
            });
            return response;
        }

        if (schema == null || schema.getProperties() == null) {
            return emptyMap();
        }
        return getResponseProperties(swagger, schema.getProperties());
    }

    private Map<String, Object> getResponseProperties(final SwaggerParseResult swagger, final Map<String, Schema> properties) {
        return properties.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
            if (e.getValue().getType() != null) {
                return e.getValue().getType();
            } else {
                return getResponseFromSimpleRef(swagger, e.getValue().get$ref());
            }
        }));
    }

    private Map<String, Object> getResponseExample(final Map<String, Schema> properties) {
        return properties.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().getExample()));
    }
}
