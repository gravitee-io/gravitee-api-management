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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static io.gravitee.repository.jdbc.management.JdbcHelper.*;
import static java.lang.String.format;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.search.AuditCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Audit;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 *
 * @author njt
 */
@Repository
public class JdbcAuditRepository extends JdbcAbstractPageableRepository<Audit> implements AuditRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAuditRepository.class);
    private final String AUDIT_PROPERTIES;

    JdbcAuditRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "audits");
        AUDIT_PROPERTIES = getTableNameFor("audit_properties");
    }

    @Override
    protected JdbcObjectMapper<Audit> buildOrm() {
        return JdbcObjectMapper
            .builder(Audit.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, Audit.AuditReferenceType.class)
            .addColumn("user", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("event", Types.NVARCHAR, String.class)
            .addColumn("patch", Types.NVARCHAR, String.class)
            .build();
    }

    private static final JdbcHelper.ChildAdder<Audit> CHILD_ADDER = (Audit parent, ResultSet rs) -> {
        Map<String, String> properties = parent.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
            parent.setProperties(properties);
        }
        if (rs.getString("key") != null) {
            properties.put(rs.getString("key"), rs.getString("value"));
        }
    };

    @Override
    public Optional<Audit> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcAuditRepository.findById({})", id);
        try {
            JdbcHelper.CollatingRowMapper<Audit> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                CHILD_ADDER,
                "id"
            );
            jdbcTemplate.query(
                getOrm().getSelectAllSql() + " a left join " + AUDIT_PROPERTIES + " ap on a.id = ap.audit_id where a.id = ?",
                rowMapper,
                id
            );
            Optional<Audit> result = rowMapper.getRows().stream().findFirst();
            LOGGER.debug("JdbcAuditRepository.findById({}) = {}", id, result);
            return result;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find audit by id:", ex);
            throw new TechnicalException("Failed to find audit by id", ex);
        }
    }

    @Override
    public Audit create(Audit item) throws TechnicalException {
        LOGGER.debug("JdbcAuditRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            storeProperties(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create audit", ex);
            throw new TechnicalException("Failed to create audit", ex);
        }
    }

    @Override
    public Audit update(final Audit audit) throws TechnicalException {
        LOGGER.debug("JdbcAuditRepository.update({})", audit);
        if (audit == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(audit, audit.getId()));
            storeProperties(audit, true);
            return findById(audit.getId())
                .orElseThrow(() -> new IllegalStateException(format("No audit found with id [%s]", audit.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update audit", ex);
            throw new TechnicalException("Failed to update audit", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + AUDIT_PROPERTIES + " where audit_id = ?", id);
        jdbcTemplate.update(getOrm().getDeleteSql(), id);
    }

    private void storeProperties(Audit audit, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + AUDIT_PROPERTIES + " where audit_id = ?", audit.getId());
        }
        if (audit.getProperties() != null && !audit.getProperties().isEmpty()) {
            List<Map.Entry<String, String>> entries = new ArrayList<>(audit.getProperties().entrySet());
            jdbcTemplate.batchUpdate(
                "insert into " + AUDIT_PROPERTIES + " ( audit_id, " + escapeReservedWord("key") + ", value ) values ( ?, ?, ? )",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, audit.getId());
                        ps.setString(2, entries.get(i).getKey());
                        ps.setString(3, entries.get(i).getValue());
                    }

                    @Override
                    public int getBatchSize() {
                        return entries.size();
                    }
                }
            );
        }
    }

    private String criteriaToString(AuditCriteria filter) {
        return (
            "{ " +
            "from: " +
            filter.getFrom() +
            ", " +
            "to: " +
            filter.getTo() +
            ", " +
            "references: " +
            filter.getReferences() +
            ", " +
            "props: " +
            filter.getProperties() +
            ", " +
            "events: " +
            filter.getEvents() +
            ", " +
            "environmentIds: " +
            filter.getEnvironmentIds() +
            ", " +
            "organizationId: " +
            filter.getOrganizationId() +
            " }"
        );
    }

    @Override
    public Page<Audit> search(AuditCriteria filter, Pageable page) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JdbcEventRepository.search({}, {})", criteriaToString(filter), page);
        }
        final List<Object> argsList = new ArrayList<>();
        final StringBuilder builder = new StringBuilder(
            getOrm().getSelectAllSql() + " a left join " + AUDIT_PROPERTIES + " ap on a.id = ap.audit_id "
        );
        boolean started = false;
        if (filter.getFrom() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("created_at >= ?");
            argsList.add(new Date(filter.getFrom()));
            started = true;
        }
        if (filter.getTo() > 0) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("created_at <= ?");
            argsList.add(new Date(filter.getTo()));
            started = true;
        }

        if (!isEmpty(filter.getEnvironmentIds())) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("a.environment_id in (").append(getOrm().buildInClause(filter.getEnvironmentIds())).append(")");
            argsList.addAll(filter.getEnvironmentIds());
            started = true;
        }

        if (filter.getOrganizationId() != null) {
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("a.organization_id = ?");
            argsList.add(filter.getOrganizationId());
            started = true;
        }

        started = addPropertiesWhereClause(filter, argsList, builder, started);
        started = addReferencesWhereClause(filter, argsList, builder, started);
        addStringsWhereClause(filter.getEvents(), "event", argsList, builder, started);

        builder.append(" order by created_at desc ");

        String sql = builder.toString();

        LOGGER.debug("argsList = {}", argsList);
        Object[] args = argsList.toArray();
        LOGGER.debug("SQL: {}", sql);
        LOGGER.debug("Args ({}): {}", args.length, args);
        for (int i = 0; i < args.length; ++i) {
            LOGGER.debug("args[{}] = {} {}", i, args[i], args[i].getClass());
        }

        List<Audit> audits;
        try {
            JdbcHelper.CollatingRowMapper<Audit> rowMapper = new JdbcHelper.CollatingRowMapper<>(
                getOrm().getRowMapper(),
                CHILD_ADDER,
                "id"
            );
            jdbcTemplate.query(sql, rowMapper, args);
            audits = rowMapper.getRows();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find audit records:", ex);
            throw new IllegalStateException("Failed to find audit records", ex);
        }

        LOGGER.debug("audit records found ({}): {}", audits.size(), audits);

        return getResultAsPage(page, audits);
    }

    private boolean addReferencesWhereClause(AuditCriteria filter, List<Object> argsList, StringBuilder builder, boolean started) {
        if ((filter.getReferences() != null) && !filter.getReferences().isEmpty()) {
            LOGGER.debug("filter.getReferences() = {}", filter.getReferences());
            LOGGER.debug("argsList before loop = {}", argsList);
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("(");
            for (Entry<Audit.AuditReferenceType, List<String>> ref : filter.getReferences().entrySet()) {
                builder.append("( reference_type = ?");
                argsList.add(ref.getKey().toString());
                LOGGER.debug("argsList after ref type = {}", argsList);

                if (ref.getValue() != null && !ref.getValue().isEmpty()) {
                    StringJoiner inReferenceIdsQueryString = new StringJoiner(",", " and reference_id in (", ")");

                    for (String id : ref.getValue()) {
                        inReferenceIdsQueryString.add("?");
                        argsList.add(id);
                        LOGGER.debug("argsList after ref id = {}", argsList);
                    }
                    builder.append(inReferenceIdsQueryString);
                }

                builder.append(")");

                started = true;
            }
            builder.append(") ");
            LOGGER.debug("argsList after loop = {}", argsList);
        }
        return started;
    }

    private boolean addPropertiesWhereClause(AuditCriteria filter, List<Object> argsList, StringBuilder builder, boolean started) {
        if ((filter.getProperties() != null) && !filter.getProperties().isEmpty()) {
            builder.append(" left join " + AUDIT_PROPERTIES + " prop on prop.audit_id = a.id ");
            builder.append(started ? AND_CLAUSE : WHERE_CLAUSE);
            builder.append("(");
            boolean first = true;
            for (Entry<String, String> property : filter.getProperties().entrySet()) {
                first = addCondition(first, builder, property.getKey(), property.getValue(), argsList);
            }
            builder.append(")");
            started = true;
        }
        return started;
    }

    @Override
    public Set<Audit> findAll() throws TechnicalException {
        throw new IllegalStateException("not implemented cause of high amount of data. Use pageable search instead");
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, Audit.AuditReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcAuditRepository.deleteByReferenceIdAndReferenceType({}/{})", referenceId, referenceType);
        try {
            final var auditIds = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where reference_id = ? and reference_type = ?",
                String.class,
                referenceId,
                referenceType.name()
            );

            if (!auditIds.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + AUDIT_PROPERTIES + " where audit_id IN (" + getOrm().buildInClause(auditIds) + ")",
                    auditIds.toArray()
                );
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
            }

            LOGGER.debug("JdbcAuditRepository.deleteByReferenceIdAndReferenceType({}/{}) - Done", referenceId, referenceType);
            return auditIds;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete audit for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete audit by reference", ex);
        }
    }
}
