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
package io.gravitee.management.api.service.impl;

import io.gravitee.management.api.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.api.model.ApiEntity;
import io.gravitee.management.api.model.NewApiEntity;
import io.gravitee.management.api.model.UpdateApiEntity;
import io.gravitee.management.api.service.ApiService;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.LifecycleState;
import io.gravitee.repository.model.OwnerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApiServiceImpl implements ApiService {

    @Autowired
    private ApiRepository apiRepository;

    @Override
    public ApiEntity createForUser(NewApiEntity api, String username) throws ApiAlreadyExistsException {
        return create(api, OwnerType.USER, username);
    }

    @Override
    public ApiEntity createForTeam(NewApiEntity api, String teamName) throws ApiAlreadyExistsException {
        return create(api, OwnerType.TEAM, teamName);
    }

    @Override
    public Optional<ApiEntity> findByName(String apiName) {
        return apiRepository.findByName(apiName).map(api -> convert(api));
    }

    @Override
    public Set<ApiEntity> findAll() {
        Set<Api> apis = apiRepository.findAll();
        Set<ApiEntity> publicApis = new HashSet<>(apis.size());

        for(Api api : apis) {
            publicApis.add(convert(api));
        }

        return publicApis;
    }

    @Override
    public Set<ApiEntity> findByTeam(String teamName, boolean publicOnly) {
        Set<Api> apis = apiRepository.findByTeam(teamName, publicOnly);
        Set<ApiEntity> teamApis = new HashSet<>(apis.size());

        for(Api api : apis) {
            teamApis.add(convert(api));
        }

        return teamApis;
    }

    @Override
    public Set<ApiEntity> findByUser(String username, boolean publicOnly) {
        Set<Api> apis = apiRepository.findByUser(username, publicOnly);
        Set<ApiEntity> userApis = new HashSet<>(apis.size());

        for(Api api : apis) {
            userApis.add(convert(api));
        }

        return userApis;
    }

    @Override
    public ApiEntity update(String apiName, UpdateApiEntity apiCreation) {
        Api api = convert(apiCreation);

        api.setUpdatedAt(new Date());

        Api updatedApi =  apiRepository.update(api);
        return convert(updatedApi);
    }

    @Override
    public void delete(String apiName) {
        apiRepository.delete(apiName);
    }

    @Override
    public void start(String apiName) {
        updateLifecycle(apiName, LifecycleState.STARTED);
    }

    @Override
    public void stop(String apiName) {
        updateLifecycle(apiName, LifecycleState.STOPPED);
    }

    private void updateLifecycle(String apiName, LifecycleState lifecycleState) {
        Optional<Api> optApi = apiRepository.findByName(apiName);
        if (optApi.isPresent()) {
            Api api = optApi.get();
            api.setUpdatedAt(new Date());
            api.setLifecycleState(lifecycleState);
            apiRepository.update(api);
        }
    }

    private ApiEntity create(NewApiEntity newApiEntity, OwnerType ownerType, String owner) throws ApiAlreadyExistsException {
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
        api.setCreator(owner);

        Api createdApi =  apiRepository.create(api);
        return convert(createdApi);
    }

    private static ApiEntity convert(Api api) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setName(api.getName());
        apiEntity.setCreatedAt(api.getCreatedAt());
        apiEntity.setPrivate(api.isPrivate());
        apiEntity.setPublicURI(api.getPublicURI());
        apiEntity.setTargetURI(api.getTargetURI());
        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setVersion(api.getVersion());

        return apiEntity;
    }

    private static Api convert(NewApiEntity newApiEntity) {
        Api api = new Api();

        api.setName(newApiEntity.getName());
        api.setPrivate(newApiEntity.isPrivate());
        api.setPublicURI(newApiEntity.getPublicURI());
        api.setTargetURI(newApiEntity.getTargetURI());
        api.setVersion(newApiEntity.getVersion());

        return api;
    }

    private static Api convert(UpdateApiEntity updateApiEntity) {
        Api api = new Api();

        api.setPrivate(updateApiEntity.isPrivate());
        api.setPublicURI(updateApiEntity.getPublicURI());
        api.setTargetURI(updateApiEntity.getTargetURI());
        api.setVersion(updateApiEntity.getVersion());

        return api;
    }
}
