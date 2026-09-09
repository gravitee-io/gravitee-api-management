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
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * Reproduction: inserts a dictionary document directly as raw BSON (bypassing the normal
 * Dictionary/DictionaryMongo write path entirely), so `properties` genuinely contains a bare
 * string the way a pre-existing, never-migrated dictionary actually does on disk — unlike the
 * shared fixture-based tests, whose setup deserializes JSON through Jackson (dual-reading the
 * bare string into a typed DictionaryProperty already) before ever writing it back out, which
 * silently upgrades the stored shape and never exercises this read path.
 */
class LegacyDictionaryPropertyRawBsonReproTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoOperations mongoOperations;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    void should_read_a_genuine_legacy_raw_bson_bare_string_property() throws Exception {
        mongoOperations
            .getCollection("test_prefix_dictionaries")
            .insertOne(
                new Document()
                    .append("_id", "dic-raw-legacy")
                    .append("environmentId", "DEFAULT")
                    .append("name", "Raw Legacy Dic")
                    .append("key", "dic-raw-legacy")
                    .append("type", "MANUAL")
                    .append("properties", new Document("legacy-key", "legacy-value"))
            );

        var found = dictionaryRepository.findById("dic-raw-legacy");

        assertThat(found).isPresent();
        assertThat(found.get().getProperties().get("legacy-key").value()).isEqualTo("legacy-value");
        assertThat(found.get().getProperties().get("legacy-key").encrypted()).isFalse();
    }
}
