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
import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.accesspoint.AccessPoint;
import io.gravitee.cockpit.api.command.organization.OrganizationCommand;
import io.gravitee.cockpit.api.command.organization.OrganizationPayload;
import io.gravitee.cockpit.api.command.organization.OrganizationReply;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationCommandHandler implements CommandHandler<OrganizationCommand, OrganizationReply> {

    private final OrganizationService organizationService;
    private final AccessPointCrudService accessPointService;

    @Override
    public Command.Type handleType() {
        return Command.Type.ORGANIZATION_COMMAND;
    }

    @Override
    public Single<OrganizationReply> handle(OrganizationCommand command) {
        OrganizationPayload organizationPayload = command.getPayload();

        try {
            String organizationId = this.getOrganizationId(organizationPayload);

            UpdateOrganizationEntity newOrganization = new UpdateOrganizationEntity();
            newOrganization.setCockpitId(organizationPayload.getCockpitId());
            newOrganization.setHrids(organizationPayload.getHrids());
            newOrganization.setName(organizationPayload.getName());
            newOrganization.setDescription(organizationPayload.getDescription());
            final OrganizationEntity organization = organizationService.createOrUpdate(organizationId, newOrganization);

            List<io.gravitee.apim.core.access_point.model.AccessPoint> accessPointsToCreate;
            if (organizationPayload.getAccessPoints() != null) {
                accessPointsToCreate =
                    organizationPayload
                        .getAccessPoints()
                        .stream()
                        .map(cockpitAccessPoint ->
                            io.gravitee.apim.core.access_point.model.AccessPoint
                                .builder()
                                .referenceType(io.gravitee.apim.core.access_point.model.AccessPoint.ReferenceType.ORGANIZATION)
                                .referenceId(organization.getId())
                                .target(
                                    io.gravitee.apim.core.access_point.model.AccessPoint.Target.valueOf(
                                        cockpitAccessPoint.getTarget().name()
                                    )
                                )
                                .host(cockpitAccessPoint.getHost())
                                .secured(cockpitAccessPoint.isSecured())
                                .overriding(cockpitAccessPoint.isOverriding())
                                .build()
                        )
                        .toList();
            } else {
                accessPointsToCreate = new ArrayList<>();
            }
            accessPointService.updateAccessPoints(
                io.gravitee.apim.core.access_point.model.AccessPoint.ReferenceType.ORGANIZATION,
                organization.getId(),
                accessPointsToCreate
            );
            log.info("Organization [{}] handled with id [{}].", organization.getName(), organization.getId());
            return Single.just(new OrganizationReply(command.getId(), CommandStatus.SUCCEEDED));
        } catch (Exception e) {
            log.error(
                "Error occurred when handling organization [{}] with id [{}].",
                organizationPayload.getName(),
                organizationPayload.getId(),
                e
            );
            return Single.just(new OrganizationReply(command.getId(), CommandStatus.ERROR));
        }
    }

    private String getOrganizationId(OrganizationPayload organizationPayload) {
        try {
            OrganizationEntity byCockpitId = this.organizationService.findByCockpitId(organizationPayload.getCockpitId());
            return byCockpitId.getId();
        } catch (OrganizationNotFoundException ex) {
            return organizationPayload.getId();
        }
    }
}
