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

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.createPagingClause;
import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Secret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
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

/** {@code JdbcObjectMapper} and {@code JdbcTemplate}'s argument binding log bound values, so this repository binds its own statements to keep the encrypted value out of the logs. */
@CustomLog
@Repository
public class JdbcSecretRepository extends TransactionalRepository implements SecretRepository {

    private static final String KEY = escapeReservedWord("key");
    private static final String IN_SCOPE = " WHERE organization_id = ? AND environment_id = ?";

    private final String tableName;
    private final String columns;
    private final String selectFrom;

    private final TransactionTemplate savepoint;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    JdbcSecretRepository(
        @Value("${management.jdbc.prefix:}") String tablePrefix,
        @Lazy @Autowired @Qualifier("graviteeTransactionManager") PlatformTransactionManager transactionManager
    ) {
        this.tableName = tablePrefix + "secrets";
        this.columns = "id, organization_id, environment_id, " + KEY + ", key_hash, encrypted_value, created_at, updated_at";
        this.selectFrom = "SELECT " + columns + " FROM " + tableName;
        this.savepoint = new TransactionTemplate(transactionManager);
        this.savepoint.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    @Override
    public Secret create(Secret secret) throws TechnicalException {
        log.debug("JdbcSecretRepository.create({})", secret.getId());
        try {
            savepoint.executeWithoutResult(status ->
                jdbcTemplate.update("INSERT INTO " + tableName + " (" + columns + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)", ps -> {
                    ps.setString(1, secret.getId());
                    ps.setString(2, secret.getOrganizationId());
                    ps.setString(3, secret.getEnvironmentId());
                    ps.setString(4, secret.getKey());
                    ps.setString(5, keyHash(secret.getKey()));
                    ps.setString(6, secret.getEncryptedValue());
                    ps.setTimestamp(7, timestamp(secret.getCreatedAt()));
                    ps.setTimestamp(8, timestamp(secret.getUpdatedAt()));
                })
            );
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new DuplicateKeyException(
                "A secret with id [" +
                    secret.getId() +
                    "] or key [" +
                    secret.getKey() +
                    "] already exists in environment [" +
                    secret.getEnvironmentId() +
                    "]",
                ex
            );
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create secret " + secret.getId(), ex);
        }
        return findById(secret.getOrganizationId(), secret.getEnvironmentId(), secret.getId()).orElseThrow(() ->
            new TechnicalException("Secret " + secret.getId() + " not found after its creation")
        );
    }

    @Override
    public Optional<Secret> findById(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("JdbcSecretRepository.findById({}, {}, {})", organizationId, environmentId, id);
        try {
            return jdbcTemplate
                .query(selectFrom + IN_SCOPE + " AND id = ?", JdbcSecretRepository::toSecret, organizationId, environmentId, id)
                .stream()
                .findFirst();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find secret " + id, ex);
        }
    }

    @Override
    public Optional<Secret> findByKey(String organizationId, String environmentId, String key) throws TechnicalException {
        log.debug("JdbcSecretRepository.findByKey({}, {}, {})", organizationId, environmentId, key);
        try {
            return jdbcTemplate
                .query(
                    selectFrom + IN_SCOPE + " AND key_hash = ?",
                    JdbcSecretRepository::toSecret,
                    organizationId,
                    environmentId,
                    keyHash(key)
                )
                .stream()
                .findFirst();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find secret by key " + key, ex);
        }
    }

    @Override
    public Secret update(Secret secret) throws TechnicalException {
        log.debug("JdbcSecretRepository.update({})", secret.getId());
        int updated;
        try {
            // MariaDB gives the first TIMESTAMP column ON UPDATE CURRENT_TIMESTAMP unless the statement sets it.
            updated = jdbcTemplate.update(
                "UPDATE " + tableName + " SET encrypted_value = ?, updated_at = ?, created_at = created_at" + IN_SCOPE + " AND id = ?",
                ps -> {
                    ps.setString(1, secret.getEncryptedValue());
                    ps.setTimestamp(2, timestamp(secret.getUpdatedAt()));
                    ps.setString(3, secret.getOrganizationId());
                    ps.setString(4, secret.getEnvironmentId());
                    ps.setString(5, secret.getId());
                }
            );
        } catch (Exception ex) {
            throw new TechnicalException("Failed to update secret " + secret.getId(), ex);
        }
        if (updated == 0) {
            throw new TechnicalException("Secret " + secret.getId() + " not found in environment " + secret.getEnvironmentId());
        }
        return findById(secret.getOrganizationId(), secret.getEnvironmentId(), secret.getId()).orElseThrow(() ->
            new TechnicalException("Secret " + secret.getId() + " disappeared after its update")
        );
    }

    @Override
    public void delete(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("JdbcSecretRepository.delete({}, {}, {})", organizationId, environmentId, id);
        try {
            jdbcTemplate.update("DELETE FROM " + tableName + IN_SCOPE + " AND id = ?", organizationId, environmentId, id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete secret " + id, ex);
        }
    }

    @Override
    public Page<Secret> search(String organizationId, String environmentId, Pageable pageable) throws TechnicalException {
        log.debug("JdbcSecretRepository.search({}, {})", organizationId, environmentId);
        try {
            var total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + IN_SCOPE,
                Long.class,
                organizationId,
                environmentId
            );
            if (total == null || total == 0) {
                return new Page<>(List.of(), pageable.pageNumber(), 0, 0);
            }
            // id breaks ties: MySQL, MariaDB and SQL Server collate keys that differ only by case as equal.
            var content = jdbcTemplate.query(
                selectFrom + IN_SCOPE + " ORDER BY " + KEY + ", id " + createPagingClause(pageable.pageSize(), pageable.from()),
                JdbcSecretRepository::toSecret,
                organizationId,
                environmentId
            );
            return new Page<>(content, pageable.pageNumber(), content.size(), total);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to search secrets of environment " + environmentId, ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcSecretRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("DELETE FROM " + tableName + " WHERE environment_id = ?", environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete secrets of environment " + environmentId, ex);
        }
    }

    @Override
    public boolean existsByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("JdbcSecretRepository.existsByOrganizationId({})", organizationId);
        try {
            return !jdbcTemplate
                .queryForList("SELECT 1 FROM " + tableName + " WHERE organization_id = ?", Integer.class, organizationId)
                .isEmpty();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to check for secrets of organization " + organizationId, ex);
        }
    }

    private static Secret toSecret(ResultSet rs, int rowNum) throws SQLException {
        return Secret.builder()
            .id(rs.getString("id"))
            .organizationId(rs.getString("organization_id"))
            .environmentId(rs.getString("environment_id"))
            .key(rs.getString("key"))
            .encryptedValue(rs.getString("encrypted_value"))
            .createdAt(date(rs.getTimestamp("created_at")))
            .updatedAt(date(rs.getTimestamp("updated_at")))
            .build();
    }

    private static String keyHash(String key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static Timestamp timestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    private static Date date(Timestamp timestamp) {
        return timestamp == null ? null : new Date(timestamp.getTime());
    }
}
