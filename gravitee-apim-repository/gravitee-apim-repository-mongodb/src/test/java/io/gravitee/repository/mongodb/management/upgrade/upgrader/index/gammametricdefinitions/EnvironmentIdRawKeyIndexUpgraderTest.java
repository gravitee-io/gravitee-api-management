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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.gammametricdefinitions;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class EnvironmentIdRawKeyIndexUpgraderTest {

    @Test
    void should_define_a_unique_index_on_environment_and_raw_key() {
        Index index = new EnvironmentIdRawKeyIndexUpgrader().buildIndex();

        assertThat(index.getCollection()).isEqualTo("gamma_metric_definitions");
        assertThat(index.options().getName()).isEqualTo("ei1rk1_unique");
        assertThat(index.options().isUnique()).isTrue();

        Document keys = index.toIndexDefinition().getIndexKeys();
        assertThat(keys).containsExactly(
            Map.entry("environmentId", 1),
            Map.entry("rawKey", 1)
        );
    }
}
