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
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.model.Dashboard;
import io.gravitee.repository.management.model.DashboardReferenceType;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcDashboardRepository extends JdbcAbstractCrudRepository<Dashboard, String> implements DashboardRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcDashboardRepository.class);

    JdbcDashboardRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "dashboards");
    }

    @Override
    protected JdbcObjectMapper<Dashboard> buildOrm() {
        return JdbcObjectMapper
            .builder(Dashboard.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("query_filter", Types.NVARCHAR, String.class)
            .addColumn("order", Types.INTEGER, int.class)
            .addColumn("enabled", Types.BOOLEAN, boolean.class)
            .addColumn("definition", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(final Dashboard item) {
        return item.getId();
    }

    @Override
    public List<Dashboard> findByReference(String referenceType, String referenceId) throws TechnicalException {
        LOGGER.debug("JdbcDashboardRepository.findByReference({},{})", referenceType, referenceId);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where reference_type = ? and reference_id = ? ",
                getOrm().getRowMapper(),
                referenceType,
                referenceId
            );
        } catch (final Exception ex) {
            final String error = "Failed to find dashboards by reference";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public List<Dashboard> findByReferenceAndType(String referenceType, String referenceId, String type) throws TechnicalException {
        LOGGER.debug("JdbcDashboardRepository.findByReferenceAndType({},{},{})", referenceType, referenceId, type);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                " where reference_type = ? and reference_id = ? and " +
                escapeReservedWord("type") +
                "= ? order by " +
                escapeReservedWord("order"),
                getOrm().getRowMapper(),
                referenceType,
                referenceId,
                type
            );
        } catch (final Exception ex) {
            final String error = "Failed to find dashboards by type";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public Optional<Dashboard> findByReferenceAndId(String referenceType, String referenceId, String id) throws TechnicalException {
        LOGGER.debug("JdbcDashboardRepository.findByReferenceAndId({},{},{})", referenceType, referenceId, id);
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectByIdSql() + " and reference_type = ? and reference_id = ?",
                    getOrm().getRowMapper(),
                    id,
                    referenceType,
                    referenceId
                )
                .stream()
                .findFirst();
        } catch (final Exception ex) {
            final String error = "Failed to find dashboards by id";
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, DashboardReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcDashboardRepository.deleteByReferenceIdAndReferenceType({},{})", referenceId, referenceType);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_type = ? and reference_id = ?",
                String.class,
                referenceType.name(),
                referenceId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_type = ? and reference_id = ?",
                    referenceType.name(),
                    referenceId
                );
            }

            LOGGER.debug("JdbcDashboardRepository.deleteByReferenceIdAndReferenceType({}, {}) - Done", referenceId, referenceType);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete dashboards for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete dashboards by reference", ex);
        }
    }
}
