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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity.Format;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.exceptions.SwaggerDescriptorException;
import io.gravitee.rest.api.service.impl.swagger.converter.api.OAIToAPIConverter;
import io.gravitee.rest.api.service.impl.swagger.converter.api.OAIToAPIV2Converter;
import io.gravitee.rest.api.service.impl.swagger.parser.OAIParser;
import io.gravitee.rest.api.service.impl.swagger.parser.WsdlParser;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.impl.swagger.transformer.SwaggerTransformer;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.gravitee.rest.api.service.swagger.OAIDescriptor;
import io.gravitee.rest.api.service.swagger.SwaggerDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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

    @Autowired
    private GroupService groupService;

    @Autowired
    private TagService tagService;

    @Override
    public SwaggerApiEntity createAPI(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        return this.createAPI(swaggerDescriptor, DefinitionVersion.V1);
    }

    @Override
    public SwaggerApiEntity createAPI(ImportSwaggerDescriptorEntity swaggerDescriptor, DefinitionVersion definitionVersion) {
        ParseOptions options = new ParseOptions();
        // In the context of an import, we need to fully resolve $ref which are required for OAIDescriptorVisitors (ie. policies).
        options.setResolveFully(true);

        boolean wsdlImport = Format.WSDL.equals(swaggerDescriptor.getFormat());
        SwaggerDescriptor descriptor = parse(swaggerDescriptor.getPayload(), wsdlImport, options);
        if (wsdlImport) {
            overridePayload(swaggerDescriptor, descriptor);
            populateXmlToJsonPolicy(swaggerDescriptor);
        }

        if (descriptor != null) {
            List<OAIOperationVisitor> visitors = new ArrayList<>();
            if (swaggerDescriptor.isWithPolicyPaths()) {
                visitors = policyOperationVisitorManager.getPolicyVisitors().stream()
                        .filter(operationVisitor -> swaggerDescriptor.getWithPolicies() != null
                                && swaggerDescriptor.getWithPolicies().contains(operationVisitor.getId()))
                        .map(operationVisitor -> policyOperationVisitorManager.getOAIOperationVisitor(operationVisitor.getId()))
                        .collect(Collectors.toList());
            }

            if(definitionVersion.equals(DefinitionVersion.V2)){
                return new OAIToAPIV2Converter(visitors, groupService, tagService)
                    .convert((OAIDescriptor) descriptor);
            }

            return new OAIToAPIConverter(visitors, groupService, tagService)
                    .convert((OAIDescriptor) descriptor);

        }

        throw new SwaggerDescriptorException();
    }

    /**
     * Override the payload attribute of swaggerDescriptor by the JSON representation of the descriptor.
     * This is useful in case of WSDL import to generate a swagger page instead of exposing the WSDL.
     *
     * @param swaggerDescriptor
     * @param descriptor
     */
    private void overridePayload(ImportSwaggerDescriptorEntity swaggerDescriptor, SwaggerDescriptor descriptor) {
        try {
            swaggerDescriptor.setPayload(descriptor.toYaml());
            swaggerDescriptor.setType(ImportSwaggerDescriptorEntity.Type.INLINE);
        } catch (JsonProcessingException e) {
            logger.debug("JSON serialization failed, unable to override payload attribute", e);
        }
    }

    /**
     * Enable the Xml-to-Json policy OperationVisitor if the rest-to-soap policy is required.
     * @param swaggerDescriptor
     */
    private void populateXmlToJsonPolicy(ImportSwaggerDescriptorEntity swaggerDescriptor) {
        if (swaggerDescriptor.getWithPolicies().contains("rest-to-soap")) {
            swaggerDescriptor.getWithPolicies().add("xml-json");
        }
    }

    @Override
    public <Y, T extends SwaggerDescriptor<Y>> void transform(T descriptor, Collection<SwaggerTransformer<T>> transformers) {
        if (transformers != null) {
            transformers.forEach(transformer -> transformer.transform(descriptor));
        }
    }

    @Override
    public SwaggerDescriptor parse(String content) {
        return parse(content, false, null);
    }

    public SwaggerDescriptor parse(String content, boolean wsdl) {
        return parse(content, wsdl, null);
    }

    public SwaggerDescriptor parse(String content, boolean wsdl, ParseOptions options) {
        OpenAPI descriptor;

        if (isUrl(content)) {
            UrlSanitizerUtils.checkAllowed(content, importConfiguration.getImportWhitelist(), importConfiguration.isAllowImportFromPrivate());
        }

        if (wsdl) {
            // try to read wsdl
            logger.debug("Trying to load a Wsdl descriptor");

            descriptor = new WsdlParser().parse(content);

            if (descriptor != null) {
                return new OAIDescriptor(descriptor);
            }
        } else {

            OAIDescriptor oaiDescriptor = new OAIParser().parse(content, options);
            if (oaiDescriptor == null || oaiDescriptor.getSpecification() == null) {
                throw new SwaggerDescriptorException();
            }

            return oaiDescriptor;
        }
        throw new SwaggerDescriptorException();
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
