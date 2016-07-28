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

import io.gravitee.management.model.ImportSwaggerDescriptorEntity;
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.service.SwaggerService;
import io.gravitee.management.service.exceptions.SwaggerDescriptorException;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.parser.SwaggerParser;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
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
    private final Logger LOGGER = LoggerFactory.getLogger(SwaggerServiceImpl.class);

    @Value("${swagger.scheme:https}")
    private String defaultScheme;

    public NewApiEntity prepare(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        Swagger swagger = null;

        switch (swaggerDescriptor.getVersion()) {
            case VERSION_1_0:
                try {
                    LOGGER.info("Loading an old Swagger descriptor from {}", swaggerDescriptor.getPayload());

                    // For spec < 2.0, only read by url is possible
                    swagger = new SwaggerCompatConverter().read(swaggerDescriptor.getPayload());
                } catch (IOException ioe) {
                    LOGGER.error("Can not read old Swagger specification", ioe);
                    throw new SwaggerDescriptorException();
                }
            case VERSION_2_0:
                if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.INLINE) {
                    LOGGER.info("Loading an inline Swagger descriptor");
                    swagger = new SwaggerParser().parse(swaggerDescriptor.getPayload());
                } else if (swaggerDescriptor.getType() == ImportSwaggerDescriptorEntity.Type.URL) {
                    LOGGER.info("Loading a Swagger descriptor from URL: ", swaggerDescriptor.getPayload());
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

        String scheme = (swagger.getSchemes().isEmpty()) ? defaultScheme :
                swagger.getSchemes().iterator().next().toValue();

        apiEntity.setEndpoint(scheme + "://" + swagger.getHost() + swagger.getBasePath());
        apiEntity.setPaths(new ArrayList<>(
                swagger.getPaths().keySet()
                        .stream()
                        .map(path -> path.replaceAll("\\{(.[^/]*)\\}", ":$1"))
                        .collect(Collectors.toList())));

        return apiEntity;
    }
}
