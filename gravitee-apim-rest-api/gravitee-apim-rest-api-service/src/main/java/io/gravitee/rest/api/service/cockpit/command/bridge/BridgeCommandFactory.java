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
package io.gravitee.rest.api.service.cockpit.command.bridge;

import io.gravitee.cockpit.api.command.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.bridge.BridgePayload;
import io.gravitee.cockpit.api.command.bridge.BridgeTarget;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import java.util.Collections;
import org.springframework.stereotype.Component;

@Component
public class BridgeCommandFactory {

    private static final String BRIDGE_SCOPE_APIM = "APIM";

    private final InstallationService installationService;

    public BridgeCommandFactory(InstallationService installationService) {
        this.installationService = installationService;
    }

    public BridgeCommand createListEnvironmentCommand(String organizationId, String environmentId) {
        BridgeCommand listEnvironmentCommand = initBridgeCommand(organizationId, environmentId);

        BridgeTarget target = new BridgeTarget();
        target.setScopes(Collections.singletonList(BRIDGE_SCOPE_APIM));
        listEnvironmentCommand.setTarget(target);

        listEnvironmentCommand.setOperation(BridgeOperation.LIST_ENVIRONMENT.name());

        return listEnvironmentCommand;
    }

    public BridgeCommand createPromoteApiCommand(
        String organizationId,
        String environmentId,
        String targetEnvironmentId,
        String serializedPromotion
    ) {
        BridgeCommand createPromoteApiCommand = initBridgeCommand(organizationId, environmentId);
        createPromoteApiCommand.setOperation(BridgeOperation.PROMOTE_API.name());

        BridgePayload payload = new BridgePayload();
        payload.setContent(serializedPromotion);
        createPromoteApiCommand.setPayload(payload);

        BridgeTarget target = new BridgeTarget();
        target.setScopes(Collections.singletonList(BRIDGE_SCOPE_APIM));
        target.setEnvironmentId(targetEnvironmentId);
        createPromoteApiCommand.setTarget(target);

        return createPromoteApiCommand;
    }

    public BridgeCommand createProcessPromotionCommand(
        String organizationId,
        String environmentId,
        String sourceEnvCockpitId,
        String serializedPromotion
    ) {
        BridgeCommand processPromotionCommand = initBridgeCommand(organizationId, environmentId);
        processPromotionCommand.setOperation(BridgeOperation.PROCESS_API_PROMOTION.name());

        BridgePayload payload = new BridgePayload();
        payload.setContent(serializedPromotion);
        processPromotionCommand.setPayload(payload);

        BridgeTarget target = new BridgeTarget();
        target.setScopes(Collections.singletonList(BRIDGE_SCOPE_APIM));
        target.setEnvironmentId(sourceEnvCockpitId);
        processPromotionCommand.setTarget(target);

        return processPromotionCommand;
    }

    private BridgeCommand initBridgeCommand(String organizationId, String environmentId) {
        BridgeCommand command = new BridgeCommand();
        command.setEnvironmentId(environmentId);
        command.setOrganizationId(organizationId);
        command.setInstallationId(installationService.get().getId());
        return command;
    }
}
