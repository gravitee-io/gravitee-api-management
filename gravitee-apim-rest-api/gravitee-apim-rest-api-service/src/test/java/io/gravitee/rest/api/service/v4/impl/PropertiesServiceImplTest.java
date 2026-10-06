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

import static io.gravitee.apim.core.utils.EncryptedValueMask.ENCRYPTED_VALUE_MASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.api.exception.MaskedApiPropertyValueException;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.rest.api.model.v4.api.properties.PropertyEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.PropertiesService;
import java.security.GeneralSecurityException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PropertiesServiceImplTest {

    private PropertiesService propertiesService;

    @Mock
    private DataEncryptor dataEncryptor;

    @BeforeEach
    public void before() {
        propertiesService = new PropertiesServiceImpl(dataEncryptor);
    }

    @Test
    public void encryptProperties_should_call_data_encryptor_for_each_encryptable_property_not_yet_encrypted()
        throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();

        propertiesService.encryptProperties(null, properties);

        verify(dataEncryptor, times(1)).encrypt("value2");
        verify(dataEncryptor, times(1)).encrypt("value4");
        verifyNoMoreInteractions(dataEncryptor);
    }

    @Test
    public void encryptProperties_should_set_encrypted_boolean_true_for_each_encrypted_property() throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();

        propertiesService.encryptProperties(null, properties);

        assertFalse(properties.get(0).isEncrypted());
        assertTrue(properties.get(1).isEncrypted());
        assertTrue(properties.get(2).isEncrypted());
        assertTrue(properties.get(3).isEncrypted());
    }

    @Test
    public void encryptProperties_should_set_value_of_each_encrypted_property() throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();
        when(dataEncryptor.encrypt("value2")).thenReturn("encryptedValue2");
        when(dataEncryptor.encrypt("value4")).thenReturn("encryptedValue4");

        propertiesService.encryptProperties(null, properties);

        assertEquals("value1", properties.get(0).getValue());
        assertEquals("encryptedValue2", properties.get(1).getValue());
        assertEquals("value3", properties.get(2).getValue());
        assertEquals("encryptedValue4", properties.get(3).getValue());
    }

    private List<PropertyEntity> buildProperties() {
        return List.of(
            new PropertyEntity("key1", "value1", false, false),
            new PropertyEntity("key2", "value2", true, false),
            new PropertyEntity("key3", "value3", true, true),
            new PropertyEntity("key4", "value4", true, false)
        );
    }

    @Test
    public void should_fail_without_exposing_the_value_when_encryption_fails() throws GeneralSecurityException {
        var property = new PropertyEntity("key", "plaintext", true, false);
        when(dataEncryptor.encrypt("plaintext")).thenThrow(new GeneralSecurityException());

        var throwable = catchThrowable(() -> propertiesService.encryptProperties(null, List.of(property)));

        assertThat(throwable)
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("key")
            .hasMessageNotContaining("plaintext");
    }

    @Test
    public void encryptProperties_should_keep_the_stored_encrypted_value_when_the_mask_is_resubmitted() throws GeneralSecurityException {
        PropertyEntity storedProperty = new PropertyEntity("key1", "ciphertext", true, true);
        PropertyEntity resubmittedMaskedProperty = new PropertyEntity("key1", ENCRYPTED_VALUE_MASK, true, false);

        List<PropertyEntity> result = propertiesService.encryptProperties(List.of(storedProperty), List.of(resubmittedMaskedProperty));

        verifyNoMoreInteractions(dataEncryptor);
        assertEquals("ciphertext", result.get(0).getValue());
        assertTrue(result.get(0).isEncrypted());
    }

    @Test
    public void encryptProperties_should_reject_the_mask_when_no_stored_encrypted_value_exists_for_the_key() {
        PropertyEntity resubmittedMaskedProperty = new PropertyEntity("key1", ENCRYPTED_VALUE_MASK, true, false);

        assertThrows(MaskedApiPropertyValueException.class, () ->
            propertiesService.encryptProperties(null, List.of(resubmittedMaskedProperty))
        );
    }
}
