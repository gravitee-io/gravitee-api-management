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
package io.gravitee.rest.api.management.v2.rest.resource;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.rest.api.management.v2.rest.JerseySpringTest;
import io.gravitee.rest.api.management.v2.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.v4.ApiImportExportService;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.PlanService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ResourceContextConfiguration.class })
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
    protected PlanService planService;

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
    protected ApiMetadataService apiMetadataService;

    @Autowired
    protected PageService pageService;

    @Autowired
    protected MediaService mediaService;

    @Before
    public void setUp() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }
}
