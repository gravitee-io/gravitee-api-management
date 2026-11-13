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
package io.gravitee.rest.api.model.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Settings of an {@link io.gravitee.repository.management.model.ApplicationType#AGENT} application: a machine
 * identity acting for exactly one AI agent. The {@code entityId} is the authoritative link to that agent and is
 * required; the {@code client_id} is supplied externally (e.g. by an identity provisioning flow) — no dynamic
 * client registration is involved.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class AgentSettings {

    @JsonProperty("entity_id")
    private String entityId;

    @JsonProperty("client_id")
    private String clientId;

    /**
     * The agent identity backing this application in the identity service, when one does. It lives here rather
     * than on the agent so the whole link — which agent, which identity, which client id it presents — is one
     * object with one owner. On update, {@code null} keeps whatever is stored and an empty string clears it: an
     * omission never detaches an identity, only an explicit word does.
     */
    @JsonProperty("identity_id")
    private String identityId;

    /** An application that acts for an agent but has no identity attached yet — the common case at creation. */
    public AgentSettings(String entityId, String clientId) {
        this(entityId, clientId, null);
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }
}
