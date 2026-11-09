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
package inmemory;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** A reversible stand-in that, like the real encryptor, decrypts only for the secret it encrypted for. */
public class SecretValueEncryptorInMemory implements SecretValueEncryptor {

    @Override
    public String encrypt(Secret secret, String plaintext) {
        return boundTo(secret) + Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String decrypt(Secret secret, String ciphertext) {
        if (!ciphertext.startsWith(boundTo(secret))) {
            throw new TechnicalDomainException("Failed to decrypt the value of secret " + secret.id());
        }
        return new String(Base64.getDecoder().decode(ciphertext.substring(boundTo(secret).length())), StandardCharsets.UTF_8);
    }

    private static String boundTo(Secret secret) {
        return String.join("/", secret.organizationId(), secret.environmentId(), secret.id()) + ":";
    }
}
