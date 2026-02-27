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
import io.gravitee.repository.management.api.PortalPageRepository;
import io.gravitee.repository.management.model.ExpandsViewContext;
import io.gravitee.repository.management.model.PortalPage;
import java.sql.Types;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@Repository
public class JdbcPortalPageRepository extends JdbcAbstractCrudRepository<PortalPage, String> implements PortalPageRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPortalPageRepository.class);

    JdbcPortalPageRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "portal_pages");
    }

    @Override
    protected JdbcObjectMapper<PortalPage> buildOrm() {
        return JdbcObjectMapper.builder(PortalPage.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("content", Types.LONGNVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(PortalPage item) {
        return item.getId();
    }

    @Override
    protected PreparedStatementCreator buildUpdatePreparedStatementCreator(PortalPage item) {
        LOGGER.debug("Building update statement for PortalPage: {}", item);
        return getOrm().buildUpdatePreparedStatementCreator(item, getId(item));
    }

    @Override
    public List<PortalPage> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        try {
            var sql = new StringBuilder("SELECT id, environment_id, name, content, created_at, updated_at FROM " + this.tableName);
            getOrm().buildInCondition(true, sql, "id", ids);
            return jdbcTemplate.query(sql.toString(), getOrm().getRowMapper(), ids.toArray());
        } catch (Exception ex) {
            LOGGER.error("Failed to find PortalPages by ids {}", ids, ex);
            return List.of();
        }
    }

    @Override
    public List<PortalPage> findByIdsWithExpand(List<String> ids, List<ExpandsViewContext> expands) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        try {
            Set<String> selected = new LinkedHashSet<>();
            selected.add("id");
            if (expands != null) {
                expands.forEach(e -> selected.add(e.getValue()));
            }

            StringBuilder sql = new StringBuilder("SELECT ").append(String.join(", ", selected)).append(" FROM ").append(this.tableName);
            getOrm().buildInCondition(true, sql, "id", ids);
            return jdbcTemplate.query(sql.toString(), getOrm().getRowMapper(), ids.toArray());
        } catch (Exception ex) {
            LOGGER.error("Failed to find PortalPages by ids {} with expands {}", ids, expands, ex);
            return List.of();
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("Deleting PortalPage by environmentId {}", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (final Exception ex) {
            final String error = "Failed to delete PortalPage by environmentId:" + environmentId;
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }
}
