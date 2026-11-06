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
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Secret;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@CustomLog
@Repository
public class JdbcSecretRepository extends JdbcAbstractRepository<Secret> implements SecretRepository {

    JdbcSecretRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "secrets");
    }

    @Override
    protected JdbcObjectMapper<Secret> buildOrm() {
        return JdbcObjectMapper.builder(Secret.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("organization_id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("encrypted_value", Types.NCLOB, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    public Secret create(Secret secret) throws TechnicalException {
        log.debug("JdbcSecretRepository.create({})", secret.getId());
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(secret));
            return findById(secret.getOrganizationId(), secret.getEnvironmentId(), secret.getId()).orElseThrow();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create secret " + secret.getId(), ex);
        }
    }

    @Override
    public Optional<Secret> findById(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("JdbcSecretRepository.findById({}, {}, {})", organizationId, environmentId, id);
        try {
            return jdbcTemplate
                .query(
                    getOrm().getSelectAllSql() + " WHERE id = ? AND organization_id = ? AND environment_id = ?",
                    getOrm().getRowMapper(),
                    id,
                    organizationId,
                    environmentId
                )
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
                    getOrm().getSelectAllSql() +
                        " WHERE organization_id = ? AND environment_id = ? AND " +
                        escapeReservedWord("key") +
                        " = ?",
                    getOrm().getRowMapper(),
                    organizationId,
                    environmentId,
                    key
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
        if (findById(secret.getOrganizationId(), secret.getEnvironmentId(), secret.getId()).isEmpty()) {
            throw new TechnicalException("Secret " + secret.getId() + " not found in environment " + secret.getEnvironmentId());
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(secret, secret.getId()));
            return findById(secret.getOrganizationId(), secret.getEnvironmentId(), secret.getId()).orElseThrow();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to update secret " + secret.getId(), ex);
        }
    }

    @Override
    public void delete(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("JdbcSecretRepository.delete({}, {}, {})", organizationId, environmentId, id);
        try {
            jdbcTemplate.update(
                "DELETE FROM " + this.tableName + " WHERE id = ? AND organization_id = ? AND environment_id = ?",
                id,
                organizationId,
                environmentId
            );
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete secret " + id, ex);
        }
    }

    @Override
    public Page<Secret> search(String organizationId, String environmentId, Pageable pageable) throws TechnicalException {
        log.debug("JdbcSecretRepository.search({}, {})", organizationId, environmentId);
        try {
            Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + this.tableName + " WHERE organization_id = ? AND environment_id = ?",
                Long.class,
                organizationId,
                environmentId
            );
            if (total == null || total == 0) {
                return new Page<>(List.of(), pageable.pageNumber(), 0, 0);
            }
            var content = jdbcTemplate.query(
                getOrm().getSelectAllSql() +
                    " WHERE organization_id = ? AND environment_id = ? ORDER BY " +
                    escapeReservedWord("key") +
                    " " +
                    createPagingClause(pageable.pageSize(), pageable.from()),
                getOrm().getRowMapper(),
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
            jdbcTemplate.update("DELETE FROM " + this.tableName + " WHERE environment_id = ?", environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete secrets of environment " + environmentId, ex);
        }
    }
}
