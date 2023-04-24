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
package io.gravitee.gateway.services.sync.process.common.deployer;

import io.gravitee.definition.model.Organization;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class OrganizationDeployer implements Deployer<OrganizationDeployable> {

    private final OrganizationManager organizationManager;
    private final DistributedSyncService distributedSyncService;

    @Override
    public Completable deploy(final OrganizationDeployable deployable) {
        return Completable.fromRunnable(() -> {
            Organization organization = deployable.organization();
            try {
                // Update definition with required information for deployment phase
                final io.gravitee.gateway.platform.Organization organizationPlatform = new io.gravitee.gateway.platform.Organization(
                    organization
                );
                organizationPlatform.setUpdatedAt(organization.getUpdatedAt());
                organizationManager.register(organizationPlatform);
                log.debug("Organization [{}] deployed ", deployable.id());
            } catch (Exception e) {
                throw new SyncException(
                    String.format(
                        "An error occurred when trying to deploy organization %s [%s].",
                        organization.getName(),
                        organization.getId()
                    ),
                    e
                );
            }
        });
    }

    @Override
    public Completable doAfterDeployment(final OrganizationDeployable deployable) {
        return distributedSyncService.distributeIfNeeded(deployable);
    }
}
