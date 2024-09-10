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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.apim.core.shared_policy_group.use_case.InitializeSharedPolicyGroupUseCase;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class InitializeSharedPolicyGroupUpgrader implements Upgrader {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(InitializeSharedPolicyGroupUpgrader.class);

    @Autowired
    private InitializeSharedPolicyGroupUseCase initializeSharedPolicyGroupUseCase;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private OrganizationService organizationService;

    @Override
    public boolean upgrade() {
        try {
            organizationService
                .findAll()
                .forEach(organization -> {
                    environmentService
                        .findByOrganization(organization.getId())
                        .forEach(environment -> {
                            // Create default shared policy group for each environment
                            initializeSharedPolicyGroupUseCase.execute(
                                InitializeSharedPolicyGroupUseCase.Input
                                    .builder()
                                    .organizationId(organization.getId())
                                    .environmentId(environment.getId())
                                    .build()
                            );
                        });
                });
        } catch (Exception e) {
            logger.error("unable to apply upgrader {}", getClass().getSimpleName(), e);
            return false;
        }

        return true;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.INITIALIZE_SHARED_POLICY_GROUP_UPGRADER;
    }
}
