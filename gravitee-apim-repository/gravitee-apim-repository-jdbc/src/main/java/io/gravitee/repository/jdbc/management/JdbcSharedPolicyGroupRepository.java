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
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.SharedPolicyGroupRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SharedPolicyGroupCriteria;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.SharedPolicyGroup;
import io.gravitee.repository.management.model.SharedPolicyGroupLifecycleState;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSharedPolicyGroupRepository
    extends JdbcAbstractCrudRepository<SharedPolicyGroup, String>
    implements SharedPolicyGroupRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSharedPolicyGroupRepository.class);

    JdbcSharedPolicyGroupRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "sharedpolicygroups");
    }

    @Override
    protected JdbcObjectMapper<SharedPolicyGroup> buildOrm() {
        return JdbcObjectMapper
            .builder(SharedPolicyGroup.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("cross_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("prerequisite_message", Types.NVARCHAR, String.class)
            .addColumn("version", Types.INTEGER, Integer.class)
            .addColumn("api_type", Types.NVARCHAR, ApiType.class)
            .addColumn("phase", Types.NVARCHAR, SharedPolicyGroup.ExecutionPhase.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("lifecycle_state", Types.NVARCHAR, SharedPolicyGroupLifecycleState.class)
            .addColumn("deployed_at", Types.TIMESTAMP, Date.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(SharedPolicyGroup item) {
        return item.getId();
    }

    @Override
    public SharedPolicyGroup create(SharedPolicyGroup item) throws TechnicalException {
        return super.create(item);
    }

    @Override
    public SharedPolicyGroup update(final SharedPolicyGroup item) throws TechnicalException {
        return super.update(item);
    }

    @Override
    public void delete(String id) throws TechnicalException {
        super.delete(id);
    }

    @Override
    public Optional<SharedPolicyGroup> findById(String id) throws TechnicalException {
        return super.findById(id);
    }

    @Override
    public Set<SharedPolicyGroup> findAll() throws TechnicalException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Page<SharedPolicyGroup> search(SharedPolicyGroupCriteria criteria, Pageable pageable, Sortable sortable)
        throws TechnicalException {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        Objects.requireNonNull(criteria, "SharedPolicyGroupCriteria must not be null");
        Objects.requireNonNull(criteria.getEnvironmentId(), "EnvironmentId must not be null");
        LOGGER.debug("JdbcSharedPolicyGroupRepository.search({}, {})", criteria.toString(), pageable.toString());

        try {
            StringJoiner andWhere = new StringJoiner(" AND ");
            List<Object> andWhereParams = new ArrayList<>();

            andWhere.add("environment_id = ?");
            andWhereParams.add(criteria.getEnvironmentId());

            if (criteria.getName() != null) {
                andWhere.add("lower(name) like ?");
                andWhereParams.add("%" + criteria.getName().toLowerCase() + "%");
            }
            if (criteria.getLifecycleState() != null) {
                andWhere.add("lifecycle_state = ?");
                andWhereParams.add(criteria.getLifecycleState().name());
            }

            var total = jdbcTemplate.queryForObject(
                "select count(*) from " + this.tableName + " WHERE " + andWhere,
                Long.class,
                andWhereParams.toArray()
            );

            if (total == null || total == 0) {
                return new Page<>(List.of(), pageable.pageNumber(), 0, 0);
            }

            sortable = sortable == null ? new SortableBuilder().field("created_at").setAsc(true).build() : sortable;
            final var sortOrder = sortable.order() == Order.ASC ? "ASC" : "DESC";
            final var sortField = toSnakeCase(sortable.field());

            var result = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " WHERE " +
                andWhere +
                " ORDER BY " +
                sortField +
                " " +
                sortOrder +
                " " +
                createPagingClause(pageable.pageSize(), pageable.from()),
                getOrm().getRowMapper(),
                andWhereParams.toArray()
            );

            return new Page<>(result, pageable.pageNumber(), result.size(), total);
        } catch (Exception ex) {
            LOGGER.error("Failed to search for SharedPolicyGroups:", ex);
            throw new TechnicalException("Failed to search for SharedPolicyGroups", ex);
        }
    }

    @Override
    public Optional<SharedPolicyGroup> findByEnvironmentIdAndCrossId(String environmentId, String crossId) throws TechnicalException {
        LOGGER.debug("Find shared policy group by environment ID [{}] and cross ID [{}]", environmentId, crossId);

        try {
            final var result = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " WHERE environment_id = ? AND cross_id = ?",
                getOrm().getRowMapper(),
                environmentId,
                crossId
            );

            final var sharedPolicyGroup = result.isEmpty() ? null : result.get(0);

            LOGGER.debug("Find shared policy group by environment ID [{}] and cross ID [{}] - Done", environmentId, crossId);
            return Optional.ofNullable(sharedPolicyGroup);
        } catch (Exception ex) {
            LOGGER.error("Failed to find SharedPolicyGroup by environment ID and cross ID:", ex);
            throw new TechnicalException("Failed to find SharedPolicyGroup by environment ID and cross ID", ex);
        }
    }
}
