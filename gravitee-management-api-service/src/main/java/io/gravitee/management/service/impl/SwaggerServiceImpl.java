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
import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.ImportSwaggerDescriptorEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.api.NewApiEntity;
import io.gravitee.management.service.SwaggerService;
import io.gravitee.management.service.exceptions.SwaggerDescriptorException;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SwaggerServiceImpl implements SwaggerService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SwaggerServiceImpl.class);

    @Value("${swagger.scheme:https}")
    private String defaultScheme;

    static {
        System.setProperty(String.format("%s.trustAll", RemoteUrl.class.getName()), Boolean.TRUE.toString());
        System.setProperty(String.format("%s.trustAll", io.swagger.v3.parser.util.RemoteUrl.class.getName()), Boolean.TRUE.toString());
    }

    @Override
    public NewApiEntity prepare(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        NewApiEntity apiEntity;

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

    private NewApiEntity prepareV1(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        NewApiEntity apiEntity;
        try {
            logger.info("Loading an old Swagger descriptor from {}", swaggerDescriptor.getPayload());
            if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.INLINE) {
                File temp = null;
                try {
                    temp = createTmpSwagger1File(swaggerDescriptor.getPayload());
                    apiEntity = mapSwagger12ToNewApi(new SwaggerCompatConverter().read(temp.getAbsolutePath()));
                } finally {
                    if (temp != null) {
                        temp.delete();
                    }
                }
            } else {
                apiEntity = mapSwagger12ToNewApi(new SwaggerCompatConverter().read(swaggerDescriptor.getPayload()));
            }
        } catch (IOException ioe) {
            logger.error("Can not read old Swagger specification", ioe);
            throw new SwaggerDescriptorException();
        }
        return apiEntity;
    }

    private NewApiEntity prepareV2(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        NewApiEntity apiEntity;
        logger.info("Trying to loading a Swagger descriptor in v2");
        if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.INLINE) {
            apiEntity = mapSwagger12ToNewApi(new SwaggerParser().parse(swaggerDescriptor.getPayload()));
        } else {
            apiEntity = mapSwagger12ToNewApi(new SwaggerParser().read(swaggerDescriptor.getPayload()));
        }
        return apiEntity;
    }

    private NewApiEntity prepareV3(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        NewApiEntity apiEntity;
        logger.info("Trying to loading an OpenAPI descriptor");
        if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.INLINE) {
            apiEntity = mapOpenApiToNewApi(new OpenAPIV3Parser().readContents(swaggerDescriptor.getPayload()));
        } else {
            apiEntity = mapOpenApiToNewApi(new OpenAPIV3Parser().readWithInfo(swaggerDescriptor.getPayload(), null));
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

    private NewApiEntity mapSwagger12ToNewApi(Swagger swagger) {
        if (swagger == null || swagger.getInfo() == null) {
            return null;
        }
        NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName(swagger.getInfo().getTitle());
        apiEntity.setDescription(swagger.getInfo().getDescription());
        apiEntity.setVersion(swagger.getInfo().getVersion());

        String scheme = (swagger.getSchemes() == null || swagger.getSchemes().isEmpty()) ? defaultScheme :
                swagger.getSchemes().iterator().next().toValue();

        apiEntity.setEndpoint(scheme + "://" + swagger.getHost() + swagger.getBasePath());
        apiEntity.setPaths(new ArrayList<>(
                swagger.getPaths().keySet()
                        .stream()
                        .map(path -> path.replaceAll("\\{(.[^/]*)\\}", ":$1"))
                        .collect(Collectors.toList())));

        return apiEntity;
    }

    private NewApiEntity mapOpenApiToNewApi(SwaggerParseResult swagger) {
        if (swagger == null || swagger.getOpenAPI() == null || swagger.getOpenAPI().getInfo() == null) {
            return null;
        }
        NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName(swagger.getOpenAPI().getInfo().getTitle());
        apiEntity.setDescription(swagger.getOpenAPI().getInfo().getDescription());
        apiEntity.setVersion(swagger.getOpenAPI().getInfo().getVersion());

        if (!swagger.getOpenAPI().getServers().isEmpty()) {
            apiEntity.setEndpoint(swagger.getOpenAPI().getServers().get(0).getUrl());
        }

        apiEntity.setPaths(new ArrayList<>(
                swagger.getOpenAPI().getPaths().keySet()
                        .stream()
                        .map(path -> path.replaceAll("\\{(.[^/]*)\\}", ":$1"))
                        .collect(Collectors.toList())));
        return apiEntity;
    }

}
