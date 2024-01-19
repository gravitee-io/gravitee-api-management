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

import static org.mockito.Mockito.*;

import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.node.api.license.LicenseFactory;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.license.DefaultLicenseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class LicenseDeployerTest {

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private LicenseFactory licenseFactory;

    private LicenseDeployer cut;

    @BeforeEach
    public void beforeEach() {
        cut = new LicenseDeployer(licenseManager, licenseFactory, new NoopDistributedSyncService());
    }

    @Nested
    class DeployTest {

        @Test
        void should_deploy_license() throws Exception {
            var licenseDeployable = LicenseDeployable.builder().license("license").id("id").build();
            var license = licenseFactory.create("ORGANIZATION", "id", "license");
            cut.deploy(licenseDeployable).test().assertComplete();
            verify(licenseManager).registerOrganizationLicense("id", license);
        }

        @Test
        void should_return_error_when_license_manager_throw_exception() throws Exception {
            var licenseDeployable = LicenseDeployable.builder().license("license").id("id").build();
            var license = licenseFactory.create("ORGANIZATION", "id", "license");
            doThrow(new SyncException("test")).when(licenseManager).registerOrganizationLicense("id", license);
            cut.deploy(licenseDeployable).test().assertFailure(SyncException.class);
            verify(licenseManager).registerOrganizationLicense("id", DefaultLicenseManager.OSS_LICENSE);
        }
    }
}
