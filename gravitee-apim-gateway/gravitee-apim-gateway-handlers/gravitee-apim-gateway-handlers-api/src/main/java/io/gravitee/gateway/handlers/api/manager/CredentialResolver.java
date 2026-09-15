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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.secrets.api.el.SecretFieldAccessControl;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * Resolves one field of a credential deployed to the gateway.
 *
 * <p>A credential is only resolved while a secret field is being evaluated, which is what the
 * {@link SecretFieldAccessControl} marker says. Anywhere else the resolution is refused, so an expression in an
 * arbitrary policy cannot read a credential. Only the APIs listed on the credential may resolve it, so a reference
 * copied into another API is refused as well. The secret is decrypted on each resolution and never kept in clear text.
 */
public class CredentialResolver {

    private static final TypeReference<Map<String, Object>> FIELDS = new TypeReference<>() {};

    private final CredentialManager credentialManager;
    private final DataEncryptor dataEncryptor;
    private final ObjectMapper objectMapper;

    public CredentialResolver(CredentialManager credentialManager, DataEncryptor dataEncryptor, ObjectMapper objectMapper) {
        this.credentialManager = credentialManager;
        this.dataEncryptor = dataEncryptor;
        this.objectMapper = objectMapper;
    }

    /**
     * @param environmentId the environment of the API asking for the credential
     * @param apiId the id of the API asking for the credential
     * @param credentialId the id of the credential
     * @param field the name of the field to return, e.g. {@code clientSecret}
     * @param accessControl the marker set while a secret field is evaluated, or {@code null} outside one
     * @return the value of the field
     * @throws CredentialResolutionException when the resolution is refused, or the credential or field cannot be found or read
     */
    public String resolve(String environmentId, String apiId, String credentialId, String field, SecretFieldAccessControl accessControl) {
        if (accessControl == null || !accessControl.allowed()) {
            throw new CredentialResolutionException("Credential [%s] can only be resolved in a secret field".formatted(credentialId));
        }

        DeployedCredential credential = credentialManager
            .get(environmentId, credentialId)
            .orElseThrow(() ->
                new CredentialResolutionException("Credential [%s] not found in environment [%s]".formatted(credentialId, environmentId))
            );

        if (apiId == null || !credential.allowedApiIds().contains(apiId)) {
            throw new CredentialResolutionException("Credential [%s] is not allowed for API [%s]".formatted(credentialId, apiId));
        }

        if (decrypt(credential).get(field) instanceof String value) {
            return value;
        }
        throw new CredentialResolutionException("Field [%s] not found in credential [%s]".formatted(field, credentialId));
    }

    private Map<String, Object> decrypt(DeployedCredential credential) {
        String plaintext;
        try {
            plaintext = dataEncryptor.decrypt(credential.encryptedSecret());
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new CredentialResolutionException("Unable to decrypt credential [%s]".formatted(credential.id()), e);
        }

        Map<String, Object> fields;
        try {
            fields = objectMapper.readValue(plaintext, FIELDS);
        } catch (JsonProcessingException e) {
            // Not chained: the parser's message quotes the decrypted input, which is the secret.
            throw new CredentialResolutionException("Unable to read credential [%s]".formatted(credential.id()));
        }
        if (fields == null) {
            throw new CredentialResolutionException("Unable to read credential [%s]".formatted(credential.id()));
        }
        return fields;
    }
}
