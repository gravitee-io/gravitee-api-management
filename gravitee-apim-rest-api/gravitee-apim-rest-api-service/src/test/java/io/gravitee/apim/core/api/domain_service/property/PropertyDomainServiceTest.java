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
        void keeps_stored_property_when_decryption_fails() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("ciphertext")).thenThrow(new GeneralSecurityException("bad padding"));

            var result = cut.keepStoredEncryption(
                "api-id",
                List.of(storedEncrypted),
                List.of(Property.builder().key("secret").value("n3w").dynamic(true).build())
            );

            assertThat(result).containsExactly(storedEncrypted);
        }

        @Test
        void keeps_stored_property_when_the_stored_value_is_malformed() throws GeneralSecurityException {
            when(dataEncryptor.decrypt("ciphertext")).thenThrow(new IllegalArgumentException("Illegal base64 character"));

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
    class EncryptOnFetch {

        @Test
        void encrypts_a_not_yet_encrypted_property() throws GeneralSecurityException {
            when(dataEncryptor.encrypt("s3cret")).thenReturn("ciphertext");

            var result = cut.encryptOnFetch("api-id", List.of(Property.builder().key("secret").value("s3cret").dynamic(true).build()));

            assertThat(result).containsExactly(Property.builder().key("secret").value("ciphertext").encrypted(true).dynamic(true).build());
        }

        @Test
        void leaves_an_already_encrypted_property_untouched() {
            var alreadyEncrypted = Property.builder().key("secret").value("ciphertext").encrypted(true).dynamic(true).build();

            var result = cut.encryptOnFetch("api-id", List.of(alreadyEncrypted));

            assertThat(result).containsExactly(alreadyEncrypted);
            verifyNoInteractions(dataEncryptor);
        }

        @Test
        void keeps_a_property_plain_when_its_encryption_fails_while_another_still_encrypts() throws GeneralSecurityException {
            when(dataEncryptor.encrypt("s3cret")).thenThrow(new GeneralSecurityException());
            when(dataEncryptor.encrypt("other-value")).thenReturn("other-ciphertext");

            var result = cut.encryptOnFetch(
                "api-id",
                List.of(
                    Property.builder().key("secret").value("s3cret").dynamic(true).build(),
                    Property.builder().key("other").value("other-value").dynamic(true).build()
                )
            );

            assertThat(result).containsExactly(
                Property.builder().key("secret").value("s3cret").dynamic(true).build(),
                Property.builder().key("other").value("other-ciphertext").encrypted(true).dynamic(true).build()
            );
        }
    }
}
