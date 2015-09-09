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

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.model.Owner;
import io.gravitee.management.model.UpdateApiEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.LifecycleState;
import io.gravitee.repository.model.OwnerType;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApiServiceImpl implements ApiService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(ApiServiceImpl.class);

    @Autowired
    private ApiRepository apiRepository;

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
            return apiRepository.findByName(apiName).map(ApiServiceImpl::convert);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an API using its name {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its name " + apiName, ex);
        }
    }

    @Override
    public Set<ApiEntity> findAll() {
        try {
            LOGGER.debug("Find all APIs");
            Set<Api> apis = apiRepository.findAll();
            Set<ApiEntity> publicApis = new LinkedHashSet<>(apis.size());

            for (Api api : apis) {
                publicApis.add(convert(api));
            }

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

            Set<Api> apis = apiRepository.findByTeam(teamName, publicOnly);
            Set<ApiEntity> teamApis = new HashSet<>(apis.size());

            for(Api api : apis) {
                teamApis.add(convert(api));
            }

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
            Set<Api> apis = apiRepository.findByUser(username, publicOnly);
            Set<ApiEntity> userApis = new HashSet<>(apis.size());

            for(Api api : apis) {
                userApis.add(convert(api));
            }

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
            Api api = convert(updateApiEntity);

            api.setName(apiName);
            api.setUpdatedAt(new Date());

            // Copy fields from existing values
            api.setCreatedAt(apiToUpdate.getCreatedAt());
            api.setLifecycleState(LifecycleState.STOPPED);
            api.setOwner(apiToUpdate.getOwner());
            api.setOwnerType(apiToUpdate.getOwnerType());
            api.setCreator(apiToUpdate.getCreator());

            Api updatedApi =  apiRepository.update(api);
            return convert(updatedApi);
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

        Api createdApi =  apiRepository.create(api);
        return convert(createdApi);
    }

    private static ApiEntity convert(Api api) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setName(api.getName());
        apiEntity.setCreatedAt(api.getCreatedAt());
        apiEntity.setPrivate(api.isPrivateApi());
        apiEntity.setPublicURI(api.getPublicURI());
        apiEntity.setTargetURI(api.getTargetURI());
        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setVersion(api.getVersion());
        apiEntity.setDescription(api.getDescription());
        apiEntity.setState(Lifecycle.State.valueOf(api.getLifecycleState().name()));

        final Owner owner = new Owner();
        owner.setLogin(api.getOwner());
        owner.setType(Owner.OwnerType.valueOf(api.getOwnerType().toString()));
        apiEntity.setOwner(owner);

        return apiEntity;
    }

    private static Api convert(NewApiEntity newApiEntity) {
        Api api = new Api();

        api.setName(newApiEntity.getName());
        api.setPublicURI(newApiEntity.getPublicURI());
        api.setTargetURI(newApiEntity.getTargetURI());
        api.setVersion(newApiEntity.getVersion());
        api.setDescription(newApiEntity.getDescription());

        return api;
    }

    private static Api convert(UpdateApiEntity updateApiEntity) {
        Api api = new Api();

        api.setPrivateApi(updateApiEntity.isPrivate());
        api.setPublicURI(updateApiEntity.getPublicURI());
        api.setTargetURI(updateApiEntity.getTargetURI());
        api.setVersion(updateApiEntity.getVersion());
        api.setDescription(updateApiEntity.getDescription());

        return api;
    }
}
