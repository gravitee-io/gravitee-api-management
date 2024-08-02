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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.AsyncJobRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.AsyncJob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAsyncJobRepository extends JdbcAbstractCrudRepository<AsyncJob, String> implements AsyncJobRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAsyncJobRepository.class);

    JdbcAsyncJobRepository(@Value("${management.jdbc.prefix:}") String prefix) {
        super(prefix, "asyncjobs");
    }

    @Override
    public Optional<AsyncJob> findPendingJobFor(String sourceId) throws TechnicalException {
        LOGGER.debug("JdbcAsyncJobRepository.findPendingJobFor({})", sourceId);
        final List<AsyncJob> jobs;
        try {
            jobs =
                jdbcTemplate.query(
                    getOrm().getSelectAllSql() + " where source_id = ? and status = 'PENDING'",
                    getOrm().getRowMapper(),
                    sourceId
                );
            return jobs.stream().findFirst();
        } catch (final Exception ex) {
            final String message = "Failed to find pending AsyncJob for: " + sourceId;
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    @Override
    public Page<AsyncJob> search(SearchCriteria criteria, Pageable pageable) throws TechnicalException {
        LOGGER.debug("JdbcAsyncJobRepository.search({})", criteria);
        final List<AsyncJob> jobs;
        try {
            jobs =
                jdbcTemplate.query(
                    getOrm().getSelectAllSql() + " where " + convert(criteria) + " order by updated_at desc",
                    ps -> fillPreparedStatement(criteria, ps),
                    getOrm().getRowMapper()
                );
            return getResultAsPage(pageable, jobs);
        } catch (final Exception ex) {
            final String message = "Failed to search AsyncJob with: " + criteria;
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcAsyncJobRepository.deleteByEnvironmentId({})", environmentId);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update("delete from " + tableName + " where environment_id = ?", environmentId);
            }

            LOGGER.debug("JdbcAsyncJobRepository.deleteByEnvironmentId({}) - Done", environmentId);
            return rows;
        } catch (final Exception ex) {
            final String message = "Failed to find integrations jobs of environment: " + environmentId;
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    @Override
    protected String getId(AsyncJob item) {
        return item.getId();
    }

    @Override
    protected JdbcObjectMapper<AsyncJob> buildOrm() {
        return JdbcObjectMapper
            .builder(AsyncJob.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("source_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("initiator_Id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("status", Types.NVARCHAR, String.class)
            .addColumn("error_message", Types.NVARCHAR, String.class)
            .addColumn("upper_limit", Types.INTEGER, Long.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    private String convert(SearchCriteria criteria) {
        List<String> clauses = new ArrayList<>();

        clauses.add("environment_id = ?");
        criteria.initiatorId().ifPresent(initiatorId -> clauses.add("initiator_id = ?"));
        criteria.type().ifPresent(type -> clauses.add("type = ?"));
        criteria.status().ifPresent(status -> clauses.add("status = ?"));
        criteria.sourceId().ifPresent(sourceId -> clauses.add("source_id = ?"));

        if (!clauses.isEmpty()) {
            return String.join(" AND ", clauses);
        }
        return null;
    }

    private void fillPreparedStatement(SearchCriteria criteria, PreparedStatement ps) throws SQLException {
        var index = 1;

        ps.setString(index++, criteria.environmentId());

        if (criteria.initiatorId().isPresent()) {
            ps.setString(index++, criteria.initiatorId().get());
        }
        if (criteria.type().isPresent()) {
            ps.setString(index++, criteria.type().get());
        }
        if (criteria.status().isPresent()) {
            ps.setString(index++, criteria.status().get());
        }
        if (criteria.sourceId().isPresent()) {
            ps.setString(index++, criteria.sourceId().get());
        }
    }
}
