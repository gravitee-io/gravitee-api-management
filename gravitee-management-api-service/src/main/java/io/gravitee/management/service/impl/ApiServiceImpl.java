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

    @Override
    public ApiEntity create(NewApiEntity newApiEntity, String username) throws ApiAlreadyExistsException {
        UpdateApiEntity apiEntity = new UpdateApiEntity();

        apiEntity.setName(newApiEntity.getName());
        apiEntity.setDescription(newApiEntity.getDescription());
        apiEntity.setVersion(newApiEntity.getVersion());
        apiEntity.setGroup(newApiEntity.getGroup());

        Proxy proxy = new Proxy();
        proxy.setContextPath(newApiEntity.getContextPath());
        Endpoint defaultEdp = new Endpoint(newApiEntity.getEndpoint());
        defaultEdp.setName("default");
        proxy.setEndpoints(Collections.singletonList(defaultEdp));
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

                return convert(createdApi, primaryOwner);
            } else {
                LOGGER.error("Unable to create API {} because of previous error.");
                throw new TechnicalManagementException("Unable to create API " + id);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", api, username, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + api + " for user " + username, ex);
        }
    }

    private void checkContextPath(final String newContextPath) throws TechnicalException {
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
                return convert(api.get(), userService.findByName(primaryOwnerMembership.get().getUserId()));
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
            return convert(apiRepository.findByVisibility(Visibility.valueOf(visibility.name())));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public Set<ApiEntity> findAll() {
        try {
            LOGGER.debug("Find all APIs");
            return convert(apiRepository.findAll());
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

            final Set<ApiEntity> apis = new HashSet<>(publicApis.size() + userApis.size());

            apis.addAll(convert(publicApis));
            apis.addAll(convert(userApis));

            apis.addAll(convert(groupApis));

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
            return convert(apiRepository.findByGroups(Collections.singletonList(groupId)));
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
                return convert(Collections.singleton(updatedApi)).iterator().next();
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

                // Delete plans
                planService.findByApi(apiName)
                        .forEach(plan -> planService.delete(plan.getId()));

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

                boolean sync = apiSynchronizationProcessor.processCheckSynchronization(convert(payloadEntity), api);

                // 2_ If APY definition is synchronized, check if there is any modification for API's plans
                if (sync) {
                    Set<PlanEntity> plans = planService.findByApi(api.getId());
                    sync = plans.stream().filter(plan -> plan.getUpdatedAt().after(api.getUpdatedAt())).count() == 0;
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
            Map<String, String> properties = new HashMap<>();
            properties.put(Event.EventProperties.API_ID.getValue(), api.get().getId());
            properties.put(Event.EventProperties.USERNAME.getValue(), username);
            EventEntity event = eventService.create(eventType, objectMapper.writeValueAsString(api.get()), properties);
            // add deployment date
            if (event != null) {
                Api apiValue = api.get();
                apiValue.setDeployedAt(event.getCreatedAt());
                apiValue.setUpdatedAt(event.getCreatedAt());
                apiRepository.update(apiValue);
            }
            return convert(Collections.singleton(api.get())).iterator().next();
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
                lastPublishedAPI.setDeployedAt(lastPublishedAPI.getUpdatedAt());
                Map<String, String> properties = new HashMap<>();
                properties.put(Event.EventProperties.API_ID.getValue(), lastPublishedAPI.getId());
                properties.put(Event.EventProperties.USERNAME.getValue(), username);
                eventService.create(eventType, objectMapper.writeValueAsString(lastPublishedAPI), properties);
                return convert(Collections.singleton(lastPublishedAPI)).iterator().next();
            } else {
                throw new TechnicalException("No event found for API " + apiId);
            }
        } catch (Exception e) {
            LOGGER.error("An error occurs while trying to deploy last published API {}", apiId, e);
            throw new TechnicalException("An error occurs while trying to deploy last published API " + apiId, e);
        }
    }

    @Override
    public String exportAsJson(final String apiId, io.gravitee.management.model.MembershipType membershipType) {
        final ApiEntity apiEntity = findById(apiId);

        apiEntity.setId(null);
        apiEntity.setCreatedAt(null);
        apiEntity.setUpdatedAt(null);
        apiEntity.setDeployedAt(null);
        apiEntity.setPrimaryOwner(null);
        apiEntity.setState(null);
        apiEntity.setPermission(membershipType);

        Set<MemberEntity> members = membershipService.getMembers(MembershipReferenceType.API, apiId);
        if (members != null) {
            members.forEach(m -> {
                m.setCreatedAt(null);
                m.setUpdatedAt(null);
            });
        }
        List<PageListItem> pageListItems = pageService.findByApi(apiId);
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
        try {
            ObjectNode apiJsonNode = objectMapper.valueToTree(apiEntity);
            apiJsonNode.remove("permission");
            apiJsonNode.remove("group");
            if (apiEntity.getGroup() != null) {
                apiJsonNode.put("group", apiEntity.getGroup().getName());
            }
            apiJsonNode.putPOJO("members", members == null ? Collections.emptyList() : members);
            apiJsonNode.putPOJO("pages", pages == null ? Collections.emptyList() : pages);
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

            //create group if not exist
            if (importedApi.getGroup() != null && importedApi.getGroup() != null) {
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
                    pageService.create(createdOrUpdatedApiEntity.getId(), objectMapper.readValue(pageNode.toString(), NewPageEntity.class));
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
            if (api.getViews()
                    .removeIf(view -> view.equals(viewId))) {
                update(api.getId(), convert(api));
            }
        });
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

    private UpdateApiEntity convert(final ApiEntity apiEntity) {
        final UpdateApiEntity updateApiEntity = new UpdateApiEntity();

        updateApiEntity.setDescription(apiEntity.getDescription());
        updateApiEntity.setName(apiEntity.getName());
        updateApiEntity.setViews(apiEntity.getViews());
        updateApiEntity.setPaths(apiEntity.getPaths());
        updateApiEntity.setPicture(apiEntity.getPicture());
        updateApiEntity.setProperties(apiEntity.getProperties());
        updateApiEntity.setProxy(apiEntity.getProxy());
        updateApiEntity.setResources(apiEntity.getResources());
        updateApiEntity.setServices(apiEntity.getServices());
        updateApiEntity.setTags(apiEntity.getTags());
        updateApiEntity.setVersion(apiEntity.getVersion());
        updateApiEntity.setVisibility(apiEntity.getVisibility());
        if (apiEntity.getGroup() != null && (
                apiEntity.getGroup().getId() != null && !apiEntity.getGroup().getId().isEmpty())) {
            updateApiEntity.setGroup(apiEntity.getGroup().getId());
        }

        return updateApiEntity;
    }

    private Set<ApiEntity> convert(Set<Api> apis) throws TechnicalException {
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
        if(api.getGroup() != null) {
            apiEntity.setGroup(groupService.findById(api.getGroup()));
        }

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
                apiEntity.setViews(apiDefinition.getViews());
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while generating API definition", ioe);
            }
        }
        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setVersion(api.getVersion());
        apiEntity.setDescription(api.getDescription());
        apiEntity.setPicture(api.getPicture());
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

        if (updateApiEntity.getGroup() != null && !updateApiEntity.getGroup().isEmpty()) {
            api.setGroup(updateApiEntity.getGroup());
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
            apiDefinition.setViews(updateApiEntity.getViews());

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
}
