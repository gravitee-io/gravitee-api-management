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
package io.gravitee.rest.api.service.impl.configuration.application.registration;

import static io.gravitee.repository.management.model.Audit.AuditProperties.CLIENT_REGISTRATION_PROVIDER;
import static java.util.Collections.singletonMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.utils.UUID;
import io.gravitee.el.TemplateEngine;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ClientRegistrationProviderRepository;
import io.gravitee.repository.management.model.ClientRegistrationProvider;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.InitialAccessTokenType;
import io.gravitee.rest.api.model.configuration.application.registration.NewClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.configuration.application.registration.UpdateClientRegistrationProviderEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.DiscoveryBasedDynamicClientRegistrationProviderClient;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.DynamicClientRegistrationProviderClient;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationRequest;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.token.ClientCredentialsInitialAccessTokenProvider;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.token.InitialAccessTokenProvider;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.token.PlainInitialAccessTokenProvider;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ClientRegistrationServiceImpl extends AbstractService implements ClientRegistrationService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientRegistrationServiceImpl.class);

    @Lazy
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
    public Set<ClientRegistrationProviderEntity> findAll(ExecutionContext executionContext) {
        try {
            return clientRegistrationProviderRepository
                .findAllByEnvironment(executionContext.getEnvironmentId())
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to retrieve client registration providers", ex);
            throw new TechnicalManagementException("An error occurs while trying to retrieve client registration providers", ex);
        }
    }

    @Override
    public ClientRegistrationProviderEntity create(
        ExecutionContext executionContext,
        NewClientRegistrationProviderEntity newClientRegistrationProvider
    ) {
        try {
            LOGGER.debug("Create client registration provider {}", newClientRegistrationProvider);

            Set<ClientRegistrationProviderEntity> clientRegistrationProviders = this.findAll(executionContext);
            // For now, we are supporting only a single client registration provider.
            if (clientRegistrationProviders.size() == 1) {
                throw new IllegalStateException(
                    "Until now, supports only a single client registration provider. " +
                    "Please update the existing one: " +
                    clientRegistrationProviders.iterator().next().getName()
                );
            }

            if (
                newClientRegistrationProvider.getInitialAccessTokenType() == InitialAccessTokenType.INITIAL_ACCESS_TOKEN &&
                (
                    newClientRegistrationProvider.getInitialAccessToken() == null ||
                    newClientRegistrationProvider.getInitialAccessToken().isEmpty()
                )
            ) {
                throw new EmptyInitialAccessTokenException();
            }

            ClientRegistrationProvider clientRegistrationProvider = convert(newClientRegistrationProvider);

            // Check renew_client_secret configuration
            renewClientSecretSupport(clientRegistrationProvider);

            clientRegistrationProvider.setId(UuidString.generateRandom());
            clientRegistrationProvider.setEnvironmentId(executionContext.getEnvironmentId());

            DynamicClientRegistrationProviderClient registrationProviderClient = getDCRClient(true, convert(clientRegistrationProvider));

            // Ensure that the client credentials are valid
            registrationProviderClient.getInitialAccessToken();

            LOGGER.debug("Found a DCR Client for provider: {}", clientRegistrationProvider.getName(), registrationProviderClient);

            // Set date fields
            clientRegistrationProvider.setCreatedAt(new Date());
            clientRegistrationProvider.setUpdatedAt(clientRegistrationProvider.getCreatedAt());

            ClientRegistrationProvider createdClientRegistrationProvider = clientRegistrationProviderRepository.create(
                clientRegistrationProvider
            );

            auditService.createAuditLog(
                executionContext,
                singletonMap(CLIENT_REGISTRATION_PROVIDER, createdClientRegistrationProvider.getId()),
                ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_CREATED,
                createdClientRegistrationProvider.getUpdatedAt(),
                null,
                createdClientRegistrationProvider
            );

            return convert(createdClientRegistrationProvider);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create client registration provider {}", newClientRegistrationProvider, ex);
            throw new TechnicalManagementException("An error occurs while trying to create " + newClientRegistrationProvider, ex);
        }
    }

    @Override
    public ClientRegistrationProviderEntity update(
        ExecutionContext executionContext,
        String id,
        UpdateClientRegistrationProviderEntity updateClientRegistrationProvider
    ) {
        try {
            LOGGER.debug("Update client registration provider {}", updateClientRegistrationProvider);

            Optional<ClientRegistrationProvider> optClientRegistrationProvider = clientRegistrationProviderRepository
                .findById(id)
                .filter(crp -> crp.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()));

            if (!optClientRegistrationProvider.isPresent()) {
                throw new ClientRegistrationProviderNotFoundException(updateClientRegistrationProvider.getName());
            }

            if (
                updateClientRegistrationProvider.getInitialAccessTokenType() == InitialAccessTokenType.INITIAL_ACCESS_TOKEN &&
                (
                    updateClientRegistrationProvider.getInitialAccessToken() == null ||
                    updateClientRegistrationProvider.getInitialAccessToken().isEmpty()
                )
            ) {
                throw new EmptyInitialAccessTokenException();
            }

            ClientRegistrationProvider clientRegistrationProvider = convert(updateClientRegistrationProvider);

            // Check renew_client_secret configuration
            renewClientSecretSupport(clientRegistrationProvider);

            clientRegistrationProvider.setId(id);
            clientRegistrationProvider.setEnvironmentId(executionContext.getEnvironmentId());

            DynamicClientRegistrationProviderClient registrationProviderClient = getDCRClient(true, convert(clientRegistrationProvider));

            // Ensure that the client credentials are valid
            registrationProviderClient.getInitialAccessToken();

            LOGGER.debug("Found a DCR Client for provider: {}", clientRegistrationProvider.getName(), registrationProviderClient);

            final ClientRegistrationProvider clientProviderToUpdate = optClientRegistrationProvider.get();
            clientRegistrationProvider.setId(id);
            clientRegistrationProvider.setCreatedAt(clientProviderToUpdate.getCreatedAt());
            clientRegistrationProvider.setUpdatedAt(new Date());

            ClientRegistrationProvider updatedClientRegistrationProvider = clientRegistrationProviderRepository.update(
                clientRegistrationProvider
            );

            // Audit
            auditService.createAuditLog(
                executionContext,
                singletonMap(CLIENT_REGISTRATION_PROVIDER, id),
                ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_UPDATED,
                clientRegistrationProvider.getUpdatedAt(),
                clientProviderToUpdate,
                updatedClientRegistrationProvider
            );

            return convert(updatedClientRegistrationProvider);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update client registration provider {}", updateClientRegistrationProvider, ex);
            throw new TechnicalManagementException("An error occurs while trying to update " + updateClientRegistrationProvider, ex);
        }
    }

    private void renewClientSecretSupport(ClientRegistrationProvider provider) {
        if (provider.isRenewClientSecretSupport()) {
            HttpMethod method = HttpMethod.valueOf(provider.getRenewClientSecretMethod().toUpperCase());
            if (method != HttpMethod.POST && method != HttpMethod.PUT && method != HttpMethod.PATCH) {
                throw new InvalidRenewClientSecretException();
            }

            if (
                provider.getRenewClientSecretEndpoint() == null ||
                (
                    !provider.getRenewClientSecretEndpoint().startsWith("http://") &&
                    !provider.getRenewClientSecretEndpoint().startsWith("https://")
                )
            ) {
                throw new InvalidRenewClientSecretException();
            }
        }
    }

    @Override
    public ClientRegistrationProviderEntity findById(String environmentId, String id) {
        try {
            LOGGER.debug("Find client registration provider by ID: {}", id);

            Optional<ClientRegistrationProvider> clientRegistrationProvider = clientRegistrationProviderRepository
                .findById(id)
                .filter(crp -> crp.getEnvironmentId().equalsIgnoreCase(environmentId));

            if (clientRegistrationProvider.isPresent()) {
                return convert(clientRegistrationProvider.get());
            }

            throw new ClientRegistrationProviderNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a client registration provider using its ID {}", id, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to delete a client registration provider using its ID " + id,
                ex
            );
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String id) {
        try {
            LOGGER.debug("Delete client registration provider: {}", id);

            Optional<ClientRegistrationProvider> clientRegistrationProvider = clientRegistrationProviderRepository
                .findById(id)
                .filter(crp -> crp.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()));

            if (!clientRegistrationProvider.isPresent()) {
                throw new ClientRegistrationProviderNotFoundException(id);
            }

            clientRegistrationProviderRepository.delete(id);

            auditService.createAuditLog(
                executionContext,
                Collections.singletonMap(CLIENT_REGISTRATION_PROVIDER, id),
                ClientRegistrationProvider.AuditEvent.CLIENT_REGISTRATION_PROVIDER_DELETED,
                new Date(),
                clientRegistrationProvider.get(),
                null
            );
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete a client registration provider using its ID {}", id, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to delete a client registration provider using its ID " + id,
                ex
            );
        }
    }

    @Override
    public ClientRegistrationResponse register(ExecutionContext executionContext, NewApplicationEntity application) {
        // Create an OAuth client
        Set<ClientRegistrationProviderEntity> providers = findAll(executionContext);
        if (providers == null || providers.isEmpty()) {
            throw new MissingDynamicClientRegistrationProviderException();
        }

        // For now, took the first provider
        ClientRegistrationProviderEntity provider = providers.iterator().next();

        // Get provider client
        DynamicClientRegistrationProviderClient registrationProviderClient = getDCRClient(false, provider);

        ClientRegistrationRequest clientRegistrationRequest = convert(application);

        if (provider.getSoftwareId() != null && !provider.getSoftwareId().isEmpty()) {
            clientRegistrationRequest.setSoftwareId(provider.getSoftwareId());
        }

        return registrationProviderClient.register(clientRegistrationRequest);
    }

    private ClientRegistrationRequest convert(NewApplicationEntity application) {
        Map<String, String> metadata = application.getSettings().getOauth().getAdditionalClientMetadata();
        if (metadata != null) {
            ClientRegistrationRequest request = mapper.convertValue(metadata, ClientRegistrationRequest.class);

            return ClientRegistrationMapper.INSTANCE.toClientRegistrationRequest(request, application);
        }
        return ClientRegistrationMapper.INSTANCE.toClientRegistrationRequest(new ClientRegistrationRequest(), application);
    }

    private DynamicClientRegistrationProviderClient getDCRClient(
        final boolean forceRefresh,
        final ClientRegistrationProviderEntity clientRegistrationProvider
    ) {
        // Get provider client
        try {
            InitialAccessTokenProvider atProvider;

            if (clientRegistrationProvider.getInitialAccessTokenType() == InitialAccessTokenType.CLIENT_CREDENTIALS) {
                atProvider =
                    new ClientCredentialsInitialAccessTokenProvider(
                        clientRegistrationProvider.getClientId(),
                        clientRegistrationProvider.getClientSecret(),
                        clientRegistrationProvider.getScopes()
                    );
            } else {
                atProvider = new PlainInitialAccessTokenProvider(clientRegistrationProvider.getInitialAccessToken());
            }

            if (forceRefresh) {
                DiscoveryBasedDynamicClientRegistrationProviderClient registrationProviderClient =
                    new DiscoveryBasedDynamicClientRegistrationProviderClient(
                        clientRegistrationProvider.getDiscoveryEndpoint(),
                        atProvider
                    );

                // Provider ID may be null when we are trying to test a client registration provider
                if (clientRegistrationProvider.getId() != null) {
                    clients.put(clientRegistrationProvider.getId(), registrationProviderClient);
                }

                return registrationProviderClient;
            } else {
                return clients.get(
                    clientRegistrationProvider.getId(),
                    () ->
                        new DiscoveryBasedDynamicClientRegistrationProviderClient(
                            clientRegistrationProvider.getDiscoveryEndpoint(),
                            atProvider
                        )
                );
            }
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while getting a dynamic client registration client", ex);
            throw new InvalidClientRegistrationProviderException();
        }
    }

    @Override
    public ClientRegistrationResponse update(
        ExecutionContext executionContext,
        String previousRegistrationResponse,
        UpdateApplicationEntity application
    ) {
        try {
            ClientRegistrationResponse registrationResponse = mapper.readValue(
                previousRegistrationResponse,
                ClientRegistrationResponse.class
            );

            if (
                registrationResponse.getRegistrationAccessToken() == null ||
                registrationResponse.getRegistrationAccessToken().isEmpty() ||
                registrationResponse.getRegistrationClientUri() == null ||
                registrationResponse.getRegistrationClientUri().isEmpty()
            ) {
                throw new RegisteredClientNotUpdatableException();
            }
            // Update an OAuth client
            Set<ClientRegistrationProviderEntity> providers = findAll(executionContext);
            if (providers == null || providers.isEmpty()) {
                throw new MissingDynamicClientRegistrationProviderException();
            }

            // For now, took the first provider
            ClientRegistrationProviderEntity provider = providers.iterator().next();

            // Get provider client
            DynamicClientRegistrationProviderClient registrationProviderClient = getDCRClient(false, provider);

            ClientRegistrationRequest registrationRequest = mapper.readValue(previousRegistrationResponse, ClientRegistrationRequest.class);

            registrationRequest.setSoftwareId(provider.getSoftwareId());

            return registrationProviderClient.update(
                registrationResponse.getRegistrationAccessToken(),
                registrationResponse.getRegistrationClientUri(),
                convert(registrationRequest, application),
                application.getSettings().getOauth().getClientId()
            );
        } catch (JsonProcessingException ex) {
            LOGGER.error("Unexpected error while updating a client", ex);
            throw new RegisteredClientNotUpdatableException();
        }
    }

    @Override
    public ClientRegistrationResponse renewClientSecret(ExecutionContext executionContext, String previousRegistrationResponse) {
        try {
            ClientRegistrationResponse registrationResponse = mapper.readValue(
                previousRegistrationResponse,
                ClientRegistrationResponse.class
            );

            if (
                registrationResponse.getRegistrationAccessToken() == null ||
                registrationResponse.getRegistrationAccessToken().isEmpty() ||
                registrationResponse.getRegistrationClientUri() == null ||
                registrationResponse.getRegistrationClientUri().isEmpty()
            ) {
                throw new RegisteredClientNotUpdatableException();
            }

            Set<ClientRegistrationProviderEntity> providers = findAll(executionContext);
            if (providers == null || providers.isEmpty()) {
                throw new MissingDynamicClientRegistrationProviderException();
            }

            // For now, take the first provider
            ClientRegistrationProviderEntity provider = providers.iterator().next();

            // Get provider client
            DynamicClientRegistrationProviderClient registrationProviderClient = getDCRClient(false, provider);

            String renewClientSecretEndpoint = provider.getRenewClientSecretEndpoint();

            TemplateEngine templateEngine = TemplateEngine.templateEngine();
            templateEngine.getTemplateContext().setVariable("client_id", registrationResponse.getClientId());

            if (registrationProviderClient instanceof DiscoveryBasedDynamicClientRegistrationProviderClient) {
                ((DiscoveryBasedDynamicClientRegistrationProviderClient) registrationProviderClient).getMetadata()
                    .forEach((s, o) -> templateEngine.getTemplateContext().setVariable(s, o));
            }

            return registrationProviderClient.renewClientSecret(
                provider.getRenewClientSecretMethod(),
                templateEngine.getValue(renewClientSecretEndpoint, String.class),
                registrationResponse.getRegistrationAccessToken()
            );
        } catch (IOException ioe) {
            LOGGER.error("Unexpected error while updating a client", ioe);
            return null;
        }
    }

    private ClientRegistrationRequest convert(ClientRegistrationRequest request, UpdateApplicationEntity application) {
        ClientRegistrationMapper.INSTANCE.toClientRegistrationRequest(request, application);
        Map<String, String> clientMetadata = application.getSettings().getOauth().getAdditionalClientMetadata();
        if (clientMetadata != null) {
            ClientRegistrationRequest additionalMetadata = mapper.convertValue(clientMetadata, ClientRegistrationRequest.class);
            ClientRegistrationMapper.INSTANCE.mergeClientRegistrationRequest(request, additionalMetadata);
        }

        return request;
    }

    private ClientRegistrationProviderEntity convert(ClientRegistrationProvider clientRegistrationProvider) {
        ClientRegistrationProviderEntity entity = new ClientRegistrationProviderEntity();

        entity.setId(clientRegistrationProvider.getId());
        entity.setName(clientRegistrationProvider.getName());
        entity.setDescription(clientRegistrationProvider.getDescription());
        entity.setDiscoveryEndpoint(clientRegistrationProvider.getDiscoveryEndpoint());
        entity.setRenewClientSecretSupport(clientRegistrationProvider.isRenewClientSecretSupport());
        entity.setRenewClientSecretMethod(clientRegistrationProvider.getRenewClientSecretMethod());
        entity.setRenewClientSecretEndpoint(clientRegistrationProvider.getRenewClientSecretEndpoint());
        entity.setSoftwareId(clientRegistrationProvider.getSoftwareId());

        if (
            clientRegistrationProvider.getInitialAccessTokenType() == null ||
            clientRegistrationProvider.getInitialAccessTokenType() == ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS
        ) {
            entity.setInitialAccessTokenType(InitialAccessTokenType.CLIENT_CREDENTIALS);
            entity.setClientId(clientRegistrationProvider.getClientId());
            entity.setClientSecret(clientRegistrationProvider.getClientSecret());
            entity.setScopes(clientRegistrationProvider.getScopes());
        } else {
            entity.setInitialAccessTokenType(InitialAccessTokenType.INITIAL_ACCESS_TOKEN);
            entity.setInitialAccessToken(clientRegistrationProvider.getInitialAccessToken());
        }

        entity.setCreatedAt(clientRegistrationProvider.getCreatedAt());
        entity.setUpdatedAt(clientRegistrationProvider.getUpdatedAt());

        return entity;
    }

    private ClientRegistrationProvider convert(NewClientRegistrationProviderEntity newClientRegistrationProvider) {
        ClientRegistrationProvider provider = new ClientRegistrationProvider();
        provider.setId(UUID.toString(UUID.random()));
        provider.setName(newClientRegistrationProvider.getName());
        provider.setDescription(newClientRegistrationProvider.getDescription());
        provider.setDiscoveryEndpoint(newClientRegistrationProvider.getDiscoveryEndpoint());
        provider.setRenewClientSecretSupport(newClientRegistrationProvider.isRenewClientSecretSupport());
        provider.setRenewClientSecretMethod(newClientRegistrationProvider.getRenewClientSecretMethod());
        provider.setRenewClientSecretEndpoint(newClientRegistrationProvider.getRenewClientSecretEndpoint());
        provider.setSoftwareId(newClientRegistrationProvider.getSoftwareId());

        if (newClientRegistrationProvider.getInitialAccessTokenType() == InitialAccessTokenType.CLIENT_CREDENTIALS) {
            provider.setInitialAccessTokenType(ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS);
            provider.setClientId(newClientRegistrationProvider.getClientId());
            provider.setClientSecret(newClientRegistrationProvider.getClientSecret());
            provider.setScopes(newClientRegistrationProvider.getScopes());
        } else {
            provider.setInitialAccessTokenType(ClientRegistrationProvider.InitialAccessTokenType.INITIAL_ACCESS_TOKEN);
            provider.setInitialAccessToken(newClientRegistrationProvider.getInitialAccessToken());
        }

        return provider;
    }

    private ClientRegistrationProvider convert(UpdateClientRegistrationProviderEntity updateClientRegistrationProvider) {
        ClientRegistrationProvider provider = new ClientRegistrationProvider();

        provider.setName(updateClientRegistrationProvider.getName());
        provider.setDescription(updateClientRegistrationProvider.getDescription());
        provider.setDiscoveryEndpoint(updateClientRegistrationProvider.getDiscoveryEndpoint());
        provider.setRenewClientSecretSupport(updateClientRegistrationProvider.isRenewClientSecretSupport());
        provider.setRenewClientSecretMethod(updateClientRegistrationProvider.getRenewClientSecretMethod());
        provider.setRenewClientSecretEndpoint(updateClientRegistrationProvider.getRenewClientSecretEndpoint());
        provider.setSoftwareId(updateClientRegistrationProvider.getSoftwareId());

        if (updateClientRegistrationProvider.getInitialAccessTokenType() == InitialAccessTokenType.CLIENT_CREDENTIALS) {
            provider.setInitialAccessTokenType(ClientRegistrationProvider.InitialAccessTokenType.CLIENT_CREDENTIALS);
            provider.setClientId(updateClientRegistrationProvider.getClientId());
            provider.setClientSecret(updateClientRegistrationProvider.getClientSecret());
            provider.setScopes(updateClientRegistrationProvider.getScopes());
        } else {
            provider.setInitialAccessTokenType(ClientRegistrationProvider.InitialAccessTokenType.INITIAL_ACCESS_TOKEN);
            provider.setInitialAccessToken(updateClientRegistrationProvider.getInitialAccessToken());
        }

        return provider;
    }
}
