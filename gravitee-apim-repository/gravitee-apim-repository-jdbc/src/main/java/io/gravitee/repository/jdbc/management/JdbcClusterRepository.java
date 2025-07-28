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
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ClusterRepository;
import io.gravitee.repository.management.api.search.ClusterCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Cluster;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcClusterRepository extends JdbcAbstractCrudRepository<Cluster, String> implements ClusterRepository {

    JdbcClusterRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "clusters");
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

    @Override
    public Page<Cluster> search(ClusterCriteria criteria, Pageable pageable, Sortable sortable) {
        Long total = jdbcTemplate.queryForObject("select count(*) from " + this.tableName, Long.class);

        if (total == null || total == 0) {
            return new Page<>(List.of(), pageable.pageNumber(), 0, 0);
        }

        sortable = sortable == null ? new SortableBuilder().field("name").setAsc(true).build() : sortable;
        final var sortOrder = sortable.order() == Order.ASC ? "ASC" : "DESC";
        final var sortField = toSnakeCase(sortable.field());

        var result = jdbcTemplate.query(
            getOrm().getSelectAllSql() +
            " ORDER BY " +
            sortField +
            " " +
            sortOrder +
            " " +
            createPagingClause(pageable.pageSize(), pageable.from()),
            getOrm().getRowMapper()
        );

        return new Page<>(result, pageable.pageNumber(), result.size(), total);
    }
}
