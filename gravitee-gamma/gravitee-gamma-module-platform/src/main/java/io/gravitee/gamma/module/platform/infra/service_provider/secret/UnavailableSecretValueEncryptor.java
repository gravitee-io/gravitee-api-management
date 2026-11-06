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
package io.gravitee.gamma.module.platform.infra.service_provider.secret;

import io.gravitee.gamma.module.platform.core.secret.exception.SecretEncryptionUnavailableException;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretValueEncryptor;

/** Stands in when the encryption key is missing or invalid, so that the module still starts. */
public class UnavailableSecretValueEncryptor implements SecretValueEncryptor {

    @Override
    public String encrypt(Secret secret, String plaintext) {
        throw new SecretEncryptionUnavailableException(AesGcmSecretValueEncryptor.KEY_PROPERTY);
    }

    @Override
    public String decrypt(Secret secret, String ciphertext) {
        throw new SecretEncryptionUnavailableException(AesGcmSecretValueEncryptor.KEY_PROPERTY);
    }
}
