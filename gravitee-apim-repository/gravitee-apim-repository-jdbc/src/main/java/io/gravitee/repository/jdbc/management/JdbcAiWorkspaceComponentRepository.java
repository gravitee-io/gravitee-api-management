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
import io.gravitee.repository.management.api.AiWorkspaceComponentRepository;
import io.gravitee.repository.management.model.AiWorkspaceComponent;
import io.gravitee.repository.management.model.AiWorkspaceComponentType;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Repository
public class JdbcAiWorkspaceComponentRepository
    extends JdbcAbstractCrudRepository<AiWorkspaceComponent, String>
    implements AiWorkspaceComponentRepository {

    JdbcAiWorkspaceComponentRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "ai_workspace_components");
    }

    @Override
    protected JdbcObjectMapper<AiWorkspaceComponent> buildOrm() {
        return JdbcObjectMapper.builder(AiWorkspaceComponent.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("api_product_id", Types.NVARCHAR, String.class)
            .addColumn("component_type", Types.NVARCHAR, AiWorkspaceComponentType.class)
            .addColumn("ref_id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(final AiWorkspaceComponent item) {
        return item.getId();
    }

    @Override
    public List<AiWorkspaceComponent> findByApiProductId(String apiProductId) throws TechnicalException {
        log.debug("JdbcAiWorkspaceComponentRepository.findByApiProductId({})", apiProductId);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " WHERE api_product_id = ? ORDER BY id",
                getOrm().getRowMapper(),
                apiProductId
            );
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find ai workspace components by api product id", ex);
        }
    }

    @Override
    public List<AiWorkspaceComponent> findByApiProductIdAndComponentType(String apiProductId, AiWorkspaceComponentType componentType)
        throws TechnicalException {
        log.debug("JdbcAiWorkspaceComponentRepository.findByApiProductIdAndComponentType({}, {})", apiProductId, componentType);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " WHERE api_product_id = ? AND component_type = ? ORDER BY id",
                getOrm().getRowMapper(),
                apiProductId,
                componentType.name()
            );
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find ai workspace components by api product id and type", ex);
        }
    }

    @Override
    public List<AiWorkspaceComponent> findByRefId(String refId) throws TechnicalException {
        log.debug("JdbcAiWorkspaceComponentRepository.findByRefId({})", refId);
        try {
            return jdbcTemplate.query(getOrm().getSelectAllSql() + " WHERE ref_id = ? ORDER BY id", getOrm().getRowMapper(), refId);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to find ai workspace components by ref id", ex);
        }
    }

    @Override
    public void deleteByApiProductId(String apiProductId) throws TechnicalException {
        log.debug("JdbcAiWorkspaceComponentRepository.deleteByApiProductId({})", apiProductId);
        try {
            jdbcTemplate.update("DELETE FROM " + this.tableName + " WHERE api_product_id = ?", apiProductId);
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to delete ai workspace components by api product id", ex);
        }
    }
}
