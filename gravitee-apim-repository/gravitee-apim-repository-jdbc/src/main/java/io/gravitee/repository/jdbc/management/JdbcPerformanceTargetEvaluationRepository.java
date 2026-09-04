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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PerformanceTargetEvaluationRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.repository.management.model.PerformanceTargetEvaluation;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@CustomLog
@Repository
public class JdbcPerformanceTargetEvaluationRepository
    extends JdbcAbstractRepository<PerformanceTargetEvaluation>
    implements PerformanceTargetEvaluationRepository {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final TypeReference<List<PerformanceTargetEvaluation.RuleResult>> RULES_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> API_IDS_TYPE = new TypeReference<>() {};

    JdbcPerformanceTargetEvaluationRepository(@Value("${management.jdbc.prefix:}") String prefix) {
        super(prefix, "performance_target_evaluations");
    }

    /**
     * The two JSON columns are declared last on purpose: their element types cannot be expressed as {@code Class}
     * literals, so {@code List.class} makes those calls unchecked invocations, which would erase the generics of every
     * builder call chained after them.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected JdbcObjectMapper<PerformanceTargetEvaluation> buildOrm() {
        return JdbcObjectMapper.builder(PerformanceTargetEvaluation.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("target_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("reference", Types.NVARCHAR, String.class)
            .addColumn("status", Types.NVARCHAR, PerformanceTargetEvaluation.Status.class)
            .addColumn("window_from", Types.TIMESTAMP, Date.class)
            .addColumn("window_to", Types.TIMESTAMP, Date.class)
            .addColumn("evaluated_at", Types.TIMESTAMP, Date.class)
            .addColumn("latest", Types.BOOLEAN, boolean.class)
            .addColumn(
                "covered_api_ids",
                Types.NCLOB,
                List.class,
                JdbcPerformanceTargetEvaluationRepository::serialize,
                JdbcPerformanceTargetEvaluationRepository::deserializeApiIds
            )
            .addColumn(
                "rules",
                Types.NCLOB,
                List.class,
                JdbcPerformanceTargetEvaluationRepository::serialize,
                JdbcPerformanceTargetEvaluationRepository::deserializeRules
            )
            .build();
    }

    @Override
    public PerformanceTargetEvaluation create(PerformanceTargetEvaluation evaluation) throws TechnicalException {
        log.debug("JdbcPerformanceTargetEvaluationRepository.create({})", evaluation.getId());
        try {
            if (evaluation.isLatest()) {
                jdbcTemplate.update(
                    "update " + this.tableName + " set latest = ? where target_id = ? and latest = ?",
                    false,
                    evaluation.getTargetId(),
                    true
                );
            }
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(evaluation));
            return jdbcTemplate.query(getOrm().getSelectByIdSql(), getRowMapper(), evaluation.getId()).stream().findFirst().orElse(null);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create performance target evaluation " + evaluation.getId(), ex);
        }
    }

    @Override
    public List<PerformanceTargetEvaluation> findLatestByReference(String environmentId, String reference) throws TechnicalException {
        log.debug("JdbcPerformanceTargetEvaluationRepository.findLatestByReference({}, {})", environmentId, reference);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where environment_id = ? and reference = ? and latest = ? order by id",
                getRowMapper(),
                environmentId,
                reference,
                true
            );
        } catch (Exception ex) {
            throw new TechnicalException(
                "Failed to find latest performance target evaluations by reference " + environmentId + "/" + reference,
                ex
            );
        }
    }

    @Override
    public List<PerformanceTargetEvaluation> findLatestByReferences(String environmentId, Collection<String> references)
        throws TechnicalException {
        log.debug("JdbcPerformanceTargetEvaluationRepository.findLatestByReferences({}, {})", environmentId, references);
        if (references.isEmpty()) {
            return List.of();
        }
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                    " where environment_id = ? and latest = ? and reference in (" +
                    getOrm().buildInClause(references) +
                    ") order by id",
                ps -> {
                    ps.setString(1, environmentId);
                    ps.setBoolean(2, true);
                    getOrm().setArguments(ps, references, 3);
                },
                getRowMapper()
            );
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find latest performance target evaluations by references in " + environmentId, ex);
        }
    }

    @Override
    public Page<PerformanceTargetEvaluation> findEnvironmentLatest(String environmentId, Pageable pageable) throws TechnicalException {
        log.debug("JdbcPerformanceTargetEvaluationRepository.findEnvironmentLatest({})", environmentId);
        try {
            return pageMostRecentFirst(" where environment_id = ? and latest = ?", pageable, environmentId, true);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find latest performance target evaluations of environment " + environmentId, ex);
        }
    }

    @Override
    public Page<PerformanceTargetEvaluation> findByTargetId(String targetId, Pageable pageable) throws TechnicalException {
        log.debug("JdbcPerformanceTargetEvaluationRepository.findByTargetId({})", targetId);
        try {
            return pageMostRecentFirst(" where target_id = ?", pageable, targetId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find performance target evaluations of target " + targetId, ex);
        }
    }

    private Page<PerformanceTargetEvaluation> pageMostRecentFirst(String where, Pageable pageable, Object... args) {
        Long total = jdbcTemplate.queryForObject("select count(*) from " + this.tableName + where, Long.class, args);
        if (total == null || total == 0) {
            return new Page<>(List.of(), pageable.pageNumber(), 0, 0);
        }
        var content = jdbcTemplate.query(
            getOrm().getSelectAllSql() +
                where +
                " order by evaluated_at desc, id " +
                createPagingClause(pageable.pageSize(), pageable.from()),
            getRowMapper(),
            args
        );
        return new Page<>(content, pageable.pageNumber(), content.size(), total);
    }

    @Override
    public PerformanceTargetEnvironmentSummary getEnvironmentSummary(String environmentId) throws TechnicalException {
        log.debug("JdbcPerformanceTargetEvaluationRepository.getEnvironmentSummary({})", environmentId);
        try {
            var summary = PerformanceTargetEnvironmentSummary.builder().environmentId(environmentId).build();
            jdbcTemplate.query(
                "select status, count(*) as total from " + this.tableName + " where environment_id = ? and latest = ? group by status",
                rs -> {
                    long count = rs.getLong("total");
                    switch (PerformanceTargetEvaluation.Status.valueOf(rs.getString("status"))) {
                        case PASS -> summary.setPass(count);
                        case BREACH -> summary.setBreach(count);
                        case NOT_EVALUABLE -> summary.setNotEvaluable(count);
                    }
                },
                environmentId,
                true
            );
            return summary;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to summarize performance target evaluations of environment " + environmentId, ex);
        }
    }

    @Override
    public List<String> deleteByReference(String environmentId, String reference) throws TechnicalException {
        log.debug("JdbcPerformanceTargetEvaluationRepository.deleteByReference({}, {})", environmentId, reference);
        try {
            var ids = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where environment_id = ? and reference = ?",
                String.class,
                environmentId,
                reference
            );
            if (!ids.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + this.tableName + " where environment_id = ? and reference = ?",
                    environmentId,
                    reference
                );
            }
            return ids;
        } catch (Exception ex) {
            throw new TechnicalException(
                "Failed to delete performance target evaluations by reference " + environmentId + "/" + reference,
                ex
            );
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcPerformanceTargetEvaluationRepository.deleteByEnvironmentId({})", environmentId);
        try {
            var ids = jdbcTemplate.queryForList(
                "select id from " + this.tableName + " where environment_id = ?",
                String.class,
                environmentId
            );
            if (!ids.isEmpty()) {
                jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
            }
            return ids;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete performance target evaluations of environment " + environmentId, ex);
        }
    }

    @Override
    public void deleteByTargetId(String targetId) throws TechnicalException {
        log.debug("JdbcPerformanceTargetEvaluationRepository.deleteByTargetId({})", targetId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where target_id = ?", targetId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete performance target evaluations of target " + targetId, ex);
        }
    }

    private static String serialize(List<?> values) {
        try {
            return JSON_MAPPER.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize performance target evaluation JSON column", e);
        }
    }

    private static List<PerformanceTargetEvaluation.RuleResult> deserializeRules(String json) {
        return deserialize(json, RULES_TYPE);
    }

    private static List<String> deserializeApiIds(String json) {
        return deserialize(json, API_IDS_TYPE);
    }

    private static <T> List<T> deserialize(String json, TypeReference<List<T>> type) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return JSON_MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize performance target evaluation JSON column", e);
        }
    }
}
