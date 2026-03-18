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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
public class ApplicationAccessEntriesAggregationJdbcBenchmarkTest extends AbstractRepositoryTest {

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
    public void shouldReportResponseTimesForUnifiedListAggregationAcrossDatasetSizes() {
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
        List<UnifiedAccessEntry> filteredPage = executeUnifiedEntriesQuery(List.of("member000%", "invite000%"), PAGE_OFFSET, PAGE_SIZE);

        assertThat(unfilteredPage).hasSize(PAGE_SIZE);
        assertThat(filteredPage).hasSize(PAGE_SIZE);

        List<Double> unfilteredSeriesAverages = benchmarkSeries(() -> executeUnifiedEntriesQuery(List.of(), PAGE_OFFSET, PAGE_SIZE));
        List<Double> filteredSeriesAverages = benchmarkSeries(() ->
            executeUnifiedEntriesQuery(List.of("member000%", "invite000%"), PAGE_OFFSET, PAGE_SIZE)
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

    private List<UnifiedAccessEntry> executeUnifiedEntriesQuery(List<String> displayNameFilters, int offset, int limit) {
        QuerySpec querySpec = unifiedEntriesQuery(displayNameFilters, offset, limit);
        return jdbcTemplate().query(
            querySpec.sql(),
            (rs, rowNum) ->
                new UnifiedAccessEntry(
                    rs.getString("kind"),
                    rs.getString("row_id"),
                    rs.getString("identity_value"),
                    rs.getString("display_name"),
                    rs.getString("role_value"),
                    rs.getString("reference_id"),
                    rs.getString("reference_type"),
                    rs.getTimestamp("created_at")
                ),
            querySpec.args().toArray()
        );
    }

    private QuerySpec unifiedEntriesQuery(List<String> displayNameFilters, int offset, int limit) {
        StringBuilder query = new StringBuilder(
            """
            select kind, row_id, identity_value, display_name, role_value, reference_id, reference_type, created_at
            from (
                select
                    'member' as kind,
                    m.id as row_id,
                    m.member_id as identity_value,
                    trim(concat(coalesce(u.firstname, ''), ' ', coalesce(u.lastname, ''))) as display_name,
                    lower(trim(concat(coalesce(u.firstname, ''), ' ', coalesce(u.lastname, '')))) as display_name_sort,
                    m.role_id as role_value,
                    m.reference_id,
                    m.reference_type,
                    m.created_at
                from %s m
                left join %s u on u.id = m.member_id
                where m.reference_type = ? and m.reference_id = ?
                union all
                select
                    'invitation' as kind,
                    i.id as row_id,
                    i.email as identity_value,
                    i.email as display_name,
                    lower(i.email) as display_name_sort,
                    i.application_role as role_value,
                    i.reference_id,
                    i.reference_type,
                    i.created_at
                from %s i
                where i.reference_type = ? and i.reference_id = ?
            ) unified_entries
            """.formatted(tableName("memberships"), tableName("users"), tableName("invitations"))
        );

        List<Object> args = new ArrayList<>(
            List.of(APPLICATION_REFERENCE_TYPE, APPLICATION_ID, APPLICATION_REFERENCE_TYPE, APPLICATION_ID)
        );

        if (!displayNameFilters.isEmpty()) {
            query.append(" where (");
            for (int i = 0; i < displayNameFilters.size(); i++) {
                if (i > 0) {
                    query.append(" or ");
                }
                query.append("display_name_sort like ?");
                args.add(displayNameFilters.get(i));
            }
            query.append(")");
        }

        query.append(" order by display_name_sort asc, row_id asc limit ? offset ?");
        args.add(limit);
        args.add(offset);

        return new QuerySpec(query.toString(), args);
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    private String tableName(String table) {
        return escapeReservedWord(graviteeProperties.getProperty("management.jdbc.prefix", "") + table);
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

    private record UnifiedAccessEntry(
        String kind,
        String rowId,
        String identity,
        String displayName,
        String role,
        String referenceId,
        String referenceType,
        Timestamp createdAt
    ) {}

    private record QuerySpec(String sql, List<Object> args) {}

    private record BenchmarkResult(List<Double> runsMs, double averageMs) {}

    private record SummaryStats(double minMs, double medianMs, double maxMs) {}
}
