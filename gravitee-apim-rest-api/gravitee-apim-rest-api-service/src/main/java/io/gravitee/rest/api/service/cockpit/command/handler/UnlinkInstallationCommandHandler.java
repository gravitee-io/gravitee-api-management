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
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.installation.UnlinkInstallationCommand;
import io.gravitee.cockpit.api.command.v1.installation.UnlinkInstallationCommandPayload;
import io.gravitee.cockpit.api.command.v1.installation.UnlinkInstallationReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UnlinkInstallationCommandHandler implements CommandHandler<UnlinkInstallationCommand, UnlinkInstallationReply> {

    private final OrganizationService organizationService;
    private final EnvironmentService environmentService;
    private final AccessPointCrudService accessPointService;

    @Override
    public String supportType() {
        return CockpitCommandType.UNLINK_INSTALLATION.name();
    }

    @Override
    public Single<UnlinkInstallationReply> handle(UnlinkInstallationCommand command) {
        UnlinkInstallationCommandPayload unlinkInstallationPayload = command.getPayload();

        try {
            if (unlinkInstallationPayload.organizationCockpitId() != null) {
                OrganizationEntity organization =
                    this.organizationService.findByCockpitId(unlinkInstallationPayload.organizationCockpitId());
                this.accessPointService.deleteAccessPoints(AccessPoint.ReferenceType.ORGANIZATION, organization.getId());
            }

            if (unlinkInstallationPayload.environmentCockpitId() != null) {
                EnvironmentEntity environment = this.environmentService.findByCockpitId(unlinkInstallationPayload.environmentCockpitId());
                this.accessPointService.deleteAccessPoints(
                        io.gravitee.apim.core.access_point.model.AccessPoint.ReferenceType.ENVIRONMENT,
                        environment.getId()
                    );
            }

            return Single.just(new UnlinkInstallationReply(command.getId()));
        } catch (Exception ex) {
            String errorDetails = "Error occurred when unlink installation.";
            log.info(errorDetails, ex);
            return Single.just(new UnlinkInstallationReply(command.getId(), errorDetails));
        }
    }
}
