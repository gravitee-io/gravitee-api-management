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
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationView extends Integration {

    AgentStatus agentStatus;

    AsyncJob pendingJob;
    PrimaryOwner primaryOwner;

    public enum AgentStatus {
        CONNECTED,
        DISCONNECTED,
    }

    public IntegrationView(Integration integration, AgentStatus agentStatus) {
        this(integration, agentStatus, null, null);
    }

    public IntegrationView(Integration integration, AgentStatus agentStatus, AsyncJob pendingJob, PrimaryOwner primaryOwner) {
        super(
            integration.getId(),
            integration.getName(),
            integration.getDescription(),
            integration.getProvider(),
            integration.getEnvironmentId(),
            integration.getCreatedAt(),
            integration.getUpdatedAt(),
            integration.getGroups()
        );
        this.agentStatus = agentStatus;
        this.pendingJob = pendingJob;
        this.primaryOwner = primaryOwner;
    }

    public Integration toIntegration() {
        return super.toBuilder().build();
    }

    public record PrimaryOwner(String id, String email, String displayName) {}
}
