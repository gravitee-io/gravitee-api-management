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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.model.UpdateApiEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.IdGenerator;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private UserService userService;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    public ApiEntity create(NewApiEntity newApiEntity, String username) throws ApiAlreadyExistsException {
        try {
            LOGGER.debug("Create {} for user {}", newApiEntity, username);

            String id = idGenerator.generate(newApiEntity.getName());
            Optional<Api> checkApi = apiRepository.findById(id);
            if (checkApi.isPresent()) {
                throw new ApiAlreadyExistsException(id);
            }

            Api api = convert(id, newApiEntity);

            if (api != null) {
                api.setId(id);

                // Set date fields
                api.setCreatedAt(new Date());
                api.setUpdatedAt(api.getCreatedAt());

                // Be sure that lifecycle is set to STOPPED by default and visibility is private
                api.setLifecycleState(LifecycleState.STOPPED);
                api.setVisibility(Visibility.PRIVATE);

                Api createdApi = apiRepository.create(api);

                // Add the primary owner of the newly created API
                apiRepository.saveMember(createdApi.getId(), username, MembershipType.PRIMARY_OWNER);

                return convert(createdApi);
            } else {
                LOGGER.error("Unable to create API {} because of previous error.");
                throw new TechnicalManagementException("Unable to create API " + id);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", newApiEntity, username, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + newApiEntity + " for user " + username, ex);
        }
    }

    @Override
    public ApiEntity findById(String apiId) {
        try {
            LOGGER.debug("Find API by ID: {}", apiId);

            Optional<Api> api = apiRepository.findById(apiId);

            if (api.isPresent()) {
                return convert(api.get());
            }

            throw new ApiNotFoundException(apiId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an API using its ID: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, ex);
        }
    }

    @Override
    public Set<ApiEntity> findByVisibility(io.gravitee.management.model.Visibility visibility) {
        try {
            LOGGER.debug("Find APIs by visibility {}", visibility);
            Set<Api> publicApis = apiRepository.findByMember(null, null, Visibility.PUBLIC);

            return publicApis.stream()
                .map(this::convert).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public Set<ApiEntity> findByUser(String username) {
        try {
            LOGGER.debug("Find APIs by user {}", username);

            Set<Api> publicApis = apiRepository.findByMember(null, null, Visibility.PUBLIC);
            Set<Api> restrictedApis = apiRepository.findByMember(null, null, Visibility.RESTRICTED);
            Set<Api> privateApis = apiRepository.findByMember(username, null, Visibility.PRIVATE);

            final Set<ApiEntity> apis = new HashSet<>(publicApis.size() + restrictedApis.size() + privateApis.size());

            apis.addAll(publicApis.stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
            );

            apis.addAll(restrictedApis.stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
            );

            apis.addAll(privateApis.stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
            );

            return apis;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find APIs for user {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to find APIs for user " + username, ex);
        }
    }
    
    @Override
    public Set<ApiEntity> findByApplication(String applicationId) {
    	try {
             LOGGER.debug("Find APIs by application {}", applicationId);
             Set<Api> applicationApis = apiRepository.findByApplication(applicationId);
             return applicationApis.stream()
                     .map(this::convert).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public ApiEntity update(String apiId, UpdateApiEntity updateApiEntity) {
        try {
            LOGGER.debug("Update API {}", apiId);

            Optional<Api> optApiToUpdate = apiRepository.findById(apiId);
            if (! optApiToUpdate.isPresent()) {
                throw new ApiNotFoundException(apiId);
            }

            Api apiToUpdate = optApiToUpdate.get();
            Api api = convert(apiId, updateApiEntity);

            if (api != null) {
                api.setId(apiId.trim());
                api.setUpdatedAt(new Date());

                // Copy fields from existing values
                api.setCreatedAt(apiToUpdate.getCreatedAt());
                api.setLifecycleState(LifecycleState.STOPPED);

                Api updatedApi = apiRepository.update(api);
                return convert(updatedApi);
            } else {
                LOGGER.error("Unable to update API {} because of previous error.");
                throw new TechnicalManagementException("Unable to update API " + apiId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to update API " + apiId, ex);
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

    @Override
    public Set<MemberEntity> getMembers(String apiId, io.gravitee.management.model.MembershipType membershipType) {
        try {
            LOGGER.debug("Get members for API {}", apiId);

            Collection<Membership> membersRepo = apiRepository.getMembers(apiId,
                    (membershipType == null ) ? null : MembershipType.valueOf(membershipType.toString()));

            final Set<MemberEntity> members = new HashSet<>(membersRepo.size());

            members.addAll(
                    membersRepo.stream()
                            .map(member -> convert(member))
                            .collect(Collectors.toSet())
            );

            return members;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get members for API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for API " + apiId, ex);
        }
    }

    @Override
    public MemberEntity getMember(String apiId, String username) {
        try {
            LOGGER.debug("Get membership for API {} and user {}", apiId, username);

            Membership membership = apiRepository.getMember(apiId, username);

            if (membership != null) {
                return convert(membership);
            }

            return null;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for API {} and user", apiId, username, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for API " + apiId + " and user " + username, ex);
        }
    }

    @Override
    public void addOrUpdateMember(String api, String username, io.gravitee.management.model.MembershipType membershipType) {
        try {
            LOGGER.debug("Add or update a new member for API {}", api);

            apiRepository.saveMember(api, username,
                    MembershipType.valueOf(membershipType.toString()));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to add or update member for API {}", api, ex);
            throw new TechnicalManagementException("An error occurs while trying to add or update member for API " + api, ex);
        }
    }

    @Override
    public void deleteMember(String api, String username) {
        try {
            LOGGER.debug("Delete member {} for API {}", username, api);

            apiRepository.deleteMember(api, username);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete member {} for API {}", username, api, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete member " + username + " for API " + api, ex);
        }
    }

    private void updateLifecycle(String apiName, LifecycleState lifecycleState) throws TechnicalException {
        Optional<Api> optApi = apiRepository.findById(apiName);
        if (optApi.isPresent()) {
            Api api = optApi.get();
            api.setUpdatedAt(new Date());
            api.setLifecycleState(lifecycleState);
            apiRepository.update(api);
        }
    }

    private ApiEntity convert(Api api) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setName(api.getName());
        apiEntity.setCreatedAt(api.getCreatedAt());

        if (api.getDefinition() != null) {
            try {
                io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(api.getDefinition(),
                        io.gravitee.definition.model.Api.class);

                apiEntity.setProxy(apiDefinition.getProxy());
                apiEntity.setPaths(apiDefinition.getPaths());
                apiEntity.setMonitoring(apiDefinition.getMonitoring());
                apiEntity.setProperties(apiDefinition.getProperties());
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
        if (api.getVisibility() != null) {
            apiEntity.setVisibility(io.gravitee.management.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        return apiEntity;
    }

    private Api convert(String apiId, NewApiEntity newApiEntity) {
        Api api = new Api();

        api.setName(newApiEntity.getName().trim());
        api.setVersion(newApiEntity.getVersion().trim());
        api.setDescription(newApiEntity.getDescription().trim());

        try {
            io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
            apiDefinition.setId(apiId);
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

    private Api convert(String apiId, UpdateApiEntity updateApiEntity) {
        Api api = new Api();

        if (updateApiEntity.getVisibility() != null) {
            api.setVisibility(Visibility.valueOf(updateApiEntity.getVisibility().toString()));
        }

        api.setVersion(updateApiEntity.getVersion().trim());
        api.setName(updateApiEntity.getName().trim());
        api.setDescription(updateApiEntity.getDescription().trim());

        try {
            io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
            apiDefinition.setId(apiId);
            apiDefinition.setName(updateApiEntity.getName());
            apiDefinition.setVersion(updateApiEntity.getVersion());
            apiDefinition.setProxy(updateApiEntity.getProxy());
            apiDefinition.setPaths(updateApiEntity.getPaths());
            apiDefinition.setMonitoring(updateApiEntity.getMonitoring());
            apiDefinition.setProperties(updateApiEntity.getProperties());

            String definition = objectMapper.writeValueAsString(apiDefinition);
            api.setDefinition(definition);
            return api;
        } catch (JsonProcessingException jse) {
            LOGGER.error("Unexpected error while generating API definition", jse);
        }

        return null;
    }

    private MemberEntity convert(Membership membership) {
        MemberEntity member = new MemberEntity();

        member.setUser(membership.getUser());
        member.setCreatedAt(membership.getCreatedAt());
        member.setUpdatedAt(membership.getUpdatedAt());
        member.setType(io.gravitee.management.model.MembershipType.valueOf(membership.getMembershipType().toString()));

        return member;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }
}
