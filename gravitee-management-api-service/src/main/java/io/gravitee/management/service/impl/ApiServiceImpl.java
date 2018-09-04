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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.management.idp.api.identity.SearchableUser;
import io.gravitee.management.model.*;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.PageType;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.api.ApiQuery;
import io.gravitee.management.model.api.NewApiEntity;
import io.gravitee.management.model.api.UpdateApiEntity;
import io.gravitee.management.model.documentation.PageQuery;
import io.gravitee.management.model.notification.GenericNotificationConfigEntity;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.model.plan.PlanQuery;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.notification.ApiHook;
import io.gravitee.management.service.notification.HookScope;
import io.gravitee.management.service.notification.NotificationParamsBuilder;
import io.gravitee.management.service.processor.ApiSynchronizationProcessor;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.management.model.Visibility;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.gravitee.management.model.PageType.SWAGGER;
import static io.gravitee.repository.management.model.Api.AuditEvent.*;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static io.gravitee.management.model.EventType.PUBLISH_API;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static io.gravitee.repository.management.model.Visibility.PUBLIC;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiServiceImpl extends TransactionalService implements ApiService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ApiServiceImpl.class);

    @Autowired
    private ApiRepository apiRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EventService eventService;
    @Autowired
    private UserService userService;
    @Autowired
    private PageService pageService;
    @Autowired
    private MembershipService membershipService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private PlanService planService;
    @Autowired
    private ApiSynchronizationProcessor apiSynchronizationProcessor;
    @Value("${configuration.default-icon:${gravitee.home}/assets/default_api_logo.png}")
    private String defaultIcon;
    @Autowired
    private ApiMetadataService apiMetadataService;
    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private TopApiService topApiService;
    @Autowired
    private GenericNotificationConfigService genericNotificationConfigService;
    @Autowired
    private NotifierService notifierService;
    @Autowired
    private SwaggerService swaggerService;

    @Override
    public ApiEntity create(NewApiEntity newApiEntity, String userId) throws ApiAlreadyExistsException {
        UpdateApiEntity apiEntity = new UpdateApiEntity();

        apiEntity.setName(newApiEntity.getName());
        apiEntity.setDescription(newApiEntity.getDescription());
        apiEntity.setVersion(newApiEntity.getVersion());
        // check the existence of groups
        if (newApiEntity.getGroups() != null && !newApiEntity.getGroups().isEmpty()) {
            try {
                groupService.findByIds(newApiEntity.getGroups());
            } catch (GroupsNotFoundException gnfe) {
                throw new InvalidDataException("Groups [" + newApiEntity.getGroups() + "] does not exist");
            }
        }
        apiEntity.setGroups(newApiEntity.getGroups());

        Proxy proxy = new Proxy();
        proxy.setContextPath(newApiEntity.getContextPath());
        EndpointGroup group = new EndpointGroup();
        group.setEndpoints(singleton(new HttpEndpoint("default", newApiEntity.getEndpoint())));
        proxy.setGroups(singleton(group));
        apiEntity.setProxy(proxy);

        List<String> declaredPaths = (newApiEntity.getPaths() != null) ? newApiEntity.getPaths() : new ArrayList<>();
        if (!declaredPaths.contains("/")) {
            declaredPaths.add(0, "/");
        }

        // Initialize with a default path and provided paths
        Map<String, Path> paths = declaredPaths.stream().map(sPath -> {
            Path path = new Path();
            path.setPath(sPath);
            return path;
        }).collect(toMap(Path::getPath, path -> path));

        apiEntity.setPaths(paths);
        apiEntity.setPathMappings(new HashSet<>(declaredPaths));

        return create0(apiEntity, userId);
    }

    private ApiEntity create0(UpdateApiEntity api, String userId) throws ApiAlreadyExistsException {
        try {
            LOGGER.debug("Create {} for user {}", api, userId);

            String id = UUID.toString(UUID.random());
            Optional<Api> checkApi = apiRepository.findById(id);
            if (checkApi.isPresent()) {
                throw new ApiAlreadyExistsException(id);
            }

            // Format context-path and check if context path is unique
            checkContextPath(api.getProxy().getContextPath());

            Api repoApi = convert(id, api);

            if (repoApi != null) {
                repoApi.setId(id);

                // Set date fields
                repoApi.setCreatedAt(new Date());
                repoApi.setUpdatedAt(repoApi.getCreatedAt());

                // Be sure that lifecycle is set to STOPPED by default and visibility is private
                repoApi.setLifecycleState(LifecycleState.STOPPED);
                if (api.getVisibility() == null) {
                    repoApi.setVisibility(Visibility.PRIVATE);
                } else {
                    repoApi.setVisibility(Visibility.valueOf(api.getVisibility().toString()));
                }

                // Add Default groups
                Set<String> defaultGroups = groupService.findByEvent(GroupEvent.API_CREATE).
                        stream().
                        map(GroupEntity::getId).
                        collect(Collectors.toSet());
                if (!defaultGroups.isEmpty() && repoApi.getGroups() == null) {
                    repoApi.setGroups(defaultGroups);
                } else if (!defaultGroups.isEmpty()) {
                    repoApi.getGroups().addAll(defaultGroups);
                }

                Api createdApi = apiRepository.create(repoApi);
                // Audit
                auditService.createApiAuditLog(
                        createdApi.getId(),
                        Collections.emptyMap(),
                        API_CREATED,
                        createdApi.getCreatedAt(),
                        null,
                        createdApi);

                // Add the primary owner of the newly created API
                UserEntity primaryOwner = userService.findById(userId);
                Membership membership = new Membership(primaryOwner.getId(), createdApi.getId(), MembershipReferenceType.API);
                membership.setRoles(Collections.singletonMap(RoleScope.API.getId(), SystemRole.PRIMARY_OWNER.name()));
                membership.setCreatedAt(repoApi.getCreatedAt());
                membership.setUpdatedAt(repoApi.getCreatedAt());
                membershipRepository.create(membership);
                // create the default mail notification
                if (primaryOwner.getEmail() != null && !primaryOwner.getEmail().isEmpty()) {
                    GenericNotificationConfigEntity notificationConfigEntity = new GenericNotificationConfigEntity();
                    notificationConfigEntity.setName("Default Mail Notifications");
                    notificationConfigEntity.setReferenceType(HookScope.API.name());
                    notificationConfigEntity.setReferenceId(createdApi.getId());
                    notificationConfigEntity.setHooks(Arrays.stream(ApiHook.values()).map(Enum::name).collect(toList()));
                    notificationConfigEntity.setNotifier(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID);
                    notificationConfigEntity.setConfig(primaryOwner.getEmail());
                    genericNotificationConfigService.create(notificationConfigEntity);
                }

                //TODO add membership log
                return convert(createdApi, primaryOwner);
            } else {
                LOGGER.error("Unable to create API {} because of previous error.", api.getName());
                throw new TechnicalManagementException("Unable to create API " + api.getName());
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", api, userId, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + api + " for user " + userId, ex);
        }
    }

    public void checkContextPath(final String newContextPath) throws TechnicalException {
        checkContextPath(newContextPath, null);
    }

    private void checkContextPath(String newContextPath, final String apiId) throws TechnicalException {
        if (newContextPath.charAt(0) != '/') {
            newContextPath = '/' + newContextPath;
        }
        if (newContextPath.charAt(newContextPath.length() - 1) == '/') {
            newContextPath = newContextPath.substring(0, newContextPath.length() - 1);
        }

        final int indexOfEndOfNewSubContextPath = newContextPath.lastIndexOf('/', 1);
        final String newSubContextPath = newContextPath.substring(0, indexOfEndOfNewSubContextPath <= 0 ?
                newContextPath.length() : indexOfEndOfNewSubContextPath) + '/';

        final boolean contextPathExists = apiRepository.search(null).stream()
                .filter(api -> !api.getId().equals(apiId))
                .anyMatch(api -> {
                    final String contextPath = convert(api, null).getProxy().getContextPath();
                    final int indexOfEndOfSubContextPath = contextPath.lastIndexOf('/', 1);
                    final String subContextPath = contextPath.substring(0, indexOfEndOfSubContextPath <= 0 ?
                            contextPath.length() : indexOfEndOfSubContextPath) + '/';

                    return subContextPath.startsWith(newSubContextPath) || newSubContextPath.startsWith(subContextPath);
                });
        if (contextPathExists) {
            throw new ApiContextPathAlreadyExistsException(newSubContextPath);
        }
    }

    @Override
    public ApiEntity findById(String apiId) {
        try {
            LOGGER.debug("Find API by ID: {}", apiId);

            Optional<Api> api = apiRepository.findById(apiId);

            if (api.isPresent()) {
                Optional<Membership> primaryOwnerMembership = membershipRepository.findByReferenceAndRole(
                        MembershipReferenceType.API,
                        api.get().getId(),
                        RoleScope.API,
                        SystemRole.PRIMARY_OWNER.name())
                        .stream()
                        .findFirst();
                if (!primaryOwnerMembership.isPresent()) {
                    LOGGER.error("The API {} doesn't have any primary owner.", apiId);
                    throw new TechnicalException("The API " + apiId + " doesn't have any primary owner.");
                }

                return convert(api.get(), userService.findById(primaryOwnerMembership.get().getUserId()));
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
            return convert(apiRepository.search(new ApiCriteria.Builder().visibility(Visibility.valueOf(visibility.name())).build()));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public Set<ApiEntity> findAll() {
        try {
            LOGGER.debug("Find all APIs");
            return convert(apiRepository.search(null));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public Set<ApiEntity> findAllLight() {
        try {
            LOGGER.debug("Find all APIs without some fields (definition, picture...)");
            return convert(apiRepository.search(null,
                    new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build()));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs light", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs light", ex);
        }
    }

    @Override
    public Set<ApiEntity> findByUser(String userId, ApiQuery apiQuery) {
        try {
            LOGGER.debug("Find APIs by user {}", userId);

            //get all public apis
            List<Api> publicApis = apiRepository.search(queryToCriteria(apiQuery).visibility(PUBLIC).build());

            // get user apis
            List<Api> userApis = emptyList();
            final String[] userApiIds = membershipRepository
                    .findByUserAndReferenceType(userId, MembershipReferenceType.API).stream()
                    .map(Membership::getReferenceId)
                    .toArray(String[]::new);
            if (userApiIds.length > 0) {
                userApis = apiRepository.search(queryToCriteria(apiQuery).ids(userApiIds).build());
            }

            // get user groups apis
            List<Api> groupApis = emptyList();
            final String[] groupIds = membershipRepository
                    .findByUserAndReferenceType(userId, MembershipReferenceType.GROUP).stream()
                    .filter(m -> m.getRoles().keySet().contains(RoleScope.API.getId()))
                    .map(Membership::getReferenceId)
                    .toArray(String[]::new);
            if (groupIds.length > 0 && groupIds[0] != null) {
                groupApis = apiRepository.search(queryToCriteria(apiQuery).groups(groupIds).build());
            }

            // merge all apis
            final Set<ApiEntity> apis = new HashSet<>(publicApis.size() + userApis.size() + groupApis.size());
            apis.addAll(convert(publicApis));
            apis.addAll(convert(userApis));
            apis.addAll(convert(groupApis));
            return apis;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find APIs for user {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find APIs for user " + userId, ex);
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

            // Check if context path is unique
            checkContextPath(updateApiEntity.getProxy().getContextPath(), apiId);

            // check the existence of groups
            if (updateApiEntity.getGroups() != null && !updateApiEntity.getGroups().isEmpty()) {
                try {
                    groupService.findByIds(updateApiEntity.getGroups());
                } catch (GroupsNotFoundException gnfe) {
                    throw new InvalidDataException("Groups [" + updateApiEntity.getGroups() + "] does not exist");
                }
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
                if (updateApiEntity.getPicture() == null) {
                    api.setPicture(apiToUpdate.getPicture());
                }

                Api updatedApi = apiRepository.update(api);

                // Audit
                auditService.createApiAuditLog(
                        updatedApi.getId(),
                        Collections.emptyMap(),
                        API_UPDATED,
                        updatedApi.getUpdatedAt(),
                        apiToUpdate,
                        updatedApi);

                return convert(singletonList(updatedApi)).iterator().next();
            } else {
                LOGGER.error("Unable to update API {} because of previous error.", api.getId());
                throw new TechnicalManagementException("Unable to update API " + apiId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to update API " + apiId, ex);
        }
    }

    @Override
    public void delete(String apiId) {
        try {
            LOGGER.debug("Delete API {}", apiId);

            Optional<Api> optApi = apiRepository.findById(apiId);
            if (!optApi.isPresent()) {
                throw new ApiNotFoundException(apiId);
            }

            if (optApi.get().getLifecycleState() == LifecycleState.STARTED) {
                throw new ApiRunningStateException(apiId);
            } else {
                // Delete plans
                Set<PlanEntity> plans = planService.findByApi(apiId);
                Set<String> plansNotClosed = plans.stream()
                        .filter(plan -> plan.getStatus() == PlanStatus.PUBLISHED)
                        .map(PlanEntity::getName)
                        .collect(Collectors.toSet());

                if (!plansNotClosed.isEmpty()) {
                    throw new ApiNotDeletableException("Plan(s) [" + String.join(", ", plansNotClosed) +
                            "] must be closed before being able to delete the API !");
                }

                Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApi(apiId);
                subscriptions.forEach(sub -> subscriptionService.delete(sub.getId()));

                for (PlanEntity plan : plans) {
                    planService.delete(plan.getId());
                }

                // Delete events
                final EventQuery query = new EventQuery();
                query.setApi(apiId);
                eventService.search(query)
                        .forEach(event -> eventService.delete(event.getId()));

                // Delete API
                apiRepository.delete(apiId);
                // Delete top API
                topApiService.delete(apiId);
                // Audit
                auditService.createApiAuditLog(
                        apiId,
                        Collections.emptyMap(),
                        API_DELETED,
                        new Date(),
                        optApi.get(),
                        null);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete API " + apiId, ex);
        }
    }

    @Override
    public ApiEntity start(String apiId, String userId) {
        try {
            LOGGER.debug("Start API {}", apiId);
            ApiEntity apiEntity = updateLifecycle(apiId, LifecycleState.STARTED, userId);
            notifierService.trigger(
                    ApiHook.API_STARTED,
                    apiId,
                    new NotificationParamsBuilder()
                            .api(apiEntity)
                            .user(userService.findById(userId))
                            .build());
            return apiEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to start API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to start API " + apiId, ex);
        }
    }

    @Override
    public ApiEntity stop(String apiId, String userId) {
        try {
            LOGGER.debug("Stop API {}", apiId);
            ApiEntity apiEntity = updateLifecycle(apiId, LifecycleState.STOPPED, userId);
            notifierService.trigger(
                    ApiHook.API_STOPPED,
                    apiId,
                    new NotificationParamsBuilder()
                            .api(apiEntity)
                            .user(userService.findById(userId))
                            .build());
            return apiEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to stop API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to stop API " + apiId, ex);
        }
    }

    @Override
    public boolean isSynchronized(String apiId) {
        try {
            // 1_ First, check the API state
            ApiEntity api = findById(apiId);

            Map<String, Object> properties = new HashMap<>();
            properties.put(Event.EventProperties.API_ID.getValue(), apiId);

            io.gravitee.common.data.domain.Page<EventEntity> events =
                    eventService.search(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API),
                            properties, 0, 0, 0, 1);

            if (!events.getContent().isEmpty()) {
                // According to page size, we know that we have only one element in the list
                EventEntity lastEvent = events.getContent().get(0);

                //TODO: Done only for backward compatibility with 0.x. Must be removed later (1.1.x ?)
                boolean enabled = objectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                Api payloadEntity = objectMapper.readValue(lastEvent.getPayload(), Api.class);
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, enabled);

                final ApiEntity deployedApi = convert(payloadEntity);
                // Remove policy description from sync check
                removeDescriptionFromPolicies(api);
                removeDescriptionFromPolicies(deployedApi);

                boolean sync = apiSynchronizationProcessor.processCheckSynchronization(deployedApi, api);

                // 2_ If API definition is synchronized, check if there is any modification for API's plans
                // but only for published or closed plan
                if (sync) {
                    Set<PlanEntity> plans = planService.findByApi(api.getId());
                    sync = plans.stream()
                            .filter(plan -> plan.getStatus() != PlanStatus.STAGING)
                            .filter(plan -> plan.getUpdatedAt().after(api.getDeployedAt())).count() == 0;
                }

                return sync;
            }
        } catch (Exception e) {
            LOGGER.error("An error occurs while trying to check API synchronization state {}", apiId, e);
        }

        return false;
    }

    private void removeDescriptionFromPolicies(final ApiEntity api) {
        if (api.getPaths() != null) {
            api.getPaths().forEach((s, path) -> {
                if (path.getRules() != null) {
                    path.getRules().forEach(rule -> rule.setDescription(""));
                }
            });
        }

    }

    @Override
    public ApiEntity deploy(String apiId, String userId, EventType eventType) {
        try {
            LOGGER.debug("Deploy API : {}", apiId);

            return deployCurrentAPI(apiId, userId, eventType);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to deploy API: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to deploy API: " + apiId, ex);
        }
    }

    @Override
    public ApiEntity rollback(String apiId, UpdateApiEntity api) {
        LOGGER.debug("Rollback API : {}", apiId);
        try {
            return update(apiId, api);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to rollback API: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to rollback API: " + apiId, ex);
        }
    }

    private ApiEntity deployCurrentAPI(String apiId, String userId, EventType eventType) throws Exception {
        Optional<Api> api = apiRepository.findById(apiId);

        if (api.isPresent()) {
            // add deployment date
            Api apiValue = api.get();
            apiValue.setUpdatedAt(new Date());
            apiValue.setDeployedAt(apiValue.getUpdatedAt());
            apiValue = apiRepository.update(apiValue);

            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.API_ID.getValue(), apiValue.getId());
            properties.put(Event.EventProperties.USER.getValue(), userId);

            // Clear useless field for history
            apiValue.setPicture(null);

            // And create event
            eventService.create(eventType, objectMapper.writeValueAsString(apiValue), properties);

            return convert(singletonList(apiValue)).iterator().next();
        } else {
            throw new ApiNotFoundException(apiId);
        }
    }

    /**
     * Allows to deploy the last published API
     * @param apiId the API id
     * @param userId the user id
     * @param eventType the event type
     * @return The persisted API or null
     * @throws TechnicalException if an exception occurs while saving the API
     */
    private ApiEntity deployLastPublishedAPI(String apiId, String userId, EventType eventType) throws TechnicalException {
        final EventQuery query = new EventQuery();
        query.setApi(apiId);
        query.setTypes(singleton(PUBLISH_API));

        final Optional<EventEntity> optEvent =
                eventService.search(query).stream().max(comparing(EventEntity::getCreatedAt));
        try {
            if (optEvent.isPresent()) {
                EventEntity event = optEvent.get();
                JsonNode node = objectMapper.readTree(event.getPayload());
                Api lastPublishedAPI = objectMapper.convertValue(node, Api.class);
                lastPublishedAPI.setLifecycleState(convert(eventType));
                lastPublishedAPI.setUpdatedAt(new Date());
                lastPublishedAPI.setDeployedAt(new Date());
                Map<String, String> properties = new HashMap<>();
                properties.put(Event.EventProperties.API_ID.getValue(), lastPublishedAPI.getId());
                properties.put(Event.EventProperties.USER.getValue(), userId);

                // Clear useless field for history
                lastPublishedAPI.setPicture(null);

                // And create event
                eventService.create(eventType, objectMapper.writeValueAsString(lastPublishedAPI), properties);
                return null;
            } else {
                // this is the first time we start the api without previously deployed id.
                // let's do it.
                return this.deploy(apiId, userId, PUBLISH_API);
            }
        } catch (Exception e) {
            LOGGER.error("An error occurs while trying to deploy last published API {}", apiId, e);
            throw new TechnicalException("An error occurs while trying to deploy last published API " + apiId, e);
        }
    }

    @Override
    public String exportAsJson(final String apiId, String role, String... filteredFields) {
        final ApiEntity apiEntity = findById(apiId);
        List<String> filteredFiedsList = Arrays.asList(filteredFields);

        apiEntity.setId(null);
        apiEntity.setCreatedAt(null);
        apiEntity.setUpdatedAt(null);
        apiEntity.setDeployedAt(null);
        apiEntity.setPrimaryOwner(null);
        apiEntity.setState(null);

        ObjectNode apiJsonNode = objectMapper.valueToTree(apiEntity);
        String field;

        field = "groups";
        if (!filteredFiedsList.contains(field)) {
            apiJsonNode.remove(field);
            if (apiEntity.getGroups() != null && !apiEntity.getGroups().isEmpty()) {
                Set<GroupEntity> groupEntities = groupService.findByIds(apiEntity.getGroups());
                apiJsonNode.putPOJO(field, groupEntities.stream().map(GroupEntity::getName).collect(Collectors.toSet()));
            }
        }

        field = "members";
        if (!filteredFiedsList.contains(field)) {
            Set<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, apiId, RoleScope.API);
            if (members != null) {
                members.forEach(m -> {
                    m.setId(null);
                    m.setFirstname(null);
                    m.setLastname(null);
                    m.setEmail(null);
                    m.setPermissions(null);
                    m.setCreatedAt(null);
                    m.setUpdatedAt(null);
                });
            }
            apiJsonNode.putPOJO(field, members == null ? Collections.emptyList() : members);
        }

        field = "pages";
        if (!filteredFiedsList.contains(field)) {
            List<PageListItem> pageListItems = pageService.findApiPagesByApi(apiId);
            List<PageEntity> pages = null;
            if (pageListItems != null) {
                pages = new ArrayList<>(pageListItems.size());
                List<PageEntity> finalPages = pages;
                pageListItems.forEach(f -> {
                    PageEntity pageEntity = pageService.findById(f.getId());
                    pageEntity.setId(null);
                    finalPages.add(pageEntity);
                });
            }
            apiJsonNode.putPOJO(field, pages == null ? Collections.emptyList() : pages);
        }

        field = "plans";
        if (!filteredFiedsList.contains(field)) {
            Set<PlanEntity> plans = planService.findByApi(apiId);
            Set<PlanEntity> plansToAdd = plans == null
                    ? Collections.emptySet()
                    : plans.stream()
                    .filter(p -> !PlanStatus.CLOSED.equals(p.getStatus()))
                    .collect(Collectors.toSet());
            apiJsonNode.putPOJO(field, plansToAdd);
        }

        try {
            apiJsonNode.remove("permission");
            return objectMapper.writeValueAsString(apiJsonNode);
        } catch (final Exception e) {
            LOGGER.error("An error occurs while trying to JSON serialize the API {}", apiEntity, e);
        }
        return "";
    }

    @Override
    public ApiEntity createOrUpdateWithDefinition(final ApiEntity apiEntity, String apiDefinition, String userId) {
        try {
            final UpdateApiEntity importedApi = objectMapper
                    // because definition could contains other values than the api itself (pages, members)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(apiDefinition, UpdateApiEntity.class);

            // Initialize with a default path
            if (importedApi.getPaths() == null || importedApi.getPaths().isEmpty()) {
                Path path = new Path();
                path.setPath("/");
                importedApi.setPaths(Collections.singletonMap("/", path));
            }

            //create group if not exist & replace groupName by groupId
            if (importedApi.getGroups() != null) {
                Set<String> groupNames = new HashSet<>(importedApi.getGroups());
                importedApi.getGroups().clear();
                for (String name : groupNames) {
                    List<GroupEntity> groupEntities = groupService.findByName(name);
                    GroupEntity group;
                    if (groupEntities.isEmpty()) {
                        NewGroupEntity newGroupEntity = new NewGroupEntity();
                        newGroupEntity.setName(name);
                        group = groupService.create(newGroupEntity);
                    } else {
                        group = groupEntities.get(0);
                    }
                    importedApi.getGroups().add(group.getId());
                }
            }

            ApiEntity createdOrUpdatedApiEntity;
            Set<MemberToImport> members;
            if (apiEntity == null || apiEntity.getId() == null) {
                createdOrUpdatedApiEntity = create0(importedApi, userId);
                members = Collections.emptySet();
            } else {
                createdOrUpdatedApiEntity = update(apiEntity.getId(), importedApi);
                members = membershipService.getMembers(MembershipReferenceType.API, apiEntity.getId(), RoleScope.API)
                        .stream()
                        .map(member -> new MemberToImport(member.getUsername(), member.getRole())).collect(Collectors.toSet());
            }

            // Read the whole definition
            final JsonNode jsonNode = objectMapper.readTree(apiDefinition);

            // Members
            final JsonNode membersDefinition = jsonNode.path("members");
            if (membersDefinition != null && membersDefinition.isArray()) {
                MemberEntity memberAsPrimaryOwner = null;

                for (final JsonNode memberNode : membersDefinition) {
                    MemberToImport memberEntity = objectMapper.readValue(memberNode.toString(), MemberToImport.class);
                    Collection<SearchableUser> idpUsers = identityService.search(memberEntity.getUsername());

                    if (!idpUsers.isEmpty()) {
                        SearchableUser user = idpUsers.iterator().next();

                        if (!members.contains(memberEntity)
                                || members.stream().anyMatch(m ->
                                m.getUsername().equals(memberEntity.getUsername())
                                        && !m.getRole().equals(memberEntity.getRole()))) {
                            MemberEntity membership = membershipService.addOrUpdateMember(
                                    new MembershipService.MembershipReference(MembershipReferenceType.API, createdOrUpdatedApiEntity.getId()),
                                    new MembershipService.MembershipUser(user.getId(), user.getReference()),
                                    new MembershipService.MembershipRole(RoleScope.API, memberEntity.getRole()));
                            if (SystemRole.PRIMARY_OWNER.name().equals(memberEntity.getRole())) {
                                // Get the identifier of the primary owner
                                memberAsPrimaryOwner = membership;
                            }
                        }
                    }
                }

                // Transfer ownership if necessary
                if (memberAsPrimaryOwner != null && !userId.equals(memberAsPrimaryOwner.getId())) {
                    membershipService.transferApiOwnership(createdOrUpdatedApiEntity.getId(),
                            new MembershipService.MembershipUser(memberAsPrimaryOwner.getId(), null),
                            null);
                }
            }

            //Pages
            final JsonNode pagesDefinition = jsonNode.path("pages");
            if (pagesDefinition != null && pagesDefinition.isArray()) {
                for (final JsonNode pageNode : pagesDefinition) {
                    PageQuery query = new PageQuery.Builder().
                            api(createdOrUpdatedApiEntity.getId()).
                            name(pageNode.get("name").asText()).
                            type(PageType.valueOf(pageNode.get("type").asText())).
                            build();
                    List<PageEntity> pageEntities = pageService.search(query);
                    if (pageEntities == null || pageEntities.isEmpty()) {
                        pageService.createApiPage(createdOrUpdatedApiEntity.getId(), objectMapper.readValue(pageNode.toString(), NewPageEntity.class));
                    } else if (pageEntities.size() == 1) {
                        UpdatePageEntity updatePageEntity = objectMapper.readValue(pageNode.toString(), UpdatePageEntity.class);
                        pageService.update(pageEntities.get(0).getId(), updatePageEntity);
                    } else {
                        LOGGER.error("Not able to identify the page to update: {}. Too much page with the same name", pageNode.get("name").asText());
                        throw new TechnicalManagementException("Not able to identify the page to update: " + pageNode.get("name").asText() + ". Too much page with the same name");
                    }
                }
            }

            //Plans
            final JsonNode plansDefinition = jsonNode.path("plans");
            if (plansDefinition != null && plansDefinition.isArray()) {
                for (JsonNode planNode : plansDefinition) {
                    PlanQuery query = new PlanQuery.Builder().
                            api(createdOrUpdatedApiEntity.getId()).
                            name(planNode.get("name").asText()).
                            security(PlanSecurityType.valueOf(planNode.get("security").asText())).
                            build();
                    List<PlanEntity> planEntities = planService.search(query);
                    if (planEntities == null || planEntities.isEmpty()) {
                        NewPlanEntity newPlanEntity = objectMapper.readValue(planNode.toString(), NewPlanEntity.class);
                        newPlanEntity.setApi(createdOrUpdatedApiEntity.getId());
                        planService.create(newPlanEntity);
                    } else if (planEntities.size() == 1) {
                        UpdatePlanEntity updatePlanEntity = objectMapper.readValue(planNode.toString(), UpdatePlanEntity.class);
                        planService.update(updatePlanEntity);
                    } else {
                        LOGGER.error("Not able to identify the plan to update: {}. Too much plan with the same name", planNode.get("name").asText());
                        throw new TechnicalManagementException("Not able to identify the plan to update: " + planNode.get("name").asText() + ". Too much plan with the same name");
                    }
                }
            }
            return createdOrUpdatedApiEntity;
        } catch (final IOException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", apiDefinition, e);
        }
        return null;
    }

    @Override
    public InlinePictureEntity getPicture(String apiId) {
        ApiEntity apiEntity = findById(apiId);
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        if (apiEntity.getPicture() == null) {
            imageEntity.setType("image/png");
            try {
                imageEntity.setContent(IOUtils.toByteArray(new FileInputStream(defaultIcon)));
            } catch (IOException ioe) {
                LOGGER.error("Default icon for API does not exist", ioe);
            }

        } else {
            String[] parts = apiEntity.getPicture().split(";", 2);
            imageEntity.setType(parts[0].split(":")[1]);
            String base64Content = apiEntity.getPicture().split(",", 2)[1];
            imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
        }

        return imageEntity;
    }

    @Override
    public void deleteViewFromAPIs(final String viewId) {
        findAll().forEach(api -> {
            if (api.getViews() != null && api.getViews().contains(viewId)) {
                removeView(api.getId(), viewId);
            }
        });
    }

    private void removeView(String apiId, String viewId) throws TechnicalManagementException {
        try {
            Optional<Api> optApi = apiRepository.findById(apiId);
            if (optApi.isPresent()) {
                Api api = optApi.get();
                Api previousApi = new Api(api);
                api.getViews().remove(viewId);
                api.setUpdatedAt(new Date());
                apiRepository.update(api);
                // Audit
                auditService.createApiAuditLog(
                        apiId,
                        Collections.emptyMap(),
                        API_UPDATED,
                        api.getUpdatedAt(),
                        previousApi,
                        api);
            } else {
                throw new ApiNotFoundException(apiId);
            }
        } catch (Exception ex) {
            LOGGER.error("An error occurs while removing view from API: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while removing view from API: " + apiId, ex);
        }
    }

    @Override
    public void deleteTagFromAPIs(final String tagId) {
        findAll().forEach(api -> {
            if (api.getTags() != null && api.getTags().contains(tagId)) {
                removeTag(api.getId(), tagId);
            }
        });
    }

    @Override
    public ApiModelEntity findByIdForTemplates(String apiId) {
        final ApiEntity apiEntity = findById(apiId);

        final ApiModelEntity apiModelEntity = new ApiModelEntity();

        apiModelEntity.setId(apiEntity.getId());
        apiModelEntity.setName(apiEntity.getName());
        apiModelEntity.setDescription(apiEntity.getDescription());
        apiModelEntity.setCreatedAt(apiEntity.getCreatedAt());
        apiModelEntity.setDeployedAt(apiEntity.getDeployedAt());
        apiModelEntity.setUpdatedAt(apiEntity.getUpdatedAt());
        apiModelEntity.setGroups(apiEntity.getGroups());
        apiModelEntity.setVisibility(apiEntity.getVisibility());
        apiModelEntity.setViews(apiEntity.getViews());
        apiModelEntity.setVersion(apiEntity.getVersion());
        apiModelEntity.setState(apiEntity.getState());
        apiModelEntity.setTags(apiEntity.getTags());
        apiModelEntity.setServices(apiEntity.getServices());
        apiModelEntity.setPaths(apiEntity.getPaths());
        apiModelEntity.setRole(apiEntity.getRole());
        apiModelEntity.setPicture(apiEntity.getPicture());
        apiModelEntity.setPictureUrl(apiEntity.getPictureUrl());
        apiModelEntity.setPrimaryOwner(apiEntity.getPrimaryOwner());
        apiModelEntity.setProperties(apiEntity.getProperties());
        apiModelEntity.setProxy(apiEntity.getProxy());

        final List<ApiMetadataEntity> metadataList = apiMetadataService.findAllByApi(apiId);

        if (metadataList != null) {
            final Map<String, String> mapMetadata = new HashMap<>(metadataList.size());
            metadataList.forEach(metadata -> mapMetadata.put(metadata.getKey(),
                    metadata.getValue() == null ? metadata.getDefaultValue() : metadata.getValue()));
            apiModelEntity.setMetadata(mapMetadata);
        }
        return apiModelEntity;
    }

    @Override
    public boolean exists(final String apiId) {
        try {
            return apiRepository.findById(apiId).isPresent();
        } catch (final TechnicalException te) {
            final String msg = "An error occurs while checking if the API exists: " + apiId;
            LOGGER.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    @Override
    public ApiEntity importPathMappingsFromPage(final ApiEntity apiEntity, final String page) {
        final PageEntity pageEntity = pageService.findById(page);
        if (SWAGGER.name().equals(pageEntity.getType())) {
            final ImportSwaggerDescriptorEntity importSwaggerDescriptorEntity = new ImportSwaggerDescriptorEntity();
            importSwaggerDescriptorEntity.setPayload(pageEntity.getContent());
            final NewApiEntity newApiEntity = swaggerService.prepare(importSwaggerDescriptorEntity);
            apiEntity.getPathMappings().addAll(newApiEntity.getPaths());
        }

        return update(apiEntity.getId(), ApiService.convert(apiEntity));
    }

    public Collection<ApiEntity> search(final ApiQuery query) {
        try {
            LOGGER.debug("Search APIs by {}", query);
            return convert(apiRepository.search(queryToCriteria(query).build())).stream()
                    .filter(api -> query.getTag() == null || (api.getTags() != null && api.getTags().contains(query.getTag())))
                    .filter(api -> query.getContextPath() == null || query.getContextPath().equals(api.getProxy().getContextPath()))
                    .collect(toList());
        } catch (TechnicalException ex) {
            final String errorMessage = "An error occurs while trying to search for APIs: " + query;
            LOGGER.error(errorMessage, ex);
            throw new TechnicalManagementException(errorMessage, ex);
        }
    }

    private ApiCriteria.Builder queryToCriteria(ApiQuery query) {
        final ApiCriteria.Builder builder = new ApiCriteria.Builder();
        if (query == null) {
            return builder;
        }
        builder.label(query.getLabel())
                .name(query.getName())
                .version(query.getVersion())
                .view(query.getView());

        if (!isBlank(query.getGroup())) {
            builder.groups(query.getGroup());
        }
        if (!isBlank(query.getState())) {
            builder.state(LifecycleState.valueOf(query.getState()));
        }
        if (query.getVisibility() != null) {
            builder.visibility(Visibility.valueOf(query.getVisibility().name()));
        }

        return builder;
    }

    private void removeTag(String apiId, String tagId) throws TechnicalManagementException {
        try {
            ApiEntity apiEntity = this.findById(apiId);
            apiEntity.getTags().remove(tagId);
            update(apiId, ApiService.convert(apiEntity));
        } catch (Exception ex) {
            LOGGER.error("An error occurs while removing tag from API: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while removing tag from API: " + apiId, ex);
        }
    }

    private ApiEntity updateLifecycle(String apiId, LifecycleState lifecycleState, String username) throws TechnicalException {
        Optional<Api> optApi = apiRepository.findById(apiId);
        if (optApi.isPresent()) {
            Api api = optApi.get();
            Api previousApi = new Api(api);
            api.setUpdatedAt(new Date());
            api.setLifecycleState(lifecycleState);
            ApiEntity apiEntity = convert(apiRepository.update(api));
            // Audit
            auditService.createApiAuditLog(
                    apiId,
                    Collections.emptyMap(),
                    API_UPDATED,
                    api.getUpdatedAt(),
                    previousApi,
                    api);

            EventType eventType = null;
            switch (lifecycleState) {
                case STARTED:
                    eventType = EventType.START_API;
                    break;
                case STOPPED:
                    eventType = EventType.STOP_API;
                    break;
                default:
                    break;
            }
            final ApiEntity deployedApi = deployLastPublishedAPI(apiId, username, eventType);
            if (deployedApi != null) {
                return deployedApi;
            }
            return apiEntity;
        } else {
            throw new ApiNotFoundException(apiId);
        }
    }

    private Set<ApiEntity> convert(final List<Api> apis) throws TechnicalException {
        if (apis == null || apis.isEmpty()) {
            return Collections.emptySet();
        }
        //find primary owners usernames of each apis
        Set<Membership> memberships = membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.API,
                apis.stream().map(Api::getId).collect(toList()),
                RoleScope.API,
                SystemRole.PRIMARY_OWNER.name()
        );

        int poMissing = apis.size() - memberships.size();
        final Set<String> apiIds = apis.stream().map(Api::getId).collect(Collectors.toSet());
        Stream<Api> streamApis = apis.stream();
        if (poMissing > 0) {
            Set<String> apiMembershipsIds = memberships.stream().map(Membership::getReferenceId).collect(Collectors.toSet());

            apiIds.removeAll(apiMembershipsIds);
            Optional<String> optionalApisAsString = apiIds.stream().reduce((a, b) -> a + " / " + b);
            String apisAsString = "?";
            if (optionalApisAsString.isPresent()) {
                apisAsString = optionalApisAsString.get();
            }
            LOGGER.error("{} apis has no identified primary owners in this list {}.", poMissing, apisAsString);
            streamApis = streamApis.filter(api -> !apiIds.contains(api.getId()));
        }

        Map<String, String> apiToUser = new HashMap<>(memberships.size());
        memberships.forEach(membership -> apiToUser.put(membership.getReferenceId(), membership.getUserId()));

        Map<String, UserEntity> userIdToUserEntity = new HashMap<>(memberships.size());
        userService.findByIds(memberships.stream().map(Membership::getUserId).collect(toList()))
                .forEach(userEntity -> userIdToUserEntity.put(userEntity.getId(), userEntity));

        return streamApis
                .map(publicApi -> this.convert(publicApi, userIdToUserEntity.get(apiToUser.get(publicApi.getId()))))
                .collect(Collectors.toSet());
    }

    private ApiEntity convert(Api api) {
        return convert(api, null);
    }

    private ApiEntity convert(Api api, UserEntity primaryOwner) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setName(api.getName());
        apiEntity.setDeployedAt(api.getDeployedAt());
        apiEntity.setCreatedAt(api.getCreatedAt());
        apiEntity.setGroups(api.getGroups());

        if (api.getDefinition() != null) {
            try {
                io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(api.getDefinition(),
                        io.gravitee.definition.model.Api.class);

                apiEntity.setProxy(apiDefinition.getProxy());
                apiEntity.setPaths(apiDefinition.getPaths());
                apiEntity.setServices(apiDefinition.getServices());
                apiEntity.setResources(apiDefinition.getResources());
                apiEntity.setProperties(apiDefinition.getProperties());
                apiEntity.setTags(apiDefinition.getTags());
                if (apiDefinition.getPathMappings() != null) {
                    apiEntity.setPathMappings(new HashSet<>(apiDefinition.getPathMappings().keySet()));
                }
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while generating API definition", ioe);
            }
        }
        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setVersion(api.getVersion());
        apiEntity.setDescription(api.getDescription());
        apiEntity.setPicture(api.getPicture());
        apiEntity.setViews(api.getViews());
        apiEntity.setLabels(api.getLabels());

        final LifecycleState lifecycleState = api.getLifecycleState();
        if (lifecycleState != null) {
            apiEntity.setState(Lifecycle.State.valueOf(lifecycleState.name()));
        }
        if (api.getVisibility() != null) {
            apiEntity.setVisibility(io.gravitee.management.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        if (primaryOwner != null) {
            apiEntity.setPrimaryOwner(new PrimaryOwnerEntity(primaryOwner));
        }

        return apiEntity;
    }

    private Api convert(String apiId, UpdateApiEntity updateApiEntity) {
        Api api = new Api();

        if (updateApiEntity.getVisibility() != null) {
            api.setVisibility(Visibility.valueOf(updateApiEntity.getVisibility().toString()));
        }

        api.setVersion(updateApiEntity.getVersion().trim());
        api.setName(updateApiEntity.getName().trim());
        api.setDescription(updateApiEntity.getDescription().trim());
        api.setPicture(updateApiEntity.getPicture());
        api.setViews(updateApiEntity.getViews());

        if (updateApiEntity.getLabels() != null) {
            api.setLabels(new ArrayList(new LinkedHashSet<>(updateApiEntity.getLabels())));
        }

        api.setGroups(updateApiEntity.getGroups());

        try {
            io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
            apiDefinition.setId(apiId);
            apiDefinition.setName(updateApiEntity.getName());
            apiDefinition.setVersion(updateApiEntity.getVersion());
            apiDefinition.setProxy(updateApiEntity.getProxy());
            apiDefinition.setPaths(updateApiEntity.getPaths());

            apiDefinition.setServices(updateApiEntity.getServices());
            apiDefinition.setResources(updateApiEntity.getResources());
            apiDefinition.setProperties(updateApiEntity.getProperties());
            apiDefinition.setTags(updateApiEntity.getTags());
            if (updateApiEntity.getPathMappings() != null) {
                apiDefinition.setPathMappings(updateApiEntity.getPathMappings().stream()
                        .collect(toMap(pathMapping -> pathMapping, pathMapping -> Pattern.compile(""))));
            }

            String definition = objectMapper.writeValueAsString(apiDefinition);
            api.setDefinition(definition);
            return api;
        } catch (JsonProcessingException jse) {
            LOGGER.error("Unexpected error while generating API definition", jse);
        }

        return null;
    }

    private LifecycleState convert(EventType eventType) {
        LifecycleState lifecycleState;
        switch (eventType) {
            case START_API:
                lifecycleState = LifecycleState.STARTED;
                break;
            case STOP_API:
                lifecycleState = LifecycleState.STOPPED;
                break;
            default:
                throw new IllegalArgumentException("Unknown EventType " + eventType.toString() + " to convert EventType into Lifecycle");
        }
        return lifecycleState;
    }

    private static class MemberToImport {
        private String username;
        private String role;

        public MemberToImport() {
        }

        public MemberToImport(String username, String role) {
            this.username = username;
            this.role = role;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MemberToImport that = (MemberToImport) o;

            return username.equals(that.username);
        }

        @Override
        public int hashCode() {
            return username.hashCode();
        }
    }
}
