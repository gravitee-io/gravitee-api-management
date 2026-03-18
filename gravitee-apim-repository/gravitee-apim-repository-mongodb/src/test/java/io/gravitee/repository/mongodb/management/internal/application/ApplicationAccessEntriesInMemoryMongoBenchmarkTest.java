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

class ApplicationAccessEntriesInMemoryMongoBenchmarkTest {

    private static final String APPLICATION_ID = "app-1";
    private static final String APPLICATION_REFERENCE_TYPE = "APPLICATION";
    private static final String DATABASE_NAME = "test";
    private static final String COLLECTION_PREFIX = "benchmark_in_memory_prefix_";
    private static final String MEMBERSHIPS_COLLECTION = COLLECTION_PREFIX + "memberships";
    private static final String INVITATIONS_COLLECTION = COLLECTION_PREFIX + "invitations";
    private static final String USERS_COLLECTION = COLLECTION_PREFIX + "users";
    private static final int PAGE_OFFSET = 100;
    private static final int PAGE_SIZE = 50;
    private static final int BENCHMARK_RUNS = 5;
    private static final int BENCHMARK_SERIES = 3;
    private static final String FILTER_REGEX = "^(member000|invite000)";

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
        resetCollections();
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
    void shouldReportResponseTimesForUnifiedListInMemoryAcrossDatasetSizes() {
        List.of(500, 2_000, 5_000, 10_000).forEach(this::benchmarkDataset);
    }

    private void benchmarkDataset(int totalEntries) {
        resetCollections();

        int membersCount = totalEntries / 2;
        int invitationsCount = totalEntries - membersCount;
        seedBenchmarkData(membersCount, invitationsCount);

        List<UnifiedEntry> unfilteredPage = executeUnifiedEntriesQuery(null, PAGE_OFFSET, PAGE_SIZE);
        List<UnifiedEntry> filteredPage = executeUnifiedEntriesQuery(FILTER_REGEX, PAGE_OFFSET, PAGE_SIZE);

        assertThat(unfilteredPage).hasSize(PAGE_SIZE);
        assertThat(filteredPage).hasSize(PAGE_SIZE);

        List<Double> unfilteredSeriesAverages = benchmarkSeries(() -> executeUnifiedEntriesQuery(null, PAGE_OFFSET, PAGE_SIZE));
        List<Double> filteredSeriesAverages = benchmarkSeries(() -> executeUnifiedEntriesQuery(FILTER_REGEX, PAGE_OFFSET, PAGE_SIZE));
        SummaryStats unfilteredStats = summaryStats(unfilteredSeriesAverages);
        SummaryStats filteredStats = summaryStats(filteredSeriesAverages);

        System.out.printf(
            Locale.ROOT,
            "datasetSize=%d members=%d invitations=%d offset=%d limit=%d series=%d runsPerSeries=%d unfilteredSeriesAvgMs=%s unfilteredMinMs=%.2f unfilteredMedianMs=%.2f unfilteredMaxMs=%.2f filteredSeriesAvgMs=%s filteredMinMs=%.2f filteredMedianMs=%.2f filteredMaxMs=%.2f%n",
            totalEntries,
            membersCount,
            invitationsCount,
            PAGE_OFFSET,
            PAGE_SIZE,
            BENCHMARK_SERIES,
            BENCHMARK_RUNS,
            unfilteredSeriesAverages,
            unfilteredStats.minMs(),
            unfilteredStats.medianMs(),
            unfilteredStats.maxMs(),
            filteredSeriesAverages,
            filteredStats.minMs(),
            filteredStats.medianMs(),
            filteredStats.maxMs()
        );
    }

    private List<Double> benchmarkSeries(QueryOperation queryOperation) {
        List<Double> seriesAverages = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_SERIES; i++) {
            seriesAverages.add(benchmark(queryOperation).averageMs());
        }
        return seriesAverages;
    }

    private BenchmarkResult benchmark(QueryOperation queryOperation) {
        queryOperation.execute();

        List<Double> runsMs = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_RUNS; i++) {
            long start = System.nanoTime();
            queryOperation.execute();
            runsMs.add((System.nanoTime() - start) / 1_000_000.0);
        }

        double averageMs = runsMs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return new BenchmarkResult(runsMs, averageMs);
    }

    private SummaryStats summaryStats(List<Double> values) {
        List<Double> sortedValues = values.stream().sorted().toList();
        int middle = sortedValues.size() / 2;
        return new SummaryStats(sortedValues.get(0), sortedValues.get(middle), sortedValues.get(sortedValues.size() - 1));
    }

    private List<UnifiedEntry> executeUnifiedEntriesQuery(String displayNameRegex, int skip, int limit) {
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

    private void seedBenchmarkData(int membersCount, int invitationsCount) {
        List<Document> userDocuments = new ArrayList<>(membersCount);
        List<Document> membershipDocuments = new ArrayList<>(membersCount);
        List<Document> invitationDocuments = new ArrayList<>(invitationsCount);

        for (int i = 1; i <= membersCount; i++) {
            String suffix = String.format(Locale.ROOT, "%05d", i);
            String userId = "user-" + suffix;
            Date createdAt = new Date(i * 1_000L);

            userDocuments.add(userDocument(userId, "Member" + suffix, "User" + suffix, "member" + suffix + "@example.com"));
            membershipDocuments.add(membershipDocument("membership-" + suffix, userId, roleForIndex(i), createdAt));
        }

        for (int i = 1; i <= invitationsCount; i++) {
            String suffix = String.format(Locale.ROOT, "%05d", i);
            Date createdAt = new Date((membersCount + i) * 1_000L);

            invitationDocuments.add(
                invitationDocument("invitation-" + suffix, "invite" + suffix + "@example.com", invitationRoleForIndex(i), createdAt)
            );
        }

        users().insertMany(userDocuments);
        memberships().insertMany(membershipDocuments);
        invitations().insertMany(invitationDocuments);
    }

    private void resetCollections() {
        memberships().drop();
        invitations().drop();
        users().drop();
        memberships().createIndex(Indexes.ascending("referenceType", "referenceId"));
        invitations().createIndex(Indexes.ascending("referenceType", "referenceId"));
        users().createIndex(Indexes.ascending("_id"));
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

    private String roleForIndex(int index) {
        return switch (index % 3) {
            case 0 -> "APP_ADMIN";
            case 1 -> "APP_USER";
            default -> "APP_REVIEWER";
        };
    }

    private String invitationRoleForIndex(int index) {
        return index % 2 == 0 ? "APPLICATION_ADMIN" : "APPLICATION_USER";
    }

    @FunctionalInterface
    private interface QueryOperation {
        List<UnifiedEntry> execute();
    }

    private record BenchmarkResult(List<Double> runsMs, double averageMs) {}

    private record SummaryStats(double minMs, double medianMs, double maxMs) {}

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
