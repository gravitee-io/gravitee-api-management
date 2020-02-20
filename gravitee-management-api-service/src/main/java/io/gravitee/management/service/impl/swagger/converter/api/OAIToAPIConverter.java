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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.management.model.api.NewSwaggerApiEntity;
import io.gravitee.management.model.api.SwaggerPath;
import io.gravitee.management.model.api.SwaggerVerb;
import io.gravitee.management.service.swagger.OAIDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAIToAPIConverter implements SwaggerToApiConverter<OAIDescriptor> {

    private final boolean includePolicies;

    private final ObjectMapper mapper = new ObjectMapper();

    public OAIToAPIConverter(boolean includePolicies) {
        this.includePolicies = includePolicies;
    }

    @Override
    public NewSwaggerApiEntity convert(OAIDescriptor descriptor) {
        if (descriptor == null || descriptor.getSpecification() == null) {
            return null;
        }

        OpenAPI oai = descriptor.getSpecification();
        final NewSwaggerApiEntity apiEntity = new NewSwaggerApiEntity();
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
                    final SwaggerPath swaggerPath = new SwaggerPath();
                    swaggerPath.setPath(entry.getKey().replaceAll("\\{(.[^/]*)\\}", ":$1"));
                    if (includePolicies) {
                        final ArrayList<SwaggerVerb> verbs = new ArrayList<>();
                        final PathItem pathItem = entry.getValue();
                        if (pathItem.getGet() != null)
                            verbs.add(getSwaggerVerb(oai, pathItem.getGet(), "GET"));
                        if (pathItem.getPut() != null)
                            verbs.add(getSwaggerVerb(oai, pathItem.getPut(), "PUT"));
                        if (pathItem.getPost() != null)
                            verbs.add(getSwaggerVerb(oai, pathItem.getPost(), "POST"));
                        if (pathItem.getDelete() != null)
                            verbs.add(getSwaggerVerb(oai, pathItem.getDelete(), "DELETE"));
                        if (pathItem.getPatch() != null)
                            verbs.add(getSwaggerVerb(oai, pathItem.getPatch(), "PATCH"));
                        if (pathItem.getHead() != null)
                            verbs.add(getSwaggerVerb(oai, pathItem.getHead(), "HEAD"));
                        if (pathItem.getOptions() != null)
                            verbs.add(getSwaggerVerb(oai, pathItem.getOptions(), "OPTIONS"));
                        if (pathItem.getTrace() != null)
                            verbs.add(getSwaggerVerb(oai, pathItem.getTrace(), "TRACE"));
                        swaggerPath.setVerbs(verbs);
                    }
                    return swaggerPath;
                })
                .collect(toCollection(ArrayList::new)));
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
                List<String> evaluatedUrls = Arrays.asList(serverUrl);
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

    private SwaggerVerb getSwaggerVerb(final OpenAPI oai, final Operation operation, final String verb) {
        final SwaggerVerb swaggerVerb = new SwaggerVerb();
        swaggerVerb.setVerb(verb);
        swaggerVerb.setDescription(operation.getSummary() == null ?
                (operation.getOperationId() == null ? operation.getDescription() : operation.getOperationId()) :
                operation.getSummary());
        final Map.Entry<String, ApiResponse> responseEntry = operation.getResponses().entrySet().iterator().next();
        swaggerVerb.setResponseStatus(responseEntry.getKey());
        if (responseEntry.getValue().getContent() != null) {
            final Map.Entry<String, MediaType> contentByMediatype =
                    responseEntry.getValue().getContent().entrySet().iterator().next();
            swaggerVerb.setContentType(contentByMediatype.getKey());
            final io.swagger.v3.oas.models.media.MediaType mediaType = contentByMediatype.getValue();
            if (mediaType.getExample() != null) {
                swaggerVerb.setResponseProperties(mapper.convertValue(mediaType.getExample(), Map.class));
            } else if (mediaType.getExamples() != null) {
                final Map.Entry<String, Example> next = mediaType.getExamples().entrySet().iterator().next();
                swaggerVerb.setResponseProperties(singletonMap(next.getKey(), next.getValue().getValue()));
            } else {
                final Schema responseSchema = mediaType.getSchema();
                if (responseSchema != null) {
                    if (responseSchema instanceof ArraySchema) {
                        final ArraySchema arraySchema = (ArraySchema) responseSchema;
                        processResponseSchema(oai, swaggerVerb, "array", arraySchema.getItems());
                    } else {
                        processResponseSchema(oai, swaggerVerb, responseSchema.getType() == null ? "object" :
                                responseSchema.getType(), responseSchema);
                    }
                }
            }
        }
        return swaggerVerb;
    }

    private void processResponseSchema(OpenAPI oai, SwaggerVerb swaggerVerb, String type, Schema responseSchema) {
        if (responseSchema.getProperties() == null) {
            swaggerVerb.setArray("array".equals(type));
            if (responseSchema.getAdditionalProperties() != null) {
                swaggerVerb.setResponseProperties(Collections.singletonMap("additionalProperty", ((ObjectSchema) responseSchema.getAdditionalProperties()).getType()));
            } else if (responseSchema.get$ref() != null) {
                if (!"array".equals(type)) {
                    swaggerVerb.setArray(isRefArray(oai, responseSchema.get$ref()));
                }
                swaggerVerb.setResponseProperties(getResponseFromSimpleRef(oai, responseSchema.get$ref()));
            } else {
                swaggerVerb.setResponseProperties(singletonMap(responseSchema.getType(), getResponsePropertiesFromType(responseSchema.getType())));
            }
        } else {
            swaggerVerb.setResponseProperties(getResponseProperties(oai, responseSchema.getProperties()));
        }
    }

    private boolean isRefArray(OpenAPI oai, final String ref) {
        final String simpleRef = ref.substring(ref.lastIndexOf('/') + 1);
        final Schema schema = oai.getComponents().getSchemas().get(simpleRef);
        return schema instanceof ArraySchema;
    }

    private Object getResponseFromSimpleRef(final OpenAPI oai, String ref) {
        if (ref == null){
            return null;
        }
        final String simpleRef = ref.substring(ref.lastIndexOf('/') + 1);
        final Schema schema = oai.getComponents().getSchemas().get(simpleRef);
        return getSchemaValue(oai, schema);
    }

    private Map<String, Object> getResponseProperties(final OpenAPI oai, final Map<String, Schema> properties) {
        if (properties == null) {
            return null;
        }
        return properties.entrySet()
                .stream()
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                e -> this.getSchemaValue(oai, e.getValue())));
    }

    private Object getSchemaValue(final OpenAPI oai, Schema schema) {
        if (schema == null) {
            return null;
        }

        final Object example = schema.getExample();
        if (example != null) {
            return example;
        }

        final List enums = schema.getEnum();
        if (enums != null) {
            return enums.get(0);
        }

        if (schema instanceof ObjectSchema) {
            return getResponseProperties(oai, schema.getProperties());
        }

        if (schema instanceof ArraySchema) {
            Schema<?> items = ((ArraySchema) schema).getItems();
            Object sample = items.getExample();
            if (sample != null) {
                return singletonList(sample);
            }

            if (items.getEnum() != null) {
                return singletonList(items.getEnum().get(0));
            }

            if (items.get$ref() != null) {
                return getResponseFromSimpleRef(oai, items.get$ref());
            }

            return singleton(getResponsePropertiesFromType(items.getType()));
        }

        if (schema instanceof ComposedSchema) {
            final Map<String, Object> response = new HashMap<>();
            ((ComposedSchema) schema).getAllOf().forEach(composedSchema -> {
                if (composedSchema.get$ref() != null) {
                    Object responseFromSimpleRef = getResponseFromSimpleRef(oai, composedSchema.get$ref());
                    if (responseFromSimpleRef instanceof Map) {
                        response.putAll((Map) responseFromSimpleRef);
                    }
                }
                if (composedSchema.getProperties() != null) {
                    response.putAll(getResponseProperties(oai, composedSchema.getProperties()));
                }
            });
            return response;
        }

        if (schema.getProperties() != null) {
            return getResponseProperties(oai, schema.getProperties());
        }

        if (schema.get$ref() != null) {
            return getResponseFromSimpleRef(oai, schema.get$ref());
        }

        return getResponsePropertiesFromType(schema.getType());
    }

    private Object getResponsePropertiesFromType(final String responseType) {
        if (responseType == null) {
            return null;
        }
        final Random random = new Random();
        switch (responseType) {
            case "string":
                return "Mocked string";
            case "boolean":
                return random.nextBoolean();
            case "integer":
                return random.nextInt(1000);
            case "number":
                return random.nextDouble();
            case "array":
                return singletonList(getResponsePropertiesFromType("string"));
            default:
                return emptyMap();
        }
    }
}
