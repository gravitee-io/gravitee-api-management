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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PerformanceTargetRepository;
import io.gravitee.repository.management.model.PerformanceTarget;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@CustomLog
@Repository
public class JdbcPerformanceTargetRepository
    extends JdbcAbstractCrudRepository<PerformanceTarget, String>
    implements PerformanceTargetRepository {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final TypeReference<List<PerformanceTarget.Rule>> RULES_TYPE = new TypeReference<>() {};

    private final String PERFORMANCE_TARGET_APIS;

    JdbcPerformanceTargetRepository(@Value("${management.jdbc.prefix:}") String prefix) {
        super(prefix, "performance_targets");
        PERFORMANCE_TARGET_APIS = getTableNameFor("performance_target_apis");
    }

    @Override
    protected String getId(PerformanceTarget item) {
        return item.getId();
    }

    /**
     * {@code rules} is declared last on purpose: its element type cannot be expressed as a {@code Class} literal, so
     * {@code List.class} makes that call an unchecked invocation, which would erase the generics of every builder call
     * chained after it.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected JdbcObjectMapper<PerformanceTarget> buildOrm() {
        return JdbcObjectMapper.builder(PerformanceTarget.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("reference", Types.NVARCHAR, String.class)
            .addColumn("window_seconds", Types.BIGINT, long.class)
            .addColumn("interval_seconds", Types.BIGINT, long.class)
            .addColumn("min_sample_size", Types.INTEGER, int.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn(
                "rules",
                Types.NCLOB,
                List.class,
                JdbcPerformanceTargetRepository::serializeRules,
                JdbcPerformanceTargetRepository::deserializeRules
            )
            .build();
    }

    @Override
    public PerformanceTarget create(PerformanceTarget target) throws TechnicalException {
        log.debug("JdbcPerformanceTargetRepository.create({})", target.getId());
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(target));
            storeApiIds(target.getId(), target.getApiIds());
            return findById(target.getId()).orElse(null);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create performance target " + target.getId(), ex);
        }
    }

    @Override
    public Optional<PerformanceTarget> findById(String id) throws TechnicalException {
        var found = super.findById(id);
        found.ifPresent(this::loadApiIds);
        return found;
    }

    @Override
    public PerformanceTarget update(PerformanceTarget target) throws TechnicalException {
        log.debug("JdbcPerformanceTargetRepository.update({})", target.getId());
        try {
            int rows = jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(target, target.getId()));
            if (rows == 0) {
                throw new IllegalStateException("Unable to update performance target " + target.getId());
            }
            deleteApiIds(target.getId());
            storeApiIds(target.getId(), target.getApiIds());
            return findById(target.getId()).orElse(null);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to update performance target " + target.getId(), ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("JdbcPerformanceTargetRepository.delete({})", id);
        try {
            deleteApiIds(id);
            jdbcTemplate.update(getOrm().getDeleteSql(), id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete performance target " + id, ex);
        }
    }

    @Override
    public List<PerformanceTarget> findByReference(String environmentId, String reference) throws TechnicalException {
        log.debug("JdbcPerformanceTargetRepository.findByReference({}, {})", environmentId, reference);
        try {
            var targets = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where environment_id = ? and reference = ? order by id",
                getRowMapper(),
                environmentId,
                reference
            );
            targets.forEach(this::loadApiIds);
            return targets;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find performance targets by reference " + environmentId + "/" + reference, ex);
        }
    }

    @Override
    public List<String> deleteByReference(String environmentId, String reference) throws TechnicalException {
        log.debug("JdbcPerformanceTargetRepository.deleteByReference({}, {})", environmentId, reference);
        try {
            var ids = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where environment_id = ? and reference = ?",
                String.class,
                environmentId,
                reference
            );
            if (!ids.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " +
                        PERFORMANCE_TARGET_APIS +
                        " where target_id in (select id from " +
                        this.tableName +
                        " where environment_id = ? and reference = ?)",
                    environmentId,
                    reference
                );
                jdbcTemplate.update(
                    "delete from " + this.tableName + " where environment_id = ? and reference = ?",
                    environmentId,
                    reference
                );
            }
            return ids;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete performance targets by reference " + environmentId + "/" + reference, ex);
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcPerformanceTargetRepository.deleteByEnvironmentId({})", environmentId);
        try {
            var ids = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where environment_id = ?",
                String.class,
                environmentId
            );
            if (!ids.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " +
                        PERFORMANCE_TARGET_APIS +
                        " where target_id in (select id from " +
                        this.tableName +
                        " where environment_id = ?)",
                    environmentId
                );
                jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
            }
            return ids;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete performance targets of environment " + environmentId, ex);
        }
    }

    @Override
    public List<String> removeApiId(String apiId) throws TechnicalException {
        log.debug("JdbcPerformanceTargetRepository.removeApiId({})", apiId);
        try {
            var targetIds = jdbcTemplate.queryForList(
                "select distinct target_id from " + PERFORMANCE_TARGET_APIS + " where api_id = ?",
                String.class,
                apiId
            );
            if (!targetIds.isEmpty()) {
                jdbcTemplate.update("delete from " + PERFORMANCE_TARGET_APIS + " where api_id = ?", apiId);
            }
            return targetIds;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to remove api " + apiId + " from performance targets", ex);
        }
    }

    private void loadApiIds(PerformanceTarget target) {
        target.setApiIds(
            jdbcTemplate.queryForList(
                "select api_id from " + PERFORMANCE_TARGET_APIS + " where target_id = ?",
                String.class,
                target.getId()
            )
        );
    }

    private void deleteApiIds(String targetId) {
        jdbcTemplate.update("delete from " + PERFORMANCE_TARGET_APIS + " where target_id = ?", targetId);
    }

    private void storeApiIds(String targetId, List<String> apiIds) {
        if (apiIds == null || apiIds.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
            "insert into " + PERFORMANCE_TARGET_APIS + " (target_id, api_id) values (?, ?)",
            apiIds
                .stream()
                .map(apiId -> new Object[] { targetId, apiId })
                .toList()
        );
    }

    private static String serializeRules(List<?> rules) {
        try {
            return JSON_MAPPER.writeValueAsString(rules == null ? List.of() : rules);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize performance target rules", e);
        }
    }

    private static List<PerformanceTarget.Rule> deserializeRules(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return JSON_MAPPER.readValue(json, RULES_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize performance target rules", e);
        }
    }
}
