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
import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.model.IdentityProviderActivation;
import io.gravitee.repository.management.model.IdentityProviderActivationReferenceType;
import java.sql.Types;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Repository
public class JdbcIdentityProviderActivationRepository
    extends JdbcAbstractRepository<IdentityProviderActivation>
    implements IdentityProviderActivationRepository {

    JdbcIdentityProviderActivationRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "identity_provider_activations");
    }

    @Override
    protected JdbcObjectMapper<IdentityProviderActivation> buildOrm() {
        return JdbcObjectMapper.builder(IdentityProviderActivation.class, this.tableName)
            .addColumn("identity_provider_id", Types.NVARCHAR, String.class)
            .addColumn("reference_id", Types.NVARCHAR, String.class)
            .addColumn("reference_type", Types.NVARCHAR, IdentityProviderActivationReferenceType.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public Optional<IdentityProviderActivation> findById(
        String identityProviderId,
        String referenceId,
        IdentityProviderActivationReferenceType referenceType
    ) throws TechnicalException {
        log.debug("JdbcIdentityProviderActivationRepository.findById({}, {}, {})", identityProviderId, referenceId, referenceType);
        try {
            final List<IdentityProviderActivation> identityProviderActivations = jdbcTemplate.query(
                "select" +
                    " identity_provider_id, reference_id, reference_type, created_at " +
                    " from " +
                    this.tableName +
                    " where identity_provider_id = ? and reference_id = ? and reference_type= ?",
                getOrm().getRowMapper(),
                identityProviderId,
                referenceId,
                referenceType.name()
            );
            return identityProviderActivations.stream().findFirst();
        } catch (final Exception ex) {
            final String error = "Failed to find identityProviderActivation by id";
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public Set<IdentityProviderActivation> findAll() throws TechnicalException {
        log.debug("JdbcIdentityProviderActivationRepository.findAll()");
        try {
            final List<IdentityProviderActivation> identityProviderActivations = jdbcTemplate.query(
                "select" + " identity_provider_id, reference_id, reference_type, created_at " + " from " + this.tableName,
                getOrm().getRowMapper()
            );
            return new HashSet<>(identityProviderActivations);
        } catch (final Exception ex) {
            final String error = "Failed to find all identityProviderActivations";
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public Set<IdentityProviderActivation> findAllByIdentityProviderId(String identityProviderId) throws TechnicalException {
        log.debug("JdbcIdentityProviderActivationRepository.findAllByIdentityProviderId({})", identityProviderId);
        try {
            final List<IdentityProviderActivation> identityProviderActivations = jdbcTemplate.query(
                "select" +
                    " identity_provider_id, reference_id, reference_type, created_at " +
                    " from " +
                    this.tableName +
                    " where identity_provider_id = ?",
                getOrm().getRowMapper(),
                identityProviderId
            );
            return new HashSet<>(identityProviderActivations);
        } catch (final Exception ex) {
            final String error = "Failed to find all identityProviderActivations by identity_provider_id";
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public Set<IdentityProviderActivation> findAllByReferenceIdAndReferenceType(
        String referenceId,
        IdentityProviderActivationReferenceType referenceType
    ) throws TechnicalException {
        log.debug("JdbcIdentityProviderActivationRepository.findAllByIdentityProviderId({}, {})", referenceId, referenceType);
        try {
            final List<IdentityProviderActivation> identityProviderActivations = jdbcTemplate.query(
                "select" +
                    " identity_provider_id, reference_id, reference_type, created_at " +
                    " from " +
                    this.tableName +
                    " where reference_id = ? and reference_type= ?",
                getOrm().getRowMapper(),
                referenceId,
                referenceType.name()
            );
            return new HashSet<>(identityProviderActivations);
        } catch (final Exception ex) {
            final String error = "Failed to find all identityProviderActivations by reference";
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public IdentityProviderActivation create(IdentityProviderActivation identityProviderActivation) throws TechnicalException {
        log.debug("JdbcIdentityProviderActivationRepository.create({})", identityProviderActivation);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(identityProviderActivation));
            return findById(
                identityProviderActivation.getIdentityProviderId(),
                identityProviderActivation.getReferenceId(),
                identityProviderActivation.getReferenceType()
            ).orElse(null);
        } catch (final Exception ex) {
            final String error = "Failed to create identityProviderActivation";
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void delete(String identityProviderId, String referenceId, IdentityProviderActivationReferenceType referenceType)
        throws TechnicalException {
        log.debug("JdbcIdentityProviderActivationRepository.delete({}, {}, {})", identityProviderId, referenceId, referenceType);
        try {
            jdbcTemplate.update(
                "delete from " + this.tableName + " where identity_provider_id = ? and reference_id = ? and reference_type = ? ",
                identityProviderId,
                referenceId,
                referenceType.name()
            );
        } catch (final Exception ex) {
            final String error = "Failed to delete identityProviderActivation";
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void deleteByIdentityProviderId(String identityProviderId) throws TechnicalException {
        log.debug("JdbcIdentityProviderActivationRepository.deleteByIdentityProviderId({})", identityProviderId);
        try {
            jdbcTemplate.update("delete from " + this.tableName + " where identity_provider_id = ? ", identityProviderId);
        } catch (final Exception ex) {
            final String error = "Failed to delete identityProviderActivations by identityProvider id";
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public void deleteByReferenceIdAndReferenceType(String referenceId, IdentityProviderActivationReferenceType referenceType)
        throws TechnicalException {
        log.debug("JdbcIdentityProviderActivationRepository.deleteByReferenceIdAndReferenceType({}, {})", referenceId, referenceType);
        try {
            jdbcTemplate.update(
                "delete from " + this.tableName + " where reference_id = ? and reference_type = ? ",
                referenceId,
                referenceType.name()
            );
        } catch (final Exception ex) {
            final String error = "Failed to delete identityProviderActivations by reference";
            log.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }
}
