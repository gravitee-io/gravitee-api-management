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
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
public class ApplicationAccessEntriesAggregationJdbcTest extends AbstractRepositoryTest {

    private static final String APPLICATION_ID = "app-1";
    private static final String APPLICATION_REFERENCE_TYPE = "APPLICATION";

    @Inject
    private DataSource dataSource;

    @Inject
    private Properties graviteeProperties;

    @Test
    public void shouldAggregateMembersAndInvitationsIntoOneListUsingSingleSqlQuery() {
        seedData();
        printTable("users", "select * from " + tableName("users") + " order by created_at asc, id asc");
        printTable("memberships", "select * from " + tableName("memberships") + " order by created_at asc, id asc");
        printTable("invitations", "select * from " + tableName("invitations") + " order by created_at asc, id asc");

        List<UnifiedAccessEntry> unifiedEntries = executeUnifiedEntriesQuery(List.of(), null, null);

        printUnifiedEntries(unifiedEntries);

        assertThat(unifiedEntries).hasSize(7);
        assertThat(unifiedEntries)
            .extracting(UnifiedAccessEntry::kind)
            .containsExactly("member", "invitation", "invitation", "member", "member", "member", "invitation");
        assertThat(unifiedEntries)
            .extracting(UnifiedAccessEntry::rowId)
            .containsExactly(
                "membership-1",
                "invitation-1",
                "invitation-2",
                "membership-2",
                "membership-3",
                "membership-4",
                "invitation-3"
            );
        assertThat(unifiedEntries)
            .extracting(UnifiedAccessEntry::identity)
            .containsExactly("alice", "amelia@example.com", "brian@example.com", "bruce", "charlie", "dana", "zoe@example.com");
        assertThat(unifiedEntries)
            .extracting(UnifiedAccessEntry::displayName)
            .containsExactly(
                "Alice Anderson",
                "amelia@example.com",
                "brian@example.com",
                "Bruce Banner",
                "Charlie Clark",
                "Dana Doe",
                "zoe@example.com"
            );
        assertThat(unifiedEntries)
            .extracting(UnifiedAccessEntry::role)
            .containsExactly(
                "APP_USER",
                "APPLICATION_USER",
                "APPLICATION_ADMIN",
                "APP_REVIEWER",
                "APP_ADMIN",
                "APP_USER",
                "APPLICATION_USER"
            );
    }

    @Test
    public void shouldFilterSortAndPaginateUnifiedEntriesByDisplayName() {
        seedData();

        List<UnifiedAccessEntry> filteredSortedPagedEntries = executeUnifiedEntriesQuery(List.of("a%", "b%"), 1, 3);

        printUnifiedEntries("filteredSortedPagedEntries", filteredSortedPagedEntries);

        assertThat(filteredSortedPagedEntries).hasSize(3);
        assertThat(filteredSortedPagedEntries)
            .extracting(UnifiedAccessEntry::displayName)
            .containsExactly("amelia@example.com", "brian@example.com", "Bruce Banner");
        assertThat(filteredSortedPagedEntries).extracting(UnifiedAccessEntry::kind).containsExactly("invitation", "invitation", "member");
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

    private void seedData() {
        insertUser("dana", "Dana", "Doe", "dana@example.com", new Date(400L));
        insertUser("edgar", "Edgar", "Evans", "edgar@example.com", new Date(500L));
        insertUser("alice", "Alice", "Anderson", "alice@example.com", new Date(100L));
        insertUser("bruce", "Bruce", "Banner", "bruce@example.com", new Date(200L));
        insertUser("charlie", "Charlie", "Clark", "charlie@example.com", new Date(300L));

        insertMembership("membership-1", "alice", "APP_USER", new Date(1_000L), APPLICATION_ID, APPLICATION_REFERENCE_TYPE);
        insertMembership("membership-2", "bruce", "APP_REVIEWER", new Date(2_000L), APPLICATION_ID, APPLICATION_REFERENCE_TYPE);
        insertMembership("membership-3", "charlie", "APP_ADMIN", new Date(3_000L), APPLICATION_ID, APPLICATION_REFERENCE_TYPE);
        insertMembership("membership-4", "dana", "APP_USER", new Date(4_000L), APPLICATION_ID, APPLICATION_REFERENCE_TYPE);
        insertMembership("membership-other", "edgar", "APP_USER", new Date(5_000L), "app-2", APPLICATION_REFERENCE_TYPE);

        insertInvitation(
            "invitation-1",
            "amelia@example.com",
            "APPLICATION_USER",
            new Date(6_000L),
            APPLICATION_ID,
            APPLICATION_REFERENCE_TYPE
        );
        insertInvitation(
            "invitation-2",
            "brian@example.com",
            "APPLICATION_ADMIN",
            new Date(7_000L),
            APPLICATION_ID,
            APPLICATION_REFERENCE_TYPE
        );
        insertInvitation(
            "invitation-3",
            "zoe@example.com",
            "APPLICATION_USER",
            new Date(8_000L),
            APPLICATION_ID,
            APPLICATION_REFERENCE_TYPE
        );
        insertInvitation("invitation-other", "yuki@example.com", "APPLICATION_USER", new Date(9_000L), "app-2", APPLICATION_REFERENCE_TYPE);
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

    private void insertMembership(String id, String memberId, String roleId, Date createdAt, String referenceId, String referenceType) {
        jdbcTemplate().update(
            "insert into " +
                tableName("memberships") +
                " (id, member_id, member_type, reference_type, reference_id, role_id, source, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id,
            memberId,
            "USER",
            referenceType,
            referenceId,
            roleId,
            "memory",
            new Timestamp(createdAt.getTime()),
            new Timestamp(createdAt.getTime())
        );
    }

    private void insertInvitation(
        String id,
        String email,
        String applicationRole,
        Date createdAt,
        String referenceId,
        String referenceType
    ) {
        jdbcTemplate().update(
            "insert into " +
                tableName("invitations") +
                " (id, reference_type, reference_id, email, application_role, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?)",
            id,
            referenceType,
            referenceId,
            email,
            applicationRole,
            new Timestamp(createdAt.getTime()),
            new Timestamp(createdAt.getTime())
        );
    }

    private List<UnifiedAccessEntry> executeUnifiedEntriesQuery(List<String> displayNameFilters, Integer offset, Integer limit) {
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

    private QuerySpec unifiedEntriesQuery(List<String> displayNameFilters, Integer offset, Integer limit) {
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

        query.append(" order by display_name_sort asc, row_id asc");

        if (limit != null) {
            query.append(" limit ?");
            args.add(limit);
        }

        if (offset != null) {
            query.append(" offset ?");
            args.add(offset);
        }

        return new QuerySpec(query.toString(), args);
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    private void printTable(String label, String query) {
        System.out.println("--- " + label + " ---");
        jdbcTemplate()
            .queryForList(query)
            .forEach(row -> System.out.println(row));
    }

    private void printUnifiedEntries(List<UnifiedAccessEntry> unifiedEntries) {
        printUnifiedEntries("unifiedEntries", unifiedEntries);
    }

    private void printUnifiedEntries(String label, List<UnifiedAccessEntry> unifiedEntries) {
        System.out.println("--- " + label + " ---");
        unifiedEntries.forEach(System.out::println);
    }

    private String tableName(String table) {
        return escapeReservedWord(graviteeProperties.getProperty("management.jdbc.prefix", "") + table);
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
}
