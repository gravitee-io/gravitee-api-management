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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Organization;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReactableOrganizationDeployerTest {

    @Mock
    private OrganizationManager organizationManager;

    private OrganizationDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new OrganizationDeployer(organizationManager, new NoopDistributedSyncService());
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_organization() {
            OrganizationDeployable organizationDeployable = OrganizationDeployable.builder()
                .reactableOrganization(new ReactableOrganization(new Organization()))
                .build();
            cut.deploy(organizationDeployable).test().assertComplete();
            verify(organizationManager).register(any());
        }

        @Test
        void should_return_error_when_api_manager_throw_exception() {
            Organization organization = mock(Organization.class);
            OrganizationDeployable organizationDeployable = OrganizationDeployable.builder()
                .reactableOrganization(new ReactableOrganization(new Organization()))
                .build();
            doThrow(new SyncException("error")).when(organizationManager).register(any());
            cut.deploy(organizationDeployable).test().assertFailure(SyncException.class);
            verify(organizationManager).register(any());
        }

        @Test
        void should_ignore_do_post_action() {
            cut.doAfterDeployment(null).test().assertComplete();
            verifyNoInteractions(organizationManager);
        }
    }

    @Nested
    class UndeployTest {

        @Test
        void should_ignore_undeploy_organization() {
            cut.undeploy(null).test().assertComplete();
            verifyNoInteractions(organizationManager);
        }
    }
}
