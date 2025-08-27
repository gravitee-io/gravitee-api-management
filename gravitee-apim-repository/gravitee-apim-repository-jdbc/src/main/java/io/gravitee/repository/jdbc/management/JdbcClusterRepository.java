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
import static java.lang.String.format;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.repository.management.api.search.ClusterCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Cluster;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Slf4j
@Repository
public class JdbcClusterRepository extends JdbcAbstractCrudRepository<Cluster, String> implements ClusterRepository {

    private final String API_GROUPS;

    JdbcClusterRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "clusters");
        API_GROUPS = getTableNameFor("cluster_groups");
    }

    @Override
    protected String getId(Cluster cluster) {
        return cluster.getId();
    }

    @Override
    protected JdbcObjectMapper<Cluster> buildOrm() {
        return JdbcObjectMapper
            .builder(Cluster.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Instant.class)
            .addColumn("updated_at", Types.TIMESTAMP, Instant.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .build();
    }

    private class Rm implements RowMapper<Cluster> {

        @Override
        public Cluster mapRow(ResultSet rs, int i) throws SQLException {
            Cluster cluster = new Cluster();
            getOrm().setFromResultSet(cluster, rs);
            addGroups(cluster);
            return cluster;
        }
    }

    private final Rm mapper = new Rm();

    @Override
    public Optional<Cluster> findById(String id) throws TechnicalException {
        log.debug("JdbcApplicationRepository.findById({})", id);
        try {
            return jdbcTemplate.query(
                    "select * from " + this.tableName + " where id = ?",
                    mapper,
                    id
            ).stream().findFirst();
        } catch (final Exception ex) {
            log.error("Failed to find application by id:", ex);
            throw new TechnicalException("Failed to find application by id", ex);
        }
    }


    @Override
    public Cluster create(Cluster item) throws TechnicalException {
        log.debug("JdbcApiRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            storeGroups(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            log.error("Failed to create api:", ex);
            throw new TechnicalException("Failed to create api", ex);
        }
    }



    @Override
    public Cluster update(final Cluster application) throws TechnicalException {
        log.debug("JdbcApplicationRepository.update({})", application);
        if (application == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(application, application.getId()));
            storeGroups(application, true);
            return findById(application.getId())
                    .orElseThrow(() -> new IllegalStateException(format("No application found with id [%s]", application.getId())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            log.error("Failed to update application", ex);
            throw new TechnicalException("Failed to update application", ex);
        }
    }

    private void storeGroups(Cluster api, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + API_GROUPS + " where cluster_id = ?", api.getId());
        }
        List<String> filteredGroups = getOrm().filterStrings(api.getGroups());
        if (!filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "insert into " + API_GROUPS + " ( cluster_id, group_id ) values ( ?, ? )",
                    getOrm().getBatchStringSetter(api.getId(), filteredGroups)
            );
        }
    }

    @Override
    public Page<Cluster> search(ClusterCriteria criteria, Pageable pageable, Optional<Sortable> sortableOpt) {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        Objects.requireNonNull(criteria, "ClusterCriteria must not be null");
        Objects.requireNonNull(criteria.getEnvironmentId(), "ClusterCriteria.getEnvironmentId() must not be null");
        log.debug("JdbcClusterRepository.search({}, {})", criteria, pageable);

        StringJoiner andWhere = new StringJoiner(" AND ");
        List<Object> andWhereParams = new ArrayList<>();

        andWhere.add("environment_id = ?");
        andWhereParams.add(criteria.getEnvironmentId());

        if (!CollectionUtils.isEmpty(criteria.getIds())) {
            andWhere.add("id in (" + getOrm().buildInClause(criteria.getIds()) + ")");
            andWhereParams.addAll(criteria.getIds());
        }

        Long total = jdbcTemplate.queryForObject(
            "select count(*) from " + this.tableName + " WHERE " + andWhere,
            Long.class,
            andWhereParams.toArray()
        );

        if (total == null || total == 0) {
            return new Page<>(List.of(), pageable.pageNumber(), 0, 0);
        }

        Sortable sortable = sortableOpt.orElse(new SortableBuilder().field("name").setAsc(true).build());
        final String sortOrder = sortable.order().name();
        final String sortField = toSnakeCase(sortable.field());

        List<Cluster> result = jdbcTemplate.query(
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

        result.forEach(this::addGroups);

        return new Page<>(result, pageable.pageNumber(), result.size(), total);
    }

    private void addGroups(Cluster cluster) {
        List<String> groups = getGroups(cluster.getId());
        cluster.setGroups(new HashSet<>(groups));
    }

    private List<String> getGroups(String clusterId) {
        return jdbcTemplate.queryForList("select group_id from " + API_GROUPS + " where cluster_id = ?", String.class, clusterId);
    }
}
