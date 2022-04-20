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
package io.gravitee.rest.api.service.impl.upgrade;

import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.Upgrader;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class CockpitIdUpgrader implements Upgrader, Ordered {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private EnvironmentService environmentService;

    @Override
    public boolean upgrade() {
        // FIXME : this upgrader uses the default ExecutionContext, but should handle all environments/organizations
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        Collection<OrganizationEntity> organizations = organizationService.findAll();

        organizations
            .stream()
            .filter(org -> !org.getId().equals(GraviteeContext.getDefaultOrganization()) && org.getCockpitId() == null)
            .forEach(
                org -> {
                    UpdateOrganizationEntity newOrganization = new UpdateOrganizationEntity(org);
                    newOrganization.setCockpitId(org.getId());
                    organizationService.update(executionContext, newOrganization);
                }
            );

        organizations.forEach(
            org ->
                environmentService
                    .findByOrganization(org.getId())
                    .stream()
                    .filter(env -> !env.getId().equals(GraviteeContext.getDefaultEnvironment()) && env.getCockpitId() == null)
                    .forEach(
                        env -> {
                            UpdateEnvironmentEntity updateEnv = new UpdateEnvironmentEntity(env);
                            updateEnv.setCockpitId(env.getId());
                            environmentService.createOrUpdate(org.getId(), env.getId(), updateEnv);
                        }
                    )
        );

        return true;
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
