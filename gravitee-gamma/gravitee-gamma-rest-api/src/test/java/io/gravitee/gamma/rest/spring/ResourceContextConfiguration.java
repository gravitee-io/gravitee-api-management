/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gamma.rest.spring;

import static org.mockito.Mockito.mock;

import gamma.inmemory.ResourceCrudServiceInMemory;
import gamma.inmemory.ResourceQueryServiceInMemory;
import gamma.inmemory.spring.InMemoryConfiguration;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.gamma.core.domain.resource.domain_service.ValidateCreateResourceCommandDomainService;
import io.gravitee.gamma.core.domain.resource.domain_service.ValidateUpdateResourceCommandDomainService;
import io.gravitee.gamma.core.domain.resource.use_case.CreateResourceUseCase;
import io.gravitee.gamma.core.domain.resource.use_case.DeleteResourceUseCase;
import io.gravitee.gamma.core.domain.resource.use_case.GetResourceUseCase;
import io.gravitee.gamma.core.domain.resource.use_case.SearchResourceUseCase;
import io.gravitee.gamma.core.domain.resource.use_case.UpdateResourceUseCase;
import io.gravitee.gamma.core.port.service_provider.gravitee_plugin.ResourcePluginProvider;
import io.gravitee.json.validation.JsonSchemaValidator;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.gamma.internal.GammaModuleService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PermissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(InMemoryConfiguration.class)
public class ResourceContextConfiguration {

    @Bean
    public ConfigurableInstallationAccessQueryService installationAccessQueryService() {
        return new ConfigurableInstallationAccessQueryService();
    }

    @Bean
    public PermissionService permissionService() {
        return mock(PermissionService.class);
    }

    @Bean
    public LicenseManager licenseManager() {
        return mock(LicenseManager.class);
    }

    @Bean
    public ParameterService parameterService() {
        return mock(ParameterService.class);
    }

    @Bean
    public GammaModuleService gammaModuleService() {
        return mock(GammaModuleService.class);
    }

    @Bean
    public EnvironmentService environmentService() {
        return mock(EnvironmentService.class);
    }

    @Bean
    public AuditDomainService auditDomainService() {
        return mock(AuditDomainService.class);
    }

    @Bean
    public JsonSchemaValidatorImpl jsonSchemaValidator() {
        return new JsonSchemaValidatorImpl();
    }

    @Bean
    public ValidateCreateResourceCommandDomainService validateCreateResourceCommandDomainService(
        ResourceCrudServiceInMemory resourceCrudService,
        JsonSchemaValidator jsonSchemaValidator,
        ResourcePluginProvider resourcePluginProvider
    ) {
        return new ValidateCreateResourceCommandDomainService(resourceCrudService, jsonSchemaValidator, resourcePluginProvider);
    }

    @Bean
    public ValidateUpdateResourceCommandDomainService validateUpdateResourceCommandDomainService(
        ResourceCrudServiceInMemory resourceCrudService,
        JsonSchemaValidator jsonSchemaValidator,
        ResourcePluginProvider resourcePluginProvider
    ) {
        return new ValidateUpdateResourceCommandDomainService(resourceCrudService, jsonSchemaValidator, resourcePluginProvider);
    }

    @Bean
    public CreateResourceUseCase createResourceUseCase(
        ResourceCrudServiceInMemory resourceCrudService,
        ValidateCreateResourceCommandDomainService validateCreateResourceCommandDomainService,
        AuditDomainService auditDomainService
    ) {
        return new CreateResourceUseCase(resourceCrudService, validateCreateResourceCommandDomainService, auditDomainService);
    }

    @Bean
    public GetResourceUseCase getResourceUseCase(ResourceCrudServiceInMemory resourceCrudService) {
        return new GetResourceUseCase(resourceCrudService);
    }

    @Bean
    public UpdateResourceUseCase updateResourceUseCase(
        ResourceCrudServiceInMemory resourceCrudService,
        ValidateUpdateResourceCommandDomainService validateUpdateResourceCommandDomainService,
        AuditDomainService auditDomainService
    ) {
        return new UpdateResourceUseCase(resourceCrudService, validateUpdateResourceCommandDomainService, auditDomainService);
    }

    @Bean
    public DeleteResourceUseCase deleteResourceUseCase(
        ResourceCrudServiceInMemory resourceCrudService,
        AuditDomainService auditDomainService
    ) {
        return new DeleteResourceUseCase(resourceCrudService, auditDomainService);
    }

    @Bean
    public SearchResourceUseCase searchResourceUseCase(ResourceQueryServiceInMemory resourceQueryService) {
        return new SearchResourceUseCase(resourceQueryService);
    }
}
