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
package io.gravitee.rest.api.service.cockpit.command.bridge;

import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommand;
import io.gravitee.cockpit.api.command.v1.bridge.BridgeCommandPayload;
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
        BridgeCommandPayload.BridgeTarget target = new BridgeCommandPayload.BridgeTarget(
            Collections.singletonList(BRIDGE_SCOPE_APIM),
            null
        );
        return new BridgeCommand(
            BridgeCommandPayload
                .builder()
                .environmentId(environmentId)
                .organizationId(organizationId)
                .installationId(installationService.get().getId())
                .operation(BridgeOperation.LIST_ENVIRONMENT.name())
                .target(target)
                .build()
        );
    }

    public BridgeCommand createPromoteApiCommand(
        String organizationId,
        String environmentId,
        String targetEnvironmentId,
        String serializedPromotion
    ) {
        BridgeCommandPayload.BridgeTarget target = new BridgeCommandPayload.BridgeTarget(
            Collections.singletonList(BRIDGE_SCOPE_APIM),
            targetEnvironmentId
        );
        return new BridgeCommand(
            BridgeCommandPayload
                .builder()
                .environmentId(environmentId)
                .organizationId(organizationId)
                .installationId(installationService.get().getId())
                .operation(BridgeOperation.PROMOTE_API.name())
                .target(target)
                .content(serializedPromotion)
                .build()
        );
    }

    public BridgeCommand createProcessPromotionCommand(
        String organizationId,
        String environmentId,
        String sourceEnvCockpitId,
        String serializedPromotion
    ) {
        BridgeCommandPayload.BridgeTarget target = new BridgeCommandPayload.BridgeTarget(
            Collections.singletonList(BRIDGE_SCOPE_APIM),
            sourceEnvCockpitId
        );
        return new BridgeCommand(
            BridgeCommandPayload
                .builder()
                .environmentId(environmentId)
                .organizationId(organizationId)
                .installationId(installationService.get().getId())
                .operation(BridgeOperation.PROCESS_API_PROMOTION.name())
                .target(target)
                .content(serializedPromotion)
                .build()
        );
    }
}
