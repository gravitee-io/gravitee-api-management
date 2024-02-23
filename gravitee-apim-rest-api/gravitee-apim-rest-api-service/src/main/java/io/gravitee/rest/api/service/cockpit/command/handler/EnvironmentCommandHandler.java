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
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.environment.EnvironmentCommand;
import io.gravitee.cockpit.api.command.v1.environment.EnvironmentCommandPayload;
import io.gravitee.cockpit.api.command.v1.environment.EnvironmentReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.exchange.api.command.CommandStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UpdateEnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
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
public class EnvironmentCommandHandler implements CommandHandler<EnvironmentCommand, EnvironmentReply> {

    private final EnvironmentService environmentService;
    private final AccessPointCrudService accessPointService;

    @Override
    public String supportType() {
        return CockpitCommandType.ENVIRONMENT.name();
    }

    @Override
    public Single<EnvironmentReply> handle(EnvironmentCommand command) {
        EnvironmentCommandPayload environmentPayload = command.getPayload();

        try {
            EnvironmentEntity existingEnvironment = this.getEnvironment(environmentPayload);

            UpdateEnvironmentEntity newEnvironment = new UpdateEnvironmentEntity();
            newEnvironment.setCockpitId(environmentPayload.cockpitId());
            newEnvironment.setHrids(environmentPayload.hrids());
            newEnvironment.setName(environmentPayload.name());
            newEnvironment.setDescription(environmentPayload.description());

            final EnvironmentEntity environment = environmentService.createOrUpdate(
                existingEnvironment != null ? existingEnvironment.getOrganizationId() : environmentPayload.organizationId(),
                existingEnvironment != null ? existingEnvironment.getId() : environmentPayload.id(),
                newEnvironment
            );
            List<io.gravitee.apim.core.access_point.model.AccessPoint> accessPointsToCreate;
            if (environmentPayload.accessPoints() != null) {
                accessPointsToCreate =
                    environmentPayload
                        .accessPoints()
                        .stream()
                        .map(cockpitAccessPoint ->
                            io.gravitee.apim.core.access_point.model.AccessPoint
                                .builder()
                                .referenceType(io.gravitee.apim.core.access_point.model.AccessPoint.ReferenceType.ENVIRONMENT)
                                .referenceId(environment.getId())
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
                io.gravitee.apim.core.access_point.model.AccessPoint.ReferenceType.ENVIRONMENT,
                environment.getId(),
                accessPointsToCreate
            );
            log.info("Environment [{}] handled with id [{}].", environment.getName(), environment.getId());
            return Single.just(new EnvironmentReply(command.getId()));
        } catch (Exception e) {
            String errorDetails =
                "Error occurred when handling environment [%s] with id [%s]".formatted(environmentPayload.name(), environmentPayload.id());
            log.error(errorDetails, e);
            return Single.just(new EnvironmentReply(command.getId(), errorDetails));
        }
    }

    private EnvironmentEntity getEnvironment(EnvironmentCommandPayload environmentPayload) {
        try {
            return this.environmentService.findByCockpitId(environmentPayload.cockpitId());
        } catch (EnvironmentNotFoundException ex) {
            return null;
        }
    }
}
