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
package io.gravitee.management.service.impl;

import static java.util.Collections.emptySet;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.model.Owner;
import io.gravitee.management.model.UpdateApiEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.OwnerType;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApiServiceImpl extends TransactionalService implements ApiService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(ApiServiceImpl.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ApiEntity createForUser(NewApiEntity api, String username) throws ApiAlreadyExistsException {
        try {
            LOGGER.debug("Create {} for user {}", api, username);
            return create(api, OwnerType.USER, username, username);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", api, username, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + api + " for user " + username, ex);
        }
    }

    @Override
    public ApiEntity createForTeam(NewApiEntity api, String teamName, String currentUser) throws ApiAlreadyExistsException {
        try {
            LOGGER.debug("Create {} for team {}", api, teamName);
            return create(api, OwnerType.TEAM, teamName, currentUser);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for team {}", api, teamName, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + api + " for team " + teamName, ex);
        }
    }

    @Override
    public Optional<ApiEntity> findByName(String apiName) {
        try {
            LOGGER.debug("Find API by name: {}", apiName);
            return apiRepository.findByName(apiName).map(this::convert);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an API using its name {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its name " + apiName, ex);
        }
    }

    @Override
    public Set<ApiEntity> findAll() {
        try {
            LOGGER.debug("Find all APIs");
            final Set<Api> apis = apiRepository.findAll();

            if (apis == null || apis.isEmpty()) {
                return emptySet();
            }

            final Set<ApiEntity> publicApis = new LinkedHashSet<>(apis.size());

            publicApis.addAll(apis.stream()
                .map(api -> convert(api))
                .collect(Collectors.toSet())
            );

            return publicApis;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public Set<ApiEntity> findByTeam(String teamName, boolean publicOnly) {
        try {
            LOGGER.debug("Find APIs for team {} (public: {})", teamName, publicOnly);

            final Set<Api> apis = apiRepository.findByTeam(teamName, publicOnly);

            if (apis == null || apis.isEmpty()) {
                return emptySet();
            }

            final Set<ApiEntity> teamApis = new HashSet<>(apis.size());

            teamApis.addAll(apis.stream()
                    .map(api -> convert(api))
                    .collect(Collectors.toSet())
            );

            return teamApis;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find APIs for team {} (public: {})", teamName, publicOnly, ex);
            throw new TechnicalManagementException("An error occurs while trying to find APIs for team " + teamName +
                    " (public: " + publicOnly + ")", ex);
        }
    }

    @Override
    public Set<ApiEntity> findByUser(String username, boolean publicOnly) {
        try {
            LOGGER.debug("Find APIs for user {} (public: {})", username, publicOnly);
            final Set<Api> apis = apiRepository.findByUser(username, publicOnly);

            if (apis == null || apis.isEmpty()) {
                return emptySet();
            }

            final Set<ApiEntity> userApis = new HashSet<>(apis.size());

            userApis.addAll(apis.stream()
                    .map(api -> convert(api))
                    .collect(Collectors.toSet())
            );

            return userApis;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find APIs for user {} (public: {})", username, publicOnly, ex);
            throw new TechnicalManagementException("An error occurs while trying to find APIs for user " + username +
                    " (public: " + publicOnly + ")", ex);
        }
    }

    @Override
    public ApiEntity update(String apiName, UpdateApiEntity updateApiEntity) {
        try {
            LOGGER.debug("Update API {}", apiName);

            Optional<Api> optApiToUpdate = apiRepository.findByName(apiName);
            if (! optApiToUpdate.isPresent()) {
                throw new ApiNotFoundException(apiName);
            }

            Api apiToUpdate = optApiToUpdate.get();
            Api api = convert(apiName, updateApiEntity);

            if (api != null) {
                api.setName(apiName);
                api.setUpdatedAt(new Date());

                // Copy fields from existing values
                api.setCreatedAt(apiToUpdate.getCreatedAt());
                api.setLifecycleState(LifecycleState.STOPPED);
                api.setOwner(apiToUpdate.getOwner());
                api.setOwnerType(apiToUpdate.getOwnerType());
                api.setCreator(apiToUpdate.getCreator());

                Api updatedApi = apiRepository.update(api);
                return convert(updatedApi);
            } else {
                LOGGER.error("Unable to update API {} because of previous error.");
                throw new TechnicalManagementException("Unable to update API " + apiName);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update API {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to update API " + apiName, ex);
        }
    }

    @Override
    public void delete(String apiName) {
        try {
            LOGGER.debug("Delete API {}", apiName);
            apiRepository.delete(apiName);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete API {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete API " + apiName, ex);
        }
    }

    @Override
    public void start(String apiName) {
        try {
            LOGGER.debug("Start API {}", apiName);
            updateLifecycle(apiName, LifecycleState.STARTED);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to start API {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to start API " + apiName, ex);
        }
    }

    @Override
    public void stop(String apiName) {
        try {
            LOGGER.debug("Stop API {}", apiName);
            updateLifecycle(apiName, LifecycleState.STOPPED);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to stop API {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to stop API " + apiName, ex);
        }
    }

    private void updateLifecycle(String apiName, LifecycleState lifecycleState) throws TechnicalException {
        Optional<Api> optApi = apiRepository.findByName(apiName);
        if (optApi.isPresent()) {
            Api api = optApi.get();
            api.setUpdatedAt(new Date());
            api.setLifecycleState(lifecycleState);
            apiRepository.update(api);
        }
    }

    private ApiEntity create(NewApiEntity newApiEntity, OwnerType ownerType, String owner, String currentUser) throws ApiAlreadyExistsException, TechnicalException {
        Optional<ApiEntity> checkApi = findByName(newApiEntity.getName());
        if (checkApi.isPresent()) {
            throw new ApiAlreadyExistsException(newApiEntity.getName());
        }

        Api api = convert(newApiEntity);

        if (api != null) {
            // Set date fields
            api.setCreatedAt(new Date());
            api.setUpdatedAt(api.getCreatedAt());

            // Be sure that lifecycle is set to STOPPED by default
            api.setLifecycleState(LifecycleState.STOPPED);

            // Set owner and owner type
            api.setOwner(owner);
            api.setOwnerType(ownerType);

            api.setCreator(currentUser);

            // Private by default
            api.setPrivateApi(true);

            Api createdApi = apiRepository.create(api);
            return convert(createdApi);
        } else {
            LOGGER.error("Unable to update API {} because of previous error.");
            throw new TechnicalManagementException("Unable to update API " + newApiEntity.getName());
        }
    }

    private ApiEntity convert(Api api) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setName(api.getName());
        apiEntity.setCreatedAt(api.getCreatedAt());
        apiEntity.setPrivate(api.isPrivateApi());

        if (api.getDefinition() != null) {
            try {
                ApiDefinition definition = objectMapper.readValue(api.getDefinition(), ApiDefinition.class);
                apiEntity.setProxy(definition.getProxy());
                apiEntity.setPaths(definition.getPaths());
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while generating API definition", ioe);
            }
        }
        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setVersion(api.getVersion());
        apiEntity.setDescription(api.getDescription());
        final LifecycleState lifecycleState = api.getLifecycleState();
        if (lifecycleState != null) {
            apiEntity.setState(Lifecycle.State.valueOf(lifecycleState.name()));
        }

        final Owner owner = new Owner();
        owner.setLogin(api.getOwner());
        final OwnerType ownerType = api.getOwnerType();
        if (ownerType != null) {
            owner.setType(Owner.OwnerType.valueOf(ownerType.toString()));
        }
        apiEntity.setOwner(owner);

        return apiEntity;
    }

    private Api convert(NewApiEntity newApiEntity) {
        Api api = new Api();

        api.setName(newApiEntity.getName());
        api.setVersion(newApiEntity.getVersion());
        api.setDescription(newApiEntity.getDescription());

        try {
            ApiDefinition apiDefinition = new ApiDefinition();
            apiDefinition.setName(newApiEntity.getName());
            apiDefinition.setVersion(newApiEntity.getVersion());
            apiDefinition.setProxy(newApiEntity.getProxy());
            apiDefinition.setPaths(newApiEntity.getPaths());

            String definition = objectMapper.writeValueAsString(apiDefinition);
            api.setDefinition(definition);
            return api;
        } catch (JsonProcessingException jse) {
            LOGGER.error("Unexpected error while generating API definition", jse);
        }

        return null;
    }

    private Api convert(String apiName, UpdateApiEntity updateApiEntity) {
        Api api = new Api();

        api.setPrivateApi(updateApiEntity.isPrivate());
        api.setVersion(updateApiEntity.getVersion());
        api.setDescription(updateApiEntity.getDescription());

        try {
            ApiDefinition apiDefinition = new ApiDefinition();
            apiDefinition.setName(apiName);
            apiDefinition.setVersion(updateApiEntity.getVersion());
            apiDefinition.setProxy(updateApiEntity.getProxy());
            apiDefinition.setPaths(updateApiEntity.getPaths());

            String definition = objectMapper.writeValueAsString(apiDefinition);
            api.setDefinition(definition);
            return api;
        } catch (JsonProcessingException jse) {
            LOGGER.error("Unexpected error while generating API definition", jse);
        }

        return null;
    }
}
