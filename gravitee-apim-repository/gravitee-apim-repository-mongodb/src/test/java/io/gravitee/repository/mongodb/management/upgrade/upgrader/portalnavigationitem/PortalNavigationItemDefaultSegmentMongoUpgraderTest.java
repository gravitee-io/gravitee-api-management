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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.portalnavigationitem;

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.portalnavigationitem.PortalNavigationItemDefaultSegmentMongoUpgrader.DEFAULT_SEGMENT;
import static io.gravitee.repository.mongodb.management.upgrade.upgrader.portalnavigationitem.PortalNavigationItemDefaultSegmentMongoUpgrader.SEGMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemDefaultSegmentMongoUpgraderTest extends AbstractManagementRepositoryTest {

    private static final String COLLECTION_NAME = "portal_navigation_items";

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private PortalNavigationItemDefaultSegmentMongoUpgrader upgrader;
    private String targetCollectionName;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collection directly.
        return null;
    }

    @BeforeEach
    public void initUpgrader() {
        upgrader = new PortalNavigationItemDefaultSegmentMongoUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        targetCollectionName = environment.getProperty("management.mongodb.prefix", "") + COLLECTION_NAME;
        // deleteMany, not drop: drop() also removes the collection's indexes created once at context
        // bootstrap, which would then make the notablescan-guarded updateMany fail with no index.
        mongoTemplate.getCollection(targetCollectionName).deleteMany(new Document());
    }

    @AfterEach
    public void cleanUp() {
        mongoTemplate.getCollection(targetCollectionName).deleteMany(new Document());
    }

    private Document findById(String id) {
        return mongoTemplate.getCollection(targetCollectionName).find(new Document("_id", id)).first();
    }

    @Test
    public void upgrade_should_backfill_segment_when_field_is_missing() throws Exception {
        // Given
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-missing"));

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        assertThat(findById("item-missing").getString(SEGMENT)).isEqualTo(DEFAULT_SEGMENT);
    }

    @Test
    public void upgrade_should_backfill_segment_when_field_is_null() throws Exception {
        // Given
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-null").append(SEGMENT, null));

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        assertThat(findById("item-null").getString(SEGMENT)).isEqualTo(DEFAULT_SEGMENT);
    }

    @Test
    public void upgrade_should_backfill_segment_when_field_is_empty() throws Exception {
        // Given
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-empty").append(SEGMENT, ""));

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        assertThat(findById("item-empty").getString(SEGMENT)).isEqualTo(DEFAULT_SEGMENT);
    }

    @Test
    public void upgrade_should_leave_existing_segment_untouched() throws Exception {
        // Given
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-set").append(SEGMENT, "my-page"));

        // When
        boolean result = upgrader.upgrade();

        // Then
        assertThat(result).isTrue();
        assertThat(findById("item-set").getString(SEGMENT)).isEqualTo("my-page");
    }

    @Test
    public void upgrade_should_be_idempotent() throws Exception {
        // Given
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-missing"));

        // When
        boolean first = upgrader.upgrade();
        boolean second = upgrader.upgrade();

        // Then
        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(findById("item-missing").getString(SEGMENT)).isEqualTo(DEFAULT_SEGMENT);
    }
}
