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
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.license.model.License;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.organization.OrganizationCommand;
import io.gravitee.cockpit.api.command.v1.organization.OrganizationCommandPayload;
import io.gravitee.cockpit.api.command.v1.organization.OrganizationReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final LicenseDomainService organizationLicenseService;

    @Override
    public String supportType() {
        return CockpitCommandType.ORGANIZATION.name();
    }

    @Override
    public Single<OrganizationReply> handle(OrganizationCommand command) {
        OrganizationCommandPayload organizationPayload = command.getPayload();

        try {
            final OrganizationEntity organization = createOrUpdateOrganization(organizationPayload);

            handleLicense(organization, command.getPayload().license());

            handleAccessPoints(organizationPayload, organization);
            log.info("Organization [{}] handled with id [{}].", organization.getName(), organization.getId());
            return Single.just(new OrganizationReply(command.getId()));
        } catch (Exception e) {
            String errorDetails = "Error occurred when handling organization [%s] with id [%s].".formatted(
                organizationPayload.name(),
                organizationPayload.id()
            );
            log.error(errorDetails, e);
            return Single.just(new OrganizationReply(command.getId(), errorDetails));
        }
    }

    private void handleAccessPoints(OrganizationCommandPayload organizationPayload, OrganizationEntity organization) {
        List<io.gravitee.apim.core.access_point.model.AccessPoint> accessPointsToCreate;
        if (organizationPayload.accessPoints() != null) {
            accessPointsToCreate = organizationPayload
                .accessPoints()
                .stream()
                .map(cockpitAccessPoint ->
                    io.gravitee.apim.core.access_point.model.AccessPoint.builder()
                        .referenceType(io.gravitee.apim.core.access_point.model.AccessPoint.ReferenceType.ORGANIZATION)
                        .referenceId(organization.getId())
                        .target(io.gravitee.apim.core.access_point.model.AccessPoint.Target.valueOf(cockpitAccessPoint.getTarget().name()))
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
    }

    private void handleLicense(OrganizationEntity organization, String license) {
        organizationLicenseService.createOrUpdateOrganizationLicense(organization.getId(), license);
    }

    private OrganizationEntity createOrUpdateOrganization(OrganizationCommandPayload organizationPayload) {
        String organizationId = this.getOrganizationId(organizationPayload);

        UpdateOrganizationEntity newOrganization = new UpdateOrganizationEntity();
        newOrganization.setCockpitId(organizationPayload.cockpitId());
        newOrganization.setHrids(organizationPayload.hrids());
        newOrganization.setName(organizationPayload.name());
        newOrganization.setDescription(organizationPayload.description());
        return organizationService.createOrUpdate(organizationId, newOrganization);
    }

    private String getOrganizationId(OrganizationCommandPayload organizationPayload) {
        try {
            OrganizationEntity byCockpitId = this.organizationService.findByCockpitId(organizationPayload.cockpitId());
            return byCockpitId.getId();
        } catch (OrganizationNotFoundException ex) {
            return organizationPayload.id();
        }
    }
}
