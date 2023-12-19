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

import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.node.api.license.LicenseFactory;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.license.DefaultLicenseManager;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class LicenseDeployer implements Deployer<LicenseDeployable> {

    private final LicenseManager licenseManager;
    private final LicenseFactory licenseFactory;

    @Override
    public Completable deploy(final LicenseDeployable deployable) {
        return Completable.fromRunnable(() -> {
            try {
                var license = licenseFactory.create("ORGANIZATION", deployable.id(), deployable.license());
                licenseManager.registerOrganizationLicense(deployable.id(), license);
                log.debug("License for organization [{}] deployed ", deployable.id());
            } catch (Exception e) {
                licenseManager.registerOrganizationLicense(deployable.id(), DefaultLicenseManager.OSS_LICENSE);
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to deploy license for organization %s. Fallback to OSS license",
                        deployable.id()
                    ),
                    e
                );
            }
        });
    }
}
