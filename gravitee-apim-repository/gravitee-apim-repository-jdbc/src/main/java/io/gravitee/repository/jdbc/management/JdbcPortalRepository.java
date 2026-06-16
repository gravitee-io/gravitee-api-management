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
import io.gravitee.repository.management.api.PortalRepository;
import io.gravitee.repository.management.model.Portal;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Repository
public class JdbcPortalRepository extends JdbcAbstractCrudRepository<Portal, String> implements PortalRepository {

    JdbcPortalRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "portals");
    }

    @Override
    protected JdbcObjectMapper<Portal> buildOrm() {
        return JdbcObjectMapper.builder(Portal.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("portal_navigation", Types.NVARCHAR, String.class)
            .build();
    }

    @Override
    protected String getId(final Portal item) {
        return item.getId();
    }

    @Override
    public Optional<Portal> findByIdAndEnvironmentId(String portalId, String environmentId) throws TechnicalException {
        log.debug("JdbcPortalRepository.findByIdAndEnvironmentId({}, {})", portalId, environmentId);
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() + " WHERE id = ? and environment_id = ?",
                    getOrm().getRowMapper(),
                    portalId,
                    environmentId
                )
                .stream()
                .findFirst();
        } catch (final Exception ex) {
            final String error = String.format("Failed to find portal by id (%s) and environment id (%s)", portalId, environmentId);
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public List<Portal> findByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcPortalRepository.findByEnvironmentId({})", environmentId);
        try {
            return jdbcTemplate.query(getOrm().getSelectAllSql() + " WHERE environment_id = ?", getOrm().getRowMapper(), environmentId);
        } catch (final Exception ex) {
            final String error = "Failed to find portals by environment id: " + environmentId;
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcPortalRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (final Exception ex) {
            final String error = "Failed to delete portals by environment id: " + environmentId;
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcPortalRepository.deleteByOrganizationId({})", organizationId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where organization_id = ?", organizationId);
        } catch (final Exception ex) {
            final String error = "Failed to delete portals by organization id: " + organizationId;
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }
}
