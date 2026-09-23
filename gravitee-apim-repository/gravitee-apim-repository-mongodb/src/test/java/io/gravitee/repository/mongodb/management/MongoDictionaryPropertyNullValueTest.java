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

import io.gravitee.definition.model.dictionary.DictionaryProperty;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * Guards updates after reading a raw BSON {@code null} property. Spring Data does not invoke a
 * converter for a null source, so the malformed entry must be dropped rather than dereferenced.
 */
class MongoDictionaryPropertyNullValueTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoOperations mongoOperations;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    void should_drop_a_raw_null_property_when_updating_a_dictionary() throws Exception {
        mongoOperations
            .getCollection("test_prefix_dictionaries")
            .insertOne(
                new Document()
                    .append("_id", "dic-raw-null")
                    .append("environmentId", "DEFAULT")
                    .append("name", "Raw Null Dictionary")
                    .append("key", "dic-raw-null")
                    .append("type", "MANUAL")
                    .append("state", "STOPPED")
                    .append("properties", new Document("valid", "value").append("invalid", null))
            );

        var dictionary = dictionaryRepository.findById("dic-raw-null").orElseThrow();
        assertThat(dictionary.getProperties().get("invalid")).isNull();

        var updated = dictionaryRepository.update(dictionary);

        assertThat(updated.getProperties()).containsEntry("valid", new DictionaryProperty("value", false)).doesNotContainKey("invalid");
    }
}
