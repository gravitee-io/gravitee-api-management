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
import static io.gravitee.repository.jdbc.orm.JdbcColumn.getDBName;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcColumn;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.CustomDashboardRepository;
import io.gravitee.repository.management.model.CustomDashboard;
import io.gravitee.repository.management.model.CustomDashboardWidget;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Repository
public class JdbcCustomDashboardRepository
    extends JdbcAbstractCrudRepository<CustomDashboard, String>
    implements CustomDashboardRepository {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final String INSERT_SQL;
    private final String UPDATE_SQL;

    JdbcCustomDashboardRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "custom_dashboards");
        INSERT_SQL = buildInsertSql();
        UPDATE_SQL = buildUpdateSql();
    }

    @Override
    protected JdbcObjectMapper<CustomDashboard> buildOrm() {
        return JdbcObjectMapper.builder(CustomDashboard.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("created_by", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("last_modified", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(CustomDashboard item) {
        return item.getId();
    }

    @Override
    protected RowMapper<CustomDashboard> getRowMapper() {
        return (rs, i) -> {
            var dashboard = new CustomDashboard();
            getOrm().setFromResultSet(dashboard, rs);
            dashboard.setLabels(deserialize(rs.getString("labels"), new TypeReference<>() {}));
            dashboard.setWidgets(deserialize(rs.getString("widgets"), new TypeReference<>() {}));
            return dashboard;
        };
    }

    @Override
    public Optional<CustomDashboard> findById(String id) throws TechnicalException {
        log.debug("JdbcCustomDashboardRepository.findById({})", id);
        try {
            return jdbcTemplate.query(getOrm().getSelectAllSql() + " where id = ?", getRowMapper(), id).stream().findFirst();
        } catch (Exception ex) {
            log.error("Failed to find custom dashboard by id:", ex);
            throw new TechnicalException("Failed to find custom dashboard by id", ex);
        }
    }

    @Override
    protected PreparedStatementCreator buildInsertPreparedStatementCreator(CustomDashboard item) {
        return new CustomDashboardPsc(INSERT_SQL, item, getOrm());
    }

    @Override
    protected PreparedStatementCreator buildUpdatePreparedStatementCreator(CustomDashboard item) {
        return new CustomDashboardPsc(UPDATE_SQL, item, getOrm(), item.getId());
    }

    @Override
    public List<CustomDashboard> findByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcCustomDashboardRepository.findByOrganizationId({})", organizationId);
        try {
            return jdbcTemplate.query(getOrm().getSelectAllSql() + " where organization_id = ?", getRowMapper(), organizationId);
        } catch (Exception ex) {
            log.error("Failed to find custom dashboards by organization id:", ex);
            throw new TechnicalException("Failed to find custom dashboards by organization id", ex);
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcCustomDashboardRepository.deleteByOrganizationId({})", organizationId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where organization_id = ?", organizationId);
        } catch (Exception ex) {
            log.error("Failed to delete custom dashboards by organization id:", ex);
            throw new TechnicalException("Failed to delete custom dashboards by organization id", ex);
        }
    }

    private String buildInsertSql() {
        var builder = new StringBuilder("insert into " + this.tableName + " (");
        var first = true;
        for (JdbcColumn column : getOrm().getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
        }
        builder.append(", labels, widgets) values (");
        first = true;
        for (int i = 0; i < getOrm().getColumns().size(); i++) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append("?");
        }
        builder.append(", ?, ?)");
        return builder.toString();
    }

    private String buildUpdateSql() {
        var builder = new StringBuilder("update " + this.tableName + " set ");
        var first = true;
        for (JdbcColumn column : getOrm().getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            first = false;
            builder.append(escapeReservedWord(getDBName(column.name)));
            builder.append(" = ?");
        }
        builder.append(", labels = ?, widgets = ? where id = ?");
        return builder.toString();
    }

    private <T> T deserialize(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize JSON", e);
        }
    }

    private String serialize(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private class CustomDashboardPsc implements PreparedStatementCreator {

        private final String sql;
        private final CustomDashboard dashboard;
        private final JdbcObjectMapper<CustomDashboard> orm;
        private final Object[] ids;

        CustomDashboardPsc(String sql, CustomDashboard dashboard, JdbcObjectMapper<CustomDashboard> orm, Object... ids) {
            this.sql = sql;
            this.dashboard = dashboard;
            this.orm = orm;
            this.ids = ids;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            log.debug("SQL: {}", sql);
            log.debug("custom_dashboard: {}", dashboard);
            var stmt = con.prepareStatement(sql);
            var idx = orm.setStatementValues(stmt, dashboard, 1);
            stmt.setString(idx++, serialize(dashboard.getLabels()));
            stmt.setString(idx++, serialize(dashboard.getWidgets()));
            for (Object id : ids) {
                stmt.setObject(idx++, id);
            }
            return stmt;
        }
    }
}
