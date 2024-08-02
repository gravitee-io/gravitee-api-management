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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createPagingClause;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.repository.management.model.AccessPointTarget;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
@Slf4j
public class JdbcAccessPointRepository extends JdbcAbstractCrudRepository<AccessPoint, String> implements AccessPointRepository {

    static String createdStatusClause = " and status = '" + AccessPointStatus.CREATED.name() + "'";

    JdbcAccessPointRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "access_points");
    }

    @Override
    protected JdbcObjectMapper<AccessPoint> buildOrm() {
        return JdbcObjectMapper
            .builder(AccessPoint.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, AccessPointReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("target", Types.NVARCHAR, AccessPointTarget.class)
            .addColumn("host", Types.NVARCHAR, String.class)
            .addColumn("secured", Types.BOOLEAN, boolean.class)
            .addColumn("overriding", Types.BOOLEAN, boolean.class)
            .addColumn("status", Types.NVARCHAR, AccessPointStatus.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(final AccessPoint item) {
        return item.getId();
    }

    @Override
    public Optional<AccessPoint> findByHost(final String host) throws TechnicalException {
        try {
            return jdbcTemplate
                .query(getOrm().getSelectAllSql() + " a where host = ?" + createdStatusClause, getOrm().getRowMapper(), host)
                .stream()
                .findFirst();
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find access point by host", ex);
        }
    }

    @Override
    public List<AccessPoint> findByTarget(final AccessPointTarget target) throws TechnicalException {
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " t where target = ?" + createdStatusClause,
                getOrm().getRowMapper(),
                target.name()
            );
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find access point by target", ex);
        }
    }

    @Override
    public List<AccessPoint> findByReferenceAndTarget(
        final AccessPointReferenceType referenceType,
        final String referenceId,
        final AccessPointTarget target
    ) throws TechnicalException {
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " t where reference_id = ? and reference_type = ? and target = ?" + createdStatusClause,
                getOrm().getRowMapper(),
                referenceId,
                referenceType.name(),
                target.name()
            );
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find access point by reference and target", ex);
        }
    }

    @Override
    public List<AccessPoint> findByCriteria(AccessPointCriteria criteria, Long page, Long size) throws TechnicalException {
        try {
            List<Object> args = new ArrayList<>();
            StringBuilder query = new StringBuilder(getOrm().getSelectAllSql()).append(buildCriteriaClauses(criteria, args));

            query.append(" order by updated_at asc ");
            if (page != null && size != null && size > 0) {
                final int limit = size.intValue();
                query.append(createPagingClause(limit, (page.intValue() * limit)));
            }

            return jdbcTemplate.query(query.toString(), getRowMapper(), args.toArray());
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find access point by criteria", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(final String referenceId, final AccessPointReferenceType referenceType)
        throws TechnicalException {
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_id = ? and reference_type = ?",
                String.class,
                referenceId,
                referenceType.name()
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
            }
            return rows;
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to delete access points by reference", ex);
        }
    }

    private String buildCriteriaClauses(AccessPointCriteria criteria, List<Object> args) {
        boolean first = true;
        var criteriaBuilder = new StringBuilder();
        if (criteria.getFrom() > 0) {
            first = addClause(first, criteriaBuilder);
            criteriaBuilder.append("updated_at > ?");
            args.add(new Date(criteria.getFrom()));
        }

        if (criteria.getTo() > 0) {
            first = addClause(first, criteriaBuilder);
            criteriaBuilder.append("updated_at < ?");
            args.add(new Date(criteria.getTo()));
        }

        if (criteria.getReferenceType() != null) {
            first = addClause(first, criteriaBuilder);
            criteriaBuilder.append("reference_type = ?");
            args.add(criteria.getReferenceType().name());
        }

        if (criteria.getTarget() != null) {
            first = addClause(first, criteriaBuilder);
            criteriaBuilder.append("target = ?");
            args.add(criteria.getTarget().name());
        }

        if (criteria.getStatus() != null) {
            first = addClause(first, criteriaBuilder);
            criteriaBuilder.append("status = ?");
            args.add(criteria.getStatus().name());
        }

        if (criteria.getReferenceIds() != null && !criteria.getReferenceIds().isEmpty()) {
            first = addClause(first, criteriaBuilder);
            criteriaBuilder.append("reference_id IN (");
            criteriaBuilder.append(criteria.getReferenceIds().stream().map(id -> "?").collect(Collectors.joining(",")));
            criteriaBuilder.append(")");
            args.addAll(criteria.getReferenceIds());
        }
        return criteriaBuilder.toString();
    }

    private boolean addClause(boolean first, StringBuilder query) {
        if (first) {
            query.append(" where ");
        } else {
            query.append(" and ");
        }
        return false;
    }
}
