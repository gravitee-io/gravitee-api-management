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
import static org.springframework.util.CollectionUtils.isEmpty;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.repository.management.api.search.ClusterCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Cluster;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Slf4j
@Repository
public class JdbcClusterRepository extends JdbcAbstractCrudRepository<Cluster, String> implements ClusterRepository {

    private final String CLUSTER_GROUPS;

    JdbcClusterRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "clusters");
        CLUSTER_GROUPS = getTableNameFor("cluster_groups");
    }

    @Override
    protected String getId(Cluster cluster) {
        return cluster.getId();
    }

    @Override
    protected JdbcObjectMapper<Cluster> buildOrm() {
        return JdbcObjectMapper.builder(Cluster.class, this.tableName, "id")
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

    private List<String> getGroups(String clusterId) {
        return jdbcTemplate.queryForList("select group_id from " + CLUSTER_GROUPS + " where cluster_id = ?", String.class, clusterId);
    }

    private void addGroups(Cluster cluster) {
        if (cluster == null) return;
        List<String> groups = getGroups(cluster.getId());
        if (!isEmpty(groups)) {
            cluster.setGroups(Set.copyOf(groups));
        } else {
            cluster.setGroups(null);
        }
    }

    @Override
    public Cluster create(Cluster item) throws TechnicalException {
        log.debug("JdbcClusterRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            storeGroups(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            log.error("Failed to create cluster:", ex);
            throw new TechnicalException("Failed to create cluster.", ex);
        }
    }

    @Override
    public Cluster update(final Cluster cluster) throws TechnicalException {
        log.debug("JdbcClusterRepository.update({})", cluster);
        if (cluster == null) {
            throw new IllegalStateException("Failed to update cluster: cluster is null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(cluster, cluster.getId()));
            storeGroups(cluster, true);
            return findById(cluster.getId()).orElseThrow(() ->
                new IllegalStateException("No cluster found with id [" + cluster.getId() + "]")
            );
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            log.error("Failed to update cluster:", ex);
            throw new TechnicalException("Failed to update cluster", ex);
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

        if (criteria.getQuery() != null) {
            andWhere.add("((lower(name) like ?) OR (lower(description) like ?))");
            andWhereParams.add("%" + criteria.getQuery().toLowerCase() + "%");
            andWhereParams.add("%" + criteria.getQuery().toLowerCase() + "%");
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

        // populate groups for each cluster
        for (Cluster c : result) {
            addGroups(c);
        }

        return new Page<>(result, pageable.pageNumber(), result.size(), total);
    }

    @Override
    public Optional<Cluster> findById(String id) throws io.gravitee.repository.exceptions.TechnicalException {
        Optional<Cluster> opt = super.findById(id);
        opt.ifPresent(this::addGroups);
        return opt;
    }

    @Override
    public void updateGroups(String clusterId, Set<String> groups) {
        // delete existing groups
        jdbcTemplate.update("delete from " + CLUSTER_GROUPS + " where cluster_id = ?", clusterId);
        // insert new groups
        List<String> filtered = getOrm().filterStrings(groups);
        if (!filtered.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + CLUSTER_GROUPS + " ( cluster_id, group_id ) values ( ?, ? )",
                getOrm().getBatchStringSetter(clusterId, filtered)
            );
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcClusterRepository.deleteByEnvironmentId({})", environmentId);
        try {
            List<String> clusterIds = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!clusterIds.isEmpty()) {
                deleteClusterGroups(clusterIds);

                jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
            }
        } catch (final Exception ex) {
            final String error = "Failed to delete clusters by environment id: " + environmentId;
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcClusterRepository.deleteByOrganizationId({})", organizationId);
        try {
            List<String> clusterIds = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where organization_id = ?",
                String.class,
                organizationId
            );

            if (!clusterIds.isEmpty()) {
                deleteClusterGroups(clusterIds);

                jdbcTemplate.update("delete from " + this.tableName + " where organization_id = ?", organizationId);
            }
        } catch (final Exception ex) {
            final String error = "Failed to delete clusters by organization id: " + organizationId;
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    private void deleteClusterGroups(List<String> clusterIds) {
        String inClause = getOrm().buildInClause(clusterIds);
        jdbcTemplate.update("delete from " + CLUSTER_GROUPS + " where cluster_id in (" + inClause + ")", clusterIds.toArray());
    }

    private void storeGroups(Cluster cluster, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update("delete from " + CLUSTER_GROUPS + " where cluster_id = ?", cluster.getId());
        }
        List<String> filteredGroups = getOrm().filterStrings(cluster.getGroups());
        if (!filteredGroups.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + CLUSTER_GROUPS + " ( cluster_id, group_id ) values ( ?, ? )",
                getOrm().getBatchStringSetter(cluster.getId(), filteredGroups)
            );
        }
    }
}
