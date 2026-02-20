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
import io.gravitee.repository.management.api.PortalPageContextRepository;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import java.sql.Types;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author GraviteeSource Team
 */
@Repository
public class JdbcPortalPageContextRepository
    extends JdbcAbstractCrudRepository<PortalPageContext, String>
    implements PortalPageContextRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcPortalPageContextRepository.class);

    JdbcPortalPageContextRepository(@Value("${repositories.management.jdbc.prefix:${management.jdbc.prefix:}}") String tablePrefix) {
        super(tablePrefix, "portal_page_contexts");
    }

    @Override
    protected JdbcObjectMapper<PortalPageContext> buildOrm() {
        return JdbcObjectMapper.builder(PortalPageContext.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("page_id", Types.NVARCHAR, String.class)
            .addColumn("context_type", Types.NVARCHAR, PortalPageContextType.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("published", Types.BOOLEAN, boolean.class)
            .build();
    }

    @Override
    public PortalPageContext create(PortalPageContext portalPageContext) throws TechnicalException {
        LOGGER.debug("JdbcPortalPageContextRepository.create({}, {})", portalPageContext.getPageId(), portalPageContext.getContextType());

        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(portalPageContext));
            return this.findById(portalPageContext.getId()).orElse(null);
        } catch (Exception e) {
            throw new TechnicalException("Failed to create portal page context", e);
        }
    }

    @Override
    public List<PortalPageContext> findAllByContextTypeAndEnvironmentId(PortalPageContextType contextType, String environmentId) {
        LOGGER.debug("JdbcPortalPageContextRepository.findAllByContextTypeAndEnvironmentId({}, {})", contextType, environmentId);

        try {
            final List<PortalPageContext> portalPageContexts = jdbcTemplate.query(
                "select id, page_id, context_type, environment_id, published from " +
                    this.tableName +
                    " where context_type = ? and environment_id = ?",
                getOrm().getRowMapper(),
                contextType.name(),
                environmentId
            );
            LOGGER.debug(
                "JdbcPortalPageContextRepository.findAllByContextTypeAndEnvironmentId({}, {}) - Done, found {} contexts",
                contextType,
                environmentId,
                portalPageContexts.size()
            );
            return portalPageContexts;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find PortalPageContexts by contextType [{}] and environmentId [{}]", contextType, environmentId, ex);
            return List.of();
        }
    }

    @Override
    public PortalPageContext findByPageId(String string) {
        LOGGER.debug("JdbcPortalPageContextRepository.findByPageId({})", string);

        try {
            final PortalPageContext portalPageContext = jdbcTemplate.queryForObject(
                "select id, page_id, context_type, environment_id, published from " + this.tableName + " where page_id = ?",
                getOrm().getRowMapper(),
                string
            );
            LOGGER.debug("JdbcPortalPageContextRepository.findByPageId({}) - Done", string);
            return portalPageContext;
        } catch (final Exception ex) {
            LOGGER.error("Failed to find PortalPageContext by pageId [{}]", string, ex);
            return null;
        }
    }

    @Override
    public PortalPageContext updateByPageId(PortalPageContext item) throws TechnicalException {
        LOGGER.debug("JdbcPortalPageContextRepository.updateByPageId({})", item);

        try {
            final int rows = jdbcTemplate.update(
                "update " + this.tableName + " set context_type = ?, published = ? where page_id = ?",
                item.getContextType().name(),
                item.isPublished(),
                item.getPageId()
            );
            if (rows == 0) {
                throw new TechnicalException("Failed to update portal page context, no rows affected");
            }

            LOGGER.debug("JdbcPortalPageContextRepository.updateByPageId({}) - Done", item);

            return findByPageId(item.getPageId());
        } catch (final Exception ex) {
            throw new TechnicalException("Failed to update portal page context", ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcPortalPageContextRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where environment_id = ?", environmentId);
        } catch (final Exception ex) {
            final String error = "Failed to delete portal page context by environmentId " + environmentId;
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    protected String getId(PortalPageContext item) {
        return item.getId();
    }
}
