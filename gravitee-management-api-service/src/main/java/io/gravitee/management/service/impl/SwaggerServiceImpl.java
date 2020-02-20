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
import io.gravitee.management.model.api.NewSwaggerApiEntity;
import io.gravitee.management.service.SwaggerService;
import io.gravitee.management.service.exceptions.SwaggerDescriptorException;
import io.gravitee.management.service.impl.swagger.converter.api.OAIToAPIConverter;
import io.gravitee.management.service.impl.swagger.converter.api.SwaggerV2ToAPIConverter;
import io.gravitee.management.service.impl.swagger.parser.OAIParser;
import io.gravitee.management.service.impl.swagger.parser.SwaggerV1Parser;
import io.gravitee.management.service.impl.swagger.parser.SwaggerV2Parser;
import io.gravitee.management.service.impl.swagger.transformer.SwaggerTransformer;
import io.gravitee.management.service.swagger.OAIDescriptor;
import io.gravitee.management.service.swagger.SwaggerDescriptor;
import io.gravitee.management.service.swagger.SwaggerV1Descriptor;
import io.gravitee.management.service.swagger.SwaggerV2Descriptor;
import io.swagger.models.Swagger;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;

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

    @Override
    public NewSwaggerApiEntity createAPI(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        SwaggerDescriptor descriptor = parse(swaggerDescriptor.getPayload());
        if (descriptor != null) {
            if (descriptor.getVersion() == SwaggerDescriptor.Version.SWAGGER_V1 || descriptor.getVersion() == SwaggerDescriptor.Version.SWAGGER_V2) {
                return new SwaggerV2ToAPIConverter(swaggerDescriptor.isWithPolicyMocks(), defaultScheme).convert((SwaggerV2Descriptor) descriptor);
            } else if (descriptor.getVersion() == SwaggerDescriptor.Version.OAI_V3) {
                return new OAIToAPIConverter(swaggerDescriptor.isWithPolicyMocks()).convert((OAIDescriptor) descriptor);
            }
        }

        throw new SwaggerDescriptorException();
    }

    @Override
    public <Y, T extends SwaggerDescriptor<Y>> void transform(T descriptor, Collection<SwaggerTransformer<T>> transformers) {
        if (transformers != null) {
            transformers.forEach(transformer -> transformer.transform(descriptor));
        }
    }

    @Override
    public SwaggerDescriptor parse(String content) {
        Object descriptor;

        // try to read swagger in version 2
        logger.debug("Trying to load a Swagger v2 descriptor");
        descriptor = new SwaggerV2Parser().parse(content);

        if (descriptor != null) {
            return new SwaggerV2Descriptor((Swagger) descriptor);
        }

        // try to read swagger in version 3 (openAPI)
        logger.debug("Trying to load an OpenAPI descriptor");
        descriptor = new OAIParser().parse(content);

        if (descriptor != null) {
            return new OAIDescriptor((OpenAPI) descriptor);
        }

        // try to read swagger in version 1
        logger.debug("Trying to load an old Swagger descriptor");
        descriptor = new SwaggerV1Parser().parse(content);

        if (descriptor != null) {
            return new SwaggerV1Descriptor((Swagger) descriptor);
        }

        throw new SwaggerDescriptorException();
    }
}
