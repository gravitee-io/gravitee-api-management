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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import java.util.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DictionaryRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/dictionary-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Dictionary> dictionaries = dictionaryRepository.findAll();

        assertNotNull(dictionaries);
        assertEquals(7, dictionaries.size());
    }

    @Test
    public void shouldFindAllByEnvironments() throws Exception {
        final Set<Dictionary> dictionaries = dictionaryRepository.findAllByEnvironments(Collections.singleton("DEFAULT"));

        assertNotNull(dictionaries);
        assertEquals(3, dictionaries.size());
    }

    @Test
    public void shouldFindAllByEnvironmentEmptyList() throws Exception {
        final Set<Dictionary> dictionaries = dictionaryRepository.findAllByEnvironments(Collections.emptySet());

        assertNotNull(dictionaries);
        assertEquals(7, dictionaries.size());
    }

    @Test
    public void shouldFindAllByEnvironmentsDefaultAndOtherEnv() throws Exception {
        HashSet<String> envs = new HashSet<>();
        envs.add("DEFAULT");
        envs.add("OTHER_ENV");
        final Set<Dictionary> dictionaries = dictionaryRepository.findAllByEnvironments(envs);

        assertNotNull(dictionaries);
        assertEquals(5, dictionaries.size());
    }

    @Test
    public void shouldFindAllByEnvironmentsDefaultAndKey() throws Exception {
        Optional<Dictionary> dictionary = dictionaryRepository.findByKeyAndEnvironment("dic-1", "DEFAULT");

        assertTrue(dictionary.isPresent());
        assertEquals("dic-1", dictionary.get().getId());
        assertEquals("dic-1", dictionary.get().getKey());
    }

    @Test
    public void shouldFindAllByEnvironmentsOtherEnvAndKey() throws Exception {
        Optional<Dictionary> dictionary = dictionaryRepository.findByKeyAndEnvironment("dic-1", "OTHER_ENV");

        assertTrue(dictionary.isPresent());
        assertEquals("dic-7", dictionary.get().getId());
        assertEquals("dic-1", dictionary.get().getKey());
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<Dictionary> optionalDictionary = dictionaryRepository.findById("dic-1");
        assertNotNull(optionalDictionary);
        assertTrue(optionalDictionary.isPresent());
        final Dictionary dictionary = optionalDictionary.get();
        Assertions.assertEquals("DEFAULT", dictionary.getEnvironmentId(), "Invalid saved environment id.");
        Assertions.assertEquals("My dic 1", dictionary.getName(), "Invalid saved dictionary name.");
        Assertions.assertEquals("Description for my dic 1", dictionary.getDescription(), "Invalid dictionary description.");
        Assertions.assertTrue(compareDate(new Date(1000000000000L), dictionary.getCreatedAt()), "Invalid dictionary createdAt.");
        Assertions.assertTrue(compareDate(new Date(1439032010883L), dictionary.getUpdatedAt()), "Invalid dictionary updatedAt.");
        Assertions.assertEquals(3, dictionary.getProperties().size(), "Invalid dictionary properties.");
        Assertions.assertEquals("127.0.0.1:8082", dictionary.getProperties().get("127.0.0.1:8082"), "Invalid dictionary property.");
    }

    @Test
    public void shouldCreate() throws Exception {
        final Dictionary dictionary = new Dictionary();
        dictionary.setId("new-dictionary");
        dictionary.setEnvironmentId("DEFAULT");
        dictionary.setName("My dic 1");
        dictionary.setKey("new-dictionary");
        dictionary.setDescription("Description for my dic 1");
        dictionary.setCreatedAt(new Date(1000000000000L));
        dictionary.setUpdatedAt(new Date(1439032010883L));
        dictionary.setType(DictionaryType.MANUAL);
        final Map<String, String> properties = new HashMap<>();
        properties.put("localhost", "localhost");
        properties.put("localhost:8082", "localhost:8082");
        properties.put("127.0.0.1:8082", "127.0.0.1:8082");
        dictionary.setProperties(properties);

        int nbDictionariesBeforeCreation = dictionaryRepository.findAll().size();
        dictionaryRepository.create(dictionary);
        int nbDictionariesAfterCreation = dictionaryRepository.findAll().size();

        Assertions.assertEquals(nbDictionariesBeforeCreation + 1, nbDictionariesAfterCreation);

        Optional<Dictionary> optional = dictionaryRepository.findById("new-dictionary");
        Assertions.assertTrue(optional.isPresent(), "Dictionary saved not found");

        final Dictionary dictionarySaved = optional.get();
        Assertions.assertEquals(dictionary.getEnvironmentId(), dictionarySaved.getEnvironmentId(), "Invalid saved environment id.");
        Assertions.assertEquals(dictionary.getName(), dictionarySaved.getName(), "Invalid saved dictionary name.");
        Assertions.assertEquals(dictionary.getDescription(), dictionarySaved.getDescription(), "Invalid dictionary description.");
        Assertions.assertTrue(compareDate(dictionary.getCreatedAt(), dictionarySaved.getCreatedAt()), "Invalid dictionary createdAt.");
        Assertions.assertTrue(compareDate(dictionary.getUpdatedAt(), dictionarySaved.getUpdatedAt()), "Invalid dictionary updatedAt.");
        Assertions.assertEquals(dictionary.getType(), dictionarySaved.getType(), "Invalid dictionary type.");
        Assertions.assertEquals(dictionary.getProperties(), dictionarySaved.getProperties(), "Invalid dictionary properties.");
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Dictionary> optional = dictionaryRepository.findById("dic-1");
        Assertions.assertTrue(optional.isPresent(), "Dictionary to update not found");
        Assertions.assertEquals("My dic 1", optional.get().getName(), "Invalid saved dictionary name.");

        final Dictionary dictionary = optional.get();
        dictionary.setId("dic-1");
        dictionary.setName("My dic 1");
        dictionary.setEnvironmentId("new_DEFAULT");
        dictionary.setDescription("Description for my dic 1");
        dictionary.setCreatedAt(new Date(1000000000000L));
        dictionary.setUpdatedAt(new Date(1486771200000L));
        dictionary.setType(DictionaryType.DYNAMIC);
        final Map<String, String> properties = new HashMap<>();
        properties.put("localhost", "localhost");
        properties.put("localhost:8082", "localhost:8082");
        properties.put("127.0.0.1:8082", "127.0.0.1:8082");
        dictionary.setProperties(properties);

        int nbDictionariesBeforeUpdate = dictionaryRepository.findAll().size();
        dictionaryRepository.update(dictionary);
        int nbDictionariesAfterUpdate = dictionaryRepository.findAll().size();

        Assertions.assertEquals(nbDictionariesBeforeUpdate, nbDictionariesAfterUpdate);

        Optional<Dictionary> optionalUpdated = dictionaryRepository.findById("dic-1");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Dictionary to update not found");

        final Dictionary dictionaryUpdated = optionalUpdated.get();

        Assertions.assertEquals(dictionary.getEnvironmentId(), dictionaryUpdated.getEnvironmentId(), "Invalid saved environment id.");
        Assertions.assertEquals(dictionary.getName(), dictionaryUpdated.getName(), "Invalid saved dictionary name.");
        Assertions.assertEquals(dictionary.getDescription(), dictionaryUpdated.getDescription(), "Invalid dictionary description.");
        Assertions.assertTrue(compareDate(dictionary.getCreatedAt(), dictionaryUpdated.getCreatedAt()), "Invalid dictionary createdAt.");
        Assertions.assertTrue(compareDate(dictionary.getUpdatedAt(), dictionaryUpdated.getUpdatedAt()), "Invalid dictionary updatedAt.");
        Assertions.assertEquals(dictionary.getType(), dictionaryUpdated.getType(), "Invalid dictionary type.");
        Assertions.assertEquals(dictionary.getProperties(), dictionaryUpdated.getProperties(), "Invalid dictionary properties.");
    }

    @Test
    public void shouldUpdateWithEmptyProperties() throws Exception {
        Optional<Dictionary> optional = dictionaryRepository.findById("dic-1");

        final Dictionary dictionary = optional.get();
        dictionary.setId("dic-1");
        dictionary.setName("My dic 1");
        dictionary.setEnvironmentId("new_DEFAULT");
        dictionary.setDescription("Description for my dic 1");
        dictionary.setCreatedAt(new Date(1000000000000L));
        dictionary.setUpdatedAt(new Date(1486771200000L));
        dictionary.setType(DictionaryType.DYNAMIC);
        dictionary.setProperties(new HashMap<>());

        dictionaryRepository.update(dictionary);

        Optional<Dictionary> optionalUpdated = dictionaryRepository.findById("dic-1");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Dictionary to update not found");

        final Dictionary dictionaryUpdated = optionalUpdated.get();
        Assertions.assertEquals(dictionary.getEnvironmentId(), dictionaryUpdated.getEnvironmentId(), "Invalid saved environment id.");
        Assertions.assertEquals(dictionary.getName(), dictionaryUpdated.getName(), "Invalid saved dictionary name.");
        Assertions.assertEquals(dictionary.getDescription(), dictionaryUpdated.getDescription(), "Invalid dictionary description.");
        Assertions.assertTrue(compareDate(dictionary.getCreatedAt(), dictionaryUpdated.getCreatedAt()), "Invalid dictionary createdAt.");
        Assertions.assertTrue(compareDate(dictionary.getUpdatedAt(), dictionaryUpdated.getUpdatedAt()), "Invalid dictionary updatedAt.");
        Assertions.assertEquals(dictionary.getType(), dictionaryUpdated.getType(), "Invalid dictionary type.");
        Assertions.assertEquals(dictionary.getProperties(), dictionaryUpdated.getProperties(), "Invalid dictionary properties.");
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbDictionariesBeforeDeletion = dictionaryRepository.findAll().size();
        dictionaryRepository.delete("dic-3");
        int nbDictionariesAfterDeletion = dictionaryRepository.findAll().size();

        Assertions.assertEquals(nbDictionariesBeforeDeletion - 1, nbDictionariesAfterDeletion);
    }

    @Test
    public void shouldNotUpdateUnknownView() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Dictionary unknownDictionary = new Dictionary();
            unknownDictionary.setId("unknown");
            dictionaryRepository.update(unknownDictionary);
            fail("An unknown dictionary should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            dictionaryRepository.update(null);
            fail("A null dictionary should not be updated");
        });
    }

    @Test
    public void should_delete_by_environment_id() throws Exception {
        final var nbBeforeDeletion = dictionaryRepository.findAllByEnvironments(Set.of("ToBeDeleted")).size();
        final var deleted = dictionaryRepository.deleteByEnvironmentId("ToBeDeleted").size();
        final var nbAfterDeletion = dictionaryRepository.findAllByEnvironments(Set.of("ToBeDeleted")).size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(2, deleted);
        assertEquals(0, nbAfterDeletion);
    }
}
