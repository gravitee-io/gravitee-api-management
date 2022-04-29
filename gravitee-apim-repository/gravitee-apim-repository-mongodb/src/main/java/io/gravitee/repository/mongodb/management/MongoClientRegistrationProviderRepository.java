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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import io.gravitee.repository.mongodb.management.internal.application.ClientRegistrationProviderMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ClientRegistrationProviderMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoClientRegistrationProviderRepository implements ClientRegistrationProviderRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoClientRegistrationProviderRepository.class);

    @Autowired
    private ClientRegistrationProviderMongoRepository internalClientRegistrationProviderRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<ClientRegistrationProvider> findById(String id) throws TechnicalException {
        LOGGER.debug("Find client registration provider by ID [{}]", id);

        ClientRegistrationProviderMongo clientRegistrationProvider = internalClientRegistrationProviderRepository.findById(id).orElse(null);

        LOGGER.debug("Find client registration provider by ID [{}] - Done", id);
        return Optional.ofNullable(map(clientRegistrationProvider));
    }

    @Override
    public ClientRegistrationProvider create(ClientRegistrationProvider clientRegistrationProvider) throws TechnicalException {
        LOGGER.debug("Create client registration provider [{}]", clientRegistrationProvider.getName());

        ClientRegistrationProviderMongo identityProviderMongo = map(clientRegistrationProvider);
        ClientRegistrationProviderMongo createdIdentityProviderMongo = internalClientRegistrationProviderRepository.insert(
            identityProviderMongo
        );

        LOGGER.debug("Create client registration provider [{}] - Done", clientRegistrationProvider.getName());

        return map(createdIdentityProviderMongo);
    }

    @Override
    public ClientRegistrationProvider update(ClientRegistrationProvider clientRegistrationProvider) throws TechnicalException {
        if (clientRegistrationProvider == null) {
            throw new IllegalStateException("Client registration provider must not be null");
        }

        ClientRegistrationProviderMongo clientRegistrationProviderMongo = internalClientRegistrationProviderRepository
            .findById(clientRegistrationProvider.getId())
            .orElse(null);
        if (clientRegistrationProviderMongo == null) {
            throw new IllegalStateException(
                String.format("No client registration provider found with id [%s]", clientRegistrationProvider.getId())
            );
        }

        try {
            clientRegistrationProviderMongo.setEnvironmentId(clientRegistrationProvider.getEnvironmentId());
            clientRegistrationProviderMongo.setName(clientRegistrationProvider.getName());
            clientRegistrationProviderMongo.setDescription(clientRegistrationProvider.getDescription());
            clientRegistrationProviderMongo.setUpdatedAt(clientRegistrationProvider.getUpdatedAt());
            clientRegistrationProviderMongo.setDiscoveryEndpoint(clientRegistrationProvider.getDiscoveryEndpoint());
            clientRegistrationProviderMongo.setInitialAccessTokenType(clientRegistrationProvider.getInitialAccessTokenType().name());
            clientRegistrationProviderMongo.setClientId(clientRegistrationProvider.getClientId());
            clientRegistrationProviderMongo.setClientSecret(clientRegistrationProvider.getClientSecret());
            clientRegistrationProviderMongo.setScopes(clientRegistrationProvider.getScopes());
            clientRegistrationProviderMongo.setInitialAccessToken(clientRegistrationProvider.getInitialAccessToken());
            clientRegistrationProviderMongo.setRenewClientSecretSupport(clientRegistrationProvider.isRenewClientSecretSupport());
            clientRegistrationProviderMongo.setRenewClientSecretMethod(clientRegistrationProvider.getRenewClientSecretMethod());
            clientRegistrationProviderMongo.setRenewClientSecretEndpoint(clientRegistrationProvider.getRenewClientSecretEndpoint());
            clientRegistrationProviderMongo.setSoftwareId(clientRegistrationProvider.getSoftwareId());

            ClientRegistrationProviderMongo clientRegistrationProviderMongoUpdated = internalClientRegistrationProviderRepository.save(
                clientRegistrationProviderMongo
            );
            return map(clientRegistrationProviderMongoUpdated);
        } catch (Exception e) {
            LOGGER.error("An error occurs when updating client registration provider", e);
            throw new TechnicalException("An error occurs when updating client registration provider");
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalClientRegistrationProviderRepository.deleteById(id);
        } catch (Exception e) {
            LOGGER.error("An error occurs when deleting client registration provider [{}]", id, e);
            throw new TechnicalException("An error occurs when deleting client registration provider");
        }
    }

    @Override
    public Set<ClientRegistrationProvider> findAll() throws TechnicalException {
        LOGGER.debug("Find all client registration providers");

        List<ClientRegistrationProviderMongo> clientRegistrationProviders = internalClientRegistrationProviderRepository.findAll();
        Set<ClientRegistrationProvider> res = mapper.collection2set(
            clientRegistrationProviders,
            ClientRegistrationProviderMongo.class,
            ClientRegistrationProvider.class
        );

        LOGGER.debug("Find all client registration providers - Done");
        return res;
    }

    private ClientRegistrationProvider map(ClientRegistrationProviderMongo provider) {
        return (provider == null) ? null : mapper.map(provider, ClientRegistrationProvider.class);
    }

    private ClientRegistrationProviderMongo map(ClientRegistrationProvider provider) {
        return (provider == null) ? null : mapper.map(provider, ClientRegistrationProviderMongo.class);
    }
}
