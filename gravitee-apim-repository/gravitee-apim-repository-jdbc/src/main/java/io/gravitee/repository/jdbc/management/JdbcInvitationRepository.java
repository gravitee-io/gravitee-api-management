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
package io.gravitee.repository.jdbc.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.jdbc.utils.FieldUtils;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.api.InvitationRepository.InvitationCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Repository
public class JdbcInvitationRepository extends JdbcAbstractCrudRepository<Invitation, String> implements InvitationRepository {

    private static final String DEFAULT_SORT_FIELD = "email";
    private static final Set<String> SORTABLE_COLUMNS = Set.of(
        "id",
        "reference_type",
        "reference_id",
        "email",
        "api_role",
        "application_role",
        "created_at",
        "updated_at"
    );
    private static final Set<String> CASE_INSENSITIVE_SORTABLE_COLUMNS = Set.of(
        "id",
        "reference_type",
        "reference_id",
        "email",
        "api_role",
        "application_role"
    );

    JdbcInvitationRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "invitations");
    }

    @Override
    protected JdbcObjectMapper<Invitation> buildOrm() {
        return JdbcObjectMapper.builder(Invitation.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("email", Types.NVARCHAR, String.class)
            .addColumn("api_role", Types.NVARCHAR, String.class)
            .addColumn("application_role", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(final Invitation invitation) {
        return invitation.getId();
    }

    @Override
    public List<Invitation> findByEmail(String email) throws TechnicalException {
        log.debug("JdbcInvitationRepository.findByEmail({})", email);
        try {
            return jdbcTemplate.query(getOrm().getSelectAllSql() + " where email = ?", getOrm().getRowMapper(), email);
        } catch (final Exception ex) {
            log.error("Failed to find invitations by email: {}", email, ex);
            throw new TechnicalException("Failed to find invitations by email", ex);
        }
    }

    @Override
    public List<Invitation> findByReferenceIdAndReferenceType(final String referenceId, final InvitationReferenceType referenceType)
        throws TechnicalException {
        log.debug("JdbcInvitationRepository.findByReferenceIdAndReferenceType({}, {})", referenceId, referenceType);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where reference_type = ? and reference_id = ?",
                getOrm().getRowMapper(),
                referenceType.name(),
                referenceId
            );
        } catch (final Exception ex) {
            log.error("Failed to find invitation for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to find invitation by reference", ex);
        }
    }

    @Override
    public Page<Invitation> search(InvitationCriteria criteria, Sortable sortable, Pageable pageable) throws TechnicalException {
        log.debug("JdbcInvitationRepository.search({})", criteria);

        try {
            var query = new StringBuilder(getOrm().getSelectAllSql());
            addCriteriaClauses(query, criteria);
            applySortable(sortable, query);

            var invitations = jdbcTemplate.query(query.toString(), ps -> fillPreparedStatement(criteria, ps), getOrm().getRowMapper());
            return getResultAsPage(pageable, invitations);
        } catch (final Exception ex) {
            log.error("Failed to search invitations", ex);
            throw new TechnicalException("Failed to search invitations", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, InvitationReferenceType referenceType)
        throws TechnicalException {
        log.debug("JdbcInvitationRepository.deleteByReferenceIdAndReferenceType({}/{})", referenceId, referenceType);
        try {
            final var invitationIds = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_id = ? and reference_type = ?",
                String.class,
                referenceId,
                referenceType.name()
            );

            if (!invitationIds.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
            }

            log.debug("JdbcInvitationRepository.deleteByReferenceIdAndReferenceType({}/{}) - Done", referenceId, referenceType);
            return invitationIds;
        } catch (final Exception ex) {
            log.error("Failed to delete invitation for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete invitation by reference", ex);
        }
    }

    private void addCriteriaClauses(StringBuilder query, InvitationCriteria criteria) {
        var clauses = new ArrayList<String>();

        if (criteria != null) {
            if (StringUtils.hasText(criteria.referenceId())) {
                clauses.add("reference_id = ?");
            }
            if (criteria.referenceType() != null) {
                clauses.add("reference_type = ?");
            }
            if (StringUtils.hasText(criteria.email())) {
                clauses.add("lower(email) like ?");
            }
        }

        if (!clauses.isEmpty()) {
            query.append(" where ").append(String.join(" and ", clauses));
        }
    }

    private void fillPreparedStatement(InvitationCriteria criteria, PreparedStatement ps) throws java.sql.SQLException {
        if (criteria == null) {
            return;
        }

        var index = 1;
        if (StringUtils.hasText(criteria.referenceId())) {
            ps.setString(index++, criteria.referenceId());
        }
        if (criteria.referenceType() != null) {
            ps.setString(index++, criteria.referenceType().name());
        }
        if (StringUtils.hasText(criteria.email())) {
            ps.setString(index, "%" + criteria.email().toLowerCase(Locale.ROOT) + "%");
        }
    }

    private void applySortable(Sortable sortable, StringBuilder query) {
        var field = sortable != null && StringUtils.hasText(sortable.field())
            ? FieldUtils.toSnakeCase(sortable.field())
            : DEFAULT_SORT_FIELD;

        if (!SORTABLE_COLUMNS.contains(field)) {
            field = DEFAULT_SORT_FIELD;
        }

        query.append(" order by case when ").append(field).append(" is null then 1 else 0 end, ");

        if (CASE_INSENSITIVE_SORTABLE_COLUMNS.contains(field)) {
            query.append("lower(").append(field).append(")");
        } else {
            query.append(field);
        }

        query.append(sortable != null && Order.DESC.equals(sortable.order()) ? " desc " : " asc ");
    }
}
