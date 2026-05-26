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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.keys;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class RevokedExpireAtIndexUpgraderTest {

    @Test
    void buildIndex_definesCompoundIndexOnKeysWithExpectedNameAndKeys() {
        Index index = new RevokedExpireAtIndexUpgrader().buildIndex();

        assertThat(index.getCollection()).isEqualTo("keys");
        assertThat(index.options().getName()).isEqualTo("r1ea1");

        Document keys = index.toIndexDefinition().getIndexKeys();
        assertThat(keys).hasSize(2);
        assertThat(keys.get("revoked")).isEqualTo(1);
        assertThat(keys.get("expireAt")).isEqualTo(1);
    }
}
