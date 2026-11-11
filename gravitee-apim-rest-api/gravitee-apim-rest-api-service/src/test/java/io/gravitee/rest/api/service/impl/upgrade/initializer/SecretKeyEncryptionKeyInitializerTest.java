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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.secret.EnvelopeSecretValueEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecretKeyEncryptionKeyInitializerTest {

    @Mock
    private EnvelopeSecretValueEncryptor encryptor;

    @Test
    void should_check_the_key_encryption_key_against_the_stored_keys_on_each_start() {
        var initializer = new SecretKeyEncryptionKeyInitializer(encryptor);

        assertThat(initializer.initialize()).isTrue();
        verify(encryptor).verifyStoredKeys();
    }

    @Test
    void should_not_fail_the_start_when_the_stored_keys_cannot_be_read() {
        doThrow(new TechnicalDomainException("Failed to read the key encryption key ids of the stored secrets"))
            .when(encryptor)
            .verifyStoredKeys();
        var initializer = new SecretKeyEncryptionKeyInitializer(encryptor);

        assertThat(initializer.initialize()).isTrue();
    }

    @Test
    void should_run_after_the_default_initializers() {
        assertThat(new SecretKeyEncryptionKeyInitializer(encryptor).getOrder()).isEqualTo(
            InitializerOrder.SECRET_KEY_ENCRYPTION_KEY_INITIALIZER
        );
    }
}
