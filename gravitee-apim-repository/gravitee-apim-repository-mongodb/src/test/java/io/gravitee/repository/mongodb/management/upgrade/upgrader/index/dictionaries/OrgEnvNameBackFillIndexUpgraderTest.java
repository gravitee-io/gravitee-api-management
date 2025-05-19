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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.dictionaries;

import static org.mockito.Mockito.*;

import com.mongodb.client.MongoCollection;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.Environment;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
public class OrgEnvNameBackFillIndexUpgraderTest {

    @Mock
    private DictionaryRepository dictionaryRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @InjectMocks
    private OrgEnvNameBackFillIndexUpgrader upgrader;

    @Mock
    private MongoTemplate mongoTemplate;

    @Test
    void shouldMigrateOldDictionaryIds() throws TechnicalException {
        Dictionary oldDict = new Dictionary();
        oldDict.setId("dictId");
        oldDict.setEnvironmentId("envId");

        Environment env = new Environment();
        env.setName("dev");
        env.setOrganizationId("orgId");

        when(mongoTemplate.getCollection("dictionaries"))
            .thenReturn(mock(MongoCollection.class));
        when(dictionaryRepository.findAll()).thenReturn(Set.of(oldDict));
        when(environmentRepository.findById("envId")).thenReturn(Optional.of(env));
        when(dictionaryRepository.findById("dictId:dev:orgId"))
            .thenReturn(Optional.empty());

        boolean result = upgrader.upgrade();

        Assertions.assertTrue(result);

        ArgumentCaptor<Dictionary> insertCaptor = ArgumentCaptor.forClass(
            Dictionary.class
        );
        verify(dictionaryRepository).create(insertCaptor.capture());

        Dictionary inserted = insertCaptor.getValue();
        Assertions.assertEquals("dictId:dev:orgId", inserted.getId());

        verify(dictionaryRepository).delete("dictId");
    }

    @Test
    void shouldSkipIfAlreadyMigrated() throws TechnicalException {
        Dictionary existing = new Dictionary();
        existing.setId("dictId:dev:orgId");
        existing.setEnvironmentId("envId");

        when(dictionaryRepository.findAll()).thenReturn(Set.of(existing));

        boolean result = upgrader.upgrade();

        Assertions.assertTrue(result);
        verify(dictionaryRepository, never()).create(existing);
        verify(dictionaryRepository, never()).delete(anyString());
    }

    @Test
    void shouldTestGetters() {
        Assertions.assertEquals(0, upgrader.getOrder());
        Assertions.assertEquals("v1", upgrader.version());
    }
}
