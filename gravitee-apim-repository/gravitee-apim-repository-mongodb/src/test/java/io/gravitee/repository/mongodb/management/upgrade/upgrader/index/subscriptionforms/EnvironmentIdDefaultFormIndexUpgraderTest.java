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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.subscriptionforms;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class EnvironmentIdDefaultFormIndexUpgraderTest {

    @Test
    void buildIndex_definesUniqueIndexOnSubscriptionFormsWithExpectedNameAndKeys() {
        Index index = new EnvironmentIdDefaultFormIndexUpgrader().buildIndex();

        assertThat(index.getCollection()).isEqualTo("subscription_forms");
        assertThat(index.options().getName()).isEqualTo("ei1df1_unique");
        assertThat(index.options().isUnique()).isTrue();

        Document keys = index.toIndexDefinition().getIndexKeys();
        assertThat(keys).hasSize(2);
        assertThat(keys.get("environmentId")).isEqualTo(1);
        assertThat(keys.get("defaultForm")).isEqualTo(1);
    }

    @Test
    void buildIndex_restrictsTheIndexToTheDefaultFormsOfTheEnvironment() {
        Index index = new EnvironmentIdDefaultFormIndexUpgrader().buildIndex();

        Document options = index.toIndexDefinition().getIndexOptions();

        // Spring Data reads that option by casting it to a Document, any other Bson implementation fails at startup.
        assertThat(options.get("partialFilterExpression")).isEqualTo(
            new Document("defaultForm", true)
        );
    }
}
