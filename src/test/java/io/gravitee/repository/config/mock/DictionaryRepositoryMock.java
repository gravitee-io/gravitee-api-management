/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.config.mock;

import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;

import java.util.Date;
import java.util.Set;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DictionaryRepositoryMock extends AbstractRepositoryMock<DictionaryRepository> {

    public DictionaryRepositoryMock() {
        super(DictionaryRepository.class);
    }

    @Override
    void prepare(DictionaryRepository dictionaryRepository) throws Exception {
        final Dictionary newDictionary = mock(Dictionary.class);
        when(newDictionary.getName()).thenReturn("My dic 1");
        when(newDictionary.getEnvironmentId()).thenReturn("DEFAULT");
        when(newDictionary.getDescription()).thenReturn("Description for my dic 1");
        when(newDictionary.getCreatedAt()).thenReturn(new Date(1000000000000L));
        when(newDictionary.getUpdatedAt()).thenReturn(new Date(1439032010883L));
        when(newDictionary.getType()).thenReturn(DictionaryType.MANUAL);

        final Dictionary dic1 = new Dictionary();
        dic1.setId("dic-1");
        dic1.setName("My dic 1");
        dic1.setDescription("Description for my dic 1");
        dic1.setCreatedAt(new Date(1000000000000L));
        dic1.setUpdatedAt(new Date(1439032010883L));

        final Dictionary dictionaryUpdated = mock(Dictionary.class);
        when(dictionaryUpdated.getName()).thenReturn("My dic 1");
        when(dictionaryUpdated.getEnvironmentId()).thenReturn("new_DEFAULT");
        when(dictionaryUpdated.getDescription()).thenReturn("Description for my dic 1");
        when(dictionaryUpdated.getCreatedAt()).thenReturn(new Date(1000000000000L));
        when(dictionaryUpdated.getUpdatedAt()).thenReturn(new Date(1486771200000L));
        when(dictionaryUpdated.getType()).thenReturn(DictionaryType.DYNAMIC);

        final Set<Dictionary> dictionaries = newSet(newDictionary, dic1, mock(Dictionary.class));
        final Set<Dictionary> dictionariesAfterDelete = newSet(newDictionary, dic1);
        final Set<Dictionary> dictionariesAfterAdd = newSet(newDictionary, dic1, mock(Dictionary.class), mock(Dictionary.class));

        when(dictionaryRepository.findAll()).thenReturn(dictionaries, dictionariesAfterAdd, dictionaries, dictionariesAfterDelete, dictionaries);
        when(dictionaryRepository.findAllByEnvironment("DEFAULT")).thenReturn(dictionaries);
        
        when(dictionaryRepository.create(any(Dictionary.class))).thenReturn(newDictionary);

        when(dictionaryRepository.findById("new-dictionary")).thenReturn(of(newDictionary));
        when(dictionaryRepository.findById("unknown")).thenReturn(empty());
        when(dictionaryRepository.findById("dic-1")).thenReturn(of(dic1), of(dictionaryUpdated));

        when(dictionaryRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());
    }
}
