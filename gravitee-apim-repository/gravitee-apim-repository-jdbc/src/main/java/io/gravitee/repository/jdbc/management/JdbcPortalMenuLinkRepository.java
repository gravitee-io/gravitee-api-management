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
import io.gravitee.repository.management.api.PortalMenuLinkRepository;
import io.gravitee.repository.management.model.PortalMenuLink;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@Repository
public class JdbcPortalMenuLinkRepository extends JdbcAbstractCrudRepository<PortalMenuLink, String> implements PortalMenuLinkRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPortalMenuLinkRepository.class);

    JdbcPortalMenuLinkRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "portal_menu_links");
    }

    @Override
    protected JdbcObjectMapper<PortalMenuLink> buildOrm() {
        return JdbcObjectMapper
            .builder(PortalMenuLink.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, PortalMenuLink.PortalMenuLinkType.class)
            .addColumn("target", Types.NVARCHAR, String.class)
            .addColumn("order", Types.INTEGER, int.class)
            .build();
    }

    @Override
    protected String getId(final PortalMenuLink item) {
        return item.getId();
    }

    @Override
    public Optional<PortalMenuLink> findByIdAndEnvironmentId(String portalMenuLinkId, String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcPortalMenuLinkRepository.findByIdAndEnvironmentId({})", environmentId);
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() + " WHERE id = ? and environment_id = ? ORDER BY " + escapeReservedWord("order"),
                    getOrm().getRowMapper(),
                    portalMenuLinkId,
                    environmentId
                )
                .stream()
                .findFirst();
        } catch (final Exception ex) {
            final String error = String.format(
                "Failed to find portal menu links by id (%s) and environment id (%s)",
                portalMenuLinkId,
                environmentId
            );
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public List<PortalMenuLink> findByEnvironmentIdSortByOrder(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcPortalMenuLinkRepository.findByEnvironmentIdSortByOrder({})", environmentId);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " WHERE environment_id = ? ORDER BY " + escapeReservedWord("order"),
                getOrm().getRowMapper(),
                environmentId
            );
        } catch (final Exception ex) {
            final String error = "Failed to find portal menu links by environment id: " + environmentId;
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcPortalMenuLinkRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (final Exception ex) {
            final String error = "Failed to delete portal menu links by environment id: " + environmentId;
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }
}
