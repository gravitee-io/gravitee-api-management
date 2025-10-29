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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class JdbcPortalNavigationItemRepository
    extends JdbcAbstractCrudRepository<PortalNavigationItem, String>
    implements PortalNavigationItemRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPortalNavigationItemRepository.class);

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
    protected RowMapper<PortalNavigationItem> getRowMapper() {
        return (rs, rowNum) -> {
            PortalNavigationItem item = new PortalNavigationItem();
            getOrm().setFromResultSet(item, rs);
            return item;
        };
    }

    @Override
    public PortalNavigationItem create(PortalNavigationItem item) throws TechnicalException {
        LOGGER.debug("JdbcPortalNavigationItemRepository.create({})", item.getId());
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            return this.findById(item.getId()).orElse(null);
        } catch (Exception e) {
            throw new TechnicalException("Failed to create portal navigation item", e);
        }
    }

    @Override
    public List<PortalNavigationItem> findAllByOrganizationIdAndEnvironmentId(String organizationId, String environmentId)
        throws TechnicalException {
        LOGGER.debug("JdbcPortalNavigationItemRepository.findAllByOrganizationIdAndEnvironmentId({}, {})", organizationId, environmentId);
        try {
            final String sql =
                "select id, organization_id, environment_id, title, type, area, parent_id, \"order\", configuration from " +
                this.tableName +
                " where organization_id = ? and environment_id = ?";
            return jdbcTemplate.query(sql, getOrm().getRowMapper(), organizationId, environmentId);
        } catch (Exception ex) {
            LOGGER.error("Failed to find portal navigation items", ex);
            return List.of();
        }
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndOrganizationIdAndEnvironmentId(
        PortalNavigationItem.Area area,
        String organizationId,
        String environmentId
    ) throws TechnicalException {
        LOGGER.debug(
            "JdbcPortalNavigationItemRepository.findAllByAreaAndOrganizationIdAndEnvironmentId({}, {}, {})",
            area,
            organizationId,
            environmentId
        );
        try {
            final String sql =
                "select id, organization_id, environment_id, title, type, area, parent_id, \"order\", configuration from " +
                this.tableName +
                " where area = ? and organization_id = ? and environment_id = ?";
            return jdbcTemplate.query(sql, getOrm().getRowMapper(), area.name(), organizationId, environmentId);
        } catch (Exception ex) {
            LOGGER.error("Failed to find portal navigation items by area", ex);
            return List.of();
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        LOGGER.debug("JdbcPortalNavigationItemRepository.deleteByOrganizationId({})", organizationId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where organization_id = ?", organizationId);
        } catch (Exception ex) {
            LOGGER.error("Failed to delete portal navigation items by organizationId", ex);
            throw new TechnicalException("Failed to delete portal navigation items by organizationId", ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcPortalNavigationItemRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (Exception ex) {
            LOGGER.error("Failed to delete portal navigation items by environmentId", ex);
            throw new TechnicalException("Failed to delete portal navigation items by environmentId", ex);
        }
    }

    @Override
    protected String getId(PortalNavigationItem item) {
        return item.getId();
    }
}
