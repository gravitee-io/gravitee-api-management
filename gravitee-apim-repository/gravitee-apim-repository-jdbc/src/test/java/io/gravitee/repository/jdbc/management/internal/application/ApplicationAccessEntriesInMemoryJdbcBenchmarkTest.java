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
package io.gravitee.repository.jdbc.management.internal.application;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import jakarta.inject.Inject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
public class ApplicationAccessEntriesInMemoryJdbcBenchmarkTest extends AbstractRepositoryTest {

    private static final String APPLICATION_ID = "app-1";
    private static final String APPLICATION_REFERENCE_TYPE = "APPLICATION";
    private static final int PAGE_OFFSET = 100;
    private static final int PAGE_SIZE = 50;
    private static final int BENCHMARK_RUNS = 5;
    private static final int BENCHMARK_SERIES = 3;

    @Inject
    private DataSource dataSource;

    @Inject
    private Properties graviteeProperties;

    @Test
    public void shouldReportResponseTimesForUnifiedListInMemoryAcrossDatasetSizes() {
        List.of(500, 2_000, 5_000, 10_000).forEach(this::benchmarkDataset);
    }

    @Override
    protected String getTestCasesPath() {
        return "";
    }

    @Override
    protected String getModelPackage() {
        return "";
    }

    @Override
    protected void createModel(Object object) throws TechnicalException {}

    private void benchmarkDataset(int totalEntries) {
        resetTables();

        int membersCount = totalEntries / 2;
        int invitationsCount = totalEntries - membersCount;
        seedBenchmarkData(membersCount, invitationsCount);

        List<UnifiedAccessEntry> unfilteredPage = executeUnifiedEntriesQuery(List.of(), PAGE_OFFSET, PAGE_SIZE);
        List<UnifiedAccessEntry> filteredPage = executeUnifiedEntriesQuery(List.of("member000", "invite000"), PAGE_OFFSET, PAGE_SIZE);

        assertThat(unfilteredPage).hasSize(PAGE_SIZE);
        assertThat(filteredPage).hasSize(PAGE_SIZE);

        List<Double> unfilteredSeriesAverages = benchmarkSeries(() -> executeUnifiedEntriesQuery(List.of(), PAGE_OFFSET, PAGE_SIZE));
        List<Double> filteredSeriesAverages = benchmarkSeries(() ->
            executeUnifiedEntriesQuery(List.of("member000", "invite000"), PAGE_OFFSET, PAGE_SIZE)
        );
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

    private void seedBenchmarkData(int membersCount, int invitationsCount) {
        for (int i = 1; i <= membersCount; i++) {
            String suffix = String.format(Locale.ROOT, "%05d", i);
            String userId = "user-" + suffix;
            Date createdAt = new Date(i * 1_000L);

            insertUser(userId, "Member" + suffix, "User" + suffix, "member" + suffix + "@example.com", createdAt);
            insertMembership("membership-" + suffix, userId, roleForIndex(i), createdAt);
        }

        for (int i = 1; i <= invitationsCount; i++) {
            String suffix = String.format(Locale.ROOT, "%05d", i);
            Date createdAt = new Date((membersCount + i) * 1_000L);

            insertInvitation("invitation-" + suffix, "invite" + suffix + "@example.com", invitationRoleForIndex(i), createdAt);
        }
    }

    private void resetTables() {
        jdbcTemplate().update("delete from " + tableName("memberships"));
        jdbcTemplate().update("delete from " + tableName("invitations"));
        jdbcTemplate().update("delete from " + tableName("users"));
    }

    private void insertUser(String id, String firstname, String lastname, String email, Date createdAt) {
        jdbcTemplate().update(
            "insert into " +
                tableName("users") +
                " (id, source, source_id, email, firstname, lastname, created_at, updated_at, organization_id, status) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id,
            "memory",
            id,
            email,
            firstname,
            lastname,
            new Timestamp(createdAt.getTime()),
            new Timestamp(createdAt.getTime()),
            "DEFAULT",
            "ACTIVE"
        );
    }

    private void insertMembership(String id, String memberId, String roleId, Date createdAt) {
        jdbcTemplate().update(
            "insert into " +
                tableName("memberships") +
                " (id, member_id, member_type, reference_type, reference_id, role_id, source, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id,
            memberId,
            "USER",
            APPLICATION_REFERENCE_TYPE,
            APPLICATION_ID,
            roleId,
            "memory",
            new Timestamp(createdAt.getTime()),
            new Timestamp(createdAt.getTime())
        );
    }

    private void insertInvitation(String id, String email, String applicationRole, Date createdAt) {
        jdbcTemplate().update(
            "insert into " +
                tableName("invitations") +
                " (id, reference_type, reference_id, email, application_role, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?)",
            id,
            APPLICATION_REFERENCE_TYPE,
            APPLICATION_ID,
            email,
            applicationRole,
            new Timestamp(createdAt.getTime()),
            new Timestamp(createdAt.getTime())
        );
    }

    private List<UnifiedAccessEntry> executeUnifiedEntriesQuery(List<String> displayNameContains, int offset, int limit) {
        List<MembershipRow> membershipRows = jdbcTemplate().query(
            "select id, member_id, role_id, reference_id, reference_type, created_at from " +
                tableName("memberships") +
                " where reference_type = ? and reference_id = ?",
            (rs, rowNum) ->
                new MembershipRow(
                    rs.getString("id"),
                    rs.getString("member_id"),
                    rs.getString("role_id"),
                    rs.getString("reference_id"),
                    rs.getString("reference_type"),
                    rs.getTimestamp("created_at")
                ),
            APPLICATION_REFERENCE_TYPE,
            APPLICATION_ID
        );
        List<InvitationRow> invitationRows = jdbcTemplate().query(
            "select id, email, application_role, reference_id, reference_type, created_at from " +
                tableName("invitations") +
                " where reference_type = ? and reference_id = ?",
            (rs, rowNum) ->
                new InvitationRow(
                    rs.getString("id"),
                    rs.getString("email"),
                    rs.getString("application_role"),
                    rs.getString("reference_id"),
                    rs.getString("reference_type"),
                    rs.getTimestamp("created_at")
                ),
            APPLICATION_REFERENCE_TYPE,
            APPLICATION_ID
        );

        List<String> memberIds = membershipRows.stream().map(MembershipRow::memberId).toList();
        Map<String, UserRow> usersById = jdbcTemplate()
            .query(
                "select id, firstname, lastname from " + tableName("users") + " where id in (" + placeholders(memberIds.size()) + ")",
                (rs, rowNum) -> new UserRow(rs.getString("id"), rs.getString("firstname"), rs.getString("lastname")),
                memberIds.toArray()
            )
            .stream()
            .collect(Collectors.toMap(UserRow::id, user -> user, (left, right) -> left, LinkedHashMap::new));

        List<UnifiedAccessEntry> unifiedEntries = new ArrayList<>(membershipRows.size() + invitationRows.size());

        membershipRows.forEach(row -> {
            UserRow user = usersById.get(row.memberId());
            if (user != null) {
                String displayName = (user.firstname() + " " + user.lastname()).trim();
                unifiedEntries.add(
                    new UnifiedAccessEntry(
                        "member",
                        row.id(),
                        row.memberId(),
                        displayName,
                        displayName.toLowerCase(Locale.ROOT),
                        row.roleId(),
                        row.referenceId(),
                        row.referenceType(),
                        row.createdAt()
                    )
                );
            }
        });

        invitationRows.forEach(row -> {
            String displayName = row.email();
            unifiedEntries.add(
                new UnifiedAccessEntry(
                    "invitation",
                    row.id(),
                    row.email(),
                    displayName,
                    displayName.toLowerCase(Locale.ROOT),
                    row.applicationRole(),
                    row.referenceId(),
                    row.referenceType(),
                    row.createdAt()
                )
            );
        });

        return unifiedEntries
            .stream()
            .filter(
                entry ->
                    displayNameContains.isEmpty() ||
                    displayNameContains.stream().anyMatch(filter -> entry.displayNameSort().contains(filter.toLowerCase(Locale.ROOT)))
            )
            .sorted(Comparator.comparing(UnifiedAccessEntry::displayNameSort).thenComparing(UnifiedAccessEntry::rowId))
            .skip(offset)
            .limit(limit)
            .toList();
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    private String tableName(String table) {
        return escapeReservedWord(graviteeProperties.getProperty("management.jdbc.prefix", "") + table);
    }

    private String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
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
        List<UnifiedAccessEntry> execute();
    }

    private record UserRow(String id, String firstname, String lastname) {}

    private record MembershipRow(
        String id,
        String memberId,
        String roleId,
        String referenceId,
        String referenceType,
        Timestamp createdAt
    ) {}

    private record InvitationRow(
        String id,
        String email,
        String applicationRole,
        String referenceId,
        String referenceType,
        Timestamp createdAt
    ) {}

    private record UnifiedAccessEntry(
        String kind,
        String rowId,
        String identity,
        String displayName,
        String displayNameSort,
        String role,
        String referenceId,
        String referenceType,
        Timestamp createdAt
    ) {}

    private record BenchmarkResult(List<Double> runsMs, double averageMs) {}

    private record SummaryStats(double minMs, double medianMs, double maxMs) {}
}
