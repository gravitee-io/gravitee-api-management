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
package io.gravitee.apim.core.credential;

import inmemory.SecretCrudServiceInMemory;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The secrets of the credentials under test. Each secret gets its own id, as the use case tests fix the generated id, and
 * a write fails when the secret key ends with a field given to {@link #failOn(String)}, a delete when it ends with a field
 * given to {@link #failDeleteOf(String)}.
 */
public class CredentialSecretsInMemory extends SecretCrudServiceInMemory {

    private static final String SEALED = "sealed:";

    /** Not bound to the secret id, so a secret can take the id this store gives it. */
    public final SecretValueEncryptor encryptor = new SecretValueEncryptor() {
        @Override
        public String encrypt(Secret secret, String plaintext) {
            return SEALED + plaintext;
        }

        @Override
        public String decrypt(Secret secret, String ciphertext) {
            return ciphertext.substring(SEALED.length());
        }
    };

    private final Set<String> failingFields = new HashSet<>();
    private final Set<String> failingDeletes = new HashSet<>();
    private int sequence;

    public void failOn(String field) {
        failingFields.add(field);
    }

    public void failDeleteOf(String field) {
        failingDeletes.add(field);
    }

    public Optional<String> valueOf(String key) {
        return storage
            .stream()
            .filter(s -> s.key().equals(key))
            .findFirst()
            .flatMap(s -> encryptedValueOf(s.id()))
            .map(ciphertext -> encryptor.decrypt(null, ciphertext));
    }

    public Set<String> keys() {
        return storage.stream().map(Secret::key).collect(Collectors.toSet());
    }

    public void removeSecret(String id) {
        storage
            .stream()
            .filter(s -> s.id().equals(id))
            .findFirst()
            .ifPresent(super::delete);
    }

    @Override
    public Secret create(Secret secret, String encryptedValue) {
        failIfAsked(failingFields, secret.key());
        return super.create(secret.toBuilder().id("secret-" + (++sequence)).build(), encryptedValue);
    }

    @Override
    public Secret update(Secret secret, String encryptedValue) {
        failIfAsked(failingFields, secret.key());
        return super.update(secret, encryptedValue);
    }

    @Override
    public void delete(Secret secret) {
        failIfAsked(failingDeletes, secret.key());
        super.delete(secret);
    }

    private static void failIfAsked(Set<String> fields, String key) {
        if (fields.stream().anyMatch(field -> key.endsWith("." + field))) {
            throw new IllegalStateException("secret store unavailable for " + key);
        }
    }
}
