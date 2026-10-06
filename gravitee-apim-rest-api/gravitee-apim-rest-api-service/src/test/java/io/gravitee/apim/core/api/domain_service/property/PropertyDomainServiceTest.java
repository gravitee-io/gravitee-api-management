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
package io.gravitee.apim.core.api.domain_service.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fixtures.definition.ApiDefinitionFixtures;
import io.gravitee.apim.core.api.exception.ApiPropertyEncryptedToPlainException;
import io.gravitee.apim.core.api.exception.ApiPropertyNotCiphertextException;
import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.property.Property;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PropertyDomainServiceTest {

    private final DataEncryptor dataEncryptor = mock(DataEncryptor.class);
    PropertyDomainService cut;

    @BeforeEach
    public void setUp() throws Exception {
        cut = new PropertyDomainService(dataEncryptor);
    }

    @AfterEach
    public void tearDown() throws Exception {
        reset(dataEncryptor);
    }

    @Test
    public void should_encrypt_properties() throws GeneralSecurityException {
        // Given
        var encryptedProperty = EncryptableProperty.builder()
            .key("encrypted")
            .value("encrypted")
            .dynamic(false)
            .encrypted(true)
            .encryptable(true)
            .build();
        var encryptableProperty = EncryptableProperty.builder()
            .key("encryptable")
            .value("not encrypted")
            .encrypted(false)
            .dynamic(false)
            .encryptable(true)
            .build();
        var notEncryptableDynamicProperty = EncryptableProperty.builder()
            .key("notEncryptableDynamic")
            .value("not encrypted")
            .encrypted(false)
            .dynamic(true)
            .encryptable(false)
            .build();

        when(dataEncryptor.encrypt(eq(encryptableProperty.getValue()))).thenReturn("encrypted value");

        // When
        var result = cut.encryptProperties(List.of(encryptedProperty, encryptableProperty, notEncryptableDynamicProperty));

        // Then
        verify(dataEncryptor, times(1)).encrypt(anyString());

        assertThat(result).containsExactly(
            Property.builder().key("encrypted").value("encrypted").dynamic(false).encrypted(true).build(),
            Property.builder().key("encryptable").value("encrypted value").dynamic(false).encrypted(true).build(),
            Property.builder().key("notEncryptableDynamic").value("not encrypted").dynamic(true).encrypted(false).build()
        );
    }

    @Test
    public void should_fail_without_exposing_the_value_when_encryption_fails() throws GeneralSecurityException {
        var encryptableProperty = EncryptableProperty.builder().key("encryptable").value("not encrypted").encryptable(true).build();
        when(dataEncryptor.encrypt("not encrypted")).thenThrow(new GeneralSecurityException());

        assertThatThrownBy(() -> cut.encryptProperties(List.of(encryptableProperty)))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessageContaining("encryptable")
            .hasMessageNotContaining("not encrypted");
    }

    @Test
    public void should_remove_null_properties() {
        // Given
        var encryptableProperties = new ArrayList<EncryptableProperty>();
        encryptableProperties.add(EncryptableProperty.builder().build());
        encryptableProperties.add(null);

        // When
        var result = cut.encryptProperties(encryptableProperties);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    public void should_handle_null_property_list() {
        // When
        var result = cut.encryptProperties(null);

        // Then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void should_encrypt_restored_plain_values_of_keys_stored_encrypted() throws GeneralSecurityException {
        when(dataEncryptor.encrypt("old")).thenReturn("old-ciphertext");
        var stored = List.of(Property.builder().key("secret").value("current-ciphertext").encrypted(true).build());
        var restored = List.of(Property.builder().key("secret").value("old").build(), Property.builder().key("plain").value("v").build());

        var result = cut.encryptRestoredValuesOfEncryptedKeys(stored, restored);

        assertThat(result).containsExactly(
            Property.builder().key("secret").value("old-ciphertext").encrypted(true).build(),
            Property.builder().key("plain").value("v").build()
        );
    }

    @Test
    public void should_keep_restored_ciphertext_as_is() {
        var stored = List.of(Property.builder().key("secret").value("current-ciphertext").encrypted(true).build());
        var restored = List.of(Property.builder().key("secret").value("old-ciphertext").encrypted(true).build());

        assertThat(cut.encryptRestoredValuesOfEncryptedKeys(stored, restored)).isEqualTo(restored);
        verifyNoInteractions(dataEncryptor);
    }

    @Nested
    class KeepStoredEncryption {

        private final Property storedEncrypted = Property.builder().key("secret").value("ciphertext").encrypted(true).dynamic(true).build();

        @Test
        void keeps_stored_ciphertext_when_fetched_value_is_unchanged() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("ciphertext")).thenReturn("s3cret");

            var result = cut.keepStoredEncryption(
                "api-id",
                List.of(storedEncrypted),
                List.of(Property.builder().key("secret").value("s3cret").dynamic(true).build())
            );

            assertThat(result).containsExactly(storedEncrypted);
            verify(dataEncryptor, never()).encrypt(anyString());
        }

        @Test
        void re_encrypts_a_changed_value() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("ciphertext")).thenReturn("s3cret");
            when(dataEncryptor.encrypt("n3w")).thenReturn("new-ciphertext");

            var result = cut.keepStoredEncryption(
                "api-id",
                List.of(storedEncrypted),
                List.of(Property.builder().key("secret").value("n3w").dynamic(true).build())
            );

            assertThat(result).containsExactly(
                Property.builder().key("secret").value("new-ciphertext").encrypted(true).dynamic(true).build()
            );
        }

        @Test
        void passes_through_keys_not_stored_as_encrypted_dynamic() {
            var fetchedPlain = Property.builder().key("plain").value("v").dynamic(true).build();
            var fetchedShadowingManual = Property.builder().key("manual").value("v").dynamic(true).build();
            var storedManualEncrypted = Property.builder().key("manual").value("ciphertext").encrypted(true).dynamic(false).build();

            var result = cut.keepStoredEncryption("api-id", List.of(storedManualEncrypted), List.of(fetchedPlain, fetchedShadowingManual));

            assertThat(result).containsExactly(fetchedPlain, fetchedShadowingManual);
            verifyNoInteractions(dataEncryptor);
        }

        @Test
        void re_encrypts_the_fetched_value_when_the_stored_value_cannot_be_decrypted() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("ciphertext")).thenThrow(new GeneralSecurityException("bad padding"));
            when(dataEncryptor.encrypt("n3w")).thenReturn("new-ciphertext");

            var result = cut.keepStoredEncryption(
                "api-id",
                List.of(storedEncrypted),
                List.of(Property.builder().key("secret").value("n3w").dynamic(true).build())
            );

            assertThat(result).containsExactly(
                Property.builder().key("secret").value("new-ciphertext").encrypted(true).dynamic(true).build()
            );
        }

        @Test
        void re_encrypts_the_fetched_value_when_the_stored_value_is_malformed() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("ciphertext")).thenThrow(new IllegalArgumentException("Illegal base64 character"));
            when(dataEncryptor.encrypt("n3w")).thenReturn("new-ciphertext");

            var result = cut.keepStoredEncryption(
                "api-id",
                List.of(storedEncrypted),
                List.of(Property.builder().key("secret").value("n3w").dynamic(true).build())
            );

            assertThat(result).containsExactly(
                Property.builder().key("secret").value("new-ciphertext").encrypted(true).dynamic(true).build()
            );
        }

        @Test
        void keeps_stored_property_when_the_stored_value_cannot_be_decrypted_and_encryption_fails() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("ciphertext")).thenThrow(new GeneralSecurityException("bad padding"));
            when(dataEncryptor.encrypt("n3w")).thenThrow(new GeneralSecurityException("bad key"));

            var result = cut.keepStoredEncryption(
                "api-id",
                List.of(storedEncrypted),
                List.of(Property.builder().key("secret").value("n3w").dynamic(true).build())
            );

            assertThat(result).containsExactly(storedEncrypted);
        }

        @Test
        void re_encrypts_the_fetched_value_when_the_stored_value_is_null() throws GeneralSecurityException {
            var storedWithoutValue = Property.builder().key("secret").value(null).encrypted(true).dynamic(true).build();
            when(dataEncryptor.decrypt(null)).thenThrow(new NullPointerException());
            when(dataEncryptor.encrypt("n3w")).thenReturn("new-ciphertext");

            var result = cut.keepStoredEncryption(
                "api-id",
                List.of(storedWithoutValue),
                List.of(Property.builder().key("secret").value("n3w").dynamic(true).build())
            );

            assertThat(result).containsExactly(
                Property.builder().key("secret").value("new-ciphertext").encrypted(true).dynamic(true).build()
            );
        }

        @Test
        void keeps_stored_property_when_encrypting_a_changed_value_fails() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("ciphertext")).thenReturn("s3cret");
            when(dataEncryptor.encrypt("n3w")).thenThrow(new GeneralSecurityException("bad key"));

            var result = cut.keepStoredEncryption(
                "api-id",
                List.of(storedEncrypted),
                List.of(Property.builder().key("secret").value("n3w").dynamic(true).build())
            );

            assertThat(result).containsExactly(storedEncrypted);
        }

        @Test
        void keeps_first_stored_property_when_keys_are_duplicated() throws GeneralSecurityException {
            var duplicate = Property.builder().key("secret").value("other-ciphertext").encrypted(true).dynamic(true).build();
            when(dataEncryptor.decrypt("ciphertext")).thenReturn("s3cret");

            var result = cut.keepStoredEncryption(
                "api-id",
                List.of(storedEncrypted, duplicate),
                List.of(Property.builder().key("secret").value("s3cret").dynamic(true).build())
            );

            assertThat(result).containsExactly(storedEncrypted);
        }
    }

    @Nested
    class ValidateClassification {

        private final Property storedEncrypted = Property.builder().key("secret").value("ciphertext").encrypted(true).build();

        @Test
        void accepts_the_stored_ciphertext_without_decrypting() {
            var incoming = List.of(EncryptableProperty.builder().key("secret").value("ciphertext").encrypted(true).build());

            assertThatCode(() -> cut.validateClassification(List.of(storedEncrypted), incoming)).doesNotThrowAnyException();
        }

        @Test
        void accepts_other_ciphertext_this_installation_can_decrypt() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("promoted-ciphertext")).thenReturn("anything");
            var incoming = List.of(EncryptableProperty.builder().key("secret").value("promoted-ciphertext").encrypted(true).build());

            assertThatCode(() -> cut.validateClassification(List.of(storedEncrypted), incoming)).doesNotThrowAnyException();
        }

        @Test
        void rejects_a_changed_plaintext_flagged_encrypted() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("n3w-plaintext")).thenThrow(new GeneralSecurityException("bad padding"));
            var incoming = List.of(EncryptableProperty.builder().key("secret").value("n3w-plaintext").encrypted(true).build());

            assertThatThrownBy(() -> cut.validateClassification(List.of(storedEncrypted), incoming))
                .isInstanceOf(ApiPropertyNotCiphertextException.class)
                .hasMessageContaining("secret")
                .hasMessageNotContaining("n3w-plaintext")
                .extracting("technicalCode")
                .isEqualTo("api.property.notCiphertext");
        }

        @Test
        void rejects_a_value_that_is_not_base64() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("n3w!")).thenThrow(new IllegalArgumentException("Illegal base64 character"));
            var incoming = List.of(EncryptableProperty.builder().key("secret").value("n3w!").encrypted(true).build());

            assertThatThrownBy(() -> cut.validateClassification(List.of(storedEncrypted), incoming)).isInstanceOf(
                ApiPropertyNotCiphertextException.class
            );
        }

        @Test
        void rejects_a_changed_plaintext_flagged_both_encrypted_and_encryptable() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("n3w")).thenThrow(new GeneralSecurityException("bad padding"));
            var incoming = List.of(EncryptableProperty.builder().key("secret").value("n3w").encrypted(true).encryptable(true).build());

            assertThatThrownBy(() -> cut.validateClassification(List.of(storedEncrypted), incoming)).isInstanceOf(
                ApiPropertyNotCiphertextException.class
            );
        }

        @Test
        void rejects_a_null_value_flagged_encrypted() {
            var incoming = List.of(EncryptableProperty.builder().key("secret").value(null).encrypted(true).build());

            assertThatThrownBy(() -> cut.validateClassification(List.of(storedEncrypted), incoming)).isInstanceOf(
                ApiPropertyNotCiphertextException.class
            );
        }

        @Test
        void allows_renewing_with_encryptable_plaintext() {
            var incoming = List.of(EncryptableProperty.builder().key("secret").value("n3w").encryptable(true).build());

            assertThatCode(() -> cut.validateClassification(List.of(storedEncrypted), incoming)).doesNotThrowAnyException();
            verifyNoInteractions(dataEncryptor);
        }

        @Test
        void leaves_new_keys_flagged_encrypted_unchecked() {
            var incoming = List.of(EncryptableProperty.builder().key("new-key").value("whatever").encrypted(true).build());

            assertThatCode(() -> cut.validateClassification(List.of(storedEncrypted), incoming)).doesNotThrowAnyException();
            verifyNoInteractions(dataEncryptor);
        }

        @Test
        void still_rejects_making_an_encrypted_property_plain() {
            var incoming = List.of(EncryptableProperty.builder().key("secret").value("ciphertext").build());

            assertThatThrownBy(() -> cut.validateClassification(List.of(storedEncrypted), incoming)).isInstanceOf(
                ApiPropertyEncryptedToPlainException.class
            );
        }

        @Test
        void reads_stored_properties_from_a_v4_api() {
            var definition = ApiDefinitionFixtures.anApiV4();
            definition.setProperties(List.of(storedEncrypted));
            var incoming = List.of(EncryptableProperty.builder().key("secret").value("ciphertext").build());

            assertThatThrownBy(() -> cut.validateClassification(definition, incoming)).isInstanceOf(
                ApiPropertyEncryptedToPlainException.class
            );
        }
    }

    @Nested
    class EncryptOnFetch {

        @Test
        void encrypts_a_not_yet_encrypted_property() throws GeneralSecurityException {
            when(dataEncryptor.encrypt("s3cret")).thenReturn("ciphertext");

            var result = cut.encryptOnFetch(
                "api-id",
                List.of(),
                List.of(Property.builder().key("secret").value("s3cret").dynamic(true).build())
            );

            assertThat(result).containsExactly(Property.builder().key("secret").value("ciphertext").encrypted(true).dynamic(true).build());
        }

        @Test
        void leaves_an_already_encrypted_property_untouched() {
            var alreadyEncrypted = Property.builder().key("secret").value("ciphertext").encrypted(true).dynamic(true).build();

            var result = cut.encryptOnFetch("api-id", List.of(), List.of(alreadyEncrypted));

            assertThat(result).containsExactly(alreadyEncrypted);
            verifyNoInteractions(dataEncryptor);
        }

        @Test
        void drops_a_new_property_when_its_encryption_fails_while_another_still_encrypts() throws GeneralSecurityException {
            when(dataEncryptor.encrypt("s3cret")).thenThrow(new GeneralSecurityException());
            when(dataEncryptor.encrypt("other-value")).thenReturn("other-ciphertext");

            var result = cut.encryptOnFetch(
                "api-id",
                List.of(),
                List.of(
                    Property.builder().key("secret").value("s3cret").dynamic(true).build(),
                    Property.builder().key("other").value("other-value").dynamic(true).build()
                )
            );

            assertThat(result).containsExactly(
                Property.builder().key("other").value("other-ciphertext").encrypted(true).dynamic(true).build()
            );
        }

        @Test
        void falls_back_to_the_stored_property_when_its_encryption_fails() throws GeneralSecurityException {
            var stored = Property.builder().key("secret").value("ciphertext").encrypted(true).dynamic(true).build();
            when(dataEncryptor.encrypt("n3w-s3cret")).thenThrow(new GeneralSecurityException());

            var result = cut.encryptOnFetch(
                "api-id",
                List.of(stored),
                List.of(Property.builder().key("secret").value("n3w-s3cret").dynamic(true).build())
            );

            assertThat(result).containsExactly(stored);
        }
    }
}
