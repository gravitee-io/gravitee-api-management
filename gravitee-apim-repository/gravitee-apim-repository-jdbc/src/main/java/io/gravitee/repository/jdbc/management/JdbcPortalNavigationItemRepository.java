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
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.sql.Types;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class JdbcPortalNavigationItemRepository
    extends JdbcAbstractCrudRepository<PortalNavigationItem, String>
    implements PortalNavigationItemRepository {

    JdbcPortalNavigationItemRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "portal_navigation_items");
    }

    @Override
    protected JdbcObjectMapper<PortalNavigationItem> buildOrm() {
        return JdbcObjectMapper.builder(PortalNavigationItem.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("title", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, PortalNavigationItem.Type.class)
            .addColumn("area", Types.NVARCHAR, PortalNavigationItem.Area.class)
            .addColumn("parent_id", Types.NVARCHAR, String.class)
            .addColumn("order", Types.INTEGER, Integer.class)
            .addColumn("configuration", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    public List<PortalNavigationItem> findAllByOrganizationIdAndEnvironmentId(String organizationId, String environmentId)
        throws TechnicalException {
        log.debug("JdbcPortalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId({}, {})", organizationId, environmentId);
        try {
            final String sql = getOrm().getSelectAllSql() + " where organization_id = ? and environment_id = ?";
            return jdbcTemplate.query(sql, getOrm().getRowMapper(), organizationId, environmentId);
        } catch (Exception ex) {
            log.error("Failed to find portal navigation items", ex);
            throw new TechnicalException("Failed to find portal navigation items", ex);
        }
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndEnvironmentId(PortalNavigationItem.Area area, String environmentId)
        throws TechnicalException {
        log.debug("JdbcPortalNavigationItemRepository.findAllByAreaAndEnvironmentId({}, {})", area, environmentId);
        try {
            final String sql = getOrm().getSelectAllSql() + " where area = ? and environment_id = ?";
            return jdbcTemplate.query(sql, getOrm().getRowMapper(), area.name(), environmentId);
        } catch (Exception ex) {
            log.error("Failed to find portal navigation items by area", ex);
            throw new TechnicalException("Failed to find portal navigation items by area", ex);
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcPortalNavigationItemRepository.deleteByOrganizationId({})", organizationId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where organization_id = ?", organizationId);
        } catch (Exception ex) {
            log.error("Failed to delete portal navigation items by organizationId", ex);
            throw new TechnicalException("Failed to delete portal navigation items by organizationId", ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcPortalNavigationItemRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (Exception ex) {
            log.error("Failed to delete portal navigation items by environmentId", ex);
            throw new TechnicalException("Failed to delete portal navigation items by environmentId", ex);
        }
    }

    @Override
    protected String getId(PortalNavigationItem item) {
        return item.getId();
    }
}
