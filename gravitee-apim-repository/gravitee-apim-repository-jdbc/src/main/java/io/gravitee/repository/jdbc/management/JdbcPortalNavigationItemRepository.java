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

import static org.springframework.util.StringUtils.hasText;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.common.CriteriaClauses;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import io.gravitee.repository.management.api.search.PortalNavigationItemCriteria;
import io.gravitee.repository.management.model.PortalNavigationItem;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
@CustomLog
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
            .addColumn("published", Types.BOOLEAN, boolean.class)
            .addColumn("apiId", Types.NVARCHAR, String.class)
            .addColumn("visibility", Types.NVARCHAR, PortalNavigationItem.Visibility.class)
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
            throw new TechnicalException("Failed to find portal navigation items", ex);
        }
    }

    @Override
    public List<PortalNavigationItem> findAllByParentIdAndEnvironmentId(String parentId, String environmentId) throws TechnicalException {
        log.debug("JdbcPortalNavigationItemRepository.findAllByParentIdAndEnvironmentId({}, {})", parentId, environmentId);
        try {
            final String sql = getOrm().getSelectAllSql() + " where parent_id = ? and environment_id = ?";
            return jdbcTemplate.query(sql, getOrm().getRowMapper(), parentId, environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find portal navigation items by parentId", ex);
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
            throw new TechnicalException("Failed to find portal navigation items by area", ex);
        }
    }

    @Override
    public List<PortalNavigationItem> findAllByAreaAndEnvironmentIdAndParentIdIsNull(PortalNavigationItem.Area area, String environmentId)
        throws TechnicalException {
        log.debug("JdbcPortalNavigationItemRepository.findAllByAreaAndEnvironmentIdAndParentIdIsNull({}, {})", area, environmentId);
        try {
            final String sql = getOrm().getSelectAllSql() + " where area = ? and environment_id = ? and parent_id is null";
            return jdbcTemplate.query(sql, getOrm().getRowMapper(), area.name(), environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find top level portal navigation items by area", ex);
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcPortalNavigationItemRepository.deleteByOrganizationId({})", organizationId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where organization_id = ?", organizationId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete portal navigation items by organizationId", ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcPortalNavigationItemRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete portal navigation items by environmentId", ex);
        }
    }

    @Override
    public List<PortalNavigationItem> searchByCriteria(PortalNavigationItemCriteria criteria) throws TechnicalException {
        log.debug("JdbcPortalNavigationItemRepository.searchByCriteria({})", criteria);
        try {
            StringBuilder sql = new StringBuilder(getOrm().getSelectAllSql());
            CriteriaClauses clauses = buildCriteriaClauses(criteria);

            if (!clauses.clauses().isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", clauses.clauses()));
            }

            return executeQueryByCriteria(sql.toString(), clauses.params());
        } catch (Exception ex) {
            log.error("Failed to search portal navigation items by criteria", ex);
            throw new TechnicalException("Failed to search portal navigation items by criteria", ex);
        }
    }

    private CriteriaClauses buildCriteriaClauses(PortalNavigationItemCriteria criteria) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (criteria != null) {
            if (hasText(criteria.getEnvironmentId())) {
                clauses.add("environment_id = ?");
                params.add(criteria.getEnvironmentId());
            }

            if (hasText(criteria.getParentId())) {
                clauses.add("parent_id = ?");
                params.add(criteria.getParentId());
            } else if (Boolean.TRUE.equals(criteria.getRoot())) {
                clauses.add("parent_id IS NULL");
            }

            if (hasText(criteria.getPortalArea())) {
                try {
                    PortalNavigationItem.Area area = PortalNavigationItem.Area.valueOf(criteria.getPortalArea());
                    clauses.add("area = ?");
                    params.add(area.name());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid portal area value: {}", criteria.getPortalArea());
                }
            }
            if (criteria.getPublished() != null) {
                clauses.add("published = ?");
                params.add(criteria.getPublished());
            }
            if (criteria.getVisibility() != null) {
                clauses.add("visibility = ?");
                params.add(criteria.getVisibility());
            }
        }

        return new CriteriaClauses(clauses, params);
    }

    private List<PortalNavigationItem> executeQueryByCriteria(String sql, List<Object> params) {
        return jdbcTemplate.query(sql, ps -> fillPreparedStatement(params, ps), getOrm().getRowMapper());
    }

    private void fillPreparedStatement(List<Object> params, PreparedStatement ps) throws java.sql.SQLException {
        int index = 1;
        for (Object param : params) {
            switch (param) {
                case String s -> ps.setString(index++, s);
                case Boolean b -> ps.setBoolean(index++, b);
                case null, default -> ps.setObject(index++, param);
            }
        }
    }

    @Override
    protected String getId(PortalNavigationItem item) {
        return item.getId();
    }
}
