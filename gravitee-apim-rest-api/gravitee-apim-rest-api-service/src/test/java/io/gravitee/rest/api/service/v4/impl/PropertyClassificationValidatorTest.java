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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.rest.api.model.v4.api.properties.PropertyEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropertyClassificationValidatorTest {

    @Test
    void rejects_encrypted_to_plain() {
        var existing = List.of(new Property("secret", "cipher", true, false)); // stored encrypted
        var incoming = List.of(new PropertyEntity("secret", "cipher", false, false)); // encryptable=false, encrypted=false
        assertThatThrownBy(() -> PropertyClassificationValidator.rejectEncryptedToPlain(existing, incoming))
            .isInstanceOf(InvalidDataException.class)
            .hasMessageContaining("secret");
    }

    @Test
    void allows_plain_to_encrypted() {
        var existing = List.of(new Property("k", "v", false, false));
        var incoming = List.of(new PropertyEntity("k", "v", true, false)); // encryptable=true (D10 exception)
        assertThatCode(() -> PropertyClassificationValidator.rejectEncryptedToPlain(existing, incoming)).doesNotThrowAnyException();
    }

    @Test
    void allows_renew_of_encrypted() {
        var existing = List.of(new Property("k", "cipher", true, false));
        var incoming = List.of(new PropertyEntity("k", "", true, false)); // renew: encryptable=true, encrypted=false
        assertThatCode(() -> PropertyClassificationValidator.rejectEncryptedToPlain(existing, incoming)).doesNotThrowAnyException();
    }

    @Test
    void allows_unchanged_encrypted() {
        var existing = List.of(new Property("k", "cipher", true, false));
        var incoming = List.of(new PropertyEntity("k", "cipher", false, true)); // stays encrypted=true
        assertThatCode(() -> PropertyClassificationValidator.rejectEncryptedToPlain(existing, incoming)).doesNotThrowAnyException();
    }

    @Test
    void ignores_new_keys_and_nulls() {
        var existing = List.of(new Property("k", "cipher", true, false));
        var incoming = List.of(new PropertyEntity("brand-new", "v", false, false));
        assertThatCode(() -> PropertyClassificationValidator.rejectEncryptedToPlain(existing, incoming)).doesNotThrowAnyException();
        assertThatCode(() -> PropertyClassificationValidator.rejectEncryptedToPlain(null, incoming)).doesNotThrowAnyException();
    }
}
