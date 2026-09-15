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
package io.gravitee.gateway.handlers.api.manager;

import java.util.Set;

/**
 * A credential deployed to the gateway.
 *
 * @param id the credential id
 * @param environmentId the environment the credential belongs to
 * @param organizationId the organization of that environment, used for the license check
 * @param allowedApiIds the ids of the APIs allowed to resolve the credential; empty when no API is
 * @param encryptedSecret the secret, still encrypted with {@code api.properties.encryption.secret}
 * @param updatedAt when the credential was last changed, in epoch milliseconds
 */
public record DeployedCredential(
    String id,
    String environmentId,
    String organizationId,
    Set<String> allowedApiIds,
    String encryptedSecret,
    long updatedAt
) {
    public DeployedCredential {
        allowedApiIds = allowedApiIds == null ? Set.of() : Set.copyOf(allowedApiIds);
    }

    @Override
    public String toString() {
        return (
            "DeployedCredential[id=" +
            id +
            ", environmentId=" +
            environmentId +
            ", organizationId=" +
            organizationId +
            ", allowedApiIds=" +
            allowedApiIds +
            ", encryptedSecret=***, updatedAt=" +
            updatedAt +
            "]"
        );
    }
}
