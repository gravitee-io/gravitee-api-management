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
package io.gravitee.gateway.handlers.api.manager.impl;

import io.gravitee.gateway.handlers.api.manager.CredentialManager;
import io.gravitee.gateway.handlers.api.manager.DeployedCredential;
import io.gravitee.node.api.license.LicenseManager;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

@CustomLog
public class CredentialManagerImpl implements CredentialManager {

    static final String LICENSE_FEATURE = "gamma-aim-module";

    private final LicenseManager licenseManager;
    private final Map<String, Map<String, DeployedCredential>> credentialsByEnvironment = new ConcurrentHashMap<>();

    public CredentialManagerImpl(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    @Override
    public void deploy(DeployedCredential credential) {
        var license = licenseManager.getOrganizationLicenseOrPlatform(credential.organizationId());
        if (license == null || !license.isFeatureEnabled(LICENSE_FEATURE)) {
            log.warn(
                "The credential [{}] can not be deployed because it is not allowed by the current license ({} feature required)",
                credential.id(),
                LICENSE_FEATURE
            );
            return;
        }

        // One atomic step per environment: remove() drops an environment's map once it is empty, so creating the map and
        // writing into it separately could write into a map that has just been dropped.
        credentialsByEnvironment.compute(credential.environmentId(), (environmentId, credentials) -> {
            Map<String, DeployedCredential> environmentCredentials = credentials != null ? credentials : new ConcurrentHashMap<>();
            environmentCredentials.merge(credential.id(), credential, (existing, incoming) ->
                incoming.updatedAt() >= existing.updatedAt() ? incoming : existing
            );
            return environmentCredentials;
        });
        log.debug("Credential [{}] deployed in environment [{}]", credential.id(), credential.environmentId());
    }

    @Override
    public void undeploy(String environmentId, String credentialId) {
        if (environmentId == null) {
            credentialsByEnvironment.keySet().forEach(knownEnvironmentId -> remove(knownEnvironmentId, credentialId));
        } else {
            remove(environmentId, credentialId);
        }
        log.debug("Credential [{}] undeployed", credentialId);
    }

    @Override
    public Optional<DeployedCredential> get(String environmentId, String credentialId) {
        if (environmentId == null || credentialId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(credentialsByEnvironment.getOrDefault(environmentId, Map.of()).get(credentialId));
    }

    private void remove(String environmentId, String credentialId) {
        credentialsByEnvironment.computeIfPresent(environmentId, (id, credentials) -> {
            credentials.remove(credentialId);
            return credentials.isEmpty() ? null : credentials;
        });
    }
}
