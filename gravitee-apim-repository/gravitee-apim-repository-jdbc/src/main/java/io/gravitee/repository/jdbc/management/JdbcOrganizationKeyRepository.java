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

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.model.OrganizationKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Binds its own statements, like {@link JdbcSecretRepository}, to keep the wrapped key out of the trace logs. */
@CustomLog
@Repository
public class JdbcOrganizationKeyRepository extends TransactionalRepository implements OrganizationKeyRepository {

    private final String tableName;
    private final String columns;

    private final TransactionTemplate savepoint;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    JdbcOrganizationKeyRepository(
        @Value("${management.jdbc.prefix:}") String tablePrefix,
        @Lazy @Autowired @Qualifier("graviteeTransactionManager") PlatformTransactionManager transactionManager
    ) {
        this.tableName = tablePrefix + "organization_keys";
        this.columns = "id, organization_id, wrapped_key, kek_id, created_at";
        this.savepoint = new TransactionTemplate(transactionManager);
        this.savepoint.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    @Override
    public OrganizationKey create(OrganizationKey organizationKey) throws TechnicalException {
        log.debug("JdbcOrganizationKeyRepository.create({})", organizationKey.getId());
        try {
            savepoint.executeWithoutResult(status ->
                jdbcTemplate.update("INSERT INTO " + tableName + " (" + columns + ") VALUES (?, ?, ?, ?, ?)", ps -> {
                    ps.setString(1, organizationKey.getId());
                    ps.setString(2, organizationKey.getOrganizationId());
                    ps.setString(3, organizationKey.getWrappedKey());
                    ps.setString(4, organizationKey.getKekId());
                    ps.setTimestamp(5, new Timestamp(organizationKey.getCreatedAt().getTime()));
                })
            );
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new DuplicateKeyException("Organization [" + organizationKey.getOrganizationId() + "] already has a key", ex);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create the key of organization " + organizationKey.getOrganizationId(), ex);
        }
        return organizationKey;
    }

    @Override
    public Optional<OrganizationKey> findByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcOrganizationKeyRepository.findByOrganizationId({})", organizationId);
        try {
            return jdbcTemplate
                .query(
                    "SELECT " + columns + " FROM " + tableName + " WHERE organization_id = ?",
                    JdbcOrganizationKeyRepository::toOrganizationKey,
                    organizationId
                )
                .stream()
                .findFirst();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find the key of organization " + organizationId, ex);
        }
    }

    @Override
    public Set<String> findKekIds() throws TechnicalException {
        log.debug("JdbcOrganizationKeyRepository.findKekIds()");
        try {
            return new HashSet<>(jdbcTemplate.queryForList("SELECT DISTINCT kek_id FROM " + tableName, String.class));
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find the key encryption key ids", ex);
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcOrganizationKeyRepository.deleteByOrganizationId({})", organizationId);
        try {
            jdbcTemplate.update("DELETE FROM " + tableName + " WHERE organization_id = ?", organizationId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete the key of organization " + organizationId, ex);
        }
    }

    private static OrganizationKey toOrganizationKey(ResultSet rs, int rowNum) throws SQLException {
        var createdAt = rs.getTimestamp("created_at");
        return OrganizationKey.builder()
            .id(rs.getString("id"))
            .organizationId(rs.getString("organization_id"))
            .wrappedKey(rs.getString("wrapped_key"))
            .kekId(rs.getString("kek_id"))
            .createdAt(createdAt == null ? null : new Date(createdAt.getTime()))
            .build();
    }
}
