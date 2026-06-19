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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.edge;

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.edge.EdgeConfigCollectionRenameUpgrader.EDGE_CONFIG_COLLECTION_NAME;
import static io.gravitee.repository.mongodb.management.upgrade.upgrader.edge.EdgeConfigCollectionRenameUpgrader.LEGACY_COLLECTION_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

public class EdgeConfigCollectionRenameUpgraderTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private EdgeConfigCollectionRenameUpgrader upgrader;
    private String targetCollectionName;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates collections directly.
        return null;
    }

    @Before
    public void initUpgrader() {
        upgrader = new EdgeConfigCollectionRenameUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        targetCollectionName = environment.getProperty("management.mongodb.prefix", "") + EDGE_CONFIG_COLLECTION_NAME;
        dropCollections();
    }

    @After
    public void cleanUp() {
        dropCollections();
    }

    private void dropCollections() {
        mongoTemplate.getCollection(LEGACY_COLLECTION_NAME).drop();
        mongoTemplate.getCollection(targetCollectionName).drop();
    }

    @Test
    public void upgrade_should_rename_legacy_collection_to_prefixed_name() throws Exception {
        // Given
        mongoTemplate.getCollection(LEGACY_COLLECTION_NAME).insertOne(new Document("_id", "env-1").append("gatewayUrl", "http://gw"));

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        assertThat(mongoTemplate.collectionExists(LEGACY_COLLECTION_NAME)).isFalse();
        assertThat(mongoTemplate.collectionExists(targetCollectionName)).isTrue();
        Document migrated = mongoTemplate.getCollection(targetCollectionName).find(new Document("_id", "env-1")).first();
        assertThat(migrated).isNotNull();
        assertThat(migrated.getString("gatewayUrl")).isEqualTo("http://gw");
    }

    @Test
    public void upgrade_should_be_noop_when_legacy_collection_is_absent() throws Exception {
        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        assertThat(mongoTemplate.collectionExists(targetCollectionName)).isFalse();
    }

    @Test
    public void upgrade_should_skip_when_target_collection_already_exists() throws Exception {
        // Given
        mongoTemplate.getCollection(LEGACY_COLLECTION_NAME).insertOne(new Document("_id", "legacy"));
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "current"));

        // When
        boolean result = upgrader.upgrade();

        // Then both collections are left untouched to avoid data loss
        assertThat(result).isTrue();
        assertThat(mongoTemplate.getCollection(LEGACY_COLLECTION_NAME).countDocuments()).isEqualTo(1);
        assertThat(mongoTemplate.getCollection(targetCollectionName).find(new Document("_id", "current")).first()).isNotNull();
    }

    @Test
    public void upgrade_should_be_idempotent() throws Exception {
        // Given
        mongoTemplate.getCollection(LEGACY_COLLECTION_NAME).insertOne(new Document("_id", "env-1"));

        // When
        boolean first = upgrader.upgrade();
        boolean second = upgrader.upgrade();

        // Then
        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(mongoTemplate.collectionExists(LEGACY_COLLECTION_NAME)).isFalse();
        assertThat(mongoTemplate.getCollection(targetCollectionName).countDocuments()).isEqualTo(1);
    }
}
