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
package io.gravitee.gamma.module.platform.infra.service_provider;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.SecretValue;
import io.gravitee.apim.core.secret.use_case.CreateSecretUseCase;
import io.gravitee.apim.core.secret.use_case.DeleteSecretUseCase;
import io.gravitee.apim.core.secret.use_case.UpdateSecretUseCase;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialSecretStore;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/** Writes credential values through the host's secret use cases, so they are encrypted and audited like any secret. */
@CustomLog
@RequiredArgsConstructor
public class ApimCredentialSecretStore implements CredentialSecretStore {

    private final CreateSecretUseCase createSecretUseCase;
    private final UpdateSecretUseCase updateSecretUseCase;
    private final DeleteSecretUseCase deleteSecretUseCase;

    @Override
    public StoredSecret create(String key, String value, AuditInfo auditInfo) {
        var secret = createSecretUseCase.execute(new CreateSecretUseCase.Input(key, new SecretValue(value), auditInfo)).secret();
        return new StoredSecret(secret.id(), secret.path());
    }

    @Override
    public void update(String secretId, String value, AuditInfo auditInfo) {
        updateSecretUseCase.execute(new UpdateSecretUseCase.Input(secretId, new SecretValue(value), auditInfo));
    }

    @Override
    public void delete(String secretId, AuditInfo auditInfo) {
        try {
            deleteSecretUseCase.execute(new DeleteSecretUseCase.Input(secretId, auditInfo));
        } catch (SecretNotFoundException e) {
            // A user can delete the secret from the secrets page first; the credential delete must still go through.
            log.info("Secret {} of a credential was already deleted", secretId);
        }
    }
}
