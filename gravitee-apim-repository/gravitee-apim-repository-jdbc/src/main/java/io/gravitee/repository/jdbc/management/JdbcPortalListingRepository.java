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
import io.gravitee.repository.management.api.PortalListingRepository;
import io.gravitee.repository.management.model.PortalListing;
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
public class JdbcPortalListingRepository extends JdbcAbstractCrudRepository<PortalListing, String> implements PortalListingRepository {

    JdbcPortalListingRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "portal_listings");
    }

    @Override
    protected JdbcObjectMapper<PortalListing> buildOrm() {
        return JdbcObjectMapper.builder(PortalListing.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("portal_id", Types.NVARCHAR, String.class)
            .addColumn("apis", Types.NCLOB, String.class)
            .build();
    }

    @Override
    protected String getId(final PortalListing item) {
        return item.getId();
    }

    @Override
    public Optional<PortalListing> findByIdAndEnvironmentId(String portalListingId, String environmentId) throws TechnicalException {
        log.debug("JdbcPortalListingRepository.findByIdAndEnvironmentId({}, {})", portalListingId, environmentId);
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() + " WHERE id = ? and environment_id = ?",
                    getOrm().getRowMapper(),
                    portalListingId,
                    environmentId
                )
                .stream()
                .findFirst();
        } catch (final Exception ex) {
            throw handleException(
                ex,
                String.format("Failed to find portal listing by id (%s) and environment id (%s)", portalListingId, environmentId)
            );
        }
    }

    @Override
    public List<PortalListing> findAllByPortalIdAndEnvironmentId(String portalId, String environmentId) throws TechnicalException {
        log.debug("JdbcPortalListingRepository.findAllByPortalIdAndEnvironmentId({}, {})", portalId, environmentId);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " WHERE portal_id = ? and environment_id = ?",
                getOrm().getRowMapper(),
                portalId,
                environmentId
            );
        } catch (final Exception ex) {
            throw handleException(
                ex,
                String.format("Failed to find portal listings by portal id (%s) and environment id (%s)", portalId, environmentId)
            );
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcPortalListingRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (final Exception ex) {
            throw handleException(ex, "Failed to delete portal listings by environment id: " + environmentId);
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcPortalListingRepository.deleteByOrganizationId({})", organizationId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where organization_id = ?", organizationId);
        } catch (final Exception ex) {
            throw handleException(ex, "Failed to delete portal listings by organization id: " + organizationId);
        }
    }

    private TechnicalException handleException(Exception ex, String errorMessage) {
        log.error(errorMessage, ex);
        return new TechnicalException(errorMessage, ex);
    }
}
