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
import io.gravitee.repository.management.api.AmConnectionRepository;
import io.gravitee.repository.management.model.AmConnection;
import java.sql.Types;
import java.util.Date;
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
public class JdbcAmConnectionRepository extends JdbcAbstractRepository<AmConnection> implements AmConnectionRepository {

    JdbcAmConnectionRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "am_connections");
    }

    @Override
    protected JdbcObjectMapper<AmConnection> buildOrm() {
        return JdbcObjectMapper.builder(AmConnection.class, this.tableName, "organization_id")
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("base_url", Types.NVARCHAR, String.class)
            .addColumn("service_account_access_token_encrypted", Types.NVARCHAR, String.class)
            .addColumn("default_domain_id", Types.NVARCHAR, String.class)
            .addColumn("default_domain_hrid", Types.NVARCHAR, String.class)
            .addColumn("gateway_url", Types.NVARCHAR, String.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public Optional<AmConnection> findByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcAmConnectionRepository.findByOrganizationId({})", organizationId);
        try {
            List<AmConnection> items = jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where organization_id = ?",
                getOrm().getRowMapper(),
                organizationId
            );
            return items.stream().findFirst();
        } catch (final Exception ex) {
            log.error("Failed to find am connection by organizationId: {}", organizationId, ex);
            throw new TechnicalException("Failed to find am connection by organizationId", ex);
        }
    }

    @Override
    public AmConnection create(AmConnection amConnection) throws TechnicalException {
        log.debug("JdbcAmConnectionRepository.create({})", amConnection.getOrganizationId());
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(amConnection));
            return findByOrganizationId(amConnection.getOrganizationId()).orElse(null);
        } catch (final Exception ex) {
            log.error("Failed to create am connection:", ex);
            throw new TechnicalException("Failed to create am connection", ex);
        }
    }

    @Override
    public AmConnection update(AmConnection amConnection) throws TechnicalException {
        log.debug("JdbcAmConnectionRepository.update({})", amConnection.getOrganizationId());
        if (amConnection == null) {
            throw new IllegalStateException("Unable to update null am connection");
        }
        try {
            int rows = jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(amConnection, amConnection.getOrganizationId()));
            if (rows == 0) {
                throw new IllegalStateException("Unable to update am connection " + amConnection.getOrganizationId());
            }
            return findByOrganizationId(amConnection.getOrganizationId()).orElse(null);
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            log.error("Failed to update am connection:", ex);
            throw new TechnicalException("Failed to update am connection", ex);
        }
    }

    @Override
    public void delete(String organizationId) throws TechnicalException {
        log.debug("JdbcAmConnectionRepository.delete({})", organizationId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where organization_id = ?", organizationId);
        } catch (final Exception ex) {
            log.error("Failed to delete am connection:", ex);
            throw new TechnicalException("Failed to delete am connection", ex);
        }
    }
}
