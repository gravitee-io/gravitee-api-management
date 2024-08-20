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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.rest.api.model.PropertyEntity;
import java.security.GeneralSecurityException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServiceImplTest {

    @InjectMocks
    private ApiServiceImpl apiService;

    @Mock
    private DataEncryptor dataEncryptor;

    @Test
    public void encryptProperties_should_call_data_encryptor_for_each_encryptable_property_not_yet_encrypted()
        throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();

        apiService.encryptProperties(properties);

        verify(dataEncryptor, times(1)).encrypt("value2");
        verify(dataEncryptor, times(1)).encrypt("value4");
        verifyNoMoreInteractions(dataEncryptor);
    }

    @Test
    public void encryptProperties_should_set_encrypted_boolean_true_for_each_encrypted_property() throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();

        apiService.encryptProperties(properties);

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

        apiService.encryptProperties(properties);

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
}
