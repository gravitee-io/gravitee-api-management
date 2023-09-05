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

import io.gravitee.apim.infra.UsecaseSpringConfiguration;
import io.gravitee.node.api.license.NodeLicenseService;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.v4.ApiDuplicateService;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.ApiLicenseService;
import io.gravitee.rest.api.service.v4.ApiWorkflowStateService;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import({ InMemoryConfiguration.class, UsecaseSpringConfiguration.class })
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
    public NodeLicenseService nodeLicenseService() {
        return mock(NodeLicenseService.class);
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
}
