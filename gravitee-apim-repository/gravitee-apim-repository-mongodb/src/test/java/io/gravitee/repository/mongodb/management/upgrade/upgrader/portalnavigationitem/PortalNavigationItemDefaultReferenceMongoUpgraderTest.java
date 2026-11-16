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

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.portalnavigationitem.PortalNavigationItemDefaultReferenceMongoUpgrader.REFERENCE_ID;
import static io.gravitee.repository.mongodb.management.upgrade.upgrader.portalnavigationitem.PortalNavigationItemDefaultReferenceMongoUpgrader.UNATTACHED_REFERENCE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.model.Indexes;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.PortalNavigationReferenceType;
import jakarta.inject.Inject;
import java.util.stream.Stream;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalNavigationItemDefaultReferenceMongoUpgraderTest extends AbstractManagementRepositoryTest {

    private static final String COLLECTION_NAME = "portal_navigation_items";

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    @Inject
    private PortalNavigationItemRepository repository;

    private PortalNavigationItemDefaultReferenceMongoUpgrader upgrader;
    private String targetCollectionName;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collection directly.
        return null;
    }

    @BeforeEach
    public void initUpgrader() {
        upgrader = new PortalNavigationItemDefaultReferenceMongoUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        targetCollectionName = environment.getProperty("management.mongodb.prefix", "") + COLLECTION_NAME;
        mongoTemplate.getCollection(targetCollectionName).createIndex(Indexes.ascending("referenceType"));
        mongoTemplate.getCollection(targetCollectionName).createIndex(Indexes.ascending("referenceId"));
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
    public void upgrade_should_backfill_reference_id_when_field_is_missing() throws Exception {
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-missing"));

        boolean result = upgrader.upgrade();

        assertThat(result).isTrue();
        assertThat(findById("item-missing").getString(REFERENCE_ID)).isEqualTo(UNATTACHED_REFERENCE_ID);
        assertThat(findById("item-missing").getString("referenceType")).isEqualTo("PORTAL");
    }

    @Test
    public void upgrade_should_backfill_reference_id_when_field_is_null() throws Exception {
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-null").append(REFERENCE_ID, null));

        boolean result = upgrader.upgrade();

        assertThat(result).isTrue();
        assertThat(findById("item-null").getString(REFERENCE_ID)).isEqualTo(UNATTACHED_REFERENCE_ID);
        assertThat(findById("item-null").getString("referenceType")).isEqualTo("PORTAL");
    }

    @Test
    public void upgrade_should_backfill_reference_id_when_field_is_empty() throws Exception {
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-empty").append(REFERENCE_ID, ""));

        boolean result = upgrader.upgrade();

        assertThat(result).isTrue();
        assertThat(findById("item-empty").getString(REFERENCE_ID)).isEqualTo(UNATTACHED_REFERENCE_ID);
        assertThat(findById("item-empty").getString("referenceType")).isEqualTo("PORTAL");
    }

    @Test
    public void upgrade_should_leave_existing_reference_id_untouched() throws Exception {
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-set").append(REFERENCE_ID, "my-page"));

        boolean result = upgrader.upgrade();

        assertThat(result).isTrue();
        assertThat(findById("item-set").getString(REFERENCE_ID)).isEqualTo("my-page");
    }

    @Test
    public void upgrade_should_be_idempotent() throws Exception {
        mongoTemplate.getCollection(targetCollectionName).insertOne(new Document("_id", "item-missing"));

        boolean first = upgrader.upgrade();
        boolean second = upgrader.upgrade();

        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(findById("item-missing").getString(REFERENCE_ID)).isEqualTo(UNATTACHED_REFERENCE_ID);
        assertThat(findById("item-missing").getString("referenceType")).isEqualTo("PORTAL");
    }

    @Test
    void should_preserve_an_explicit_api_reference_and_its_other_fields() {
        var before = new Document("_id", "api-owned")
            .append("referenceType", "API")
            .append(REFERENCE_ID, "api-id")
            .append("rootId", "00000000-0000-0000-0000-000000000000");
        mongoTemplate.getCollection(targetCollectionName).insertOne(before);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(findById("api-owned")).isEqualTo(before);
    }

    @ParameterizedTest
    @MethodSource("historicalReferences")
    void should_read_a_historical_document_through_the_repository_after_upgrade(String shape, Document reference) throws Exception {
        var id = "legacy-" + shape;
        var rootId = "00000000-0000-0000-0000-000000000000";
        var document = new Document("_id", id)
            .append("organizationId", "org-id")
            .append("environmentId", "env-id")
            .append("rootId", rootId)
            .append("title", "Legacy folder")
            .append("segment", "legacy-folder")
            .append("type", "FOLDER")
            .append("area", "TOP_NAVBAR")
            .append("order", 4)
            .append("configuration", "{}")
            .append("published", true)
            .append("visibility", "PUBLIC");
        document.putAll(reference);
        mongoTemplate.getCollection(targetCollectionName).insertOne(document);

        assertThat(upgrader.upgrade()).isTrue();

        var item = repository.findById(id).orElseThrow();
        assertThat(item.getReferenceType()).isEqualTo(PortalNavigationReferenceType.PORTAL);
        assertThat(item.getReferenceId()).isEqualTo(UNATTACHED_REFERENCE_ID);
        assertThat(item)
            .extracting("organizationId", "environmentId", "rootId", "title", "configuration", "published", "order")
            .containsExactly("org-id", "env-id", rootId, "Legacy folder", "{}", true, 4);
    }

    private static Stream<Arguments> historicalReferences() {
        return Stream.of(
            Arguments.of("missing", new Document()),
            Arguments.of("null", new Document("referenceType", null).append(REFERENCE_ID, null)),
            Arguments.of("empty", new Document("referenceType", "PORTAL").append(REFERENCE_ID, ""))
        );
    }
}
