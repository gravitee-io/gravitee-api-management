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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.groups;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RemoveDeletedGroupsFromApisUpgraderTest {

    private final RemoveDeletedGroupsFromApisUpgrader upgrader = new RemoveDeletedGroupsFromApisUpgrader();

    @Test
    void mongo_build_info_is_compatible() {
        // buildInfo of a real MongoDB (including Atlas) reports "storageEngines", not "storageEngine"
        var buildInfo = new Document("version", "7.0.38").append("storageEngines", List.of("wiredTiger"));

        assertThat(upgrader.checkDatabaseCompatibility(buildInfo)).isTrue();
    }

    @Test
    void documentdb_build_info_is_not_compatible() {
        assertThat(upgrader.checkDatabaseCompatibility(new Document("version", "5.0.0"))).isFalse();
    }

    @Test
    void mongo_before_5_is_not_compatible() {
        var buildInfo = new Document("version", "4.4.29").append("storageEngines", List.of("wiredTiger"));

        assertThat(upgrader.checkDatabaseCompatibility(buildInfo)).isFalse();
    }
}
