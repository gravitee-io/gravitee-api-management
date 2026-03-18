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
package io.gravitee.repository.mongodb.management.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

class ApplicationAccessEntriesAggregationMongoTest {

    private static final String APPLICATION_ID = "app-1";
    private static final String APPLICATION_REFERENCE_TYPE = "APPLICATION";
    private static final String DATABASE_NAME = "test";
    private static final String COLLECTION_PREFIX = "test_prefix_";
    private static final String MEMBERSHIPS_COLLECTION = COLLECTION_PREFIX + "memberships";
    private static final String INVITATIONS_COLLECTION = COLLECTION_PREFIX + "invitations";
    private static final String USERS_COLLECTION = COLLECTION_PREFIX + "users";

    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(DockerImageName.parse("mongo:6.0")).withCommand(
        "--replSet",
        "docker-rs",
        "--setParameter",
        "notablescan=true"
    );

    private MongoClient mongoClient;
    private MongoDatabase database;

    @BeforeAll
    static void startContainer() {
        MONGO_DB_CONTAINER.start();
    }

    @BeforeEach
    void setUp() {
        mongoClient = MongoClients.create(MONGO_DB_CONTAINER.getReplicaSetUrl());
        database = mongoClient.getDatabase(DATABASE_NAME);
        memberships().drop();
        invitations().drop();
        users().drop();
        memberships().createIndex(Indexes.ascending("referenceType", "referenceId"));
        invitations().createIndex(Indexes.ascending("referenceType", "referenceId"));
    }

    @AfterEach
    void tearDown() {
        mongoClient.close();
    }

    @AfterAll
    static void stopContainer() {
        MONGO_DB_CONTAINER.stop();
    }

    @Test
    void shouldAggregateMembersAndInvitationsIntoOneList() {
        seedData();
        printSources();

        List<Document> unifiedEntries = memberships().aggregate(unifiedEntriesPipeline(null, null, null)).into(new ArrayList<>());
        printCollection("unifiedEntries", unifiedEntries);

        assertThat(unifiedEntries).hasSize(7);
        assertThat(unifiedEntries)
            .extracting(entry -> entry.getString("kind"))
            .containsExactly("member", "invitation", "invitation", "member", "member", "member", "invitation");
        assertThat(unifiedEntries)
            .extracting(entry -> entry.getString("identity"))
            .containsExactly("alice", "amelia@example.com", "brian@example.com", "bruce", "charlie", "dana", "zoe@example.com");
        assertThat(unifiedEntries)
            .extracting(entry -> entry.getString("role"))
            .containsExactly(
                "APP_USER",
                "APPLICATION_USER",
                "APPLICATION_ADMIN",
                "APP_REVIEWER",
                "APP_ADMIN",
                "APP_USER",
                "APPLICATION_USER"
            );
        assertThat(unifiedEntries)
            .extracting(entry -> entry.getString("displayName"))
            .containsExactly(
                "Alice Anderson",
                "amelia@example.com",
                "brian@example.com",
                "Bruce Banner",
                "Charlie Clark",
                "Dana Doe",
                "zoe@example.com"
            );
    }

    @Test
    void shouldFilterSortAndPaginateUnifiedEntriesByDisplayName() {
        seedData();
        printSources();

        List<Document> filteredSortedPagedEntries = memberships().aggregate(unifiedEntriesPipeline("^[ab]", 1, 3)).into(new ArrayList<>());

        printCollection("filteredSortedPagedEntries", filteredSortedPagedEntries);

        assertThat(filteredSortedPagedEntries).hasSize(3);
        assertThat(filteredSortedPagedEntries)
            .extracting(entry -> entry.getString("displayName"))
            .containsExactly("amelia@example.com", "brian@example.com", "Bruce Banner");
        assertThat(filteredSortedPagedEntries)
            .extracting(entry -> entry.getString("kind"))
            .containsExactly("invitation", "invitation", "member");
    }

    private List<Document> unifiedEntriesPipeline(String displayNameRegex, Integer skip, Integer limit) {
        List<Document> pipeline = new ArrayList<>(
            List.of(
                new Document("$match", new Document("referenceType", APPLICATION_REFERENCE_TYPE).append("referenceId", APPLICATION_ID)),
                new Document(
                    "$lookup",
                    new Document("from", USERS_COLLECTION)
                        .append("localField", "memberId")
                        .append("foreignField", "_id")
                        .append("as", "user")
                ),
                new Document("$unwind", "$user"),
                new Document(
                    "$project",
                    new Document("_id", 0)
                        .append("kind", new Document("$literal", "member"))
                        .append("rowId", "$_id")
                        .append("identity", "$memberId")
                        .append("displayName", new Document("$concat", List.of("$user.firstname", " ", "$user.lastname")))
                        .append(
                            "displayNameSort",
                            new Document("$toLower", new Document("$concat", List.of("$user.firstname", " ", "$user.lastname")))
                        )
                        .append("role", "$roleId")
                        .append("referenceId", "$referenceId")
                        .append("referenceType", "$referenceType")
                        .append("createdAt", "$createdAt")
                ),
                new Document(
                    "$unionWith",
                    new Document("coll", INVITATIONS_COLLECTION).append(
                        "pipeline",
                        List.of(
                            new Document(
                                "$match",
                                new Document("referenceType", APPLICATION_REFERENCE_TYPE).append("referenceId", APPLICATION_ID)
                            ),
                            new Document(
                                "$project",
                                new Document("_id", 0)
                                    .append("kind", new Document("$literal", "invitation"))
                                    .append("rowId", "$_id")
                                    .append("identity", "$email")
                                    .append("displayName", "$email")
                                    .append("displayNameSort", new Document("$toLower", "$email"))
                                    .append("role", "$applicationRole")
                                    .append("referenceId", "$referenceId")
                                    .append("referenceType", "$referenceType")
                                    .append("createdAt", "$createdAt")
                            )
                        )
                    )
                )
            )
        );

        if (displayNameRegex != null) {
            pipeline.add(new Document("$match", new Document("displayNameSort", new Document("$regex", displayNameRegex))));
        }

        pipeline.add(new Document("$sort", new Document("displayNameSort", 1)));

        if (skip != null) {
            pipeline.add(new Document("$skip", skip));
        }

        if (limit != null) {
            pipeline.add(new Document("$limit", limit));
        }

        pipeline.add(
            new Document(
                "$project",
                new Document("kind", 1)
                    .append("rowId", 1)
                    .append("identity", 1)
                    .append("displayName", 1)
                    .append("role", 1)
                    .append("referenceId", 1)
                    .append("referenceType", 1)
                    .append("createdAt", 1)
            )
        );

        return pipeline;
    }

    private MongoCollection<Document> memberships() {
        return database.getCollection(MEMBERSHIPS_COLLECTION);
    }

    private MongoCollection<Document> invitations() {
        return database.getCollection(INVITATIONS_COLLECTION);
    }

    private MongoCollection<Document> users() {
        return database.getCollection(USERS_COLLECTION);
    }

    private void printCollection(String label, List<Document> documents) {
        System.out.println("--- " + label + " ---");
        documents.forEach(document -> System.out.println(document.toJson()));
    }

    private void printSources() {
        printCollection("users", users().find().sort(new Document("_id", 1)).into(new ArrayList<>()));
        printCollection("memberships", memberships().find().sort(new Document("_id", 1)).into(new ArrayList<>()));
        printCollection("invitations", invitations().find().sort(new Document("_id", 1)).into(new ArrayList<>()));
    }

    private void seedData() {
        users().insertOne(userDocument("alice", "Alice", "Anderson", "alice@example.com"));
        users().insertOne(userDocument("bruce", "Bruce", "Banner", "bruce@example.com"));
        users().insertOne(userDocument("charlie", "Charlie", "Clark", "charlie@example.com"));
        users().insertOne(userDocument("dana", "Dana", "Doe", "dana@example.com"));

        memberships().insertOne(membershipDocument("membership-1", "alice", "APP_USER", new Date(1_000L)));
        memberships().insertOne(membershipDocument("membership-2", "bruce", "APP_REVIEWER", new Date(2_000L)));
        memberships().insertOne(membershipDocument("membership-3", "charlie", "APP_ADMIN", new Date(3_000L)));
        memberships().insertOne(membershipDocument("membership-4", "dana", "APP_USER", new Date(4_000L)));

        invitations().insertOne(invitationDocument("invitation-1", "amelia@example.com", "APPLICATION_USER", new Date(5_000L)));
        invitations().insertOne(invitationDocument("invitation-2", "brian@example.com", "APPLICATION_ADMIN", new Date(6_000L)));
        invitations().insertOne(invitationDocument("invitation-3", "zoe@example.com", "APPLICATION_USER", new Date(7_000L)));
    }

    private Document membershipDocument(String id, String memberId, String roleId, Date createdAt) {
        return new Document("_id", id)
            .append("memberId", memberId)
            .append("memberType", "USER")
            .append("referenceId", APPLICATION_ID)
            .append("referenceType", APPLICATION_REFERENCE_TYPE)
            .append("roleId", roleId)
            .append("source", "memory")
            .append("createdAt", createdAt)
            .append("updatedAt", createdAt);
    }

    private Document invitationDocument(String id, String email, String applicationRole, Date createdAt) {
        return new Document("_id", id)
            .append("referenceId", APPLICATION_ID)
            .append("referenceType", APPLICATION_REFERENCE_TYPE)
            .append("email", email)
            .append("applicationRole", applicationRole)
            .append("createdAt", createdAt)
            .append("updatedAt", createdAt);
    }

    private Document userDocument(String id, String firstname, String lastname, String email) {
        return new Document("_id", id)
            .append("firstname", firstname)
            .append("lastname", lastname)
            .append("email", email)
            .append("organizationId", "org-1")
            .append("source", "memory");
    }
}
