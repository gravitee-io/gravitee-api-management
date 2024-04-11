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
package io.gravitee.integration.controller.listener;

import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.use_case.UpdateAgentStatusUseCase;
import io.gravitee.exchange.api.controller.listeners.TargetListener;

/**
 * This listener will be notified by ExchangeController when events occur.
 */
public class ControllerEventListener implements TargetListener {

    private final UpdateAgentStatusUseCase updateAgentStatusUseCase;

    public ControllerEventListener(UpdateAgentStatusUseCase updateAgentStatusUseCase) {
        this.updateAgentStatusUseCase = updateAgentStatusUseCase;
    }

    /**
     * This method is called when the primary channel is evicted for a given target ID.
     *
     * <p>
     *     When it happens, it means that there is no Agent for that integration. Therefore, we can update the Agent Status.
     * </p>
     * @param targetId the ID of the target
     */
    @Override
    public void onPrimaryChannelEvicted(String targetId) {
        updateAgentStatusUseCase.execute(new UpdateAgentStatusUseCase.Input(targetId, Integration.AgentStatus.DISCONNECTED));
    }
}
