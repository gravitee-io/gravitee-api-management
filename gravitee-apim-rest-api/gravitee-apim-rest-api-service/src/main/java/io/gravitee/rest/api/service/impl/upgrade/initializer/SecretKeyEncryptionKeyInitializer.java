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
package io.gravitee.rest.api.service.impl.upgrade.initializer;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.secret.EnvelopeSecretValueEncryptor;
import io.gravitee.node.api.initializer.Initializer;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

/**
 * Checks on each start that the key encryption key is the one that wrapped the stored data keys. It never fails the
 * start: a wrong key only fails the secrets calls of the organizations whose data key it did not wrap.
 */
@CustomLog
@Component
public class SecretKeyEncryptionKeyInitializer implements Initializer {

    private final EnvelopeSecretValueEncryptor encryptor;

    public SecretKeyEncryptionKeyInitializer(EnvelopeSecretValueEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public boolean initialize() {
        try {
            encryptor.verifyStoredKeys();
        } catch (TechnicalDomainException e) {
            log.warn(
                "Could not check {} against the stored secrets at startup, it is checked again before each new data key",
                EnvelopeSecretValueEncryptor.KEY_PROPERTY,
                e
            );
        }
        return true;
    }

    @Override
    public int getOrder() {
        return InitializerOrder.SECRET_KEY_ENCRYPTION_KEY_INITIALIZER;
    }
}
