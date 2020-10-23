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
import io.gravitee.management.model.api.SwaggerApiEntity;
import io.gravitee.management.service.SwaggerService;
import io.gravitee.management.service.exceptions.SwaggerDescriptorException;
import io.gravitee.management.service.impl.swagger.converter.api.OAIToAPIConverter;
import io.gravitee.management.service.impl.swagger.parser.OAIParser;
import io.gravitee.management.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.management.service.impl.swagger.transformer.SwaggerTransformer;
import io.gravitee.management.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.gravitee.management.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.management.service.spring.ImportConfiguration;
import io.gravitee.management.service.swagger.OAIDescriptor;
import io.gravitee.management.service.swagger.SwaggerDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private PolicyOperationVisitorManager policyOperationVisitorManager;

    @Autowired
    private ImportConfiguration importConfiguration;

    @Override
    public SwaggerApiEntity createAPI(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        SwaggerDescriptor descriptor = parse(swaggerDescriptor.getPayload());
        if (descriptor != null) {

            List<OAIOperationVisitor> visitors = policyOperationVisitorManager.getPolicyVisitors().stream()
                .filter(operationVisitor -> swaggerDescriptor.getWithPolicies() != null
                    && swaggerDescriptor.getWithPolicies().contains(operationVisitor.getId()))
                .map(operationVisitor -> policyOperationVisitorManager.getOAIOperationVisitor(operationVisitor.getId()))
                .collect(Collectors.toList());

            return new OAIToAPIConverter(visitors)
                .convert((OAIDescriptor) descriptor);
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
        if (isUrl(content)) {
            UrlSanitizerUtils.checkAllowed(content, importConfiguration.getImportWhitelist(), importConfiguration.isAllowImportFromPrivate());
        }

        OAIDescriptor oaiDescriptor = new OAIParser().parse(content);
        if (oaiDescriptor == null || oaiDescriptor.getSpecification() == null) {
            throw new SwaggerDescriptorException();
        }

        return oaiDescriptor;
    }

    private boolean isUrl(String content) {
        try {
            new URL(content);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
