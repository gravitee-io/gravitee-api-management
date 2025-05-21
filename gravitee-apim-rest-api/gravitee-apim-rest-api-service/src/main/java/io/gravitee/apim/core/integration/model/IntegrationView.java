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
package io.gravitee.apim.core.integration.model;

import io.gravitee.apim.core.async_job.model.AsyncJob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationView {

    AgentStatus agentStatus;
    Integration integration;

    AsyncJob pendingJob;
    PrimaryOwner primaryOwner;

    public enum AgentStatus {
        CONNECTED,
        DISCONNECTED,
    }

    public IntegrationView(
        Integration.ApiIntegration integration,
        AgentStatus agentStatus,
        AsyncJob pendingJob,
        PrimaryOwner primaryOwner
    ) {
        this.integration = integration;
        this.agentStatus = agentStatus;
        this.pendingJob = pendingJob;
        this.primaryOwner = primaryOwner;
    }

    public IntegrationView(Integration.A2aIntegration integration, PrimaryOwner primaryOwner) {
        this.integration = integration;
        this.agentStatus = null;
        this.pendingJob = null;
        this.primaryOwner = primaryOwner;
    }

    public Integration toIntegration() {
        return integration;
    }

    public record PrimaryOwner(String id, String email, String displayName) {}
}
