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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.guardrails;

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.guardrails.GuardrailRenameMongoUpgrader.LEGACY_API_FIELD;
import static io.gravitee.repository.mongodb.management.upgrade.upgrader.guardrails.GuardrailRenameMongoUpgrader.LEGACY_API_INDEX;
import static io.gravitee.repository.mongodb.management.upgrade.upgrader.guardrails.GuardrailRenameMongoUpgrader.LEGACY_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.StreamSupport;
import org.assertj.core.api.SoftAssertions;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GuardrailRenameMongoUpgraderTest extends AbstractManagementRepositoryTest {

    private static final String API_ID = "api-running-a-guardrail";

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private GuardrailRenameMongoUpgrader upgrader;
    private String prefix;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collections directly.
        return null;
    }

    @BeforeEach
    public void initUpgrader() {
        upgrader = new GuardrailRenameMongoUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        prefix = environment.getProperty("management.mongodb.prefix", "");
        cleanUp();
    }

    @AfterEach
    public void cleanUp() {
        // deleteMany, not drop: drop() also removes the indexes created once at context bootstrap
        guardrails().deleteMany(new Document());
        mongoTemplate.getCollection(prefix + LEGACY_COLLECTION).drop();
        apis().deleteMany(new Document("_id", API_ID));
        if (indexNames(apis()).contains(LEGACY_API_INDEX)) {
            apis().dropIndex(LEGACY_API_INDEX);
        }
    }

    @Test
    public void should_rename_the_guardians_into_the_empty_collection_the_index_upgraders_created() throws Exception {
        givenALegacyGuardian("guardian-1");

        assertThat(upgrader.upgrade()).isTrue();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mongoTemplate.collectionExists(prefix + LEGACY_COLLECTION)).as("guardians left").isFalse();
            softly.assertThat(guardrails().find(new Document("_id", "guardian-1")).first()).as("migrated guardrail").isNotNull();
            softly.assertThat(indexNames(guardrails())).as("guardrails indexes").contains("e1n1");
        });
    }

    @Test
    public void should_fail_and_leave_both_collections_when_the_guardrails_already_hold_documents() {
        givenALegacyGuardian("guardian-1");
        guardrails().insertOne(new Document("_id", "guardrail-1").append("environmentId", "DEFAULT").append("name", "guardrail-1"));

        assertThatThrownBy(() -> upgrader.upgrade())
            .isInstanceOf(UpgraderException.class)
            .rootCause()
            .hasMessageContaining(prefix + LEGACY_COLLECTION)
            .hasMessageContaining(prefix + "guardrails");

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(mongoTemplate.getCollection(prefix + LEGACY_COLLECTION).estimatedDocumentCount())
                .as("guardians kept")
                .isEqualTo(1);
            softly.assertThat(guardrails().find(new Document("_id", "guardrail-1")).first()).as("guardrail kept").isNotNull();
        });
    }

    @Test
    public void should_move_the_guardians_an_api_runs_to_its_guardrails() throws Exception {
        apis().createIndex(new Document(LEGACY_API_FIELD, 1), new IndexOptions().name(LEGACY_API_INDEX));
        apis().insertOne(new Document("_id", API_ID).append(LEGACY_API_FIELD, List.of("guardian-1", "guardian-2")));

        assertThat(upgrader.upgrade()).isTrue();

        Document api = apis().find(new Document("_id", API_ID)).first();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(api).as("api").isNotNull();
            softly.assertThat(api.getList("guardrails", String.class)).as("guardrails").containsExactly("guardian-1", "guardian-2");
            softly.assertThat(api.containsKey(LEGACY_API_FIELD)).as("guardians field left").isFalse();
            softly.assertThat(indexNames(apis())).as("apis indexes").doesNotContain(LEGACY_API_INDEX);
        });
    }

    @Test
    public void should_move_the_guardians_an_api_runs_even_when_the_legacy_index_is_gone() throws Exception {
        apis().insertOne(new Document("_id", API_ID).append(LEGACY_API_FIELD, List.of("guardian-1")));

        assertThat(upgrader.upgrade()).isTrue();

        Document api = apis().find(new Document("_id", API_ID)).first();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(api).as("api").isNotNull();
            softly.assertThat(api.getList("guardrails", String.class)).as("guardrails").containsExactly("guardian-1");
            softly.assertThat(api.containsKey(LEGACY_API_FIELD)).as("guardians field left").isFalse();
        });
    }

    @Test
    public void should_do_nothing_on_a_store_that_never_held_guardians() throws Exception {
        assertThat(upgrader.upgrade()).isTrue();

        assertThat(mongoTemplate.collectionExists(prefix + LEGACY_COLLECTION)).isFalse();
    }

    @Test
    public void should_be_idempotent() throws Exception {
        givenALegacyGuardian("guardian-1");

        assertThat(upgrader.upgrade()).isTrue();
        assertThat(upgrader.upgrade()).isTrue();

        assertThat(guardrails().find(new Document("_id", "guardian-1")).first()).isNotNull();
    }

    private void givenALegacyGuardian(String id) {
        MongoCollection<Document> guardians = mongoTemplate.getCollection(prefix + LEGACY_COLLECTION);
        // the index the former EnvironmentIdNameIndexUpgrader created, which the rename carries over
        guardians.createIndex(new Document("environmentId", 1).append("name", 1), new IndexOptions().name("e1n1").unique(true));
        guardians.insertOne(new Document("_id", id).append("environmentId", "DEFAULT").append("name", id));
    }

    private MongoCollection<Document> guardrails() {
        return mongoTemplate.getCollection(prefix + "guardrails");
    }

    private MongoCollection<Document> apis() {
        return mongoTemplate.getCollection(prefix + "apis");
    }

    private static List<String> indexNames(MongoCollection<Document> collection) {
        return StreamSupport.stream(collection.listIndexes().spliterator(), false)
            .map(index -> index.getString("name"))
            .toList();
    }
}
