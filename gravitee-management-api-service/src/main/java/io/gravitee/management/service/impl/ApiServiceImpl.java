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
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Proxy;
import io.gravitee.management.model.*;
import io.gravitee.management.model.EventType;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.processor.ApiSynchronizationProcessor;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
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
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiServiceImpl extends TransactionalService implements ApiService {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiServiceImpl.class);

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

    @Value("${configuration.default-icon:${gravitee.home}/config/default-icon.png}")
    private String defaultIcon;

    @Autowired
    private ApiMetadataService apiMetadataService;

    @Override
    public ApiEntity create(NewApiEntity newApiEntity, String username) throws ApiAlreadyExistsException {
        UpdateApiEntity apiEntity = new UpdateApiEntity();

        apiEntity.setName(newApiEntity.getName());
        apiEntity.setDescription(newApiEntity.getDescription());
        apiEntity.setVersion(newApiEntity.getVersion());
        apiEntity.setGroup(newApiEntity.getGroup());

        Proxy proxy = new Proxy();
        proxy.setContextPath(newApiEntity.getContextPath());
        proxy.setEndpoints(new LinkedHashSet<>());
        proxy.getEndpoints().add(new Endpoint("default", newApiEntity.getEndpoint()));
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
        }).collect(Collectors.toMap(Path::getPath, path -> path));

        apiEntity.setPaths(paths);

        return create0(apiEntity, username);
    }

    private ApiEntity create0(UpdateApiEntity api, String username) throws ApiAlreadyExistsException {
        try {
            LOGGER.debug("Create {} for user {}", api, username);

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
                repoApi.setVisibility(Visibility.PRIVATE);

                Api createdApi = apiRepository.create(repoApi);

                // Add the primary owner of the newly created API
                UserEntity primaryOwner = userService.findByName(username);
                Membership membership = new Membership(primaryOwner.getUsername(), createdApi.getId(), MembershipReferenceType.API);
                membership.setType(MembershipType.PRIMARY_OWNER.name());
                membership.setCreatedAt(repoApi.getCreatedAt());
                membership.setUpdatedAt(repoApi.getCreatedAt());
                membershipRepository.create(membership);

                return convert(createdApi, primaryOwner, true);
            } else {
                LOGGER.error("Unable to create API {} because of previous error.");
                throw new TechnicalManagementException("Unable to create API " + id);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", api, username, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + api + " for user " + username, ex);
        }
    }

    public void checkContextPath(final String newContextPath) throws TechnicalException {
        checkContextPath(newContextPath, null);
    }

    private void checkContextPath(String newContextPath, final String apiId) throws TechnicalException {
        if (newContextPath.charAt(newContextPath.length() - 1) == '/') {
            newContextPath = newContextPath.substring(0, newContextPath.length() - 1);
        }

        final int indexOfEndOfNewSubContextPath = newContextPath.lastIndexOf('/', 1);
        final String newSubContextPath = newContextPath.substring(0, indexOfEndOfNewSubContextPath <= 0 ?
                newContextPath.length() : indexOfEndOfNewSubContextPath) + '/';

        final boolean contextPathExists = apiRepository.findAll().stream()
                .filter(api -> !api.getId().equals(apiId))
                .anyMatch(api -> {
                    final String contextPath = convert(api, null, true).getProxy().getContextPath();
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
                Optional<Membership> primaryOwnerMembership = membershipRepository.findByReferenceAndMembershipType(
                        MembershipReferenceType.API,
                        api.get().getId(),
                        MembershipType.PRIMARY_OWNER.name())
                        .stream()
                        .findFirst();
                if (!primaryOwnerMembership.isPresent()) {
                    LOGGER.error("The API {} doesn't have any primary owner.", apiId);
                    throw new TechnicalException("The API " + apiId + " doesn't have any primary owner.");
                }
                return convert(api.get(), userService.findByName(primaryOwnerMembership.get().getUserId()), true);
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
            return convert(apiRepository.findByVisibility(Visibility.valueOf(visibility.name())), false);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public Set<ApiEntity> findAll() {
        try {
            LOGGER.debug("Find all APIs");
            return convert(apiRepository.findAll(), true);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public Set<ApiEntity> findByUser(String username) {
        try {
            LOGGER.debug("Find APIs by user {}", username);

            Set<Api> publicApis = apiRepository.findByVisibility(Visibility.PUBLIC);
            Set<Api> userApis = apiRepository.findByIds(
                    membershipRepository.findByUserAndReferenceType(username, MembershipReferenceType.API).stream()
                            .map(Membership::getReferenceId)
                            .collect(Collectors.toList())
            );

            List<String> groupIds = membershipRepository.findByUserAndReferenceType(username, MembershipReferenceType.API_GROUP).stream()
                    .map(Membership::getReferenceId).collect(Collectors.toList());
            Set<Api> groupApis = apiRepository.findByGroups(groupIds);

            final Set<ApiEntity> apis = new HashSet<>(publicApis.size() + userApis.size() + groupApis.size());

            apis.addAll(convert(publicApis, true));
            apis.addAll(convert(userApis, true));

            apis.addAll(convert(groupApis, true));

            return apis;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find APIs for user {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to find APIs for user " + username, ex);
        }
    }

    @Override
    public Set<ApiEntity> findByGroup(String groupId) {
        LOGGER.debug("Find APIs by group {}", groupId);
        try {
            return convert(apiRepository.findByGroups(Collections.singletonList(groupId)), false);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find APIs for group {}", groupId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find APIs for group " + groupId, ex);
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
                return convert(Collections.singleton(updatedApi), true).iterator().next();
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

            Optional<Api> optApi = apiRepository.findById(apiName);
            if (! optApi.isPresent()) {
                throw new ApiNotFoundException(apiName);
            }

            if (optApi.get().getLifecycleState() == LifecycleState.STARTED) {
                throw new ApiRunningStateException(apiName);
            } else {
                // Delete plans
                Set<PlanEntity> plans = planService.findByApi(apiName);
                Set<String> plansNotClosed = plans.stream()
                        .filter(plan -> plan.getStatus() == PlanStatus.PUBLISHED)
                        .map(PlanEntity::getName)
                        .collect(Collectors.toSet());

                if (! plansNotClosed.isEmpty()) {
                    throw new ApiNotDeletableException("Plan(s) [" + String.join(", ", plansNotClosed) +
                            "] must be closed before being able to delete the API !");
                }

                plans.stream().forEach(plan -> planService.delete(plan.getId()));

                // Delete events
                eventService.findByApi(apiName)
                        .forEach(event -> eventService.delete(event.getId()));

                // Delete API
                apiRepository.delete(apiName);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete API {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete API " + apiName, ex);
        }
    }

    @Override
    public void start(String apiId, String username) {
        try {
            LOGGER.debug("Start API {}", apiId);
            updateLifecycle(apiId, LifecycleState.STARTED, username);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to start API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to start API " + apiId, ex);
        }
    }

    @Override
    public void stop(String apiId, String username) {
        try {
            LOGGER.debug("Stop API {}", apiId);
            updateLifecycle(apiId, LifecycleState.STOPPED, username);
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
                    eventService.search(Arrays.asList(EventType.PUBLISH_API, EventType.UNPUBLISH_API),
                            properties, 0, 0, 0, 1);

            if (! events.getContent().isEmpty()) {
                // According to page size, we know that we have only one element in the list
                EventEntity lastEvent = events.getContent().get(0);

                //TODO: Done only for backward compatibility with 0.x. Must be removed later (1.1.x ?)
                boolean enabled = objectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                Api payloadEntity = objectMapper.readValue(lastEvent.getPayload(), Api.class);
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, enabled);

                boolean sync = apiSynchronizationProcessor.processCheckSynchronization(convert(payloadEntity, true), api);

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

    @Override
    public ApiEntity deploy(String apiId, String username, EventType eventType) {
        try {
            LOGGER.debug("Deploy API : {}", apiId);

            return deployCurrentAPI(apiId, username, eventType);
        } catch (Exception ex) {
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

    private ApiEntity deployCurrentAPI(String apiId, String username, EventType eventType) throws Exception {
        Optional<Api> api = apiRepository.findById(apiId);

        if (api.isPresent()) {
            // add deployment date
            Api apiValue = api.get();
            apiValue.setUpdatedAt(new Date());
            apiValue.setDeployedAt(apiValue.getUpdatedAt());
            apiValue = apiRepository.update(apiValue);

            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.API_ID.getValue(), apiValue.getId());
            properties.put(Event.EventProperties.USERNAME.getValue(), username);

            // Clear useless field for history
            apiValue.setPicture(null);

            // And create event
            eventService.create(eventType, objectMapper.writeValueAsString(apiValue), properties);

            return convert(Collections.singleton(apiValue), true).iterator().next();
        } else {
            throw new ApiNotFoundException(apiId);
        }
    }

    private ApiEntity deployLastPublishedAPI(String apiId, String username, EventType eventType) throws TechnicalException {
        Optional<EventEntity> optEvent = eventService.findByApi(apiId).stream()
                .filter(event -> EventType.PUBLISH_API.equals(event.getType()))
                .sorted((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt())).findFirst();
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
                properties.put(Event.EventProperties.USERNAME.getValue(), username);

                // Clear useless field for history
                lastPublishedAPI.setPicture(null);

                // And create event
                eventService.create(eventType, objectMapper.writeValueAsString(lastPublishedAPI), properties);
                return convert(Collections.singleton(lastPublishedAPI), true).iterator().next();
            } else {
                throw new TechnicalException("No event found for API " + apiId);
            }
        } catch (Exception e) {
            LOGGER.error("An error occurs while trying to deploy last published API {}", apiId, e);
            throw new TechnicalException("An error occurs while trying to deploy last published API " + apiId, e);
        }
    }

    @Override
    public String exportAsJson(final String apiId, io.gravitee.management.model.MembershipType membershipType, String... filteredFields) {
        final ApiEntity apiEntity = findById(apiId);
        List<String> filteredFiedsList = Arrays.asList(filteredFields);

        apiEntity.setId(null);
        apiEntity.setCreatedAt(null);
        apiEntity.setUpdatedAt(null);
        apiEntity.setDeployedAt(null);
        apiEntity.setPrimaryOwner(null);
        apiEntity.setState(null);
        // because ApiEntity checks permission before export datas,
        // we have to set membershipType.
        // see annotations `MembershipTypesAllowed` ok ApiEntity class.
        apiEntity.setPermission(membershipType);
        ObjectNode apiJsonNode = objectMapper.valueToTree(apiEntity);
        String field;

        field = "group";
        if (!filteredFiedsList.contains(field)) {
             apiJsonNode.remove(field);
            if (apiEntity.getGroup() != null) {
                apiJsonNode.put(field, apiEntity.getGroup().getName());
            }
        }

        field = "members";
        if (!filteredFiedsList.contains(field)) {
            Set<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, apiId);
            if (members != null) {
                members.forEach(m -> {
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
    public ApiEntity createOrUpdateWithDefinition(final ApiEntity apiEntity, String apiDefinition, String username) {
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

            //create group if not exist
            if (importedApi.getGroup() != null) {
                List<GroupEntity> groupEntities = groupService.findByTypeAndName(GroupEntityType.API, importedApi.getGroup());
                GroupEntity group;
                if (groupEntities.isEmpty()) {
                    NewGroupEntity newGroupEntity = new NewGroupEntity();
                    newGroupEntity.setName(importedApi.getGroup());
                    newGroupEntity.setType(GroupEntityType.API);
                    group = groupService.create(newGroupEntity);
                } else {
                    group = groupEntities.get(0);
                }
                importedApi.setGroup(group.getId());
            }

            ApiEntity createdOrUpdatedApiEntity;
            if (apiEntity == null || apiEntity.getId() == null) {
                createdOrUpdatedApiEntity = create0(importedApi, username);
            }
            else {
                createdOrUpdatedApiEntity = update(apiEntity.getId(), importedApi);
            }

            final JsonNode jsonNode = objectMapper.readTree(apiDefinition);
            // Members
            final JsonNode membersDefinition = jsonNode.path("members");
            if (membersDefinition != null && membersDefinition.isArray()) {
                for (final JsonNode memberNode : membersDefinition) {
                    MemberEntity memberEntity = objectMapper.readValue(memberNode.toString(), MemberEntity.class);
                    membershipService.addOrUpdateMember(
                            MembershipReferenceType.API,
                            createdOrUpdatedApiEntity.getId(),
                            memberEntity.getUsername(),
                            memberEntity.getType());
                }
            }
            //Pages
            final JsonNode pagesDefinition = jsonNode.path("pages");
            if (pagesDefinition != null && pagesDefinition.isArray()) {
                for (final JsonNode pageNode : pagesDefinition) {
                    pageService.createApiPage(createdOrUpdatedApiEntity.getId(), objectMapper.readValue(pageNode.toString(), NewPageEntity.class));
                }
            }
            //Plans
            final JsonNode plansDefinition = jsonNode.path("plans");
            if (plansDefinition != null && plansDefinition.isArray()) {
                for (JsonNode planNode : plansDefinition) {
                    NewPlanEntity newPlanEntity = objectMapper.readValue(planNode.toString(), NewPlanEntity.class);
                    newPlanEntity.setApi(createdOrUpdatedApiEntity.getId());
                    planService.create(newPlanEntity);
                }
            }
            return createdOrUpdatedApiEntity;
        } catch (final IOException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", apiDefinition, e);
        }
        return null;
    }

    @Override
    public ImageEntity getPicture(String apiId) {
        ApiEntity apiEntity = findById(apiId);
        ImageEntity imageEntity = new ImageEntity();
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
                api.getViews().remove(viewId);
                apiRepository.update(api);
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
        apiModelEntity.setGroup(apiEntity.getGroup());
        apiModelEntity.setVisibility(apiEntity.getVisibility());
        apiModelEntity.setViews(apiEntity.getViews());
        apiModelEntity.setVersion(apiEntity.getVersion());
        apiModelEntity.setState(apiEntity.getState());
        apiModelEntity.setTags(apiEntity.getTags());
        apiModelEntity.setServices(apiEntity.getServices());
        apiModelEntity.setPaths(apiEntity.getPaths());
        apiModelEntity.setPermission(apiEntity.getPermission());
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

    private void removeTag(String apiId, String tagId) throws TechnicalManagementException {
        try {
            ApiEntity apiEntity = this.findById(apiId);
            apiEntity.getTags().remove(tagId);
            update(apiId, convert(apiEntity));
        } catch (Exception ex) {
            LOGGER.error("An error occurs while removing tag from API: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while removing tag from API: " + apiId, ex);
        }
    }

    private void updateLifecycle(String apiId, LifecycleState lifecycleState, String username) throws TechnicalException {
        Optional<Api> optApi = apiRepository.findById(apiId);
        if (optApi.isPresent()) {
            Api api = optApi.get();
            api.setUpdatedAt(new Date());
            api.setLifecycleState(lifecycleState);
            apiRepository.update(api);

            switch (lifecycleState) {
                case STARTED:
                    deployLastPublishedAPI(apiId, username, EventType.START_API);
                    break;
                case STOPPED:
                    deployLastPublishedAPI(apiId, username, EventType.STOP_API);
                    break;
                default:
                    break;
            }
        } else {
            throw new ApiNotFoundException(apiId);
        }
    }

    private Set<ApiEntity> convert(Set<Api> apis, boolean readDefinition) throws TechnicalException {
        if (apis == null || apis.isEmpty()) {
            return Collections.emptySet();
        }
        //find primary owners usernames of each apis
        Set<Membership> memberships = membershipRepository.findByReferencesAndMembershipType(
                MembershipReferenceType.API,
                apis.stream().map(Api::getId).collect(Collectors.toList()),
                MembershipType.PRIMARY_OWNER.name()
        );

        int poMissing = apis.size() - memberships.size();
        if (poMissing > 0) {
            Optional<String> optionalApisAsString = apis.stream().map(Api::getId).reduce((a, b) -> a + " / " + b);
            String apisAsString = "?";
            if (optionalApisAsString.isPresent())
                apisAsString = optionalApisAsString.get();
            LOGGER.error("{} apis has no identified primary owners in this list {}.", poMissing , apisAsString);
            throw new TechnicalManagementException(poMissing + " apis has no identified primary owners in this list " + apisAsString + ".");
        }

        Map<String, String> apiToUser = new HashMap<>(memberships.size());
        memberships.forEach(membership -> apiToUser.put(membership.getReferenceId(), membership.getUserId()));

        Map<String, UserEntity> userIdToUserEntity = new HashMap<>(memberships.size());
        userService.findByNames(memberships.stream().map(Membership::getUserId).collect(Collectors.toList()))
                .forEach(userEntity -> userIdToUserEntity.put(userEntity.getUsername(), userEntity));

        return apis.stream()
                .map(publicApi -> this.convert(publicApi, userIdToUserEntity.get(apiToUser.get(publicApi.getId())), readDefinition))
                .collect(Collectors.toSet());
    }

    private ApiEntity convert(Api api, boolean readDefinition) {
        return convert(api, null, readDefinition);
    }

    private ApiEntity convert(Api api, UserEntity primaryOwner, boolean readDefinition) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setName(api.getName());
        apiEntity.setDeployedAt(api.getDeployedAt());
        apiEntity.setCreatedAt(api.getCreatedAt());
        if(api.getGroup() != null) {
            apiEntity.setGroup(groupService.findById(api.getGroup()));
        }

        if (readDefinition && api.getDefinition() != null) {
            try {
                io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(api.getDefinition(),
                        io.gravitee.definition.model.Api.class);

                apiEntity.setProxy(apiDefinition.getProxy());
                apiEntity.setPaths(apiDefinition.getPaths());
                apiEntity.setServices(apiDefinition.getServices());
                apiEntity.setResources(apiDefinition.getResources());
                apiEntity.setProperties(apiDefinition.getProperties());
                apiEntity.setTags(apiDefinition.getTags());
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while generating API definition", ioe);
            }
        }
        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setVersion(api.getVersion());
        apiEntity.setDescription(api.getDescription());
        apiEntity.setPicture(api.getPicture());
        apiEntity.setViews(api.getViews());

        final LifecycleState lifecycleState = api.getLifecycleState();
        if (lifecycleState != null) {
            apiEntity.setState(Lifecycle.State.valueOf(lifecycleState.name()));
        }
        if (api.getVisibility() != null) {
            apiEntity.setVisibility(io.gravitee.management.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        if (primaryOwner != null) {
            final PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
            primaryOwnerEntity.setUsername(primaryOwner.getUsername());
            primaryOwnerEntity.setLastname(primaryOwner.getLastname());
            primaryOwnerEntity.setFirstname(primaryOwner.getFirstname());
            primaryOwnerEntity.setEmail(primaryOwner.getEmail());
            apiEntity.setPrimaryOwner(primaryOwnerEntity);
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

        if (updateApiEntity.getGroup() != null && !updateApiEntity.getGroup().isEmpty()) {
            try {
                groupService.findById(updateApiEntity.getGroup());
                api.setGroup(updateApiEntity.getGroup());
            } catch (GroupNotFoundException gnfe) {
                throw new InvalidDataException("Group [" + updateApiEntity.getGroup() + "] does not exist");
            }
        }

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

            String definition = objectMapper.writeValueAsString(apiDefinition);
            api.setDefinition(definition);
            return api;
        } catch (JsonProcessingException jse) {
            LOGGER.error("Unexpected error while generating API definition", jse);
        }

        return null;
    }

    private static UpdateApiEntity convert(ApiEntity apiEntity) {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();

        updateApiEntity.setProxy(apiEntity.getProxy());
        updateApiEntity.setVersion(apiEntity.getVersion());
        updateApiEntity.setName(apiEntity.getName());
        updateApiEntity.setProperties(apiEntity.getProperties());
        updateApiEntity.setDescription(apiEntity.getDescription());

        if (apiEntity.getGroup() != null) {
            updateApiEntity.setGroup(apiEntity.getGroup().getId());
        }
        updateApiEntity.setPaths(apiEntity.getPaths());
        updateApiEntity.setPicture(apiEntity.getPicture());
        updateApiEntity.setResources(apiEntity.getResources());
        updateApiEntity.setTags(apiEntity.getTags());
        updateApiEntity.setServices(apiEntity.getServices());
        updateApiEntity.setVisibility(apiEntity.getVisibility());

        return updateApiEntity;
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
}
