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
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.QualityRuleRepository;
import io.gravitee.repository.management.model.QualityRule;
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
public class JdbcQualityRuleRepository extends JdbcAbstractCrudRepository<QualityRule, String> implements QualityRuleRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcQualityRuleRepository.class);

    JdbcQualityRuleRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "quality_rules");
    }

    @Override
    protected JdbcObjectMapper<QualityRule> buildOrm() {
        return JdbcObjectMapper.builder(QualityRule.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, QualityRule.ReferenceType.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("weight", Types.INTEGER, int.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(QualityRule item) {
        return item.getId();
    }

    @Override
    public List<QualityRule> findByReference(QualityRule.ReferenceType referenceType, String referenceId) throws TechnicalException {
        LOGGER.debug("JdbcQualityRuleRepository.findByReference({}, {})", referenceType, referenceId);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where reference_type = ? and reference_id = ?",
                getOrm().getRowMapper(),
                referenceType.name(),
                referenceId
            );
        } catch (final Exception ex) {
            final String error =
                "An error occurred when finding all quality rules with findByReference " + referenceType + " [" + referenceId + "]";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(final String referenceId, final QualityRule.ReferenceType referenceType)
        throws TechnicalException {
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_id = ? and reference_type = ?",
                String.class,
                referenceId,
                referenceType.name()
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
            }
            return rows;
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to delete quality rules by reference", ex);
        }
    }
}
