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
package io.gravitee.apim.core.credential.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.credential.crud_service.CredentialCrudService;
import io.gravitee.apim.core.credential.exception.CredentialExpiredException;
import io.gravitee.apim.core.credential.exception.CredentialNotResolvableException;
import io.gravitee.apim.core.credential.exception.CredentialSecretMissingException;
import io.gravitee.apim.core.credential.exception.CredentialSecretNotOwnedException;
import io.gravitee.apim.core.credential.model.ResolvedCredential;
import io.gravitee.apim.core.secret.crud_service.SecretCrudService;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import io.gravitee.common.utils.TimeProvider;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CredentialResolverDomainService {

    private final CredentialCrudService credentialCrudService;
    private final SecretCrudService secretCrudService;
    private final SecretValueEncryptor secretValueEncryptor;

    /** Checks no permission and writes no audit: the caller must have checked that its user may use the credential. */
    public ResolvedCredential resolve(String organizationId, String environmentId, String credentialId) {
        var credential = credentialCrudService.get(organizationId, environmentId, credentialId);
        if (credential.expiresAt() != null && !credential.expiresAt().isAfter(TimeProvider.instantNow())) {
            throw new CredentialExpiredException(credentialId, credential.expiresAt());
        }
        var values = new LinkedHashMap<String, String>();
        credential
            .secretReferences()
            .forEach((field, reference) -> {
                if (!reference.isManaged()) {
                    throw new CredentialNotResolvableException(credentialId, field);
                }
                var secret = secretCrudService
                    .findById(organizationId, environmentId, reference.secretId())
                    .orElseThrow(() -> CredentialSecretMissingException.whenRead(credentialId, field));
                if (!secret.key().equals(Secret.CREDENTIAL_KEY_PREFIX + credentialId + "." + field)) {
                    throw new CredentialSecretNotOwnedException(credentialId, field);
                }
                values.put(field, decrypt(secret, credentialId, field));
            });
        return new ResolvedCredential(credential.strategy(), credential.configuration(), values);
    }

    private String decrypt(Secret secret, String credentialId, String field) {
        try {
            return secretValueEncryptor.decrypt(secret, secretCrudService.getEncryptedValue(secret));
        } catch (SecretNotFoundException e) {
            throw CredentialSecretMissingException.whenRead(credentialId, field);
        }
    }
}
