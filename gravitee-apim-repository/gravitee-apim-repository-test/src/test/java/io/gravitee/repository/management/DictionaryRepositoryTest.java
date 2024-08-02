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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;

public class DictionaryRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/dictionary-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Dictionary> dictionaries = dictionaryRepository.findAll();

        assertNotNull(dictionaries);
        assertEquals(6, dictionaries.size());
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
        assertEquals(6, dictionaries.size());
    }

    @Test
    public void shouldFindAllByEnvironmentsDefaultAndOtherEnv() throws Exception {
        HashSet<String> envs = new HashSet<>();
        envs.add("DEFAULT");
        envs.add("OTHER_ENV");
        final Set<Dictionary> dictionaries = dictionaryRepository.findAllByEnvironments(envs);

        assertNotNull(dictionaries);
        assertEquals(4, dictionaries.size());
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<Dictionary> optionalDictionary = dictionaryRepository.findById("dic-1");
        assertNotNull(optionalDictionary);
        assertTrue(optionalDictionary.isPresent());
        final Dictionary dictionary = optionalDictionary.get();
        Assert.assertEquals("Invalid saved environment id.", "DEFAULT", dictionary.getEnvironmentId());
        Assert.assertEquals("Invalid saved dictionary name.", "My dic 1", dictionary.getName());
        Assert.assertEquals("Invalid dictionary description.", "Description for my dic 1", dictionary.getDescription());
        Assert.assertTrue("Invalid dictionary createdAt.", compareDate(new Date(1000000000000L), dictionary.getCreatedAt()));
        Assert.assertTrue("Invalid dictionary updatedAt.", compareDate(new Date(1439032010883L), dictionary.getUpdatedAt()));
        Assert.assertEquals("Invalid dictionary properties.", 3, dictionary.getProperties().size());
        Assert.assertEquals("Invalid dictionary property.", "127.0.0.1:8082", dictionary.getProperties().get("127.0.0.1:8082"));
    }

    @Test
    public void shouldCreate() throws Exception {
        final Dictionary dictionary = new Dictionary();
        dictionary.setId("new-dictionary");
        dictionary.setEnvironmentId("DEFAULT");
        dictionary.setName("My dic 1");
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

        Assert.assertEquals(nbDictionariesBeforeCreation + 1, nbDictionariesAfterCreation);

        Optional<Dictionary> optional = dictionaryRepository.findById("new-dictionary");
        Assert.assertTrue("Dictionary saved not found", optional.isPresent());

        final Dictionary dictionarySaved = optional.get();
        Assert.assertEquals("Invalid saved environment id.", dictionary.getEnvironmentId(), dictionarySaved.getEnvironmentId());
        Assert.assertEquals("Invalid saved dictionary name.", dictionary.getName(), dictionarySaved.getName());
        Assert.assertEquals("Invalid dictionary description.", dictionary.getDescription(), dictionarySaved.getDescription());
        Assert.assertTrue("Invalid dictionary createdAt.", compareDate(dictionary.getCreatedAt(), dictionarySaved.getCreatedAt()));
        Assert.assertTrue("Invalid dictionary updatedAt.", compareDate(dictionary.getUpdatedAt(), dictionarySaved.getUpdatedAt()));
        Assert.assertEquals("Invalid dictionary type.", dictionary.getType(), dictionarySaved.getType());
        Assert.assertEquals("Invalid dictionary properties.", dictionary.getProperties(), dictionarySaved.getProperties());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Dictionary> optional = dictionaryRepository.findById("dic-1");
        Assert.assertTrue("Dictionary to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved dictionary name.", "My dic 1", optional.get().getName());

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

        Assert.assertEquals(nbDictionariesBeforeUpdate, nbDictionariesAfterUpdate);

        Optional<Dictionary> optionalUpdated = dictionaryRepository.findById("dic-1");
        Assert.assertTrue("Dictionary to update not found", optionalUpdated.isPresent());

        final Dictionary dictionaryUpdated = optionalUpdated.get();

        Assert.assertEquals("Invalid saved environment id.", dictionary.getEnvironmentId(), dictionaryUpdated.getEnvironmentId());
        Assert.assertEquals("Invalid saved dictionary name.", dictionary.getName(), dictionaryUpdated.getName());
        Assert.assertEquals("Invalid dictionary description.", dictionary.getDescription(), dictionaryUpdated.getDescription());
        Assert.assertTrue("Invalid dictionary createdAt.", compareDate(dictionary.getCreatedAt(), dictionaryUpdated.getCreatedAt()));
        Assert.assertTrue("Invalid dictionary updatedAt.", compareDate(dictionary.getUpdatedAt(), dictionaryUpdated.getUpdatedAt()));
        Assert.assertEquals("Invalid dictionary type.", dictionary.getType(), dictionaryUpdated.getType());
        Assert.assertEquals("Invalid dictionary properties.", dictionary.getProperties(), dictionaryUpdated.getProperties());
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
        Assert.assertTrue("Dictionary to update not found", optionalUpdated.isPresent());

        final Dictionary dictionaryUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved environment id.", dictionary.getEnvironmentId(), dictionaryUpdated.getEnvironmentId());
        Assert.assertEquals("Invalid saved dictionary name.", dictionary.getName(), dictionaryUpdated.getName());
        Assert.assertEquals("Invalid dictionary description.", dictionary.getDescription(), dictionaryUpdated.getDescription());
        Assert.assertTrue("Invalid dictionary createdAt.", compareDate(dictionary.getCreatedAt(), dictionaryUpdated.getCreatedAt()));
        Assert.assertTrue("Invalid dictionary updatedAt.", compareDate(dictionary.getUpdatedAt(), dictionaryUpdated.getUpdatedAt()));
        Assert.assertEquals("Invalid dictionary type.", dictionary.getType(), dictionaryUpdated.getType());
        Assert.assertEquals("Invalid dictionary properties.", dictionary.getProperties(), dictionaryUpdated.getProperties());
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbDictionariesBeforeDeletion = dictionaryRepository.findAll().size();
        dictionaryRepository.delete("dic-3");
        int nbDictionariesAfterDeletion = dictionaryRepository.findAll().size();

        Assert.assertEquals(nbDictionariesBeforeDeletion - 1, nbDictionariesAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownView() throws Exception {
        Dictionary unknownDictionary = new Dictionary();
        unknownDictionary.setId("unknown");
        dictionaryRepository.update(unknownDictionary);
        fail("An unknown dictionary should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        dictionaryRepository.update(null);
        fail("A null dictionary should not be updated");
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
