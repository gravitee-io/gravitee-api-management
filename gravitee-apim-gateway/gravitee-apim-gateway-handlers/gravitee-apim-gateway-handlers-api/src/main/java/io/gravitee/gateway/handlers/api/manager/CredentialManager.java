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

import java.util.Optional;

/**
 * Holds the credentials deployed to this gateway, per environment. Secrets stay encrypted here.
 */
public interface CredentialManager {
    /**
     * Deploy (or update) a credential. Ignored when the credential's organization is not licensed for it,
     * or when a newer copy of the same credential is already deployed.
     *
     * @param credential the credential to deploy
     */
    void deploy(DeployedCredential credential);

    /**
     * Undeploy a credential.
     *
     * @param environmentId the environment the credential belongs to, or {@code null} when unknown
     * @param credentialId the id of the credential
     */
    void undeploy(String environmentId, String credentialId);

    /**
     * Get a credential deployed in the given environment. A credential is never found from another environment.
     *
     * @param environmentId the environment asking for the credential
     * @param credentialId the id of the credential
     * @return the credential, or empty if it is not deployed in that environment
     */
    Optional<DeployedCredential> get(String environmentId, String credentialId);
}
