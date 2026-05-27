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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.KafkaPortRangeRepository;
import io.gravitee.repository.management.model.KafkaPortRange;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@CustomLog
@Repository
public class JdbcKafkaPortRangeRepository extends JdbcAbstractCrudRepository<KafkaPortRange, String> implements KafkaPortRangeRepository {

    JdbcKafkaPortRangeRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "kafka_port_ranges");
    }

    @Override
    protected String getId(KafkaPortRange item) {
        return item.getPlanId();
    }

    @Override
    protected JdbcObjectMapper<KafkaPortRange> buildOrm() {
        return JdbcObjectMapper.builder(KafkaPortRange.class, this.tableName, "plan_id")
            .addColumn("plan_id", Types.NVARCHAR, String.class)
            .addColumn("api_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("bootstrap_port", Types.INTEGER, int.class)
            .addColumn("range_start", Types.INTEGER, int.class)
            .addColumn("range_end", Types.INTEGER, int.class)
            .addColumn("created_at", Types.TIMESTAMP, Instant.class)
            .addColumn("updated_at", Types.TIMESTAMP, Instant.class)
            .build();
    }

    @Override
    public List<KafkaPortRange> findConflicting(String environmentId, int bootstrapPort, int rangeStart, int rangeEnd, String excludePlanId)
        throws TechnicalException {
        return runConflictQuery(environmentId, bootstrapPort, rangeStart, rangeEnd, excludePlanId, false);
    }

    @Override
    public List<KafkaPortRange> findConflictingForUpdate(
        String environmentId,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) throws TechnicalException {
        return runConflictQuery(environmentId, bootstrapPort, rangeStart, rangeEnd, excludePlanId, true);
    }

    private List<KafkaPortRange> runConflictQuery(
        String environmentId,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId,
        boolean forUpdate
    ) throws TechnicalException {
        try {
            // Four conflict conditions in a single indexed query (see KafkaPortRangeRepository javadoc).
            final StringBuilder sql = new StringBuilder(getOrm().getSelectAllSql())
                .append(" where environment_id = ?")
                .append(excludePlanId == null ? "" : " and plan_id <> ?")
                .append(" and (")
                .append("  (range_start <= ? and range_end >= ?)") // 1. broker-range overlap
                .append("  or (? between range_start and range_end)") // 2. new bootstrap inside existing range
                .append("  or (bootstrap_port between ? and ?)") // 3. existing bootstrap inside new range
                .append("  or (bootstrap_port = ?)") // 4. bootstrap port collision
                .append(")");

            // SQL Server falls back to the non-locking query — see KafkaPortRangeRepository#findConflictingForUpdate javadoc.
            if (forUpdate && !AbstractJdbcRepositoryConfiguration.isSqlServer()) {
                // Row-level lock held until transaction commit — prevents TOCTOU between concurrent
                // plan saves: the second transaction blocks on the first's locks, then re-reads the
                // freshly-inserted row in its own conflict check and fails cleanly.
                sql.append(" for update");
            }

            final var params = new java.util.ArrayList<Object>();
            params.add(environmentId);
            if (excludePlanId != null) {
                params.add(excludePlanId);
            }
            params.add(rangeEnd); // range_start <= rangeEnd
            params.add(rangeStart); // range_end >= rangeStart
            params.add(bootstrapPort); // new bootstrap inside existing range
            params.add(rangeStart); // existing bootstrap >= rangeStart
            params.add(rangeEnd); // existing bootstrap <= rangeEnd
            params.add(bootstrapPort); // bootstrap collision

            return jdbcTemplate.query(sql.toString(), getOrm().getRowMapper(), params.toArray());
        } catch (Exception ex) {
            log.error("Failed to find conflicting kafka port ranges", ex);
            throw new TechnicalException("Failed to find conflicting kafka port ranges", ex);
        }
    }

    @Override
    public void deleteByApiId(String apiId) throws TechnicalException {
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where api_id = ?", apiId);
        } catch (Exception ex) {
            log.error("Failed to delete kafka port ranges for api {}", apiId, ex);
            throw new TechnicalException("Failed to delete kafka port ranges for api " + apiId, ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (Exception ex) {
            log.error("Failed to delete kafka port ranges for environment {}", environmentId, ex);
            throw new TechnicalException("Failed to delete kafka port ranges for environment " + environmentId, ex);
        }
    }
}
