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
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.property.Property;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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
        var result = cut.encryptProperties(null, List.of(encryptedProperty, encryptableProperty, notEncryptableDynamicProperty));

        // Then
        verify(dataEncryptor, times(1)).encrypt(anyString());

        assertThat(result).containsExactly(
            Property.builder().key("encrypted").value("encrypted").dynamic(false).encrypted(true).build(),
            Property.builder().key("encryptable").value("encrypted value").dynamic(false).encrypted(true).build(),
            Property.builder().key("notEncryptableDynamic").value("not encrypted").dynamic(true).encrypted(false).build()
        );
    }

    @Test
    public void should_return_properties_non_encrypted_on_error() throws GeneralSecurityException {
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

        when(dataEncryptor.encrypt(eq(encryptableProperty.getValue()))).thenThrow(new GeneralSecurityException());

        // When
        var result = cut.encryptProperties(null, List.of(encryptedProperty, encryptableProperty, notEncryptableDynamicProperty));

        // Then
        verify(dataEncryptor, times(1)).encrypt(anyString());

        assertThat(result).containsExactly(
            Property.builder().key("encrypted").value("encrypted").dynamic(false).encrypted(true).build(),
            Property.builder().key("encryptable").value("not encrypted").dynamic(false).encrypted(false).build(),
            Property.builder().key("notEncryptableDynamic").value("not encrypted").dynamic(true).encrypted(false).build()
        );
    }

    @Test
    public void should_remove_null_properties() {
        // Given
        var encryptableProperties = new ArrayList<EncryptableProperty>();
        encryptableProperties.add(EncryptableProperty.builder().key("k1").value("v1").build());
        encryptableProperties.add(null);

        // When
        var result = cut.encryptProperties(null, encryptableProperties);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    public void should_handle_null_property_list() {
        // When
        var result = cut.encryptProperties(null, null);

        // Then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void should_reject_an_encrypted_property_reclassified_to_plain() throws GeneralSecurityException {
        // Given
        var stored = List.of(Property.builder().key("secret").value("cipher").dynamic(true).encrypted(true).build());
        var incoming = EncryptableProperty.builder()
            .key("secret")
            .value("plain-again")
            .dynamic(true)
            .encrypted(false)
            .encryptable(false)
            .build();

        // When / Then
        assertThatThrownBy(() -> cut.encryptProperties(stored, List.of(incoming)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("secret");
        verify(dataEncryptor, never()).encrypt(anyString());
    }

    @Test
    public void should_allow_renewing_an_encrypted_property_via_encryptable() throws GeneralSecurityException {
        // Given
        var stored = List.of(Property.builder().key("secret").value("old-cipher").dynamic(true).encrypted(true).build());
        var incoming = EncryptableProperty.builder()
            .key("secret")
            .value("new-plain")
            .dynamic(true)
            .encrypted(false)
            .encryptable(true)
            .build();
        when(dataEncryptor.encrypt("new-plain")).thenReturn("new-cipher");

        // When
        var result = cut.encryptProperties(stored, List.of(incoming));

        // Then
        assertThat(result).containsExactly(Property.builder().key("secret").value("new-cipher").dynamic(true).encrypted(true).build());
    }
}
