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
package io.gravitee.rest.api.service.cockpit.command.handler;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.installation.*;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UnlinkInstallationCommandHandler implements CommandHandler<UnlinkInstallationCommand, UnlinkInstallationReply> {

    private final Logger logger = LoggerFactory.getLogger(UnlinkInstallationCommandHandler.class);

    private final OrganizationService organizationService;
    private final EnvironmentService environmentService;
    private final AccessPointCrudService accessPointService;

    public UnlinkInstallationCommandHandler(
        OrganizationService organizationService,
        EnvironmentService environmentService,
        AccessPointCrudService accessPointService
    ) {
        this.organizationService = organizationService;
        this.environmentService = environmentService;
        this.accessPointService = accessPointService;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.UNLINK_INSTALLATION_COMMAND;
    }

    @Override
    public Single<UnlinkInstallationReply> handle(UnlinkInstallationCommand command) {
        UnlinkInstallationPayload unlinkInstallationPayload = command.getPayload();

        try {
            if (unlinkInstallationPayload.getOrganizationCockpitId() != null) {
                OrganizationEntity organization =
                    this.organizationService.findByCockpitId(unlinkInstallationPayload.getOrganizationCockpitId());
                this.accessPointService.deleteAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, organization.getId());
            }

            if (unlinkInstallationPayload.getEnvironmentCockpitId() != null) {
                EnvironmentEntity environment =
                    this.environmentService.findByCockpitId(unlinkInstallationPayload.getEnvironmentCockpitId());
                this.accessPointService.deleteAccessPoints(
                        io.gravitee.apim.core.access_point.model.AccessPoint.ReferenceType.ENVIRONMENT,
                        environment.getId()
                    );
            }

            return Single.just(new UnlinkInstallationReply(command.getId(), CommandStatus.SUCCEEDED));
        } catch (Exception ex) {
            logger.info("Error occurred when unlink installation.", ex);
            return Single.just(new UnlinkInstallationReply(command.getId(), CommandStatus.ERROR));
        }
    }
}
