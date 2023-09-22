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

import io.gravitee.cockpit.api.command.Command;
import io.gravitee.cockpit.api.command.CommandHandler;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.accesspoint.AccessPoint;
import io.gravitee.cockpit.api.command.organization.OrganizationCommand;
import io.gravitee.cockpit.api.command.organization.OrganizationPayload;
import io.gravitee.cockpit.api.command.organization.OrganizationReply;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointTarget;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.AccessPointService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final AccessPointService accessPointService;

    @Override
    public Command.Type handleType() {
        return Command.Type.ORGANIZATION_COMMAND;
    }

    @Override
    public Single<OrganizationReply> handle(OrganizationCommand command) {
        OrganizationPayload organizationPayload = command.getPayload();

        try {
            ExecutionContext executionContext = new ExecutionContext(organizationPayload.getId(), null);

            UpdateOrganizationEntity newOrganization = new UpdateOrganizationEntity();
            newOrganization.setCockpitId(organizationPayload.getCockpitId());
            newOrganization.setHrids(organizationPayload.getHrids());
            newOrganization.setName(organizationPayload.getName());
            newOrganization.setDescription(organizationPayload.getDescription());
            final OrganizationEntity organization = organizationService.createOrUpdate(executionContext, newOrganization);

            List<AccessPoint> accessPoints = organizationPayload.getAccessPoints();
            if (accessPoints != null) {
                List<io.gravitee.repository.management.model.AccessPoint> accessPointsToCreate = accessPoints
                    .stream()
                    .map(cockpitAccessPoint ->
                        io.gravitee.repository.management.model.AccessPoint
                            .builder()
                            .referenceType(AccessPointReferenceType.ORGANIZATION)
                            .referenceId(organization.getId())
                            .target(AccessPointTarget.valueOf(cockpitAccessPoint.getTarget().name()))
                            .host(cockpitAccessPoint.getHost())
                            .secured(cockpitAccessPoint.isSecured())
                            .overriding(cockpitAccessPoint.isOverriding())
                            .build()
                    )
                    .toList();
                accessPointService.updateAccessPoints(AccessPointReferenceType.ORGANIZATION, organization.getId(), accessPointsToCreate);
            }
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
}
