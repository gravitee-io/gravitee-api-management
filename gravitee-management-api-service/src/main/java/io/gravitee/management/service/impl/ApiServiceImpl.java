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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.EventEntity;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.MemberEntity;
import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.model.PrimaryOwnerEntity;
import io.gravitee.management.model.UpdateApiEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.EmailService;
import io.gravitee.management.service.EventService;
import io.gravitee.management.service.IdGenerator;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApiNotFoundException;
import io.gravitee.management.service.exceptions.ApiRunningStateException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipType;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.Visibility;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApiServiceImpl extends TransactionalService implements ApiService {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiServiceImpl.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdGenerator idGenerator;
    
    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

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
                    .filter(api -> api != null).map(this::convert).collect(Collectors.toSet());
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
            if (!optApiToUpdate.isPresent()) {
                throw new ApiNotFoundException(apiId);
            }

            Api apiToUpdate = optApiToUpdate.get();
            Api api = convert(apiId, updateApiEntity);

            if (api != null) {
                api.setId(apiId.trim());
                api.setUpdatedAt(new Date());

                // Copy fields from existing values
                api.setDeployedAt(apiToUpdate.getDeployedAt());
                api.setCreatedAt(apiToUpdate.getCreatedAt());
                api.setLifecycleState(apiToUpdate.getLifecycleState());

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
        ApiEntity api = findById(apiName);
        try {
            LOGGER.debug("Delete API {}", apiName);

            if (api.getState() == Lifecycle.State.STARTED) {
                throw new ApiRunningStateException(apiName);
            } else {
                Set<ApiKey> keys = apiKeyRepository.findByApi(apiName);
                keys.forEach(apiKey -> {
                    try {
                        apiKeyRepository.delete(apiKey.getKey());
                    } catch (TechnicalException e) {
                        LOGGER.error("An error occurs while deleting API Key {}", apiKey.getKey(), e);
                    }
                });
                
                Set<EventEntity> events = eventService.findByApi(apiName);
                events.forEach(event -> {
                    eventService.delete(event.getId());
                });
                
                apiRepository.delete(apiName);
            }
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
                    (membershipType == null) ? null : MembershipType.valueOf(membershipType.toString()));

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

            final UserEntity user = userService.findByName(username);

            apiRepository.saveMember(api, username,
                    MembershipType.valueOf(membershipType.toString()));

            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                emailService.sendEmailNotification(new EmailNotificationBuilder()
                        .to(user.getEmail())
                        .subject("Subscription to API " + api)
                        .content("apiMember.html")
                        .params(ImmutableMap.of("api", api, "username", username))
                        .build()
                );
            }
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
    
    @Override
    public boolean isAPISynchronized(ApiEntity api) {
    	Set<EventEntity> events = eventService.findByType(Arrays.asList(EventType.PUBLISH_API, EventType.UNPUBLISH_API));
    	List<EventEntity> eventsSorted = events.stream().sorted((e1, e2) -> e1.getCreatedAt().compareTo(e2.getCreatedAt())).collect(Collectors.toList());
    	Collections.reverse(eventsSorted);
    	try {
    		for (EventEntity event : eventsSorted) {
    			JsonNode node = objectMapper.readTree(event.getPayload());
    			Api payloadEntity = objectMapper.convertValue(node, Api.class);
    			if (api.getId().equals(payloadEntity.getId())) {
    				return api.getUpdatedAt().compareTo(payloadEntity.getUpdatedAt()) <= 0;
    			}
    		}
    	} catch (Exception e) {
    		return false;
    	}
    	return false;
    }
    
    @Override
    public ApiEntity deploy(String apiId, String username, EventType eventType) {
    	 try {
             LOGGER.debug("Deploy API : {}", apiId);

             Optional<Api> api = apiRepository.findById(apiId);

             if (api.isPresent()) {
                 Map<String, String> properties = new HashMap<String, String>();
                 properties.put(Event.EventProperties.API_ID.getValue(), api.get().getId());
                 properties.put(Event.EventProperties.USERNAME.getValue(), username);
            	 EventEntity event = eventService.create(eventType, objectMapper.writeValueAsString(api.get()), properties);
            	 // add deployment date
            	 if (event != null) {
            	     Api apiValue = api.get();
            	     apiValue.setDeployedAt(event.getCreatedAt());
            	     apiRepository.update(apiValue);
            	 }
            	 return convert(api.get());
             } else {
            	 throw new ApiNotFoundException(apiId);
             }
         } catch (TechnicalException | JsonProcessingException ex) {
             LOGGER.error("An error occurs while trying to deploy API: {}", apiId, ex);
             throw new TechnicalManagementException("An error occurs while trying to deploy API: " + apiId, ex);
         } 
    }
    
    @Override
    public ApiEntity rollback(String apiId, UpdateApiEntity api) {
        LOGGER.debug("Rollback API : {}", apiId);
        try {
            update(apiId, api);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to rollback API: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to rollback API: " + apiId, ex);
        }
        return null;
    }

    private void updateLifecycle(String apiName, LifecycleState lifecycleState) throws TechnicalException {
        Optional<Api> optApi = apiRepository.findById(apiName);
        if (optApi.isPresent()) {
            Api api = optApi.get();
            api.setUpdatedAt(new Date());
            api.setLifecycleState(lifecycleState);
            apiRepository.update(api);
        }

        //TODO: username must be changed
        if (LifecycleState.STARTED.equals(lifecycleState)) {
            deploy(apiName, "admin", EventType.PUBLISH_API);
        } else {
            deploy(apiName, "admin", EventType.UNPUBLISH_API);
        }
    }

    private ApiEntity convert(Api api) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setName(api.getName());
        apiEntity.setDeployedAt(api.getDeployedAt());
        apiEntity.setCreatedAt(api.getCreatedAt());

        if (api.getDefinition() != null) {
            try {
                io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(api.getDefinition(),
                        io.gravitee.definition.model.Api.class);

                apiEntity.setProxy(apiDefinition.getProxy());
                apiEntity.setPaths(apiDefinition.getPaths());
                apiEntity.setMonitoring(apiDefinition.getMonitoring());
                apiEntity.setProperties(apiDefinition.getProperties());
                apiEntity.setTags(apiDefinition.getTags());
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

        if (api.getMembers() != null) {
            final Optional<Membership> member = api.getMembers().stream().filter(
                    membership -> MembershipType.PRIMARY_OWNER.equals(membership.getMembershipType())
            ).findFirst();
            if (member.isPresent()) {
                final User user = member.get().getUser();
                final PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
                primaryOwnerEntity.setUsername(user.getUsername());
                primaryOwnerEntity.setLastname(user.getLastname());
                primaryOwnerEntity.setFirstname(user.getFirstname());
                primaryOwnerEntity.setEmail(user.getEmail());
                apiEntity.setPrimaryOwner(primaryOwnerEntity);
            }
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

            // Initialize with a default /* path
            Map<String, Path> paths = new HashMap<>();
            Path rootPath = new Path();
            Rule apiKeyRule = new Rule();
            Policy apiKeyPolicy = new Policy();
            apiKeyPolicy.setName("api-key");
            apiKeyPolicy.setConfiguration("{}");
            apiKeyRule.setPolicy(apiKeyPolicy);
            rootPath.getRules().add(apiKeyRule);
            paths.put("/", rootPath);

            apiDefinition.setPaths(paths);

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
            apiDefinition.setTags(updateApiEntity.getTags());

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

        member.setUser(membership.getUser().getUsername());
        member.setCreatedAt(membership.getCreatedAt());
        member.setUpdatedAt(membership.getUpdatedAt());
        member.setType(io.gravitee.management.model.MembershipType.valueOf(membership.getMembershipType().toString()));

        return member;
    }
}
