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
package io.gravitee.apim.core.api.model.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.definition.ApiDefinitionFixtures;
import io.gravitee.apim.core.api.exception.ApiPropertyEncryptedToPlainException;
import io.gravitee.definition.model.v4.property.Property;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropertyClassificationValidatorTest {

    private static final Property STORED_ENCRYPTED = Property.builder().key("secret").value("ciphertext").encrypted(true).build();

    @Test
    void rejects_an_encrypted_property_sent_back_as_plain() {
        var incoming = List.of(EncryptableProperty.builder().key("secret").value("ciphertext").build());

        assertThatThrownBy(() -> PropertyClassificationValidator.rejectEncryptedToPlain(List.of(STORED_ENCRYPTED), incoming))
            .isInstanceOf(ApiPropertyEncryptedToPlainException.class)
            .hasMessageContaining("secret")
            .hasMessageNotContaining("ciphertext")
            .extracting("technicalCode")
            .isEqualTo("api.property.encryptedToPlain");
    }

    @Test
    void allows_an_encrypted_property_that_stays_encrypted() {
        var incoming = List.of(EncryptableProperty.builder().key("secret").value("ciphertext").encrypted(true).build());

        assertThatCode(() ->
            PropertyClassificationValidator.rejectEncryptedToPlain(List.of(STORED_ENCRYPTED), incoming)
        ).doesNotThrowAnyException();
    }

    @Test
    void allows_renewing_an_encrypted_property() {
        var incoming = List.of(EncryptableProperty.builder().key("secret").value("new plaintext").encryptable(true).build());

        assertThatCode(() ->
            PropertyClassificationValidator.rejectEncryptedToPlain(List.of(STORED_ENCRYPTED), incoming)
        ).doesNotThrowAnyException();
    }

    @Test
    void allows_encrypting_a_plain_property() {
        var stored = List.of(Property.builder().key("k").value("v").build());
        var incoming = List.of(EncryptableProperty.builder().key("k").value("v").encryptable(true).build());

        assertThatCode(() -> PropertyClassificationValidator.rejectEncryptedToPlain(stored, incoming)).doesNotThrowAnyException();
    }

    @Test
    void ignores_new_keys_removed_keys_and_null_lists() {
        var incoming = List.of(EncryptableProperty.builder().key("brand-new").value("v").build());

        assertThatCode(() ->
            PropertyClassificationValidator.rejectEncryptedToPlain(List.of(STORED_ENCRYPTED), incoming)
        ).doesNotThrowAnyException();
        assertThatCode(() ->
            PropertyClassificationValidator.rejectEncryptedToPlain(List.of(STORED_ENCRYPTED), List.of())
        ).doesNotThrowAnyException();
        assertThatCode(() ->
            PropertyClassificationValidator.rejectEncryptedToPlain((List<Property>) null, incoming)
        ).doesNotThrowAnyException();
        assertThatCode(() ->
            PropertyClassificationValidator.rejectEncryptedToPlain(List.of(STORED_ENCRYPTED), null)
        ).doesNotThrowAnyException();
    }

    @Test
    void reads_stored_properties_from_a_v4_api() {
        var definition = ApiDefinitionFixtures.anApiV4();
        definition.setProperties(List.of(STORED_ENCRYPTED));
        var incoming = List.of(EncryptableProperty.builder().key("secret").value("ciphertext").build());

        assertThatThrownBy(() -> PropertyClassificationValidator.rejectEncryptedToPlain(definition, incoming)).isInstanceOf(
            ApiPropertyEncryptedToPlainException.class
        );
    }

    @Test
    void from_property_keeps_classification_and_is_not_encryptable() {
        var property = EncryptableProperty.fromProperty(Property.builder().key("k").value("v").encrypted(true).dynamic(true).build());

        assertThat(property.getKey()).isEqualTo("k");
        assertThat(property.getValue()).isEqualTo("v");
        assertThat(property.isEncrypted()).isTrue();
        assertThat(property.isDynamic()).isTrue();
        assertThat(property.isEncryptable()).isFalse();
    }
}
