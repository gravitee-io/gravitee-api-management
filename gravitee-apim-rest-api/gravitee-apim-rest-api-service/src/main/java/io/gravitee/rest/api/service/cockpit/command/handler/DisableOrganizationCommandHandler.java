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
import io.gravitee.cockpit.api.command.organization.DisableOrganizationCommand;
import io.gravitee.cockpit.api.command.organization.DisableOrganizationReply;
import io.gravitee.rest.api.service.OrganizationService;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DisableOrganizationCommandHandler implements CommandHandler<DisableOrganizationCommand, DisableOrganizationReply> {

    private final OrganizationService organizationService;
    private final AccessPointCrudService accessPointService;

    public DisableOrganizationCommandHandler(OrganizationService organizationService, AccessPointCrudService accessPointService) {
        this.organizationService = organizationService;
        this.accessPointService = accessPointService;
    }

    @Override
    public Command.Type handleType() {
        return Command.Type.DISABLE_ORGANIZATION_COMMAND;
    }

    @Override
    public Single<DisableOrganizationReply> handle(DisableOrganizationCommand command) {
        var organizationPayload = command.getPayload();
        try {
            var organization = organizationService.findByCockpitId(organizationPayload.getCockpitId());

            // Delete related access points
            this.accessPointService.deleteAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, organization.getId());

            log.info("Organization [{}] with id [{}] has been disabled.", organization.getName(), organization.getId());
            return Single.just(new DisableOrganizationReply(command.getId(), CommandStatus.SUCCEEDED));
        } catch (Exception e) {
            log.error(
                "Error occurred when disabling organization [{}] with id [{}].",
                organizationPayload.getName(),
                organizationPayload.getId(),
                e
            );
            return Single.just(new DisableOrganizationReply(command.getId(), CommandStatus.ERROR));
        }
    }
}
