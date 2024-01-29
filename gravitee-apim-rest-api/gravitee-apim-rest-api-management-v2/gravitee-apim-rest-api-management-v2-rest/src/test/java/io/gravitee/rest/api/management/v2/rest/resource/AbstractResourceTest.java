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
package io.gravitee.rest.api.management.v2.rest.resource;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.domain_service.UpdateFederatedApiDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.integration.domain_service.IntegrationDomainService;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.management.v2.rest.JerseySpringTest;
import io.gravitee.rest.api.management.v2.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.v4.ApiDuplicateService;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.ApiLicenseService;
import io.gravitee.rest.api.service.v4.ApiWorkflowStateService;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class AbstractResourceTest extends JerseySpringTest {

    @Autowired
    protected ApiRepository apiRepository;

    @Autowired
    protected ApiService apiService;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiService apiServiceV4;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiSearchService apiSearchServiceV4;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiStateService apiStateServiceV4;

    @Autowired
    protected io.gravitee.rest.api.service.v4.ApiImagesService apiImagesService;

    @Autowired
    protected ApiImportExportService apiImportExportService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected PlanService planServiceV4;

    @Autowired
    protected io.gravitee.rest.api.service.PlanService planServiceV2;

    @Autowired
    protected EnvironmentService environmentService;

    @Autowired
    protected GroupService groupService;

    @Autowired
    protected MembershipService membershipService;

    @Autowired
    protected EntrypointConnectorPluginService entrypointConnectorPluginService;

    @Autowired
    protected EndpointConnectorPluginService endpointConnectorPluginService;

    @Autowired
    protected PolicyPluginService policyPluginService;

    @Autowired
    protected ApiMetadataService apiMetadataService;

    @Autowired
    protected PageService pageService;

    @Autowired
    protected MediaService mediaService;

    @Autowired
    protected ParameterService parameterService;

    @Autowired
    protected WorkflowService workflowService;

    @Autowired
    protected ApiLicenseService apiLicenseService;

    @Autowired
    protected ApiWorkflowStateService apiWorkflowStateService;

    @Autowired
    protected RoleService roleService;

    @Autowired
    protected ApiDuplicatorService apiDuplicatorService;

    @Autowired
    protected ApiDuplicateService apiDuplicateService;

    @Autowired
    protected VerifyApiPathDomainService verifyApiPathDomainService;

    @Autowired
    protected UpdateFederatedApiDomainService updateFederatedApiDomainService;

    @Autowired
    protected IntegrationDomainService integrationDomainService;

    @BeforeEach
    public void setUp() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }
}
