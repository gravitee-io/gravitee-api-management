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
package io.gravitee.apim.plugin.gamma.api.identity;

import java.util.Optional;

/**
 * Read/write access to the per-organization AM connection, shared by gamma modules.
 * The single implementation ({@link ApimAmConnectionRepository}) delegates to APIM rest-api core,
 * which owns persistence and token encryption.
 */
public interface AmConnectionRepository {
    Optional<AmConnection> findByOrg(String orgId);

    boolean hasTokenForOrg(String orgId);

    /**
     * Save semantics for {@link AmConnection#serviceAccountAccessToken()}:
     * {@code null} preserves the existing ciphertext, blank clears the saved token,
     * non-blank encrypts and replaces.
     */
    AmConnection save(String orgId, AmConnection connection);

    default AmConnection requireByOrg(String orgId) {
        return findByOrg(orgId).filter(AmConnection::isConfigured).orElseThrow(AmNotConfiguredException::new);
    }
}
