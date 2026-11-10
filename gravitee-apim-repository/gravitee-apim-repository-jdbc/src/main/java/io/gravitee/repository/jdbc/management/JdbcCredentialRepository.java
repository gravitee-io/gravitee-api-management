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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CredentialRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Credential;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

@CustomLog
@Repository
public class JdbcCredentialRepository extends TransactionalRepository implements CredentialRepository {

    private static final String IN_SCOPE = " WHERE organization_id = ? AND environment_id = ?";

    /** Not a backslash: MySQL and MariaDB read a backslash as an escape inside the string literal too. */
    private static final String LIKE_ESCAPE = "!";

    private final String tableName;
    private final String columns;
    private final String selectFrom;

    /** A duplicate id must not abort the caller's transaction on PostgreSQL. */
    private final TransactionTemplate savepoint;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    JdbcCredentialRepository(
        @Value("${management.jdbc.prefix:}") String tablePrefix,
        @Lazy @Autowired @Qualifier("graviteeTransactionManager") PlatformTransactionManager transactionManager
    ) {
        this.tableName = tablePrefix + "credentials";
        this.columns =
            "id, organization_id, environment_id, name, strategy, service_id, owner_id, expires_at, configuration, " +
            "secret_references, created_at, updated_at, rotated_at";
        this.selectFrom = "SELECT " + columns + " FROM " + tableName;
        this.savepoint = new TransactionTemplate(transactionManager);
        this.savepoint.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    @Override
    public Credential create(Credential credential) throws TechnicalException {
        log.debug("JdbcCredentialRepository.create({})", credential.getId());
        try {
            savepoint.executeWithoutResult(status ->
                jdbcTemplate.update(
                    "INSERT INTO " + tableName + " (" + columns + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    ps -> {
                        ps.setString(1, credential.getId());
                        ps.setString(2, credential.getOrganizationId());
                        ps.setString(3, credential.getEnvironmentId());
                        ps.setString(4, credential.getName());
                        ps.setString(5, credential.getStrategy());
                        ps.setString(6, credential.getServiceId());
                        ps.setString(7, credential.getOwnerId());
                        ps.setTimestamp(8, timestamp(credential.getExpiresAt()));
                        ps.setString(9, credential.getConfiguration());
                        ps.setString(10, credential.getSecretReferences());
                        ps.setTimestamp(11, timestamp(credential.getCreatedAt()));
                        ps.setTimestamp(12, timestamp(credential.getUpdatedAt()));
                        ps.setTimestamp(13, timestamp(credential.getRotatedAt()));
                    }
                )
            );
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new DuplicateKeyException("A credential with id [" + credential.getId() + "] already exists", ex);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create credential " + credential.getId(), ex);
        }
        return findById(credential.getOrganizationId(), credential.getEnvironmentId(), credential.getId()).orElseThrow(() ->
            new TechnicalException("Credential " + credential.getId() + " not found after its creation")
        );
    }

    @Override
    public Optional<Credential> findById(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("JdbcCredentialRepository.findById({}, {}, {})", organizationId, environmentId, id);
        try {
            return jdbcTemplate
                .query(selectFrom + IN_SCOPE + " AND id = ?", JdbcCredentialRepository::toCredential, organizationId, environmentId, id)
                .stream()
                .findFirst();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find credential " + id, ex);
        }
    }

    @Override
    public Credential update(Credential credential) throws TechnicalException {
        log.debug("JdbcCredentialRepository.update({})", credential.getId());
        int updated;
        try {
            // MariaDB gives the first TIMESTAMP column ON UPDATE CURRENT_TIMESTAMP unless the statement sets it.
            updated = jdbcTemplate.update(
                "UPDATE " +
                    tableName +
                    " SET name = ?, service_id = ?, owner_id = ?, expires_at = ?, configuration = ?, secret_references = ?," +
                    " updated_at = ?, rotated_at = ?, created_at = created_at" +
                    IN_SCOPE +
                    " AND id = ?",
                ps -> {
                    ps.setString(1, credential.getName());
                    ps.setString(2, credential.getServiceId());
                    ps.setString(3, credential.getOwnerId());
                    ps.setTimestamp(4, timestamp(credential.getExpiresAt()));
                    ps.setString(5, credential.getConfiguration());
                    ps.setString(6, credential.getSecretReferences());
                    ps.setTimestamp(7, timestamp(credential.getUpdatedAt()));
                    ps.setTimestamp(8, timestamp(credential.getRotatedAt()));
                    ps.setString(9, credential.getOrganizationId());
                    ps.setString(10, credential.getEnvironmentId());
                    ps.setString(11, credential.getId());
                }
            );
        } catch (Exception ex) {
            throw new TechnicalException("Failed to update credential " + credential.getId(), ex);
        }
        if (updated == 0) {
            throw new TechnicalException("Credential " + credential.getId() + " not found in environment " + credential.getEnvironmentId());
        }
        return findById(credential.getOrganizationId(), credential.getEnvironmentId(), credential.getId()).orElseThrow(() ->
            new TechnicalException("Credential " + credential.getId() + " disappeared after its update")
        );
    }

    @Override
    public void delete(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("JdbcCredentialRepository.delete({}, {}, {})", organizationId, environmentId, id);
        try {
            jdbcTemplate.update("DELETE FROM " + tableName + IN_SCOPE + " AND id = ?", organizationId, environmentId, id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete credential " + id, ex);
        }
    }

    @Override
    public Page<Credential> search(String organizationId, String environmentId, String query, Pageable pageable) throws TechnicalException {
        log.debug("JdbcCredentialRepository.search({}, {})", organizationId, environmentId);
        try {
            var where = new StringBuilder(IN_SCOPE);
            List<Object> params = new ArrayList<>(List.of(organizationId, environmentId));
            if (query != null) {
                where.append(" AND lower(name) LIKE ? ESCAPE '").append(LIKE_ESCAPE).append("'");
                params.add("%" + escapeLikePattern(query.toLowerCase(Locale.ROOT)) + "%");
            }
            var total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + where, Long.class, params.toArray());
            if (total == null || total == 0) {
                return new Page<>(List.of(), pageable.pageNumber(), 0, 0);
            }
            var content = jdbcTemplate.query(
                selectFrom + where + " ORDER BY name, id " + createPagingClause(pageable.pageSize(), pageable.from()),
                JdbcCredentialRepository::toCredential,
                params.toArray()
            );
            return new Page<>(content, pageable.pageNumber(), content.size(), total);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to search credentials of environment " + environmentId, ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcCredentialRepository.deleteByEnvironmentId({})", environmentId);
        try {
            jdbcTemplate.update("DELETE FROM " + tableName + " WHERE environment_id = ?", environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete credentials of environment " + environmentId, ex);
        }
    }

    private static String escapeLikePattern(String value) {
        // The escape character first, or the escapes added below would be escaped again. SQL Server alone reads
        // [...] as a character class; the other flavours take an escaped '[' as a plain '['.
        return value
            .replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
            .replace("%", LIKE_ESCAPE + "%")
            .replace("_", LIKE_ESCAPE + "_")
            .replace("[", LIKE_ESCAPE + "[");
    }

    private static Credential toCredential(ResultSet rs, int rowNum) throws SQLException {
        return Credential.builder()
            .id(rs.getString("id"))
            .organizationId(rs.getString("organization_id"))
            .environmentId(rs.getString("environment_id"))
            .name(rs.getString("name"))
            .strategy(rs.getString("strategy"))
            .serviceId(rs.getString("service_id"))
            .ownerId(rs.getString("owner_id"))
            .expiresAt(date(rs.getTimestamp("expires_at")))
            .configuration(rs.getString("configuration"))
            .secretReferences(rs.getString("secret_references"))
            .createdAt(date(rs.getTimestamp("created_at")))
            .updatedAt(date(rs.getTimestamp("updated_at")))
            .rotatedAt(date(rs.getTimestamp("rotated_at")))
            .build();
    }

    private static Timestamp timestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    private static Date date(Timestamp timestamp) {
        return timestamp == null ? null : new Date(timestamp.getTime());
    }
}
