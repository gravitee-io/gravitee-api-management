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
package io.gravitee.gamma.module.platform.core.credential.port.service_provider;

import io.gravitee.apim.core.audit.model.AuditInfo;

/** The Gravitee secrets of the environment, which hold the values of the credentials Gravitee stores. */
public interface CredentialSecretStore {
    /**
     * @param id the id of the new secret
     * @param path the path the gateway resolves the secret value at
     */
    record StoredSecret(String id, String path) {}

    StoredSecret create(String key, String value, AuditInfo auditInfo);

    void update(String secretId, String value, AuditInfo auditInfo);

    /** Does nothing when the secret is already gone. */
    void delete(String secretId, AuditInfo auditInfo);
}
