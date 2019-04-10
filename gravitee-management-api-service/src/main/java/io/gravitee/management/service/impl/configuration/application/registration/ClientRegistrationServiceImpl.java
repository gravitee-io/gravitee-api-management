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
package io.gravitee.management.service.impl.configuration.application.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.management.model.NewApplicationEntity;
import io.gravitee.management.model.UpdateApplicationEntity;
import io.gravitee.management.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.management.model.configuration.application.registration.NewClientRegistrationProviderEntity;
import io.gravitee.management.model.configuration.application.registration.UpdateClientRegistrationProviderEntity;
import io.gravitee.management.service.AuditService;
import io.gravitee.management.service.configuration.application.ClientRegistrationService;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.impl.AbstractService;
import io.gravitee.management.service.impl.configuration.application.registration.client.DiscoveryBasedDynamicClientRegistrationProviderClient;
import io.gravitee.management.service.impl.configuration.application.registration.client.DynamicClientRegistrationProviderClient;
import io.gravitee.management.service.impl.configuration.application.registration.client.OIDCClient;
import io.gravitee.management.service.impl.configuration.application.registration.client.register.ClientRegistrationRequest;
import io.gravitee.management.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Audit.AuditProperties.CLIENT_REGISTRATION_PROVIDER;
import static java.util.Collections.singletonMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ClientRegistrationServiceImpl extends AbstractService implements ClientRegistrationService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(ClientRegistrationServiceImpl.class);

    @Autowired
    private ClientRegistrationProviderRepository clientRegistrationProviderRepository;

    @Autowired
    private AuditService auditService;

    private final ObjectMapper mapper = new ObjectMapper();

    private final Cache<String, DynamicClientRegistrationProviderClient> clients = CacheBuilder
            .newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    public Set<ClientRegistrationProviderEntity> findAll() {
        try {
            return clientRegistrationProviderRepository
                    .findAll()
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to retrieve client registration providers", ex);
            throw new TechnicalManagementException(
                    "An error occurs while trying to retrieve client registration providers", ex);
        }
    }

    @Override
    public ClientRegistrationProviderEntity create(NewClientRegistrationProviderEntity newClientRegistrationProvider) {
        try {
            LOGGER.debug("Create client registration provider {}", newClientRegistrationProvider);

            Set<ClientRegistrationProviderEntity> clientRegistrationProviders = this.findAll();
            // For now, we are supporting only a single client registration provider.
            if (clientRegistrationProviders.size() == 1) {
                throw new IllegalStateException("Until now, supports only a single client registration provider. " +
                        "Please update the existing one: " + clientRegistrationProviders.iterator().next().getName());
            }

            Optional<ClientRegistrationProvider> optClientRegistrationProvider = clientRegistrationProviderRepository.findById(
                    IdGenerator.generate(newClientRegistrationProvider.getName()));
            if (optClientRegistrationProvider.isPresent()) {
                throw new ClientRegistrationProviderAlreadyExistsException(newClientRegistrationProvider.getName());
            }

            ClientRegistrationProvider clientRegistrationProvider = convert(newClientRegistrationProvider);

            DynamicClientRegistrationProviderClient registrationProviderClient = getDCRClient(
                    true,
                    clientRegistrationProvider.getId(), clientRegistrationProvider.getDiscoveryEndpoint(),
                    clientRegistrationProvider.getClientId(), clientRegistrationProvider.getClientSecret(),
                    clientRegistrationProvider.getScopes());

            // Ensure that the client credentials are valid
            registrationProviderClient.generateToken();

            LOGGER.debug("Found a DCR Client for provider: {}", clientRegistrationProvider.getName(), registrationProviderClient);

            // Set date fields
            clientRegistrationProvider.setCreatedAt(new Date());
            clientRegistrationProvider.setUpdatedAt(clientRegistrationProvider.getCreatedAt());

            ClientRegistrationProvider createdClientRegistrationProvider = clientRegistrationProviderRepository.create(clientRegistrationProvider);

            auditService.createPortalAuditLog(
                    singletonMap(CLIENT_REGISTRATION_PROVIDER, createdClientRegistrationProvider.getId()),
                    ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_CREATED,
                    createdClientRegistrationProvider.getUpdatedAt(),
                    null,
                    createdClientRegistrationProvider);

            return convert(createdClientRegistrationProvider);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create client registration provider {}", newClientRegistrationProvider, ex);
            throw new TechnicalManagementException("An error occurs while trying to create " + newClientRegistrationProvider, ex);
        }
    }

    @Override
    public ClientRegistrationProviderEntity update(String id, UpdateClientRegistrationProviderEntity updateClientRegistrationProvider) {
        try {
            LOGGER.debug("Update client registration provider {}", updateClientRegistrationProvider);

            Optional<ClientRegistrationProvider> optClientRegistrationProvider = clientRegistrationProviderRepository.findById(id);

            if (!optClientRegistrationProvider.isPresent()) {
                throw new ClientRegistrationProviderNotFoundException(updateClientRegistrationProvider.getName());
            }

            ClientRegistrationProvider clientRegistrationProvider = convert(updateClientRegistrationProvider);

            DynamicClientRegistrationProviderClient registrationProviderClient = getDCRClient(
                    true,
                    clientRegistrationProvider.getId(), clientRegistrationProvider.getDiscoveryEndpoint(),
                    clientRegistrationProvider.getClientId(), clientRegistrationProvider.getClientSecret(),
                    clientRegistrationProvider.getScopes());

            // Ensure that the client credentials are valid
            registrationProviderClient.generateToken();

            LOGGER.debug("Found a DCR Client for provider: {}", clientRegistrationProvider.getName(), registrationProviderClient);

            final ClientRegistrationProvider clientProviderToUpdate = optClientRegistrationProvider.get();
            clientRegistrationProvider.setId(id);
            clientRegistrationProvider.setCreatedAt(clientProviderToUpdate.getCreatedAt());
            clientRegistrationProvider.setUpdatedAt(new Date());

            ClientRegistrationProvider updatedClientRegistrationProvider = clientRegistrationProviderRepository.update(clientRegistrationProvider);

            // Audit
            auditService.createPortalAuditLog(
                    singletonMap(CLIENT_REGISTRATION_PROVIDER, id),
                    ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_CREATED,
                    clientRegistrationProvider.getUpdatedAt(),
                    clientProviderToUpdate,
                    updatedClientRegistrationProvider);

            return convert(updatedClientRegistrationProvider);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update client registration provider {}", updateClientRegistrationProvider, ex);
            throw new TechnicalManagementException("An error occurs while trying to update " + updateClientRegistrationProvider, ex);
        }
    }

    @Override
    public ClientRegistrationProviderEntity findById(String id) {
        try {
            LOGGER.debug("Find client registration provider by ID: {}", id);

            Optional<ClientRegistrationProvider> clientRegistrationProvider = clientRegistrationProviderRepository.findById(id);

            if (clientRegistrationProvider.isPresent()) {
                return convert(clientRegistrationProvider.get());
            }

            throw new ClientRegistrationProviderNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a client registration provider using its ID {}", id, ex);
            throw new TechnicalManagementException(
                    "An error occurs while trying to delete a client registration provider using its ID " + id, ex);
        }
    }

    @Override
    public void delete(String id) {
        try {
            LOGGER.debug("Delete client registration provider: {}", id);

            Optional<ClientRegistrationProvider> clientRegistrationProvider = clientRegistrationProviderRepository.findById(id);

            if (!clientRegistrationProvider.isPresent()) {
                throw new ClientRegistrationProviderNotFoundException(id);
            }

            clientRegistrationProviderRepository.delete(id);

            auditService.createPortalAuditLog(
                    Collections.singletonMap(CLIENT_REGISTRATION_PROVIDER, id),
                    ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_DELETED,
                    new Date(),
                    clientRegistrationProvider.get(),
                    null);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete a client registration provider using its ID {}", id, ex);
            throw new TechnicalManagementException(
                    "An error occurs while trying to delete a client registration provider using its ID " + id, ex);
        }
    }

    @Override
    public ClientRegistrationResponse register(NewApplicationEntity application) {
        // Create an OAuth client
        Set<ClientRegistrationProviderEntity> providers = findAll();
        if (providers == null || providers.isEmpty()) {
            throw new MissingDynamicClientRegistrationProviderException();
        }

        // For now, took the first provider
        ClientRegistrationProviderEntity provider = providers.iterator().next();

        // Get provider client
        DynamicClientRegistrationProviderClient registrationProviderClient = getDCRClient(false,
                provider.getId(), provider.getDiscoveryEndpoint(), provider.getClientId(), provider.getClientSecret(),
                provider.getScopes());

        return registrationProviderClient.register(convert(application));
    }

    private DynamicClientRegistrationProviderClient getDCRClient(
            final boolean forceRefresh,
            final String providerId, final String discoveryEndpoint,
            final String clientId, final String clientSecret, final List<String> scopes) {
        // Get provider client
        try {
            if (forceRefresh) {
                OIDCClient client = new OIDCClient(clientId, clientSecret);
                client.setScopes(scopes);
                DiscoveryBasedDynamicClientRegistrationProviderClient registrationProviderClient =
                        new DiscoveryBasedDynamicClientRegistrationProviderClient(client, discoveryEndpoint);

                // Provider ID may be null when we are trying to test a client registration provider
                if (providerId != null) {
                    clients.put(providerId, registrationProviderClient);
                }

                return registrationProviderClient;
            } else {
                return clients.get(providerId, () -> {
                    OIDCClient client = new OIDCClient(clientId, clientSecret);
                    return new DiscoveryBasedDynamicClientRegistrationProviderClient(client, discoveryEndpoint);
                });
            }
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while getting a dynamic client registration client", ex);
            throw new InvalidClientRegistrationProviderException();
        }
    }

    @Override
    public ClientRegistrationResponse update(String previousRegistrationResponse, UpdateApplicationEntity application) {
        try {
            ClientRegistrationResponse registrationResponse = mapper.readValue(
                    previousRegistrationResponse, ClientRegistrationResponse.class);

            if (registrationResponse.getRegistrationAccessToken() == null
                    || registrationResponse.getRegistrationAccessToken().isEmpty()
                    || registrationResponse.getRegistrationClientUri() == null
                    || registrationResponse.getRegistrationClientUri().isEmpty()) {
                throw new RegisteredClientNotUpdatableException();
            }
            // Update an OAuth client
            Set<ClientRegistrationProviderEntity> providers = findAll();
            if (providers == null || providers.isEmpty()) {
                throw new MissingDynamicClientRegistrationProviderException();
            }

            // For now, took the first provider
            ClientRegistrationProviderEntity provider = providers.iterator().next();

            // Get provider client
            DynamicClientRegistrationProviderClient registrationProviderClient = getDCRClient(false,
                    provider.getId(), provider.getDiscoveryEndpoint(), provider.getClientId(), provider.getClientSecret(),
                    provider.getScopes());

            ClientRegistrationRequest registrationRequest = mapper.readValue(
                    previousRegistrationResponse, ClientRegistrationRequest.class);

            return registrationProviderClient.update(
                    registrationResponse.getRegistrationAccessToken(),
                    registrationResponse.getRegistrationClientUri(),
                    convert(registrationRequest, application));
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while updating a client", ex);
            return null;
        }
    }

    private ClientRegistrationRequest convert(ClientRegistrationRequest request, UpdateApplicationEntity application) {
        request.setClientName(application.getName());
        request.setApplicationType(application.getSettings().getoAuthClient().getApplicationType());
        request.setClientUri(application.getSettings().getoAuthClient().getClientUri());
        request.setGrantTypes(application.getSettings().getoAuthClient().getGrantTypes());
        request.setLogoUri(application.getSettings().getoAuthClient().getLogoUri());
        request.setRedirectUris(application.getSettings().getoAuthClient().getRedirectUris());
        request.setResponseTypes(application.getSettings().getoAuthClient().getResponseTypes());

        return request;
    }

    private ClientRegistrationRequest convert(NewApplicationEntity application) {
        ClientRegistrationRequest request = new ClientRegistrationRequest();

        request.setClientName(application.getName());
        request.setApplicationType(application.getSettings().getoAuthClient().getApplicationType());
        request.setClientUri(application.getSettings().getoAuthClient().getClientUri());
        request.setGrantTypes(application.getSettings().getoAuthClient().getGrantTypes());
        request.setLogoUri(application.getSettings().getoAuthClient().getLogoUri());
        request.setRedirectUris(application.getSettings().getoAuthClient().getRedirectUris());
        request.setResponseTypes(application.getSettings().getoAuthClient().getResponseTypes());

        return request;
    }

    private ClientRegistrationProviderEntity convert(ClientRegistrationProvider clientRegistrationProvider) {
        ClientRegistrationProviderEntity entity = new ClientRegistrationProviderEntity();

        entity.setId(clientRegistrationProvider.getId());
        entity.setName(clientRegistrationProvider.getName());
        entity.setDescription(clientRegistrationProvider.getDescription());
        entity.setDiscoveryEndpoint(clientRegistrationProvider.getDiscoveryEndpoint());
        entity.setClientId(clientRegistrationProvider.getClientId());
        entity.setClientSecret(clientRegistrationProvider.getClientSecret());
        entity.setScopes(clientRegistrationProvider.getScopes());
        entity.setCreatedAt(clientRegistrationProvider.getCreatedAt());
        entity.setUpdatedAt(clientRegistrationProvider.getUpdatedAt());

        return entity;
    }

    private ClientRegistrationProvider convert(NewClientRegistrationProviderEntity newClientRegistrationProvider) {
        ClientRegistrationProvider provider = new ClientRegistrationProvider();

        provider.setName(newClientRegistrationProvider.getName());
        provider.setDescription(newClientRegistrationProvider.getDescription());
        provider.setDiscoveryEndpoint(newClientRegistrationProvider.getDiscoveryEndpoint());
        provider.setClientId(newClientRegistrationProvider.getClientId());
        provider.setClientSecret(newClientRegistrationProvider.getClientSecret());
        provider.setScopes(newClientRegistrationProvider.getScopes());

        return provider;
    }

    private ClientRegistrationProvider convert(UpdateClientRegistrationProviderEntity updateClientRegistrationProvider) {
        ClientRegistrationProvider provider = new ClientRegistrationProvider();

        provider.setId(updateClientRegistrationProvider.getClientId());
        provider.setName(updateClientRegistrationProvider.getName());
        provider.setDescription(updateClientRegistrationProvider.getDescription());
        provider.setDiscoveryEndpoint(updateClientRegistrationProvider.getDiscoveryEndpoint());
        provider.setClientId(updateClientRegistrationProvider.getClientId());
        provider.setClientSecret(updateClientRegistrationProvider.getClientSecret());
        provider.setScopes(updateClientRegistrationProvider.getScopes());

        return provider;
    }
}
