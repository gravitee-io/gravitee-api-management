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
import static io.gravitee.repository.jdbc.utils.FieldUtils.toSnakeCase;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.GuardianRepository;
import io.gravitee.repository.management.api.search.GuardianCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Guardian;
import io.gravitee.repository.management.model.GuardianLifecycleState;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Repository
public class JdbcGuardianRepository extends JdbcAbstractCrudRepository<Guardian, String> implements GuardianRepository {

    JdbcGuardianRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "guardians");
    }

    @Override
    protected JdbcObjectMapper<Guardian> buildOrm() {
        return JdbcObjectMapper.builder(Guardian.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("phase", Types.NVARCHAR, Guardian.Phase.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("lifecycle_state", Types.NVARCHAR, GuardianLifecycleState.class)
            .addColumn("deployed_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(Guardian item) {
        return item.getId();
    }

    @Override
    public Set<Guardian> findAll() throws TechnicalException {
        // Not an unfinished job: a Guardian is always read within its environment, so a global
        // findAll has no meaning here. The shared repository contract asserts this refusal.
        throw new IllegalStateException("findAll is not supported: a Guardian is always read within its environment");
    }

    static String sortColumn(final String field) {
        // the allow-list itself lives on the shared criteria, so both backends offer the same fields
        return toSnakeCase(GuardianCriteria.requireSortableField(field));
    }

    /** Escapes the LIKE wildcards so the caller's query is matched as literal text. */
    private static String escapeLikePattern(final String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public Page<Guardian> search(GuardianCriteria criteria, Pageable pageable, Sortable sortable) throws TechnicalException {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        Objects.requireNonNull(criteria, "GuardianCriteria must not be null");
        Objects.requireNonNull(criteria.getEnvironmentId(), "EnvironmentId must not be null");
        log.debug("JdbcGuardianRepository.search({}, {})", criteria, pageable);

        try {
            final StringJoiner andWhere = new StringJoiner(" AND ");
            final List<Object> andWhereParams = new ArrayList<>();

            andWhere.add("environment_id = ?");
            andWhereParams.add(criteria.getEnvironmentId());

            if (criteria.getQuery() != null) {
                // ESCAPE plus an escaped pattern: without it '%' and '_' stay LIKE wildcards, so
                // q="nam_" would match "name" here while Mongo, which quotes the query, matches
                // nothing. The contract says the query is literal text on both backends.
                andWhere.add("lower(name) like ? escape '\\'");
                andWhereParams.add("%" + escapeLikePattern(criteria.getQuery().toLowerCase()) + "%");
            }
            if (criteria.getLifecycleState() != null) {
                andWhere.add("lifecycle_state = ?");
                andWhereParams.add(criteria.getLifecycleState().name());
            }

            final var total = jdbcTemplate.queryForObject(
                "select count(*) from " + this.tableName + " WHERE " + andWhere,
                Long.class,
                andWhereParams.toArray()
            );
            if (total == null || total == 0) {
                return new Page<>(List.of(), pageable.pageNumber(), 0, 0);
            }

            sortable = sortable == null ? new SortableBuilder().field("created_at").setAsc(true).build() : sortable;
            final var sortOrder = sortable.order() == Order.ASC ? "ASC" : "DESC";
            final var sortField = sortColumn(sortable.field());

            final var result = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                    " WHERE " +
                    andWhere +
                    " ORDER BY " +
                    sortField +
                    " " +
                    sortOrder +
                    // id breaks ties: a single sort key leaves rows with equal values in an
                    // undefined order, which offset paging turns into repeated or skipped rows.
                    ", id ASC " +
                    createPagingClause(pageable.pageSize(), pageable.from()),
                getOrm().getRowMapper(),
                andWhereParams.toArray()
            );

            return new Page<>(result, pageable.pageNumber(), result.size(), total);
        } catch (Exception ex) {
            // no log here: the boundary logs it once, and AGENTS.md asks lower layers to wrap with
            // context and rethrow rather than duplicate the stack trace
            throw new TechnicalException("Failed to search for Guardians", ex);
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete guardians by environment ID [{}]", environmentId);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " WHERE environment_id = ?",
                String.class,
                environmentId
            );
            if (!rows.isEmpty()) {
                jdbcTemplate.update("delete from " + tableName + " where environment_id = ?", environmentId);
            }
            log.debug("Delete guardians by environment ID [{}] - Done", environmentId);
            return rows;
        } catch (Exception ex) {
            // no log here: the boundary logs it once, and AGENTS.md asks lower layers to wrap with
            // context and rethrow rather than duplicate the stack trace
            throw new TechnicalException("Failed to delete Guardians by environment ID [%s]".formatted(environmentId), ex);
        }
    }
}
