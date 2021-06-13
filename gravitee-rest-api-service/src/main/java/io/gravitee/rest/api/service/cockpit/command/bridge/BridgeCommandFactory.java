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
import io.gravitee.cockpit.api.command.bridge.BridgeTarget;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import org.springframework.stereotype.Component;

@Component
public class BridgeCommandFactory {

    private static final String BRIDGE_SCOPE_APIM = "APIM";
    private static final long BRIDGE_OPERATION_TIMEOUT = 3000L;

    private final InstallationService installationService;

    public BridgeCommandFactory(InstallationService installationService) {
        this.installationService = installationService;
    }

    public BridgeCommand createListEnvironmentCommand() {
        BridgeCommand listEnvironmentCommand = initBridgeCommand();

        BridgeTarget target = new BridgeTarget();
        target.setScopes(Collections.singletonList(BRIDGE_SCOPE_APIM));
        listEnvironmentCommand.setTarget(target);

        listEnvironmentCommand.setOperation(BridgeOperation.LIST_ENVIRONMENT.name());

        return listEnvironmentCommand;
    }

    private BridgeCommand initBridgeCommand() {
        BridgeCommand command = new BridgeCommand();
        command.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        command.setOrganizationId(GraviteeContext.getCurrentOrganization());
        command.setInstallationId(installationService.get().getId());
        command.setTimeoutMillis(BRIDGE_OPERATION_TIMEOUT);
        return command;
    }
}
