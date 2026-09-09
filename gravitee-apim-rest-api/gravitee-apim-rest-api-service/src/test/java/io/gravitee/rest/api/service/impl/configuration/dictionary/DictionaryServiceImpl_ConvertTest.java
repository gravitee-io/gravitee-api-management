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
package io.gravitee.rest.api.service.impl.configuration.dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceImpl_ConvertTest {

    @InjectMocks
    private DictionaryServiceImpl dictionaryService = new DictionaryServiceImpl();

    @Mock
    private DictionaryRepository dictionaryRepository;

    @Mock
    private DataEncryptor dataEncryptor;

    @Test
    void should_mask_encrypted_property_on_read() throws TechnicalException {
        Dictionary dictionary = new Dictionary();
        dictionary.setId("dic-1");
        dictionary.setEnvironmentId("DEFAULT");
        dictionary.setType(DictionaryType.MANUAL);
        dictionary.setState(LifecycleState.STOPPED);
        dictionary.setCreatedAt(new Date());
        dictionary.setUpdatedAt(new Date());
        dictionary.setProperties(
            Map.of("plain", new DictionaryProperty("plain-value", false), "secret", new DictionaryProperty("ENC(cipher)", true))
        );
        when(dictionaryRepository.findById("dic-1")).thenReturn(Optional.of(dictionary));

        DictionaryEntity entity = dictionaryService.findById(new ExecutionContext("org", "DEFAULT"), "dic-1");

        assertThat(entity.getProperties()).containsEntry("plain", "plain-value");
        assertThat(entity.getProperties()).containsEntry("secret", DictionaryServiceImpl.ENCRYPTED_VALUE_MASK);
    }
}
