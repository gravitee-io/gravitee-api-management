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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.WorkflowRepository;
import io.gravitee.repository.management.model.Workflow;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcWorkflowRepository extends JdbcAbstractCrudRepository<Workflow, String> implements WorkflowRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(JdbcWorkflowRepository.class);

    JdbcWorkflowRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "workflows");
    }

    @Override
    protected JdbcObjectMapper<Workflow> buildOrm() {
        return JdbcObjectMapper
            .builder(Workflow.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("state", Types.NVARCHAR, String.class)
            .addColumn("comment", Types.NVARCHAR, String.class)
            .addColumn("user", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(final Workflow workflow) {
        return workflow.getId();
    }

    @Override
    public List<Workflow> findByReferenceAndType(final String referenceType, final String referenceId, final String type)
        throws TechnicalException {
        LOGGER.debug("JdbcWorkflowRepository.findByReferenceAndType({}, {}, {})", referenceType, referenceId, type);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " where reference_type = ? and reference_id = ? and " +
                escapeReservedWord("type") +
                " = ? order by created_at desc",
                getOrm().getRowMapper(),
                referenceType,
                referenceId,
                type
            );
        } catch (final Exception ex) {
            final String message = "Failed to find workflows by reference";
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, String referenceType) throws TechnicalException {
        LOGGER.debug("JdbcWorkflowRepository.deleteByReferenceIdAndReferenceType({}, {})", referenceId, referenceType);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_type = ? and reference_id = ?",
                String.class,
                referenceType,
                referenceId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_type = ? and reference_id = ?",
                    referenceType,
                    referenceId
                );
            }

            LOGGER.debug("JdbcWorkflowRepository.deleteByReferenceIdAndReferenceType({}, {}) - Done", referenceId, referenceType);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete workflow for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete workflow by reference", ex);
        }
    }
}
