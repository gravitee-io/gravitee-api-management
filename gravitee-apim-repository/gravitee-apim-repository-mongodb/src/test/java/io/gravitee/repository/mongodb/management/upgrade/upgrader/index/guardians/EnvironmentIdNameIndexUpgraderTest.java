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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.guardians;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import org.bson.Document;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class EnvironmentIdNameIndexUpgraderTest {

    @Test
    void should_index_the_name_of_a_guardian_within_its_environment() {
        Index index = new EnvironmentIdNameIndexUpgrader().buildIndex();

        assertThat(index.getCollection()).isEqualTo("guardians");
        assertThat(index.options().getName()).isEqualTo("e1n1");

        Document keys = index.toIndexDefinition().getIndexKeys();
        assertThat(keys).hasSize(2);
        assertThat(keys.get("environmentId")).isEqualTo(1);
        assertThat(keys.get("name")).isEqualTo(1);
    }

    /** Without this, the duplicate-key handling the repository adapter carries can never fire on Mongo. */
    @Test
    void should_refuse_two_guardians_of_one_environment_sharing_a_name() {
        Document options = new EnvironmentIdNameIndexUpgrader()
            .buildIndex()
            .toIndexDefinition()
            .getIndexOptions();

        assertThat(options.get("unique")).isEqualTo(true);
    }
}
