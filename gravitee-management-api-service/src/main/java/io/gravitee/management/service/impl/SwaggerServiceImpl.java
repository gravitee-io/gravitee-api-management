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
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.service.SwaggerService;
import io.gravitee.management.service.exceptions.SwaggerDescriptorException;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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

    public NewApiEntity prepare(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        Swagger swagger = null;

        switch (swaggerDescriptor.getVersion()) {
            case VERSION_1_0:
                try {
                    logger.info("Loading an old Swagger descriptor from {}", swaggerDescriptor.getPayload());

                    // For spec < 2.0, only read by url is possible
                    swagger = new SwaggerCompatConverter().read(swaggerDescriptor.getPayload());
                } catch (IOException ioe) {
                    logger.error("Can not read old Swagger specification", ioe);
                    throw new SwaggerDescriptorException();
                }
            case VERSION_2_0:
                if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.INLINE) {
                    logger.info("Loading an inline Swagger descriptor");
                    swagger = new SwaggerParser().parse(swaggerDescriptor.getPayload());
                } else if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.URL) {
                    logger.info("Loading a Swagger descriptor from URL: ", swaggerDescriptor.getPayload());
                    swagger = new SwaggerParser().read(swaggerDescriptor.getPayload());
                }
        }

        if (swagger == null) {
            throw new SwaggerDescriptorException();
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

    @Override
    public void transform(PageEntity page) {
        Swagger swagger;

        // Create temporary file for Swagger parser (only for descriptor version < 2.x)
        File temp = null;
        String fileName = "gio_swagger_" + System.currentTimeMillis();
        BufferedWriter bw = null;
        FileWriter out = null;

        try {
            temp = File.createTempFile(fileName, ".tmp");
            out = new FileWriter(temp);
            bw = new BufferedWriter(out);
            bw.write(page.getContent());
            bw.close();

            swagger = new SwaggerCompatConverter().read(temp.getAbsolutePath());
            if (swagger == null) {
                swagger = new SwaggerParser().parse(page.getContent());
            }
        } catch (IOException ioe) {
            // Fallback to the new parser
            swagger = new SwaggerParser().parse(page.getContent());
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
            if (temp != null) {
                temp.delete();
            }
        }

        if (swagger == null) {
            throw new SwaggerDescriptorException();
        }

        if (page.getConfiguration() != null &&
                page.getConfiguration().getTryItURL() != null) {
            URI newURI = URI.create(page.getConfiguration().getTryItURL());

            swagger.setSchemes(Collections.singletonList(Scheme.forValue(newURI.getScheme())));
            swagger.setHost((newURI.getPort() != -1) ? newURI.getHost() + ':' + newURI.getPort() : newURI.getHost());
            swagger.setBasePath((newURI.getRawPath().isEmpty()) ? "/" : newURI.getRawPath());
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
