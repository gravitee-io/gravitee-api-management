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
package io.gravitee.rest.api.management.v2.rest.spring;

import static org.mockito.Mockito.mock;

import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.DeployApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateFederatedApiDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.use_case.UpdateFederatedApiUseCase;
import io.gravitee.apim.core.integration.domain_service.IntegrationDomainService;
import io.gravitee.apim.core.integration.use_case.IntegrationCreateUseCase;
import io.gravitee.apim.core.integration.use_case.IntegrationGetEntitiesUseCase;
import io.gravitee.apim.core.integration.use_case.IntegrationImportUseCase;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.PlanSynchronizationService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonSpringConfiguration;
import io.gravitee.apim.infra.sanitizer.SanitizerSpringConfiguration;
import io.gravitee.apim.infra.spring.UsecaseSpringConfiguration;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.v4.*;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import(
    { InMemoryConfiguration.class, UsecaseSpringConfiguration.class, JacksonSpringConfiguration.class, SanitizerSpringConfiguration.class }
)
@PropertySource("classpath:/io/gravitee/rest/api/management/v2/rest/resource/jwt.properties")
public class ResourceContextConfiguration {

    @Bean
    public ApiRepository apiRepository() {
        return mock(ApiRepository.class);
    }

    @Bean
    public ApiService apiService() {
        return mock(ApiService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiService apiServiceV4() {
        return mock(io.gravitee.rest.api.service.v4.ApiService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiSearchService apiSearchServiceV4() {
        return mock(io.gravitee.rest.api.service.v4.ApiSearchService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiStateService apiStateServiceV4() {
        return mock(io.gravitee.rest.api.service.v4.ApiStateService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiImagesService apiImageService() {
        return mock(io.gravitee.rest.api.service.v4.ApiImagesService.class);
    }

    @Bean
    public ApiImportExportService apiImportExportService() {
        return mock(ApiImportExportService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.v4.ApiAuthorizationService apiAuthorizationServiceV4() {
        return mock(io.gravitee.rest.api.service.v4.ApiAuthorizationService.class);
    }

    @Bean
    public MembershipService membershipService() {
        return mock(MembershipService.class);
    }

    @Bean
    public RoleService roleService() {
        return mock(RoleService.class);
    }

    @Bean
    public GroupService groupService() {
        return mock(GroupService.class);
    }

    @Bean
    public PermissionService permissionService() {
        return mock(PermissionService.class);
    }

    @Bean
    public ParameterService parameterService() {
        return mock(ParameterService.class);
    }

    @Bean
    public PlanService planServiceV4() {
        return mock(PlanService.class);
    }

    @Bean
    public io.gravitee.rest.api.service.PlanService planService() {
        return mock(io.gravitee.rest.api.service.PlanService.class);
    }

    @Bean
    public PlanSearchService planSearchService() {
        return mock(PlanSearchService.class);
    }

    @Bean
    public EntrypointConnectorPluginService entrypointConnectorPluginService() {
        return mock(EntrypointConnectorPluginService.class);
    }

    @Bean
    public EndpointConnectorPluginService endpointConnectorPluginService() {
        return mock(EndpointConnectorPluginService.class);
    }

    @Bean
    public PolicyPluginService policyPluginService() {
        return mock(PolicyPluginService.class);
    }

    @Bean
    public EnvironmentService environmentService() {
        return mock(EnvironmentService.class);
    }

    @Bean
    public OrganizationService organizationService() {
        return mock(OrganizationService.class);
    }

    @Bean
    public ApiMetadataService apiMetadataService() {
        return mock(ApiMetadataService.class);
    }

    @Bean
    public PageService pageService() {
        return mock(PageService.class);
    }

    @Bean
    public MediaService mediaService() {
        return mock(MediaService.class);
    }

    @Bean
    public WorkflowService workflowService() {
        return mock(WorkflowService.class);
    }

    @Bean
    public SubscriptionService subscriptionService() {
        return mock(SubscriptionService.class);
    }

    @Bean
    public ApplicationService applicationService() {
        return mock(ApplicationService.class);
    }

    @Bean
    public ApiKeyService apiKeyService() {
        return mock(ApiKeyService.class);
    }

    @Bean
    public UserService userService() {
        return mock(UserService.class);
    }

    @Bean
    public ApiLicenseService apiLicenseService() {
        return mock(ApiLicenseService.class);
    }

    @Bean
    public ApiWorkflowStateService apiWorkflowStateService() {
        return mock(ApiWorkflowStateService.class);
    }

    @Bean
    public ApiDuplicatorService apiDuplicatorService() {
        return mock(ApiDuplicatorService.class);
    }

    @Bean
    public ApiDuplicateService apiDuplicateService() {
        return mock(ApiDuplicateService.class);
    }

    @Bean
    public VerifyApiPathDomainService verifyApiPathDomainService() {
        return mock(VerifyApiPathDomainService.class);
    }

    @Bean
    public LicenseManager licenseManager() {
        return mock(LicenseManager.class);
    }

    @Bean
    public GraviteeLicenseDomainService graviteeLicenseDomainService(LicenseManager licenseManager) {
        return new GraviteeLicenseDomainService(licenseManager);
    }

    @Bean
    public CreateApiDomainService createApiDomainService() {
        return mock(CreateApiDomainService.class);
    }

    @Bean
    public CreatePlanDomainService createPlanDomainService() {
        return mock(CreatePlanDomainService.class);
    }

    @Bean
    public DeployApiDomainService deployApiDomainService() {
        return mock(DeployApiDomainService.class);
    }

    @Bean
    public ApiMetadataDomainService apiMetadataDomainService() {
        return mock(ApiMetadataDomainService.class);
    }

    @Bean
    public PolicyValidationDomainService policyDomainService() {
        return mock(PolicyValidationDomainService.class);
    }

    @Bean
    public PolicyValidationDomainService policyValidationDomainService() {
        return mock(PolicyValidationDomainService.class);
    }

    @Bean
    public PlanSynchronizationService planSynchronizationService() {
        return mock(PlanSynchronizationService.class);
    }

    @Bean
    public UpdateApiDomainService updateApiDomainService() {
        return mock(UpdateApiDomainService.class);
    }

    @Bean
    public UpdateFederatedApiDomainService updateFederatedApiDomainService() {
        return mock(UpdateFederatedApiDomainService.class);
    }

    @Bean
    public IntegrationDomainService integrationDomainService() {
        return mock(IntegrationDomainService.class);
    }
}
