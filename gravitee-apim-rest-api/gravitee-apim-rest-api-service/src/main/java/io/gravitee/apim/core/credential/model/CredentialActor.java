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
package io.gravitee.apim.core.credential.model;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.credential.exception.SecretPermissionRequiredException;
import java.util.Set;

/**
 * The user who acts on credentials, and the environment they act in.
 *
 * @param admin whether the user administers the organization or the environment, which lets them manage any credential
 * @param secretAccess the secret permissions the user holds, needed when Gravitee stores the values of a credential or a
 *     credential references a secret Gravitee stores
 */
public record CredentialActor(AuditInfo auditInfo, boolean admin, Set<SecretAccess> secretAccess) {
    public enum SecretAccess {
        READ,
        CREATE,
        UPDATE,
        DELETE,
    }

    public String userId() {
        return auditInfo.actor().userId();
    }

    public String organizationId() {
        return auditInfo.organizationId();
    }

    public String environmentId() {
        return auditInfo.environmentId();
    }

    public void requireSecretAccess(SecretAccess access) {
        if (!secretAccess.contains(access)) {
            throw new SecretPermissionRequiredException(access);
        }
    }
}
