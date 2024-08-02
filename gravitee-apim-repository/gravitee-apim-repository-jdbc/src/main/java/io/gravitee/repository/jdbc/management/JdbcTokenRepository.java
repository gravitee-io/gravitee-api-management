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
import io.gravitee.repository.management.api.TokenRepository;
import io.gravitee.repository.management.model.Token;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcTokenRepository extends JdbcAbstractCrudRepository<Token, String> implements TokenRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcTokenRepository.class);

    JdbcTokenRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "tokens");
    }

    @Override
    protected JdbcObjectMapper<Token> buildOrm() {
        return JdbcObjectMapper
            .builder(Token.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("token", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("expires_at", Types.TIMESTAMP, Date.class)
            .addColumn("last_use_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(final Token item) {
        return item.getId();
    }

    @Override
    public List<Token> findByReference(final String referenceType, final String referenceId) throws TechnicalException {
        LOGGER.debug("JdbcTokenRepository.findByReference({}, {})", referenceType, referenceId);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where reference_type = ? and reference_id = ?",
                getOrm().getRowMapper(),
                referenceType,
                referenceId
            );
        } catch (final Exception ex) {
            final String message = "Failed to find tokens by reference";
            LOGGER.error(message, ex);
            throw new TechnicalException(message, ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, String referenceType) throws TechnicalException {
        LOGGER.debug("JdbcTokenRepository.deleteByReferenceIdAndReferenceType({}, {})", referenceType, referenceId);
        try {
            final var rows = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_type = ? and reference_id = ?",
                String.class,
                referenceType,
                referenceId
            );

            if (!rows.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_type = ? and reference_id = ?",
                    referenceType,
                    referenceId
                );
            }

            LOGGER.debug("JdbcTokenRepository.deleteByReferenceIdAndReferenceType({}, {}) - Done", referenceType, referenceId);
            return rows;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete tokens for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete tokens by reference", ex);
        }
    }
}
