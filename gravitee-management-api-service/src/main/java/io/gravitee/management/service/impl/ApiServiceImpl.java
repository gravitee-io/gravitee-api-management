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
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.PageType;
import io.gravitee.management.model.*;
import io.gravitee.management.model.api.*;
import io.gravitee.management.model.api.header.ApiHeaderEntity;
import io.gravitee.management.model.documentation.PageQuery;
import io.gravitee.management.model.notification.GenericNotificationConfigEntity;
import io.gravitee.management.model.parameters.Key;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.model.plan.PlanQuery;
import io.gravitee.management.service.*;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.impl.search.SearchResult;
import io.gravitee.management.service.jackson.ser.api.ApiSerializer;
import io.gravitee.management.service.notification.ApiHook;
import io.gravitee.management.service.notification.HookScope;
import io.gravitee.management.service.notification.NotificationParamsBuilder;
import io.gravitee.management.service.processor.ApiSynchronizationProcessor;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.management.service.search.query.Query;
import io.gravitee.management.service.search.query.QueryBuilder;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.management.model.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.gravitee.management.model.EventType.PUBLISH_API;
import static io.gravitee.management.model.ImportSwaggerDescriptorEntity.Type.INLINE;
import static io.gravitee.management.model.PageType.SWAGGER;
import static io.gravitee.repository.management.model.Api.AuditEvent.*;
import static io.gravitee.repository.management.model.Visibility.PUBLIC;
import static java.util.Collections.*;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiServiceImpl extends AbstractService implements ApiService {

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
    private TopApiService topApiService;
    @Autowired
    private GenericNotificationConfigService genericNotificationConfigService;
    @Autowired
    private NotifierService notifierService;
    @Autowired
    private SwaggerService swaggerService;
    @Autowired
    private SearchEngineService searchEngineService;
    @Autowired
    private ApiHeaderService apiHeaderService;
    @Autowired
    private Configuration freemarkerConfiguration;
    @Autowired
    private ParameterService parameterService;
    @Autowired
    private TagService tagService;

    private static final Pattern LOGGING_MAX_DURATION_PATTERN = Pattern.compile("(?<before>.*)\\#request.timestamp\\s*\\<\\=?\\s*(?<timestamp>\\d*)l(?<after>.*)");
    private static final String LOGGING_MAX_DURATION_CONDITION = "#request.timestamp <= %dl";

    @Override
    public ApiEntity create(final NewApiEntity newApiEntity, final String userId) throws ApiAlreadyExistsException {
        return create(newApiEntity, userId, null, null);
    }

    @Override
    public ApiEntity create(final NewSwaggerApiEntity swaggerApiEntity, final String userId,
                            final ImportSwaggerDescriptorEntity swaggerDescriptor) throws ApiAlreadyExistsException {

        final NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setVersion(swaggerApiEntity.getVersion());
        newApiEntity.setName(swaggerApiEntity.getName());
        newApiEntity.setContextPath(swaggerApiEntity.getContextPath());
        newApiEntity.setDescription(swaggerApiEntity.getDescription());
        newApiEntity.setEndpoint(swaggerApiEntity.getEndpoint());
        newApiEntity.setGroups(swaggerApiEntity.getGroups());

        return create(newApiEntity, userId, swaggerDescriptor, swaggerApiEntity.getPaths());
    }

    private ApiEntity create(final NewApiEntity newApiEntity, final String userId,
    final ImportSwaggerDescriptorEntity swaggerDescriptor, final List<SwaggerPath> swaggerPaths) throws ApiAlreadyExistsException {
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
        group.setName("default-group");
        group.setEndpoints(singleton(new HttpEndpoint("default", newApiEntity.getEndpoint())));
        proxy.setGroups(singleton(group));
        apiEntity.setProxy(proxy);

        final List<String> declaredPaths;
        if (swaggerPaths != null) {
            declaredPaths = swaggerPaths.stream().map(SwaggerPath::getPath).collect(toList());
        } else {
            declaredPaths = newApiEntity.getPaths() != null ? newApiEntity.getPaths() : new ArrayList<>();
        }
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

        if (swaggerDescriptor != null) {
            if (!swaggerDescriptor.isWithPolicyPaths()) {
                final Path path = new Path();
                path.setPath("/");
                apiEntity.setPaths(singletonMap("/", path));
            } else if (swaggerPaths != null && !swaggerPaths.isEmpty()) {
                final Map<String, Path> pathWithMocks = new HashMap<>(swaggerPaths.size());
                swaggerPaths.forEach(swaggerPath -> {
                    final Path path = new Path();
                    path.setPath(swaggerPath.getPath());

                    if (swaggerDescriptor.isWithPolicyMocks()) {
                        final List<Rule> rules = new ArrayList<>();
                        swaggerPath.getVerbs().forEach(swaggerVerb -> {
                            final Rule rule = new Rule();
                            rule.setEnabled(true);
                            rule.setDescription(swaggerVerb.getDescription());
                            rule.setMethods(singleton(HttpMethod.valueOf(swaggerVerb.getVerb())));
                            final Policy policy = new Policy();
                            policy.setName("mock");

                            final Map<String, Object> configuration = new HashMap<>();

                            String responseStatus = swaggerVerb.getResponseStatus();
                            try {
                                Integer.parseInt(responseStatus);
                            } catch (final NumberFormatException nfe) {
                                responseStatus = "200";
                            }
                            configuration.put("status", responseStatus);
                            final Map<Object, Object> header = new HashMap<>(2);
                            header.put("name", "Content-Type");
                            header.put("value", "application/json");
                            configuration.put("headers", singletonList(header));
                            try {
                                if (swaggerVerb.getResponseType() != null || swaggerVerb.getResponseExample() != null) {
                                    final Object mockContent = swaggerVerb.getResponseExample() == null ?
                                            generateMockContent(swaggerVerb.getResponseType(), swaggerVerb.getResponseProperties()) : swaggerVerb.getResponseExample();
                                    configuration.put("content", objectMapper.writeValueAsString(mockContent));
                                }
                                policy.setConfiguration(objectMapper.writeValueAsString(configuration));
                            } catch (final JsonProcessingException e) {
                                e.printStackTrace();
                            }

                            rule.setPolicy(policy);
                            rules.add(rule);
                        });

                        path.setRules(rules);
                    }
                    pathWithMocks.put(swaggerPath.getPath(), path);
                });
                apiEntity.setPaths(pathWithMocks);
            }
            if (!swaggerDescriptor.isWithPathMapping()) {
                apiEntity.setPathMappings(null);
            }
        }

        final ApiEntity createdApi = create0(apiEntity, userId);

        if (swaggerDescriptor != null && swaggerDescriptor.isWithDocumentation()) {
            final NewPageEntity page = new NewPageEntity();
            page.setName("Swagger");
            page.setType(SWAGGER);
            page.setOrder(1);
            if (INLINE.equals(swaggerDescriptor.getType())) {
                page.setContent(swaggerDescriptor.getPayload());
            } else {
                final PageSourceEntity source = new PageSourceEntity();
                page.setSource(source);
                source.setType("http-fetcher");
                source.setConfiguration(objectMapper.convertValue(singletonMap("url", swaggerDescriptor.getPayload()), JsonNode.class));
            }
            pageService.createPage(createdApi.getId(), page);
        }
        return createdApi;
    }

    private Object generateMockContent(final String responseType, final Map<String, Object> responseProperties) {
        final Random random = new Random();
        switch (responseType) {
            case "string":
                return "Mocked " + (responseProperties == null ? "response" : responseProperties.getOrDefault("key", "response"));
            case "boolean":
                return random.nextBoolean();
            case "integer":
                return random.nextInt(1000);
            case "number":
                return random.nextDouble();
            case "array":
                return responseProperties == null ? emptyList() : singletonList(generateMockContent("object", responseProperties));
            case "object":
                if (responseProperties == null) {
                    return emptyMap();
                }
                final Map<String, Object> mock = new HashMap<>(responseProperties.size());
                responseProperties.forEach((k, v) -> {
                    if (v instanceof Map) {
                        mock.put(k, generateMockContent("object", (Map) v));
                    } else {
                        mock.put(k, generateMockContent((String) v, singletonMap("key", k)));
                    }
                });
                return mock;
            default:
                return emptyMap();
        }
    }

    private ApiEntity create0(UpdateApiEntity api, String userId) throws ApiAlreadyExistsException {
        try {
            LOGGER.debug("Create {} for user {}", api, userId);

            String id = UUID.toString(UUID.random());
            Optional<Api> checkApi = apiRepository.findById(id);
            if (checkApi.isPresent()) {
                throw new ApiAlreadyExistsException(id);
            }

            // if user changes sharding tags, then check if he is allowed to do it
            checkShardingTags(api, null);

            // format context-path and check if context path is unique
            checkContextPath(api.getProxy().getContextPath());

            // check endpoints name
            checkEndpointsName(api);

            addLoggingMaxDuration(api.getProxy().getLogging());

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
                        collect(toSet());
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
                ApiEntity apiEntity = convert(createdApi, primaryOwner);
                searchEngineService.index(apiEntity);
                return apiEntity;
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

    private void checkEndpointsName(UpdateApiEntity api) {
        if (api.getProxy() != null && api.getProxy().getGroups() != null) {
            for (EndpointGroup group : api.getProxy().getGroups()) {
                assertEndpointNameNotContainsInvalidCharacters(group.getName());
                if (group.getEndpoints() != null) {
                    for (Endpoint endpoint : group.getEndpoints()) {
                        assertEndpointNameNotContainsInvalidCharacters(endpoint.getName());
                    }
                }
            }
        }
    }

    private void assertEndpointNameNotContainsInvalidCharacters(String name) {
        if (name != null && name.contains(":")) {
            throw new EndpointNameInvalidException(name);
        }
    }

    private void addLoggingMaxDuration(Logging logging) {
        if (logging != null && !LoggingMode.NONE.equals(logging.getMode())) {
            Optional<Long> optionalMaxDuration = parameterService.findAll(Key.LOGGING_DEFAULT_MAX_DURATION, Long::valueOf).stream().findFirst();
            if (optionalMaxDuration.isPresent() && optionalMaxDuration.get() > 0) {

                long maxEndDate = System.currentTimeMillis() + optionalMaxDuration.get();

                // if no condition set, add one
                if (logging.getCondition() == null || logging.getCondition().isEmpty()) {
                    logging.setCondition(String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate));
                } else {
                    Matcher matcher = LOGGING_MAX_DURATION_PATTERN.matcher(logging.getCondition());
                    if (matcher.matches()) {
                        String currentDurationAsStr = matcher.group("timestamp");
                        String before = matcher.group("before");
                        String after = matcher.group("after");
                        try {
                            Long currentDuration = Long.valueOf(currentDurationAsStr);
                            if (currentDuration > maxEndDate) {
                                logging.setCondition(before + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + after);
                            }
                        } catch (NumberFormatException nfe) {
                            LOGGER.error("Wrong format of the logging condition. Add the default one", nfe);
                            logging.setCondition(before + String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + after);
                        }
                    } else {
                        logging.setCondition(String.format(LOGGING_MAX_DURATION_CONDITION, maxEndDate) + " && " + logging.getCondition());
                    }
                }
            }
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

            // if user changes sharding tags, then check if he is allowed to do it
            checkShardingTags(updateApiEntity, convert(optApiToUpdate.get()));

            // check if context path is unique
            checkContextPath(updateApiEntity.getProxy().getContextPath(), apiId);

            // check endpoints name
            checkEndpointsName(updateApiEntity);

            addLoggingMaxDuration(updateApiEntity.getProxy().getLogging());

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
                if (updateApiEntity.getGroups() == null) {
                    api.setGroups(apiToUpdate.getGroups());
                }
                if (updateApiEntity.getLabels() == null) {
                    api.setLabels(apiToUpdate.getLabels());
                }
                if (updateApiEntity.getViews() == null) {
                    api.setViews(apiToUpdate.getViews());
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

                if (parameterService.findAsBoolean(Key.LOGGING_AUDIT_TRAIL_ENABLED)) {
                    // Audit API logging if option is enabled
                    auditApiLogging(apiToUpdate, updatedApi);
                }

                ApiEntity apiEntity = convert(singletonList(updatedApi)).iterator().next();
                searchEngineService.index(apiEntity);

                return apiEntity;
            } else {
                LOGGER.error("Unable to update API {} because of previous error.", api.getId());
                throw new TechnicalManagementException("Unable to update API " + apiId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to update API " + apiId, ex);
        }
    }

    private void checkShardingTags(final UpdateApiEntity updateApiEntity, final ApiEntity existingAPI) {
        if (!isAdmin()) {
            final Set<String> tagsToUpdate = updateApiEntity.getTags() == null ? new HashSet<>() : updateApiEntity.getTags();
            final Set<String> updatedTags;
            if (existingAPI == null) {
                updatedTags = tagsToUpdate;
            } else {
                final Set<String> existingAPITags = existingAPI.getTags() == null ? new HashSet<>() : existingAPI.getTags();
                updatedTags = existingAPITags.stream().filter(tag -> !tagsToUpdate.contains(tag)).collect(toSet());
                updatedTags.addAll(tagsToUpdate.stream().filter(tag -> !existingAPITags.contains(tag)).collect(toSet()));
            }
            if (updatedTags != null && !updatedTags.isEmpty()) {
                final Set<String> userTags = tagService.findByUser(getAuthenticatedUsername());
                if (!userTags.containsAll(updatedTags)) {
                    final String[] notAllowedTags = updatedTags.stream().filter(tag -> !userTags.contains(tag)).toArray(String[]::new);
                    throw new TagNotAllowedException(notAllowedTags);
                }
            }
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
                        .collect(toSet());

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
                            .filter(plan -> (plan.getStatus() != PlanStatus.STAGING))
                            .filter(plan -> plan.getNeedRedeployAt().after(api.getDeployedAt())).count() == 0;
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
            // Audit
            auditService.createApiAuditLog(
                    apiId,
                    Collections.emptyMap(),
                    API_ROLLBACKED,
                    new Date(),
                    null,
                    null);

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
    public String exportAsJson(final String apiId, String exportVersion, String... filteredFields) {
        ApiEntity apiEntity = findById(apiId);

        // set metadata for serialize process
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ApiSerializer.METADATA_EXPORT_VERSION, exportVersion);
        metadata.put(ApiSerializer.METADATA_FILTERED_FIELDS_LIST, Arrays.asList(filteredFields));
        apiEntity.setMetadata(metadata);

        try {
            return objectMapper.writeValueAsString(apiEntity);
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
            if (apiEntity == null || apiEntity.getId() == null) {
                createdOrUpdatedApiEntity = create0(importedApi, userId);
            } else {
                createdOrUpdatedApiEntity = update(apiEntity.getId(), importedApi);
            }

            // Read the whole definition
            final JsonNode jsonNode = objectMapper.readTree(apiDefinition);

            // Members
            final JsonNode membersToImport = jsonNode.path("members");
            if (membersToImport != null && membersToImport.isArray()) {
                // get current members of the api
                Set<MemberToImport> membersAlreadyPresent = membershipService
                        .getMembers(MembershipReferenceType.API, createdOrUpdatedApiEntity.getId(), RoleScope.API)
                        .stream()
                        .map(member -> {
                            UserEntity userEntity = userService.findById(member.getId());
                            return new MemberToImport(userEntity.getSource(), userEntity.getSourceId(), member.getRole());
                        }).collect(toSet());
                // get the current PO
                MemberToImport currentPo = membersAlreadyPresent.stream()
                        .filter(memberToImport -> SystemRole.PRIMARY_OWNER.name().equals(memberToImport.getRole()))
                        .findFirst()
                        .orElse(new MemberToImport());

                String roleUsedInTransfert = null;
                MemberToImport futurePO = null;

                // upsert members
                for (final JsonNode memberNode : membersToImport) {
                    MemberToImport memberToImport = objectMapper.readValue(memberNode.toString(), MemberToImport.class);
                    boolean presentWithSameRole = membersAlreadyPresent
                            .stream()
                            .anyMatch(m -> m.getRole().equals(memberToImport.getRole())
                                    && (m.getSourceId().equals(memberToImport.getSourceId())
                                        && m.getSource().equals(memberToImport.getSource())));

                    // add/update members if :
                    //  - not already present with the same role
                    //  - not the new PO
                    //  - not the current PO
                    if (!presentWithSameRole
                            && !SystemRole.PRIMARY_OWNER.name().equals(memberToImport.getRole())
                            && !(memberToImport.getSourceId().equals(currentPo.getSourceId())
                                && memberToImport.getSource().equals(currentPo.getSource()))) {
                        try {
                            UserEntity userEntity = userService.findBySource(memberToImport.getSource(), memberToImport.getSourceId(), false);
                            membershipService.addOrUpdateMember(
                                    new MembershipService.MembershipReference(MembershipReferenceType.API, createdOrUpdatedApiEntity.getId()),
                                    new MembershipService.MembershipUser(userEntity.getId(), null),
                                    new MembershipService.MembershipRole(RoleScope.API, memberToImport.getRole()));

                        } catch (UserNotFoundException unfe) {

                        }
                    }

                    // get the future role of the current PO
                    if (currentPo.getSourceId().equals(memberToImport.getSourceId())
                            && currentPo.getSource().equals(memberToImport.getSource())
                            && !SystemRole.PRIMARY_OWNER.name().equals(memberToImport.getRole())) {
                        roleUsedInTransfert = memberToImport.getRole();
                    }

                    if (SystemRole.PRIMARY_OWNER.name().equals(memberToImport.getRole())) {
                        futurePO = memberToImport;
                    }
                }

                // transfer the ownership
                if (futurePO != null
                        && !(currentPo.getSource().equals(futurePO.getSource())
                        && currentPo.getSourceId().equals(futurePO.getSourceId()))) {
                    try {
                        UserEntity userEntity = userService.findBySource(futurePO.getSource(), futurePO.getSourceId(), false);
                        RoleEntity roleEntity = null;
                        if (roleUsedInTransfert != null) {
                            roleEntity = new RoleEntity();
                            roleEntity.setName(roleUsedInTransfert);
                            roleEntity.setScope(io.gravitee.management.model.permissions.RoleScope.API);
                        }
                        membershipService.transferApiOwnership(
                                createdOrUpdatedApiEntity.getId(),
                                new MembershipService.MembershipUser(userEntity.getId(), null),
                                roleEntity);
                    } catch (UserNotFoundException unfe) {

                    }
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
                        pageService.createPage(createdOrUpdatedApiEntity.getId(), objectMapper.readValue(pageNode.toString(), NewPageEntity.class));
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
                        updatePlanEntity.setId(planEntities.iterator().next().getId());
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
            imageEntity.setContent(getDefaultPicture());
        } else {
            String[] parts = apiEntity.getPicture().split(";", 2);
            imageEntity.setType(parts[0].split(":")[1]);
            String base64Content = apiEntity.getPicture().split(",", 2)[1];
            imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
        }

        return imageEntity;
    }

    @Override
    public byte[] getDefaultPicture() {
        try {
            return IOUtils.toByteArray(new FileInputStream(defaultIcon));
        } catch (IOException ioe) {
            LOGGER.error("Default icon for API does not exist", ioe);
        }
        return null;
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
        apiModelEntity.setPicture(apiEntity.getPicture());
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
            final NewSwaggerApiEntity newSwaggerApiEntity = swaggerService.prepare(importSwaggerDescriptorEntity);
            apiEntity.getPathMappings().addAll(newSwaggerApiEntity.getPaths().stream().map(SwaggerPath::getPath).collect(toList()));
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

    @Override
    public Collection<ApiEntity> search(String query, Map<String, Object> filters) {
        Query<ApiEntity> apiQuery = QueryBuilder.create(ApiEntity.class)
                .setQuery(query)
                .setFilters(filters)
                .build();

        SearchResult matchApis = searchEngineService.search(apiQuery);
        return matchApis.getDocuments().stream().map(this::findById).collect(toList());
    }

    @Override
    public List<ApiHeaderEntity> getPortalHeaders(String apiId) {
            List<ApiHeaderEntity> entities = apiHeaderService.findAll();
            ApiModelEntity apiEntity = this.findByIdForTemplates(apiId);
            Map<String, Object> model = new HashMap<>();
            model.put("api", apiEntity);
            entities.forEach(entity -> {
                if (entity.getValue().contains("${")) {
                    try {
                        Template template = new Template(entity.getId() + entity.getUpdatedAt().toString(), entity.getValue(), freemarkerConfiguration);
                        entity.setValue(FreeMarkerTemplateUtils.processTemplateIntoString(template, model));
                    } catch (IOException | TemplateException e) {
                        LOGGER.error("Unable to apply templating on api headers ", e);
                        throw new TechnicalManagementException("Unable to apply templating on api headers", e);
                    }
                }
            });
            return entities;
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

        if (query.getGroups() != null && !query.getGroups().isEmpty()) {
            builder.groups(query.getGroups().toArray(new String[0]));
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

    private void auditApiLogging(Api apiToUpdate, Api apiUpdated) {
        try {
            // get old logging configuration
            io.gravitee.definition.model.Api apiToUpdateDefinition = objectMapper.readValue(apiToUpdate.getDefinition(), io.gravitee.definition.model.Api.class);
            Logging loggingToUpdate = apiToUpdateDefinition.getProxy().getLogging();

            // get new logging configuration
            io.gravitee.definition.model.Api apiUpdatedDefinition = objectMapper.readValue(apiUpdated.getDefinition(), io.gravitee.definition.model.Api.class);
            Logging loggingUpdated = apiUpdatedDefinition.getProxy().getLogging();

            // no changes for logging configuration, continue
            if (loggingToUpdate == loggingUpdated ||
                    (loggingToUpdate != null && loggingUpdated != null
                            && Objects.equals(loggingToUpdate.getMode(), loggingUpdated.getMode())
                            &&  Objects.equals(loggingToUpdate.getCondition(), loggingUpdated.getCondition()))) {
                return;
            }

            // determine the audit event type
            Api.AuditEvent auditEvent;
            if ((loggingToUpdate == null || loggingToUpdate.getMode().equals(LoggingMode.NONE)) && (!loggingUpdated.getMode().equals(LoggingMode.NONE))) {
                auditEvent = Api.AuditEvent.API_LOGGING_ENABLED;
            } else if ((loggingToUpdate != null && !loggingToUpdate.getMode().equals(LoggingMode.NONE)) && (loggingUpdated.getMode().equals(LoggingMode.NONE))) {
                auditEvent = Api.AuditEvent.API_LOGGING_DISABLED;
            } else {
                auditEvent = Api.AuditEvent.API_LOGGING_UPDATED;
            }

            // Audit
            auditService.createApiAuditLog(
                    apiUpdated.getId(),
                    Collections.emptyMap(),
                    auditEvent,
                    new Date(),
                    loggingToUpdate,
                    loggingUpdated);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while auditing API logging configuration for API: {}", apiUpdated.getId(), ex);
            throw new TechnicalManagementException("An error occurs while auditing API logging configuration for API: " + apiUpdated.getId(), ex);
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
        final Set<String> apiIds = apis.stream().map(Api::getId).collect(toSet());
        Stream<Api> streamApis = apis.stream();
        if (poMissing > 0) {
            Set<String> apiMembershipsIds = memberships.stream().map(Membership::getReferenceId).collect(toSet());

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
                .collect(toSet());
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
                apiEntity.setResponseTemplates(apiDefinition.getResponseTemplates());
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
            apiDefinition.setResponseTemplates(updateApiEntity.getResponseTemplates());

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
        private String source;
        private String sourceId;
        private String role;

        public MemberToImport() {
        }

        public MemberToImport(String source, String sourceId, String role) {
            this.source = source;
            this.sourceId = sourceId;
            this.role = role;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
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

            return source.equals(that.source) && sourceId.equals(that.sourceId);
        }

        @Override
        public int hashCode() {
            int result = source.hashCode();
            result = 31 * result + sourceId.hashCode();
            return result;
        }
    }
}
