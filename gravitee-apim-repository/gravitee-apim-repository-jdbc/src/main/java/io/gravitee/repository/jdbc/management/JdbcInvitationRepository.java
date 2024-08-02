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
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.model.Invitation;
import io.gravitee.repository.management.model.InvitationReferenceType;
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
public class JdbcInvitationRepository extends JdbcAbstractCrudRepository<Invitation, String> implements InvitationRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(JdbcInvitationRepository.class);

    JdbcInvitationRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "invitations");
    }

    @Override
    protected JdbcObjectMapper<Invitation> buildOrm() {
        return JdbcObjectMapper
            .builder(Invitation.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("email", Types.NVARCHAR, String.class)
            .addColumn("api_role", Types.NVARCHAR, String.class)
            .addColumn("application_role", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(final Invitation invitation) {
        return invitation.getId();
    }

    @Override
    public List<Invitation> findByReferenceIdAndReferenceType(final String referenceId, final InvitationReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcInvitationRepository.findByReferenceIdAndReferenceType({}, {})", referenceId, referenceType);
        try {
            return jdbcTemplate.query(
                getOrm().getSelectAllSql() + " where reference_type = ? and reference_id = ?",
                getOrm().getRowMapper(),
                referenceType.name(),
                referenceId
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to find invitation for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to find invitation by reference", ex);
        }
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, InvitationReferenceType referenceType)
        throws TechnicalException {
        LOGGER.debug("JdbcInvitationRepository.deleteByReferenceIdAndReferenceType({}/{})", referenceId, referenceType);
        try {
            final var invitationIds = jdbcTemplate.queryForList(
                "select id from " + tableName + " where reference_id = ? and reference_type = ?",
                String.class,
                referenceId,
                referenceType.name()
            );

            if (!invitationIds.isEmpty()) {
                jdbcTemplate.update(
                    "delete from " + tableName + " where reference_id = ? and reference_type = ?",
                    referenceId,
                    referenceType.name()
                );
            }

            LOGGER.debug("JdbcInvitationRepository.deleteByReferenceIdAndReferenceType({}/{}) - Done", referenceId, referenceType);
            return invitationIds;
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete invitation for refId: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete invitation by reference", ex);
        }
    }
}
