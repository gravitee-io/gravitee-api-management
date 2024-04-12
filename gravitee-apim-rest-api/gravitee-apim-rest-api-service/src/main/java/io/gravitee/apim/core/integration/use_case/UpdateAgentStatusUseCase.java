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
package io.gravitee.apim.core.integration.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.Integration;

@UseCase
public class UpdateAgentStatusUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final EnvironmentCrudService environmentCrudService;

    public UpdateAgentStatusUseCase(IntegrationCrudService integrationCrudService, EnvironmentCrudService environmentCrudService) {
        this.integrationCrudService = integrationCrudService;
        this.environmentCrudService = environmentCrudService;
    }

    public Output execute(Input input) {
        return integrationCrudService
            .findById(input.integrationId)
            .filter(integration -> {
                if (input.agentStatus == Integration.AgentStatus.DISCONNECTED) {
                    // No need to check the environment as the integration was connected
                    return true;
                }
                var environment = environmentCrudService.get(integration.getEnvironmentId());
                return environment.getOrganizationId().equals(input.organizationId);
            })
            .map(integration -> {
                if (input.agentStatus == Integration.AgentStatus.CONNECTED && !integration.getProvider().equals(input.provider)) {
                    return new Output(
                        false,
                        String.format(
                            "Integration [id=%s] does not match. Expected provider [provider=%s]",
                            integration.getId(),
                            integration.getProvider()
                        )
                    );
                }
                integrationCrudService.update(
                    switch (input.agentStatus) {
                        case CONNECTED -> integration.agentConnected();
                        case DISCONNECTED -> integration.agentDisconnected();
                    }
                );
                return new Output(true);
            })
            .orElse(new Output(false, String.format("Integration [id=%s] not found", input.integrationId)));
    }

    public record Input(String organizationId, String integrationId, String provider, Integration.AgentStatus agentStatus) {
        public Input(String integrationId, Integration.AgentStatus agentStatus) {
            this(null, integrationId, null, agentStatus);
        }
    }

    public record Output(boolean success, String message) {
        public Output(boolean success) {
            this(success, null);
        }
    }
}
