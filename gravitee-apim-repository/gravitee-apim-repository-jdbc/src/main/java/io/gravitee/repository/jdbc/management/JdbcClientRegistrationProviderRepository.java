/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import java.sql.Types;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public class JdbcClientRegistrationProviderRepository
    extends JdbcAbstractCrudRepository<ClientRegistrationProvider, String>
    implements ClientRegistrationProviderRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcClientRegistrationProviderRepository.class);

    private final String CLIENT_REGISTRATION_PROVIDER_SCOPES;

    JdbcClientRegistrationProviderRepository(@Value("${management.jdbc.prefix:}") String tablePrefix) {
        super(tablePrefix, "client_registration_providers");
        CLIENT_REGISTRATION_PROVIDER_SCOPES = getTableNameFor("client_registration_provider_scopes");
    }

    @Override
    protected JdbcObjectMapper<ClientRegistrationProvider> buildOrm() {
        return JdbcObjectMapper
            .builder(ClientRegistrationProvider.class, this.tableName, "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("discovery_endpoint", Types.NVARCHAR, String.class)
            .addColumn("initial_access_token_type", Types.NVARCHAR, ClientRegistrationProvider.InitialAccessTokenType.class)
            .addColumn("client_id", Types.NVARCHAR, String.class)
            .addColumn("client_secret", Types.NVARCHAR, String.class)
            .addColumn("initial_access_token", Types.NVARCHAR, String.class)
            .addColumn("renew_client_secret_support", Types.BOOLEAN, boolean.class)
            .addColumn("renew_client_secret_endpoint", Types.NVARCHAR, String.class)
            .addColumn("renew_client_secret_method", Types.NVARCHAR, String.class)
            .addColumn("software_id", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .build();
    }

    @Override
    protected String getId(ClientRegistrationProvider item) {
        return item.getId();
    }

    private void addScopes(ClientRegistrationProvider parent) {
        List<String> groups = getScopes(parent.getId());
        parent.setScopes(new ArrayList<>(groups));
    }

    private List<String> getScopes(String id) {
        return jdbcTemplate.queryForList(
            "select scope from " + CLIENT_REGISTRATION_PROVIDER_SCOPES + " where client_registration_provider_id = ?",
            String.class,
            id
        );
    }

    private void storeScopes(ClientRegistrationProvider clientRegistrationProvider, boolean deleteFirst) {
        if (deleteFirst) {
            jdbcTemplate.update(
                "delete from " + CLIENT_REGISTRATION_PROVIDER_SCOPES + " where client_registration_provider_id = ?",
                clientRegistrationProvider.getId()
            );
        }
        List<String> filteredScopes = getOrm().filterStrings(clientRegistrationProvider.getScopes());
        if (!filteredScopes.isEmpty()) {
            jdbcTemplate.batchUpdate(
                "insert into " + CLIENT_REGISTRATION_PROVIDER_SCOPES + " ( client_registration_provider_id, scope ) values ( ?, ? )",
                getOrm().getBatchStringSetter(clientRegistrationProvider.getId(), filteredScopes)
            );
        }
    }

    @Override
    public Optional<ClientRegistrationProvider> findById(String id) throws TechnicalException {
        final Optional<ClientRegistrationProvider> result = super.findById(id);
        result.ifPresent(this::addScopes);
        return result;
    }

    @Override
    public Set<ClientRegistrationProvider> findAll() throws TechnicalException {
        LOGGER.debug("JdbcClientRegistrationProviderRepository.findAll()");

        Set<ClientRegistrationProvider> providers = super.findAll();
        for (ClientRegistrationProvider provider : providers) {
            addScopes(provider);
        }
        LOGGER.debug("Found {} client registration providers: {}", providers.size(), providers);
        return providers;
    }

    @Override
    public ClientRegistrationProvider create(ClientRegistrationProvider item) throws TechnicalException {
        LOGGER.debug("JdbcClientRegistrationProviderRepository.create({})", item);
        try {
            jdbcTemplate.update(getOrm().buildInsertPreparedStatementCreator(item));
            storeScopes(item, false);
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create client registration provider", ex);
            throw new TechnicalException("Failed to create client registration provider", ex);
        }
    }

    @Override
    public ClientRegistrationProvider update(final ClientRegistrationProvider clientRegistrationProvider) throws TechnicalException {
        LOGGER.debug("JdbcClientRegistrationProviderRepository.update({})", clientRegistrationProvider);
        if (clientRegistrationProvider == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            jdbcTemplate.update(
                getOrm().buildUpdatePreparedStatementCreator(clientRegistrationProvider, clientRegistrationProvider.getId())
            );
            storeScopes(clientRegistrationProvider, true);
            return findById(clientRegistrationProvider.getId())
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            format("No client registration provider found with id [%s]", clientRegistrationProvider.getId())
                        )
                );
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update client registration provider", ex);
            throw new TechnicalException("Failed to update client registration provider", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        jdbcTemplate.update("delete from " + CLIENT_REGISTRATION_PROVIDER_SCOPES + " where client_registration_provider_id = ?", id);
        jdbcTemplate.update(getOrm().getDeleteSql(), id);
    }
}
