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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

class ApplicationAccessEntriesInMemoryMongoTest {

    private static final String APPLICATION_ID = "app-1";
    private static final String OTHER_APPLICATION_ID = "app-2";
    private static final String APPLICATION_REFERENCE_TYPE = "APPLICATION";
    private static final String DATABASE_NAME = "test";
    private static final String COLLECTION_PREFIX = "in_memory_prefix_";
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
        users().createIndex(Indexes.ascending("_id"));
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
    void shouldMergeMembersAndInvitationsInApplicationLayer() {
        seedData();
        printSources();

        List<UnifiedEntry> unifiedEntries = unifiedEntries(null, 0, Integer.MAX_VALUE);

        printUnifiedEntries("unifiedEntriesInMemory", unifiedEntries);

        assertThat(unifiedEntries).hasSize(7);
        assertThat(unifiedEntries)
            .extracting(UnifiedEntry::kind)
            .containsExactly("member", "invitation", "invitation", "member", "member", "member", "invitation");
        assertThat(unifiedEntries)
            .extracting(UnifiedEntry::identity)
            .containsExactly("alice", "amelia@example.com", "brian@example.com", "bruce", "charlie", "dana", "zoe@example.com");
        assertThat(unifiedEntries)
            .extracting(UnifiedEntry::displayName)
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
    void shouldFilterSortAndPaginateUnifiedEntriesInApplicationLayer() {
        seedData();
        printSources();

        List<UnifiedEntry> filteredSortedPagedEntries = unifiedEntries("^[ab]", 1, 3);

        printUnifiedEntries("filteredSortedPagedEntriesInMemory", filteredSortedPagedEntries);

        assertThat(filteredSortedPagedEntries).hasSize(3);
        assertThat(filteredSortedPagedEntries)
            .extracting(UnifiedEntry::displayName)
            .containsExactly("amelia@example.com", "brian@example.com", "Bruce Banner");
        assertThat(filteredSortedPagedEntries).extracting(UnifiedEntry::kind).containsExactly("invitation", "invitation", "member");
    }

    private List<UnifiedEntry> unifiedEntries(String displayNameRegex, int skip, int limit) {
        List<Document> membershipDocuments = memberships()
            .find(Filters.and(Filters.eq("referenceType", APPLICATION_REFERENCE_TYPE), Filters.eq("referenceId", APPLICATION_ID)))
            .into(new ArrayList<>());
        List<Document> invitationDocuments = invitations()
            .find(Filters.and(Filters.eq("referenceType", APPLICATION_REFERENCE_TYPE), Filters.eq("referenceId", APPLICATION_ID)))
            .into(new ArrayList<>());

        List<String> memberIds = membershipDocuments
            .stream()
            .map(document -> document.getString("memberId"))
            .toList();
        Map<String, Document> usersById = users()
            .find(Filters.in("_id", memberIds))
            .into(new ArrayList<>())
            .stream()
            .collect(
                Collectors.toMap(document -> document.getString("_id"), document -> document, (left, right) -> left, LinkedHashMap::new)
            );

        Pattern displayNamePattern = displayNameRegex == null ? null : Pattern.compile(displayNameRegex);

        List<UnifiedEntry> unifiedEntries = new ArrayList<>(membershipDocuments.size() + invitationDocuments.size());

        membershipDocuments.forEach(document -> {
            Document user = usersById.get(document.getString("memberId"));
            if (user != null) {
                String displayName = user.getString("firstname") + " " + user.getString("lastname");
                unifiedEntries.add(
                    new UnifiedEntry(
                        "member",
                        document.getString("_id"),
                        document.getString("memberId"),
                        displayName,
                        displayName.toLowerCase(Locale.ROOT),
                        document.getString("roleId"),
                        document.getString("referenceId"),
                        document.getString("referenceType"),
                        document.getDate("createdAt")
                    )
                );
            }
        });

        invitationDocuments.forEach(document -> {
            String displayName = document.getString("email");
            unifiedEntries.add(
                new UnifiedEntry(
                    "invitation",
                    document.getString("_id"),
                    document.getString("email"),
                    displayName,
                    displayName.toLowerCase(Locale.ROOT),
                    document.getString("applicationRole"),
                    document.getString("referenceId"),
                    document.getString("referenceType"),
                    document.getDate("createdAt")
                )
            );
        });

        return unifiedEntries
            .stream()
            .filter(entry -> displayNamePattern == null || displayNamePattern.matcher(entry.displayNameSort()).find())
            .sorted(Comparator.comparing(UnifiedEntry::displayNameSort))
            .skip(skip)
            .limit(limit)
            .toList();
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

    private void printUnifiedEntries(String label, List<UnifiedEntry> entries) {
        System.out.println("--- " + label + " ---");
        entries.forEach(System.out::println);
    }

    private void printSources() {
        printCollection("users", users().find().sort(new Document("_id", 1)).into(new ArrayList<>()));
        printCollection("memberships", memberships().find().sort(new Document("_id", 1)).into(new ArrayList<>()));
        printCollection("invitations", invitations().find().sort(new Document("_id", 1)).into(new ArrayList<>()));
    }

    private void seedData() {
        users().insertMany(
            List.of(
                userDocument("alice", "Alice", "Anderson", "alice@example.com"),
                userDocument("bruce", "Bruce", "Banner", "bruce@example.com"),
                userDocument("charlie", "Charlie", "Clark", "charlie@example.com"),
                userDocument("dana", "Dana", "Doe", "dana@example.com"),
                userDocument("edgar", "Edgar", "Evans", "edgar@example.com")
            )
        );

        memberships().insertMany(
            List.of(
                membershipDocument("membership-1", APPLICATION_ID, "alice", "APP_USER", new Date(1_000L)),
                membershipDocument("membership-2", APPLICATION_ID, "bruce", "APP_REVIEWER", new Date(2_000L)),
                membershipDocument("membership-3", APPLICATION_ID, "charlie", "APP_ADMIN", new Date(3_000L)),
                membershipDocument("membership-4", APPLICATION_ID, "dana", "APP_USER", new Date(4_000L)),
                membershipDocument("membership-5", OTHER_APPLICATION_ID, "edgar", "APP_USER", new Date(5_000L))
            )
        );

        invitations().insertMany(
            List.of(
                invitationDocument("invitation-1", APPLICATION_ID, "amelia@example.com", "APPLICATION_USER", new Date(6_000L)),
                invitationDocument("invitation-2", APPLICATION_ID, "brian@example.com", "APPLICATION_ADMIN", new Date(7_000L)),
                invitationDocument("invitation-3", APPLICATION_ID, "zoe@example.com", "APPLICATION_USER", new Date(8_000L)),
                invitationDocument("invitation-4", OTHER_APPLICATION_ID, "yuki@example.com", "APPLICATION_USER", new Date(9_000L))
            )
        );
    }

    private Document membershipDocument(String id, String applicationId, String memberId, String roleId, Date createdAt) {
        return new Document("_id", id)
            .append("memberId", memberId)
            .append("memberType", "USER")
            .append("referenceId", applicationId)
            .append("referenceType", APPLICATION_REFERENCE_TYPE)
            .append("roleId", roleId)
            .append("source", "memory")
            .append("createdAt", createdAt)
            .append("updatedAt", createdAt);
    }

    private Document invitationDocument(String id, String applicationId, String email, String applicationRole, Date createdAt) {
        return new Document("_id", id)
            .append("referenceId", applicationId)
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

    private record UnifiedEntry(
        String kind,
        String rowId,
        String identity,
        String displayName,
        String displayNameSort,
        String role,
        String referenceId,
        String referenceType,
        Date createdAt
    ) {}
}
