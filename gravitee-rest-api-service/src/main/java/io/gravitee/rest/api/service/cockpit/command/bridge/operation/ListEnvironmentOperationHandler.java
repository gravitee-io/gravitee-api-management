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
package io.gravitee.rest.api.service.cockpit.command.bridge.operation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.cockpit.api.command.CommandStatus;
import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgeMultiReply;
import io.gravitee.cockpit.api.command.bridge.BridgeReply;
import io.gravitee.cockpit.api.command.bridge.BridgeSimpleReply;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.reactivex.Single;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ListEnvironmentOperationHandler implements BridgeOperationHandler {

    private final Logger logger = LoggerFactory.getLogger(ListEnvironmentOperationHandler.class);

    private final EnvironmentService environmentService;
    private final InstallationService installationService;
    private final ObjectMapper objectMapper;

    public ListEnvironmentOperationHandler(
        EnvironmentService environmentService,
        InstallationService installationService,
        ObjectMapper objectMapper
    ) {
        this.environmentService = environmentService;
        this.installationService = installationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canHandle(String bridgeOperation) {
        return Objects.equals(BridgeOperation.LIST_ENVIRONMENT.name(), bridgeOperation);
    }

    @Override
    public Single<BridgeReply> handle(BridgeCommand bridgeCommand) {
        BridgeMultiReply multiReply = new BridgeMultiReply();
        multiReply.setCommandId(bridgeCommand.getId());
        try {
            final List<EnvironmentEntity> managedEnvironments =
                this.environmentService.findByOrganization(bridgeCommand.getOrganizationId());
            multiReply.setCommandStatus(CommandStatus.SUCCEEDED);
            multiReply.setReplies(
                managedEnvironments
                    .stream()
                    .map(
                        environmentEntity -> {
                            BridgeSimpleReply simpleReply = new BridgeSimpleReply();
                            simpleReply.setCommandId(bridgeCommand.getId());
                            simpleReply.setCommandStatus(CommandStatus.SUCCEEDED);
                            simpleReply.setOrganizationId(environmentEntity.getOrganizationId());
                            simpleReply.setEnvironmentId(environmentEntity.getId());
                            simpleReply.setInstallationId(installationService.get().getId());
                            try {
                                simpleReply.setPayload(objectMapper.writeValueAsString(environmentEntity));
                            } catch (JsonProcessingException e) {
                                logger.warn("Problem while serializing environment {}", environmentEntity.getId());
                                simpleReply.setMessage("Problem while serializing environment: " + environmentEntity.getId());
                                simpleReply.setCommandStatus(CommandStatus.ERROR);
                            }
                            return simpleReply;
                        }
                    )
                    .collect(Collectors.toList())
            );
        } catch (TechnicalManagementException ex) {
            multiReply.setCommandStatus(CommandStatus.ERROR);
            multiReply.setMessage("No environment available for organization: " + bridgeCommand.getOrganizationId());
        }
        return Single.just(multiReply);
    }
}
