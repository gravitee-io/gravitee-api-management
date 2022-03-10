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

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.Completable;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CockpitIdUpgrader implements Upgrader {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private EnvironmentService environmentService;

    @Override
    public Completable upgrade() {
        return Completable.fromRunnable(
            () -> {
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
                            organizationService.update(executionContext, org.getId(), newOrganization);
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
            }
        );
    }

    @Override
    public int order() {
        return 200;
    }
}
