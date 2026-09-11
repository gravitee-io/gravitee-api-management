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
package io.gravitee.repository.mongodb.management;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryProperty;
import io.gravitee.repository.management.model.DictionaryType;
import jakarta.inject.Inject;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * Reproduction: a dictionary saved through the repository with only unencrypted properties must
 * keep the legacy on-disk shape (a bare BSON string per key), not the new typed
 * {@code {value, encrypted}} shape — otherwise any external tooling/index doing scalar equality on
 * {@code properties.<key>} breaks the moment a dictionary is saved through the new code, and the
 * "unencrypted stays a bare string" invariant claimed for the read side never held for writes.
 */
class DictionaryPropertyWriteShapeReproTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoOperations mongoOperations;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    void should_write_an_unencrypted_property_as_a_bare_string() throws Exception {
        final Dictionary dictionary = new Dictionary();
        dictionary.setId("dic-write-shape");
        dictionary.setEnvironmentId("DEFAULT");
        dictionary.setName("Write Shape Dic");
        dictionary.setKey("dic-write-shape");
        dictionary.setType(DictionaryType.MANUAL);
        dictionary.setProperties(Map.of("hostname", new DictionaryProperty("api.example.com", false)));

        dictionaryRepository.create(dictionary);

        final Document raw = mongoOperations.getCollection("test_prefix_dictionaries").find(new Document("_id", "dic-write-shape")).first();

        assertThat(raw).isNotNull();
        final Document properties = raw.get("properties", Document.class);
        assertThat(properties.get("hostname")).isInstanceOf(String.class).isEqualTo("api.example.com");
    }
}
