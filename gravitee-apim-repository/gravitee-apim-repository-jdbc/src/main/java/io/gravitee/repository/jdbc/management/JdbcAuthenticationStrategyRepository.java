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

import static java.lang.String.format;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.AuthenticationStrategyRepository;
import io.gravitee.repository.management.model.AuthenticationStrategy;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@CustomLog
@Repository
public class JdbcAuthenticationStrategyRepository
    extends JdbcAbstractCrudRepository<AuthenticationStrategy, String>
    implements AuthenticationStrategyRepository {

    private final String AUTH_STRATEGY_SCOPES;
    private final String AUTH_STRATEGY_AUTH_METHODS;

    JdbcAuthenticationStrategyRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "authentication_strategies");
        AUTH_STRATEGY_SCOPES = getTableNameFor("authentication_strategy_scopes");
        AUTH_STRATEGY_AUTH_METHODS = getTableNameFor("authentication_strategy_auth_methods");
    }

    @Override
    protected JdbcObjectMapper<AuthenticationStrategy> buildOrm() {
        return JdbcObjectMapper.builder(AuthenticationStrategy.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("display_name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("type", Types.NVARCHAR, AuthenticationStrategy.Type.class)
            .addColumn("client_registration_provider_id", Types.NVARCHAR, String.class)
            .addColumn("credential_claims", Types.NVARCHAR, String.class)
            .addColumn("auto_approve", Types.BOOLEAN, boolean.class)
            .addColumn("hide_credentials", Types.BOOLEAN, boolean.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(AuthenticationStrategy item) {
        return item.getId();
    }

    private void addScopes(AuthenticationStrategy item) {
        List<String> scopes = jdbcTemplate.queryForList(
            "select scope from " + AUTH_STRATEGY_SCOPES + " where authentication_strategy_id = ?",
            String.class,
            item.getId()
        );
        item.setScopes(new ArrayList<>(scopes));
    }

    private void addAuthMethods(AuthenticationStrategy item) {
        List<String> methods = jdbcTemplate.queryForList(
            "select auth_method from " + AUTH_STRATEGY_AUTH_METHODS + " where authentication_strategy_id = ?",
            String.class,
            item.getId()
        );
        item.setAuthMethods(new ArrayList<>(methods));
    }

    private void addCollections(AuthenticationStrategy item) {
        addScopes(item);
        addAuthMethods(item);
    }

    private void storeScopes(AuthenticationStrategy item, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update(
                "delete from " + AUTH_STRATEGY_SCOPES + " where authentication_strategy_id = ?",
                item.getId()
            );
        }
        List<String> filtered = getOrm().filterStrings(item.getScopes());
        if (!filtered.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + AUTH_STRATEGY_SCOPES + " ( authentication_strategy_id, scope ) values ( ?, ? )",
                getOrm().getBatchStringSetter(item.getId(), filtered)
            );
        }
    }

    private void storeAuthMethods(AuthenticationStrategy item, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update(
                "delete from " + AUTH_STRATEGY_AUTH_METHODS + " where authentication_strategy_id = ?",
                item.getId()
            );
        }
        List<String> filtered = getOrm().filterStrings(item.getAuthMethods());
        if (!filtered.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + AUTH_STRATEGY_AUTH_METHODS + " ( authentication_strategy_id, auth_method ) values ( ?, ? )",
                getOrm().getBatchStringSetter(item.getId(), filtered)
            );
        }
    }

    private void storeCollections(AuthenticationStrategy item, boolean deleteFirst) {
        storeScopes(item, deleteFirst);
        storeAuthMethods(item, deleteFirst);
    }

    @Override
    public Optional<AuthenticationStrategy> findById(String id) throws TechnicalException {
        final Optional<AuthenticationStrategy> result = super.findById(id);
        result.ifPresent(this::addCollections);
        return result;
    }

    @Override
    public Set<AuthenticationStrategy> findAll() throws TechnicalException {
        log.debug("JdbcAuthenticationStrategyRepository.findAll()");
        Set<AuthenticationStrategy> strategies = super.findAll();
        for (AuthenticationStrategy s : strategies) {
            addCollections(s);
        }
        return strategies;
    }

    @Override
    public Set<AuthenticationStrategy> findAllByEnvironment(String environmentId) throws TechnicalException {
        log.debug("JdbcAuthenticationStrategyRepository.findAllByEnvironment({})", environmentId);
        try {
            Set<AuthenticationStrategy> strategies = new HashSet<>(
                jdbcTemplate.query(getOrm().getSelectAllSql() + " where environment_id = ?", getOrm().getRowMapper(), environmentId)
            );
            for (AuthenticationStrategy s : strategies) {
                addCollections(s);
            }
            return strategies;
        } catch (final Exception ex) {
            log.error("Failed to find authentication strategies by environment:", ex);
            throw new TechnicalException("Failed to find authentication strategies by environment", ex);
        }
    }

    @Override
    public Set<AuthenticationStrategy> findByClientRegistrationProviderId(String clientRegistrationProviderId) throws TechnicalException {
        log.debug("JdbcAuthenticationStrategyRepository.findByClientRegistrationProviderId({})", clientRegistrationProviderId);
        try {
            Set<AuthenticationStrategy> strategies = new HashSet<>(
                jdbcTemplate.query(
                    getOrm().getSelectAllSql() + " where client_registration_provider_id = ?",
                    getOrm().getRowMapper(),
                    clientRegistrationProviderId
                )
            );
            for (AuthenticationStrategy s : strategies) {
                addCollections(s);
            }
            return strategies;
        } catch (final Exception ex) {
            log.error("Failed to find authentication strategies by provider:", ex);
            throw new TechnicalException("Failed to find authentication strategies by provider", ex);
        }
    }

    @Override
    public AuthenticationStrategy create(AuthenticationStrategy item) throws TechnicalException {
        log.debug("JdbcAuthenticationStrategyRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            storeCollections(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            log.error("Failed to create authentication strategy", ex);
            throw new TechnicalException("Failed to create authentication strategy", ex);
        }
    }

    @Override
    public AuthenticationStrategy update(AuthenticationStrategy item) throws TechnicalException {
        log.debug("JdbcAuthenticationStrategyRepository.update({})", item);
        if (item == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(getOrm().buildUpdatePreparedStatementCreator(item, item.getId()));
            storeCollections(item, true);
            return findById(item.getId()).orElseThrow(() ->
                new IllegalStateException(format("No authentication strategy found with id [%s]", item.getId()))
            );
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            log.error("Failed to update authentication strategy", ex);
            throw new TechnicalException("Failed to update authentication strategy", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + AUTH_STRATEGY_SCOPES + " where authentication_strategy_id = ?", id);
        jdbcTemplate.update("delete from " + AUTH_STRATEGY_AUTH_METHODS + " where authentication_strategy_id = ?", id);
        jdbcTemplate.update(getOrm().getDeleteSql(), id);
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("JdbcAuthenticationStrategyRepository.deleteByEnvironmentId({})", environmentId);
        try {
            List<String> ids = jdbcTemplate.queryForList(
                "select id from " + tableName + " where environment_id = ?",
                String.class,
                environmentId
            );

            if (!ids.isEmpty()) {
                String inClause = getOrm().buildInClause(ids);
                jdbcTemplate.update(
                    "delete from " + AUTH_STRATEGY_SCOPES + " where authentication_strategy_id IN (" + inClause + ")",
                    ids.toArray()
                );
                jdbcTemplate.update(
                    "delete from " + AUTH_STRATEGY_AUTH_METHODS + " where authentication_strategy_id IN (" + inClause + ")",
                    ids.toArray()
                );
                jdbcTemplate.update("delete from " + tableName + " where environment_id = ?", environmentId);
            }

            return ids;
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete authentication strategies by environment", ex);
        }
    }
}
