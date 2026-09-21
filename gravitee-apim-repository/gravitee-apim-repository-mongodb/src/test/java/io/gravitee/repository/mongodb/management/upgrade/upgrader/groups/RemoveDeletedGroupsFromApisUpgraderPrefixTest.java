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
import static org.junit.Assume.assumeTrue;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.List;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.env.MockEnvironment;

/**
 * The test environment configures management.mongodb.prefix, so these run against prefixed collections, with one
 * sibling case pinning the behaviour when no prefix is set.
 */
public class RemoveDeletedGroupsFromApisUpgraderPrefixTest extends AbstractManagementRepositoryTest {

    private static final String ATTR_GROUPS = "groups";
    private static final String EXISTING_GROUP = "existing-group";
    private static final String ANOTHER_EXISTING_GROUP = "another-existing-group";
    private static final String DELETED_GROUP = "deleted-group";

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private RemoveDeletedGroupsFromApisUpgrader upgrader;
    private String apisCollection;
    private String groupsCollection;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates collections directly.
        return null;
    }

    @Before
    public void initUpgrader() {
        upgrader = new RemoveDeletedGroupsFromApisUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);

        var prefix = environment.getProperty("management.mongodb.prefix", "");
        assertThat(prefix).as("this test only covers the prefixed-collection bug").isNotBlank();

        apisCollection = prefix + "apis";
        groupsCollection = prefix + ATTR_GROUPS;
        clearCollections();
    }

    @After
    public void cleanUp() {
        clearCollections();
    }

    /**
     * Emptied rather than dropped: these collections are shared with the other tests on this Spring context, and
     * dropping takes the indexes created at startup with them, which notablescan=true then turns into failures.
     */
    private void clearCollections() {
        mongoTemplate.getCollection(apisCollection).deleteMany(new Document());
        mongoTemplate.getCollection(groupsCollection).deleteMany(new Document());
    }

    private void givenGroups(String... ids) {
        givenGroupsIn(groupsCollection, ids);
    }

    private void givenGroupsIn(String collection, String... ids) {
        for (String id : ids) {
            mongoTemplate.getCollection(collection).insertOne(new Document("_id", id).append("name", id));
        }
    }

    private void givenApi(String id, String... groups) {
        givenApiIn(apisCollection, id, groups);
    }

    private void givenApiIn(String collection, String id, String... groups) {
        mongoTemplate.getCollection(collection).insertOne(new Document("_id", id).append(ATTR_GROUPS, List.of(groups)));
    }

    private List<String> groupsOf(String apiId) {
        return groupsOfIn(apisCollection, apiId);
    }

    private List<String> groupsOfIn(String collection, String apiId) {
        var api = mongoTemplate.getCollection(collection).find(new Document("_id", apiId)).first();
        return api == null ? null : api.getList(ATTR_GROUPS, String.class);
    }

    /**
     * The upgrader refuses anything below MongoDB 5 and logs a skip without touching a single document, so the cases
     * exercising upgrade() have nothing to assert there. The repository test matrix still runs on MongoDB 4.4.
     */
    private void assumeTheUpgraderRunsOnThisDatabase() {
        var buildInfo = mongoTemplate.executeCommand(new Document("buildInfo", 1));
        assumeTrue("the upgrader only supports MongoDB 5 and above", upgrader.checkDatabaseCompatibility(buildInfo));
    }

    @Test
    public void upgrade_should_keep_groups_that_still_exist() throws Exception {
        assumeTheUpgraderRunsOnThisDatabase();
        givenGroups(EXISTING_GROUP, ANOTHER_EXISTING_GROUP);
        givenApi("api-1", EXISTING_GROUP, ANOTHER_EXISTING_GROUP);
        givenApi("api-2", EXISTING_GROUP);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(groupsOf("api-1")).containsExactlyInAnyOrder(EXISTING_GROUP, ANOTHER_EXISTING_GROUP);
        assertThat(groupsOf("api-2")).containsExactly(EXISTING_GROUP);
    }

    @Test
    public void upgrade_should_remove_only_the_groups_that_no_longer_exist() throws Exception {
        assumeTheUpgraderRunsOnThisDatabase();
        givenGroups(EXISTING_GROUP);
        givenApi("api-1", EXISTING_GROUP, DELETED_GROUP);
        givenApi("api-2", DELETED_GROUP);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(groupsOf("api-1")).containsExactly(EXISTING_GROUP);
        assertThat(groupsOf("api-2")).isEmpty();
    }

    @Test
    public void upgrade_should_keep_assignments_when_not_a_single_group_would_survive() throws Exception {
        assumeTheUpgraderRunsOnThisDatabase();
        givenApi("api-1", EXISTING_GROUP, ANOTHER_EXISTING_GROUP);
        givenApi("api-2", EXISTING_GROUP);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(groupsOf("api-1")).containsExactlyInAnyOrder(EXISTING_GROUP, ANOTHER_EXISTING_GROUP);
        assertThat(groupsOf("api-2")).containsExactly(EXISTING_GROUP);
    }

    @Test
    public void safeguard_should_trip_when_every_existing_group_is_reported_as_deleted() {
        givenGroups(EXISTING_GROUP, ANOTHER_EXISTING_GROUP);

        assertThat(upgrader.wouldRemoveEveryExistingGroup(List.of(EXISTING_GROUP, ANOTHER_EXISTING_GROUP))).isTrue();
    }

    @Test
    public void safeguard_should_not_trip_when_some_groups_survive() {
        givenGroups(EXISTING_GROUP, ANOTHER_EXISTING_GROUP);

        assertThat(upgrader.wouldRemoveEveryExistingGroup(List.of(DELETED_GROUP))).isFalse();
    }

    @Test
    public void upgrade_should_remove_only_the_groups_that_no_longer_exist_when_no_prefix_is_configured() throws Exception {
        assumeTheUpgraderRunsOnThisDatabase();
        var unprefixedUpgrader = new RemoveDeletedGroupsFromApisUpgrader();
        unprefixedUpgrader.setMongoTemplate(mongoTemplate);
        unprefixedUpgrader.setEnvironment(new MockEnvironment());

        // Unlike the prefixed ones, these collections belong to this test alone and carry no index from startup.
        mongoTemplate.getCollection("apis").createIndex(new Document(ATTR_GROUPS, 1));
        try {
            givenGroupsIn(ATTR_GROUPS, EXISTING_GROUP);
            givenApiIn("apis", "api-1", EXISTING_GROUP, DELETED_GROUP);
            givenApiIn("apis", "api-2", EXISTING_GROUP);

            assertThat(unprefixedUpgrader.upgrade()).isTrue();

            assertThat(groupsOfIn("apis", "api-1")).containsExactly(EXISTING_GROUP);
            assertThat(groupsOfIn("apis", "api-2")).containsExactly(EXISTING_GROUP);
        } finally {
            mongoTemplate.getCollection("apis").drop();
            mongoTemplate.getCollection(ATTR_GROUPS).drop();
        }
    }
}
