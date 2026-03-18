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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
public class ApplicationAccessEntriesInMemoryJdbcTest extends AbstractRepositoryTest {

    private static final String APPLICATION_ID = "app-1";
    private static final String OTHER_APPLICATION_ID = "app-2";
    private static final String APPLICATION_REFERENCE_TYPE = "APPLICATION";

    @Inject
    private DataSource dataSource;

    @Inject
    private Properties graviteeProperties;

    @Test
    public void shouldMergeMembersAndInvitationsInApplicationLayer() {
        seedData();
        printTable("users", "select * from " + tableName("users") + " order by created_at asc, id asc");
        printTable("memberships", "select * from " + tableName("memberships") + " order by created_at asc, id asc");
        printTable("invitations", "select * from " + tableName("invitations") + " order by created_at asc, id asc");

        List<UnifiedAccessEntry> unifiedEntries = executeUnifiedEntriesQuery(null, 0, Integer.MAX_VALUE);

        printUnifiedEntries("unifiedEntriesInMemory", unifiedEntries);

        assertThat(unifiedEntries).hasSize(7);
        assertThat(unifiedEntries)
            .extracting(UnifiedAccessEntry::kind)
            .containsExactly("member", "invitation", "invitation", "member", "member", "member", "invitation");
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
    }

    @Test
    public void shouldFilterSortAndPaginateUnifiedEntriesInApplicationLayer() {
        seedData();

        List<UnifiedAccessEntry> filteredSortedPagedEntries = executeUnifiedEntriesQuery("^[ab]", 1, 3);

        printUnifiedEntries("filteredSortedPagedEntriesInMemory", filteredSortedPagedEntries);

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
        insertMembership("membership-other", "edgar", "APP_USER", new Date(5_000L), OTHER_APPLICATION_ID, APPLICATION_REFERENCE_TYPE);

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
        insertInvitation(
            "invitation-other",
            "yuki@example.com",
            "APPLICATION_USER",
            new Date(9_000L),
            OTHER_APPLICATION_ID,
            APPLICATION_REFERENCE_TYPE
        );
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

    private List<UnifiedAccessEntry> executeUnifiedEntriesQuery(String displayNameRegex, int offset, int limit) {
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

        Pattern displayNamePattern = displayNameRegex == null ? null : Pattern.compile(displayNameRegex);
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
            .filter(entry -> displayNamePattern == null || displayNamePattern.matcher(entry.displayNameSort()).find())
            .sorted(Comparator.comparing(UnifiedAccessEntry::displayNameSort).thenComparing(UnifiedAccessEntry::rowId))
            .skip(offset)
            .limit(limit)
            .toList();
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

    private void printUnifiedEntries(String label, List<UnifiedAccessEntry> unifiedEntries) {
        System.out.println("--- " + label + " ---");
        unifiedEntries.forEach(System.out::println);
    }

    private String tableName(String table) {
        return escapeReservedWord(graviteeProperties.getProperty("management.jdbc.prefix", "") + table);
    }

    private String placeholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
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
}
