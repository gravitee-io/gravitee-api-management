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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.discovery.EndpointDiscoveryService;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.api.*;
import io.gravitee.rest.api.model.api.header.ApiHeaderEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.plan.PlanQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.upgrade.DefaultMetadataUpgrader;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.processor.ApiSynchronizationProcessor;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.gravitee.repository.management.model.Api.AuditEvent.*;
import static io.gravitee.repository.management.model.Visibility.PUBLIC;
import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity.Type.INLINE;
import static io.gravitee.rest.api.model.PageType.SWAGGER;
import static io.gravitee.rest.api.model.WorkflowReferenceType.API;
import static io.gravitee.rest.api.model.WorkflowState.DRAFT;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;
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

    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(http:|https:))[//]+");
    private static final Pattern CORS_REGEX_PATTERN = Pattern.compile("^(?:(?:[htps\\(\\)?\\|]+):\\/\\/)*(?:[\\w\\(\\)\\[\\]\\{\\}?\\|.*-](?:(?:[?+*]|\\{\\d+(?:,\\d*)?\\}))?)+(?:[a-zA-Z0-9]{2,6})?(?::\\d{1,5})?$");
    private static final String URI_PATH_SEPARATOR = "/";

    @Autowired
    private ApiRepository apiRepository;
    @Autowired
    private ApiQualityRuleRepository apiQualityRuleRepository;
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
    private PortalNotificationConfigService portalNotificationConfigService;
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
    @Autowired
    private EntrypointService entrypointService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private HttpClientService httpClientService;
    @Autowired
    private VirtualHostService virtualHostService;
    @Autowired
    private AlertService alertService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ImportConfiguration importConfiguration;
    @Autowired
    private MediaService mediaService;

    private static final Pattern LOGGING_MAX_DURATION_PATTERN = Pattern.compile("(?<before>.*)\\#request.timestamp\\s*\\<\\=?\\s*(?<timestamp>\\d*)l(?<after>.*)");
    private static final String LOGGING_MAX_DURATION_CONDITION = "#request.timestamp <= %dl";
    private static final String LOGGING_DELIMITER_BASE = "\\s+(\\|\\||\\&\\&)\\s+";
    private static final String ENDPOINTS_DELIMITER = "\n";

    @Override
    public ApiEntity create(final NewApiEntity newApiEntity, final String userId) throws ApiAlreadyExistsException {
        return create(newApiEntity, userId, null, null);
    }

    @Override
    public ApiEntity create(final SwaggerApiEntity swaggerApiEntity, final String userId,
                            final ImportSwaggerDescriptorEntity swaggerDescriptor) throws ApiAlreadyExistsException {

        final NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setVersion(swaggerApiEntity.getVersion());
        newApiEntity.setName(swaggerApiEntity.getName());
        newApiEntity.setContextPath(swaggerApiEntity.getContextPath());
        newApiEntity.setDescription(swaggerApiEntity.getDescription());
        newApiEntity.setEndpoint(String.join(ENDPOINTS_DELIMITER, swaggerApiEntity.getEndpoint()));

        return create(newApiEntity, userId, swaggerDescriptor, swaggerApiEntity);
    }

    private ApiEntity create(final NewApiEntity newApiEntity, final String userId,
                             final ImportSwaggerDescriptorEntity swaggerDescriptor, final SwaggerApiEntity swaggerApiEntity) throws ApiAlreadyExistsException {
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
        proxy.setVirtualHosts(singletonList(new VirtualHost(newApiEntity.getContextPath())));
        EndpointGroup group = new EndpointGroup();
        group.setName("default-group");

        String[] endpoints = null;
        if (newApiEntity.getEndpoint() != null) {
            endpoints = newApiEntity.getEndpoint().split(ENDPOINTS_DELIMITER);
        }

        if (endpoints == null) {
            group.setEndpoints(singleton(new HttpEndpoint("default", null)));
        } else if (endpoints.length == 1) {
            group.setEndpoints(singleton(new HttpEndpoint("default", endpoints[0])));
        } else {
            group.setEndpoints(new HashSet<>());
            for (int i = 0; i < endpoints.length; i++) {
                group.getEndpoints().add(new HttpEndpoint("server" + (i + 1), endpoints[i]));
            }
        }
        proxy.setGroups(singleton(group));
        apiEntity.setProxy(proxy);

        final List<String> declaredPaths = newApiEntity.getPaths() != null ? newApiEntity.getPaths() : new ArrayList<>();
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

        if (swaggerApiEntity != null && swaggerDescriptor != null) {
            if (swaggerDescriptor.isWithPolicyPaths()) {
                apiEntity.setPaths(swaggerApiEntity.getPaths());
            }

            if (swaggerDescriptor.isWithPathMapping()) {
                apiEntity.setPathMappings(swaggerApiEntity.getPathMappings());
            }
        }

        final ApiEntity createdApi = create0(apiEntity, userId);

        createOrUpdateDocumentation(swaggerDescriptor, createdApi, true);

        return createdApi;
    }

    private void createOrUpdateDocumentation(final ImportSwaggerDescriptorEntity swaggerDescriptor,
                                             final ApiEntity api, boolean isForCreation) {
        List<PageEntity> apiDocs = pageService.search(new PageQuery.Builder()
            .api(api.getId())
            .type(PageType.SWAGGER)
            .build());

        if (swaggerDescriptor != null && swaggerDescriptor.isWithDocumentation()) {
            if (isForCreation || (apiDocs == null || apiDocs.isEmpty())) {
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
                pageService.createPage(api.getId(), page);
            } else if (apiDocs.size() == 1) {
                PageEntity pageToUpdate = apiDocs.get(0);
                final UpdatePageEntity page = new UpdatePageEntity();
                page.setName(pageToUpdate.getName());
                page.setOrder(pageToUpdate.getOrder());
                page.setHomepage(pageToUpdate.isHomepage());
                page.setPublished(pageToUpdate.isPublished());
                page.setParentId(pageToUpdate.getParentId());
                page.setConfiguration(pageToUpdate.getConfiguration());
                if (INLINE.equals(swaggerDescriptor.getType())) {
                    page.setContent(swaggerDescriptor.getPayload());
                } else {
                    final PageSourceEntity source = new PageSourceEntity();
                    page.setSource(source);
                    source.setType("http-fetcher");
                    source.setConfiguration(objectMapper.convertValue(singletonMap("url", swaggerDescriptor.getPayload()), JsonNode.class));
                }
                pageService.update(pageToUpdate.getId(), page);
            }
        }
    }

    private ApiEntity create0(UpdateApiEntity api, String userId) throws ApiAlreadyExistsException {
        return this.create0(api, userId, true, null);
    }

    private ApiEntity create0(UpdateApiEntity api, String userId, boolean createSystemFolder) throws ApiAlreadyExistsException {
        return this.create0(api, userId, createSystemFolder, null);
    }

    private ApiEntity create0(UpdateApiEntity api, String userId, boolean createSystemFolder, String apiId) throws ApiAlreadyExistsException {
        try {
            LOGGER.debug("Create {} for user {}", api, userId);

            String id = apiId != null && UUID.fromString(apiId) != null ? apiId : RandomString.generate();

            Optional<Api> checkApi = apiRepository.findById(id);
            if (checkApi.isPresent()) {
                throw new ApiAlreadyExistsException(id);
            }

            // if user changes sharding tags, then check if he is allowed to do it
            checkShardingTags(api, null);

            // format context-path and check if context path is unique
            virtualHostService.validate(api.getProxy().getVirtualHosts());

            // check endpoints name
            checkEndpointsName(api);

            // check HC inheritance
            checkHealthcheckInheritance(api);

            addLoggingMaxDuration(api.getProxy().getLogging());

            // check if there is regex errors in plaintext fields
            validateRegexfields(api);

            Api repoApi = convert(id, api);

            if (repoApi != null) {
                repoApi.setId(id);
                repoApi.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
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

                repoApi.setApiLifecycleState(ApiLifecycleState.CREATED);
                if (parameterService.findAsBoolean(Key.API_REVIEW_ENABLED)) {
                    workflowService.create(WorkflowReferenceType.API, id, REVIEW, userId, DRAFT, "");
                }

                Api createdApi = apiRepository.create(repoApi);

                if (createSystemFolder) {
                    createSystemFolder(createdApi.getId());
                }

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
                if (primaryOwner != null) {
                    membershipService.addRoleToMemberOnReference(
                        new MembershipService.MembershipReference(MembershipReferenceType.API, createdApi.getId()),
                        new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                        new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name()));

                    // create the default mail notification
                    final String emailMetadataValue = "${(api.primaryOwner.email)!''}";

                    GenericNotificationConfigEntity notificationConfigEntity = new GenericNotificationConfigEntity();
                    notificationConfigEntity.setName("Default Mail Notifications");
                    notificationConfigEntity.setReferenceType(HookScope.API.name());
                    notificationConfigEntity.setReferenceId(createdApi.getId());
                    notificationConfigEntity.setHooks(Arrays.stream(ApiHook.values()).map(Enum::name).collect(toList()));
                    notificationConfigEntity.setNotifier(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID);
                    notificationConfigEntity.setConfig(emailMetadataValue);
                    genericNotificationConfigService.create(notificationConfigEntity);

                    // create the default mail support metadata
                    NewApiMetadataEntity newApiMetadataEntity = new NewApiMetadataEntity();
                    newApiMetadataEntity.setFormat(MetadataFormat.MAIL);
                    newApiMetadataEntity.setName(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY);
                    newApiMetadataEntity.setDefaultValue(emailMetadataValue);
                    newApiMetadataEntity.setValue(emailMetadataValue);
                    newApiMetadataEntity.setApiId(createdApi.getId());
                    apiMetadataService.create(newApiMetadataEntity);

                    //TODO add membership log
                    ApiEntity apiEntity = convert(createdApi, primaryOwner, null);

                    searchEngineService.index(apiEntity, false);
                    return apiEntity;
                } else {
                    LOGGER.error("Unable to create API {} because primary owner role has not been found.", api.getName());
                    throw new TechnicalManagementException("Unable to create API " + api.getName() + " because primary owner role has not been found");
                }
            } else {
                LOGGER.error("Unable to create API {} because of previous error.", api.getName());
                throw new TechnicalManagementException("Unable to create API " + api.getName());
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", api, userId, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + api + " for user " + userId, ex);
        }
    }

    private void createSystemFolder(String apiId) {
        NewPageEntity asideSystemFolder = new NewPageEntity();
        asideSystemFolder.setName(SystemFolderType.ASIDE.folderName());
        asideSystemFolder.setPublished(true);
        asideSystemFolder.setType(PageType.SYSTEM_FOLDER);
        pageService.createPage(apiId, asideSystemFolder);
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

    private void checkEndpointsExists(UpdateApiEntity api) {
        if (api.getProxy().getGroups() == null
            || api.getProxy().getGroups().isEmpty()) {
            throw new EndpointMissingException();
        }

        EndpointGroup endpointGroup = api.getProxy().getGroups().iterator().next();
        //Is service discovery enabled ?
        EndpointDiscoveryService endpointDiscoveryService = endpointGroup.getServices() == null ? null : endpointGroup.getServices().get(EndpointDiscoveryService.class);
        if ((endpointDiscoveryService == null || !endpointDiscoveryService.isEnabled()) &&
            (endpointGroup.getEndpoints() == null || endpointGroup.getEndpoints().isEmpty())) {
            throw new EndpointMissingException();
        }
    }

    private void checkHealthcheckInheritance(UpdateApiEntity api) {
        boolean inherit = false;

        if (api.getProxy() != null && api.getProxy().getGroups() != null) {
            for (EndpointGroup group : api.getProxy().getGroups()) {
                if (group.getEndpoints() != null) {
                    for (Endpoint endpoint : group.getEndpoints()) {
                        if (endpoint instanceof HttpEndpoint) {
                            HttpEndpoint httpEndpoint = (HttpEndpoint) endpoint;
                            if (httpEndpoint.getHealthCheck() != null && httpEndpoint.getHealthCheck().isInherit()) {
                                inherit = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (inherit) {
            //if endpoints are set to inherit HC configuration, this configuration must exists.
            boolean hcServiceExists = false;
            if (api.getServices() != null) {
                for (Service service : api.getServices().getAll()) {
                    if (service instanceof HealthCheckService) {
                        hcServiceExists = true;
                        break;
                    }
                }
            }

            if (!hcServiceExists) {
                throw new HealthcheckInheritanceException();
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
                        String before = formatExpression(matcher, "before");
                        String after = formatExpression(matcher, "after");
                        try {
                            final long currentDuration = Long.parseLong(currentDurationAsStr);
                            if (currentDuration > maxEndDate || (!before.isEmpty() || !after.isEmpty())) {
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

    private String formatExpression(final Matcher matcher, final String group) {
        String matchedExpression = matcher.group(group);
        final boolean expressionBlank = matchedExpression == null || "".equals(matchedExpression);
        final boolean after = "after".equals(group);

        String expression;
        if (after) {
            if (matchedExpression.startsWith(" && (") && matchedExpression.endsWith(")")) {
                matchedExpression = matchedExpression.substring(5, matchedExpression.length() - 1);
            }
            expression = expressionBlank ? "" : " && (" + matchedExpression + ")";
            expression = expression.replaceAll("\\(" + LOGGING_DELIMITER_BASE, "\\(");
        } else {
            if (matchedExpression.startsWith("(") && matchedExpression.endsWith(") && ")) {
                matchedExpression = matchedExpression.substring(1, matchedExpression.length() - 5);
            }
            expression = expressionBlank ? "" : "(" + matchedExpression + ") && ";
            expression = expression.replaceAll(LOGGING_DELIMITER_BASE + "\\)", "\\)");
        }
        return expression;
    }

    @Override
    public ApiEntity findById(String apiId) {
        try {
            LOGGER.debug("Find API by ID: {}", apiId);

            Optional<Api> api = apiRepository.findById(apiId);

            if (api.isPresent()) {
                ApiEntity apiEntity = convert(api.get(), getPrimaryOwner(api.get()), null);

                // Compute entrypoints
                calculateEntrypoints(apiEntity);

                return apiEntity;
            }

            throw new ApiNotFoundException(apiId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an API using its ID: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, ex);
        }
    }

    private UserEntity getPrimaryOwner(Api api) throws TechnicalException {
        MembershipEntity primaryOwnerMemberEntity = membershipService.getPrimaryOwner(io.gravitee.rest.api.model.MembershipReferenceType.API, api.getId());
        if (primaryOwnerMemberEntity == null) {
            LOGGER.error("The API {} doesn't have any primary owner.", api.getId());
            throw new TechnicalException("The API " + api.getId() + " doesn't have any primary owner.");
        }

        return userService.findById(primaryOwnerMemberEntity.getMemberId());
    }

    private void calculateEntrypoints(ApiEntity api) {
        List<ApiEntrypointEntity> apiEntrypoints = new ArrayList<>();

        if (api.getProxy() != null) {
            String defaultEntrypoint = parameterService.find(Key.PORTAL_ENTRYPOINT);
            final String scheme = getScheme(defaultEntrypoint);
            if (api.getTags() != null && !api.getTags().isEmpty()) {
                List<EntrypointEntity> entrypoints = entrypointService.findAll();
                entrypoints.forEach(entrypoint -> {
                    Set<String> tagEntrypoints = new HashSet<>(Arrays.asList(entrypoint.getTags()));
                    tagEntrypoints.retainAll(api.getTags());

                    if (tagEntrypoints.size() == entrypoint.getTags().length) {
                        api.getProxy().getVirtualHosts().forEach(virtualHost -> {
                            String targetHost = (virtualHost.getHost() == null || !virtualHost.isOverrideEntrypoint()) ?
                                entrypoint.getValue() : virtualHost.getHost();
                            if (!targetHost.toLowerCase().startsWith("http")) {
                                targetHost = scheme + "://" + targetHost;
                            }
                            apiEntrypoints.add(new ApiEntrypointEntity(
                                tagEntrypoints,
                                DUPLICATE_SLASH_REMOVER
                                    .matcher(targetHost + URI_PATH_SEPARATOR + virtualHost.getPath())
                                    .replaceAll(URI_PATH_SEPARATOR),
                                virtualHost.getHost())
                            );
                        });
                    }
                });
            }

            // If empty, get the default entrypoint
            if (apiEntrypoints.isEmpty()) {
                api.getProxy().getVirtualHosts().forEach(virtualHost -> {
                    String targetHost = (virtualHost.getHost() == null || !virtualHost.isOverrideEntrypoint()) ?
                        defaultEntrypoint : virtualHost.getHost();
                    if (!targetHost.toLowerCase().startsWith("http")) {
                        targetHost = scheme + "://" + targetHost;
                    }
                    apiEntrypoints.add(new ApiEntrypointEntity(
                        DUPLICATE_SLASH_REMOVER
                            .matcher(targetHost + URI_PATH_SEPARATOR + virtualHost.getPath())
                            .replaceAll(URI_PATH_SEPARATOR), virtualHost.getHost())
                    );
                });
            }
        }

        api.setEntrypoints(apiEntrypoints);
    }

    private String getScheme(String defaultEntrypoint) {
        String scheme = "https";
        if (defaultEntrypoint != null) {
            try {
                scheme = new URL(defaultEntrypoint).getProtocol();
            } catch (MalformedURLException e) {
                // return default scheme
            }
        }
        return scheme;
    }

    @Override
    public Set<ApiEntity> findByVisibility(io.gravitee.rest.api.model.Visibility visibility) {
        try {
            LOGGER.debug("Find APIs by visibility {}", visibility);
            return convert(apiRepository.search(new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).visibility(Visibility.valueOf(visibility.name())).build()));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public Set<ApiEntity> findAll() {
        try {
            LOGGER.debug("Find all APIs");
            return convert(apiRepository.search(new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build()));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs", ex);
        }
    }

    @Override
    public Set<ApiEntity> findAllLight() {
        try {
            LOGGER.debug("Find all APIs without some fields (definition, picture...)");
            return convert(apiRepository.search(new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).build(),
                new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build()));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all APIs light", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all APIs light", ex);
        }
    }

    @Override
    public Set<ApiEntity> findByUser(String userId, ApiQuery apiQuery, boolean portal) {
        try {
            LOGGER.debug("Find APIs by user {}", userId);

            //get all public apis
            List<Api> publicApis;
            if (portal) {
                publicApis = apiRepository.search(queryToCriteria(apiQuery).visibility(PUBLIC).build());
            } else {
                publicApis = emptyList();
            }

            List<Api> userApis = emptyList();
            List<Api> groupApis = emptyList();
            List<Api> subscribedApis = emptyList();

            // for others API, user must be authenticated
            if (userId != null) {
                // get user apis
                final String[] userApiIds = membershipService
                    .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.API).stream()
                    .map(MembershipEntity::getReferenceId)
                    .toArray(String[]::new);
                if (userApiIds.length > 0) {
                    userApis = apiRepository.search(queryToCriteria(apiQuery).ids(userApiIds).build());
                }

                // get user groups apis
                final String[] groupIds = membershipService
                    .getMembershipsByMemberAndReference(MembershipMemberType.USER, userId, MembershipReferenceType.GROUP).stream()
                    .filter(m -> m.getRoleId() != null && roleService.findById(m.getRoleId()).getScope().equals(RoleScope.API))
                    .map(MembershipEntity::getReferenceId)
                    .toArray(String[]::new);
                if (groupIds.length > 0 && groupIds[0] != null) {
                    groupApis = apiRepository.search(queryToCriteria(apiQuery).groups(groupIds).build());
                }

                // get user subscribed apis, useful when an API becomes private and an app owner is not anymore in members
                if (portal) {
                    final Set<String> applications =
                        applicationService.findByUser(userId).stream().map(ApplicationListItem::getId).collect(toSet());
                    if (!applications.isEmpty()) {
                        final SubscriptionQuery query = new SubscriptionQuery();
                        query.setApplications(applications);
                        final Collection<SubscriptionEntity> subscriptions = subscriptionService.search(query);
                        if (subscriptions != null && !subscriptions.isEmpty()) {
                            subscribedApis = apiRepository
                                .search(queryToCriteria(apiQuery).ids(subscriptions.stream()
                                    .map(SubscriptionEntity::getApi).distinct().toArray(String[]::new)).build());
                        }
                    }
                }
            }

            // merge all apis
            final Set<ApiEntity> apis = new HashSet<>();
            apis.addAll(convert(publicApis));
            apis.addAll(convert(userApis));
            apis.addAll(convert(groupApis));
            apis.addAll(convert(subscribedApis));
            return filterApiByQuery(apis.stream(), apiQuery).collect(toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find APIs for user {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find APIs for user " + userId, ex);
        }
    }

    @Override
    public Set<ApiEntity> findPublishedByUser(String userId, ApiQuery apiQuery) {
        if (apiQuery == null) {
            apiQuery = new ApiQuery();
        }
        apiQuery.setLifecycleStates(Arrays.asList(io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED));
        return findByUser(userId, apiQuery, true);
    }

    @Override
    public Set<ApiEntity> findPublishedByUser(String userId) {
        return findPublishedByUser(userId, null);
    }

    private Stream<ApiEntity> filterApiByQuery(Stream<ApiEntity> apiEntityStream, ApiQuery query) {
        if (query == null) {
            return apiEntityStream;
        }
        return apiEntityStream
            .filter(api -> query.getTag() == null || (api.getTags() != null && api.getTags().contains(query.getTag())))
            .filter(api -> query.getContextPath() == null || api.getProxy().getVirtualHosts().stream().anyMatch(
                virtualHost -> query.getContextPath().equals(virtualHost.getPath())));
    }

    @Override
    public ApiEntity update(String apiId, SwaggerApiEntity swaggerApiEntity, ImportSwaggerDescriptorEntity swaggerDescriptor) {
        final ApiEntity apiEntityToUpdate = this.findById(apiId);
        final UpdateApiEntity updateApiEntity = convert(apiEntityToUpdate);

        // Overwrite from swagger
        updateApiEntity.setVersion(swaggerApiEntity.getVersion());
        updateApiEntity.setName(swaggerApiEntity.getName());
        updateApiEntity.setDescription(swaggerApiEntity.getDescription());

        // Overwrite from swagger, if asked
        if (swaggerApiEntity != null) {
            updateApiEntity.setPaths(swaggerApiEntity.getPaths());

            if (swaggerDescriptor.isWithPathMapping()) {
                updateApiEntity.setPathMappings(swaggerApiEntity.getPathMappings());
            }
        }

        createOrUpdateDocumentation(swaggerDescriptor, apiEntityToUpdate, false);

        return update(apiId, updateApiEntity);
    }

    @Override
    public ApiEntity update(String apiId, UpdateApiEntity updateApiEntity) {
        try {
            LOGGER.debug("Update API {}", apiId);

            Optional<Api> optApiToUpdate = apiRepository.findById(apiId);
            if (!optApiToUpdate.isPresent()) {
                throw new ApiNotFoundException(apiId);
            }

            // check if entrypoints are unique
            virtualHostService.validate(updateApiEntity.getProxy().getVirtualHosts(), apiId);

            // check endpoints presence
            checkEndpointsExists(updateApiEntity);

            // check endpoints name
            checkEndpointsName(updateApiEntity);

            // check HC inheritance
            checkHealthcheckInheritance(updateApiEntity);

            // check CORS Allow-origin format
            checkAllowOriginFormat(updateApiEntity);

            addLoggingMaxDuration(updateApiEntity.getProxy().getLogging());

            // check if there is regex errors in plaintext fields
            validateRegexfields(updateApiEntity);

            final ApiEntity apiToCheck = convert(optApiToUpdate.get());

            // if user changes sharding tags, then check if he is allowed to do it
            checkShardingTags(updateApiEntity, apiToCheck);

            // if lifecycle state not provided, set the saved one
            if (updateApiEntity.getLifecycleState() == null) {
                updateApiEntity.setLifecycleState(apiToCheck.getLifecycleState());
            }

            // check lifecycle state
            checkLifecycleState(updateApiEntity, apiToCheck);

            // check the existence of groups
            if (updateApiEntity.getGroups() != null && !updateApiEntity.getGroups().isEmpty()) {
                try {
                    groupService.findByIds(updateApiEntity.getGroups());
                } catch (GroupsNotFoundException gnfe) {
                    throw new InvalidDataException("Groups [" + updateApiEntity.getGroups() + "] does not exist");
                }
            }

            // add a default path
            if (updateApiEntity.getPaths() == null || updateApiEntity.getPaths().isEmpty()) {
                updateApiEntity.setPaths(singletonMap("/", new Path()));
            }

            Api apiToUpdate = optApiToUpdate.get();
            Api api = convert(apiId, updateApiEntity);

            if (api != null) {
                api.setId(apiId.trim());
                api.setUpdatedAt(new Date());

                // Copy fields from existing values
                api.setEnvironmentId(apiToUpdate.getEnvironmentId());
                api.setDeployedAt(apiToUpdate.getDeployedAt());
                api.setCreatedAt(apiToUpdate.getCreatedAt());
                api.setLifecycleState(apiToUpdate.getLifecycleState());
                // If no new picture and the current picture url is not the default one, keep the current picture
                if (updateApiEntity.getPicture() == null && updateApiEntity.getPictureUrl() != null && updateApiEntity.getPictureUrl().indexOf("?hash") > 0) {
                    api.setPicture(apiToUpdate.getPicture());
                }
                if (updateApiEntity.getGroups() == null) {
                    api.setGroups(apiToUpdate.getGroups());
                }
                if (updateApiEntity.getLabels() == null) {
                    api.setLabels(apiToUpdate.getLabels());
                }
                if (updateApiEntity.getCategories() == null) {
                    api.setCategories(apiToUpdate.getCategories());
                }

                if (ApiLifecycleState.DEPRECATED.equals(api.getApiLifecycleState())) {
                    planService.findByApi(api.getId()).forEach(plan -> {
                        if (PlanStatus.PUBLISHED.equals(plan.getStatus()) || PlanStatus.STAGING.equals(plan.getStatus())) {
                            planService.depreciate(plan.getId(), true);
                        }
                    });
                    notifierService.trigger(ApiHook.API_DEPRECATED, apiId,
                        new NotificationParamsBuilder()
                            .api(apiToCheck)
                            .user(userService.findById(getAuthenticatedUsername()))
                            .build());
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
                searchEngineService.index(apiEntity, false);
                return apiEntity;
            } else {
                LOGGER.error("Unable to update API {} because of previous error.", apiId);
                throw new TechnicalManagementException("Unable to update API " + apiId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update API {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to update API " + apiId, ex);
        }
    }

    private void checkAllowOriginFormat(UpdateApiEntity updateApiEntity) {
        if (updateApiEntity.getProxy() != null && updateApiEntity.getProxy().getCors() != null) {
            final Set<String> accessControlAllowOrigin = updateApiEntity.getProxy().getCors().getAccessControlAllowOrigin();
            if (accessControlAllowOrigin != null && !accessControlAllowOrigin.isEmpty()) {
                for (String allowOriginItem : accessControlAllowOrigin) {
                    if (! CORS_REGEX_PATTERN.matcher(allowOriginItem).matches()) {
                        throw new AllowOriginNotAllowedException(allowOriginItem);
                    }
                }
            }
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

    private void validateRegexfields(final UpdateApiEntity updateApiEntity) {
        // validate regex on paths
        if (updateApiEntity.getPaths() != null) {
            updateApiEntity.getPaths().forEach((path, v) -> {
                try {
                    Pattern.compile(path);
                } catch (java.util.regex.PatternSyntaxException pse) {
                    LOGGER.error("An error occurs while trying to parse the path {}", path, pse);
                    throw new TechnicalManagementException("An error occurs while trying to parse the path " + path, pse);
                }
            });
        }

        // validate regex on pathMappings
        if (updateApiEntity.getPathMappings() != null) {
            updateApiEntity.getPathMappings().forEach(pathMapping -> {
                try {
                    Pattern.compile(pathMapping);
                } catch (java.util.regex.PatternSyntaxException pse) {
                    LOGGER.error("An error occurs while trying to parse the path mapping {}", pathMapping, pse);
                    throw new TechnicalManagementException("An error occurs while trying to parse the path mapping" + pathMapping, pse);
                }
            });
        }
    }

    private void checkLifecycleState(final UpdateApiEntity updateApiEntity, final ApiEntity existingAPI) {
        if (io.gravitee.rest.api.model.api.ApiLifecycleState.DEPRECATED.equals(existingAPI.getLifecycleState())) {
            throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
        }
        if (existingAPI.getLifecycleState().name().equals(updateApiEntity.getLifecycleState().name())) {
            return;
        }
        if (io.gravitee.rest.api.model.api.ApiLifecycleState.ARCHIVED.equals(existingAPI.getLifecycleState())) {
            if (!io.gravitee.rest.api.model.api.ApiLifecycleState.ARCHIVED.equals(updateApiEntity.getLifecycleState())) {
                throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
            }
        } else if (io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED.equals(existingAPI.getLifecycleState())) {
            if (io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED.equals(updateApiEntity.getLifecycleState())) {
                throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
            }
        } else if (io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED.equals(existingAPI.getLifecycleState())) {
            if (WorkflowState.IN_REVIEW.equals(existingAPI.getWorkflowState())) {
                throw new LifecycleStateChangeNotAllowedException(updateApiEntity.getLifecycleState().name());
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
                    throw new ApiNotDeletableException(plansNotClosed);
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

                // https://github.com/gravitee-io/issues/issues/4130
                // Ensure we are sending a last UNPUBLISH_API event because the gateway couldn't be aware that the API (and
                // all its relative events) have been deleted.
                Map<String, String> properties = new HashMap<>(2);
                properties.put(Event.EventProperties.API_ID.getValue(), apiId);
                if (getAuthenticatedUser() != null) {
                    properties.put(Event.EventProperties.USER.getValue(), getAuthenticatedUser().getUsername());
                }
                eventService.create(EventType.UNPUBLISH_API, null, properties);

                // Delete pages
                pageService.deleteAllByApi(apiId);

                // Delete top API
                topApiService.delete(apiId);
                // Delete API
                apiRepository.delete(apiId);
                // Delete memberships
                membershipService.deleteReference(MembershipReferenceType.API, apiId);
                // Delete notifications
                genericNotificationConfigService.deleteReference(NotificationReferenceType.API, apiId);
                portalNotificationConfigService.deleteReference(NotificationReferenceType.API, apiId);
                // Delete alerts
                final List<AlertTriggerEntity> alerts = alertService.findByReference(AlertReferenceType.API, apiId);
                alerts.forEach(alert -> alertService.delete(alert.getId(), alert.getReferenceId()));
                // delete all reference on api quality rule
                apiQualityRuleRepository.deleteByApi(apiId);
                // Audit
                auditService.createApiAuditLog(
                    apiId,
                    Collections.emptyMap(),
                    API_DELETED,
                    new Date(),
                    optApi.get(),
                    null);
                // remove from search engine
                searchEngineService.delete(convert(optApi.get()), false);

                mediaService.deleteAllByApi(apiId);

                apiMetadataService.deleteAllByApi(apiId);
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
    public ApiEntity createWithImportedDefinition(ApiEntity apiEntity, String apiDefinitionOrURL, String userId) {
        String apiDefinition = fetchApiDefinitionContentFromURL(apiDefinitionOrURL);
        try {
            // Read the whole definition
            final JsonNode jsonNode = objectMapper.readTree(apiDefinition);
            String apiId = jsonNode.has("id") ? jsonNode.get("id").asText() : null;
            UpdateApiEntity importedApi = this.convertToEntity(apiDefinition, jsonNode, apiId);
            ApiEntity createdApiEntity = create0(importedApi, userId, false, apiId);
            createPageAndMedia(createdApiEntity, jsonNode);
            updateApiReferences(createdApiEntity, jsonNode);
            return createdApiEntity;
        } catch (JsonProcessingException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", apiDefinition, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the API definition.");
        }
    }

    private void createPageAndMedia(ApiEntity createdApiEntity, JsonNode jsonNode) {
        final JsonNode apiMedia = jsonNode.path("apiMedia");
        if (apiMedia != null && apiMedia.isArray()) {
            for (JsonNode media : apiMedia) {
                mediaService.createWithDefinition(createdApiEntity.getId(), media.toString());
            }
        }

        final JsonNode pages = jsonNode.path("pages");
        if (pages != null && pages.isArray()) {
            for (JsonNode page : pages) {
                pageService.createWithDefinition(createdApiEntity.getId(), page.toString());
            }
        }

        List<PageEntity> search = pageService
            .search(new PageQuery.Builder().api(createdApiEntity.getId())
                .name(SystemFolderType.ASIDE.folderName())
                .type(PageType.SYSTEM_FOLDER).build());
        if (search.isEmpty()) {
            createSystemFolder(createdApiEntity.getId());
        }
    }

    @Override
    public ApiEntity updateWithImportedDefinition(ApiEntity apiEntity, String apiDefinitionOrURL, String userId) {
        String apiDefinition = fetchApiDefinitionContentFromURL(apiDefinitionOrURL);
        try {
            // Read the whole definition
            final JsonNode jsonNode = objectMapper.readTree(apiDefinition);
            UpdateApiEntity importedApi = this.convertToEntity(apiDefinition, jsonNode);
            ApiEntity updatedApiEntity = update(apiEntity.getId(), importedApi);
            updateApiReferences(updatedApiEntity, jsonNode);
            return updatedApiEntity;
        } catch (JsonProcessingException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", apiDefinition, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the API definition.");
        }
    }

    private UpdateApiEntity convertToEntity(String apiDefinition, JsonNode jsonNode) throws JsonProcessingException {
        return convertToEntity(apiDefinition, jsonNode, null);
    }

    private UpdateApiEntity convertToEntity(String apiDefinition, JsonNode jsonNode, String apiId) throws JsonProcessingException {
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

        // Views & Categories
        // Before 3.0.2, API 'categories' were called 'views'. This is for compatibility.
        final JsonNode viewsDefinition = jsonNode.path("views");
        if (viewsDefinition != null && viewsDefinition.isArray()) {
            Set<String> categories = new HashSet<>();
            for (JsonNode viewNode : viewsDefinition) {
                categories.add(viewNode.asText());
            }
            importedApi.setCategories(categories);
        }

        return importedApi;
    }

    private void updateApiReferences(ApiEntity createdOrUpdatedApiEntity, JsonNode jsonNode) throws JsonProcessingException {

        // Members
        final JsonNode membersToImport = jsonNode.path("members");
        if (membersToImport != null && membersToImport.isArray()) {
            // get current members of the api
            Set<MemberToImport> membersAlreadyPresent = membershipService
                .getMembersByReference(MembershipReferenceType.API, createdOrUpdatedApiEntity.getId())
                .stream()
                .map(member -> {
                    UserEntity userEntity = userService.findById(member.getId());
                    return new MemberToImport(userEntity.getSource(), userEntity.getSourceId(), member.getRoles().stream().map(RoleEntity::getId).collect(Collectors.toList()), null);
                }).collect(toSet());
            // get the current PO
            Optional<RoleEntity> optPoRole = roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name());
            if (optPoRole.isPresent()) {
                String poRoleId = optPoRole.get().getId();
                MemberToImport currentPo = membersAlreadyPresent.stream()
                    .filter(memberToImport -> memberToImport.getRoles().contains(poRoleId))
                    .findFirst()
                    .orElse(new MemberToImport());

                List<String> roleUsedInTransfert = null;
                MemberToImport futurePO = null;


                // upsert members
                for (final JsonNode memberNode : membersToImport) {
                    MemberToImport memberToImport = objectMapper.readValue(memberNode.toString(), MemberToImport.class);
                    String roleToAdd = memberToImport.getRole();
                    List<String> rolesToImport = memberToImport.getRoles();
                    if (roleToAdd != null && !roleToAdd.isEmpty()) {
                        if (rolesToImport == null) {
                            rolesToImport = new ArrayList<>();
                            memberToImport.setRoles(rolesToImport);
                        }
                        Optional<RoleEntity> optRoleToAddEntity = roleService.findByScopeAndName(RoleScope.API, roleToAdd);
                        if (optRoleToAddEntity.isPresent()) {
                            rolesToImport.add(optRoleToAddEntity.get().getId());
                        } else {
                            LOGGER.warn("Role {} does not exist", roleToAdd);
                        }
                    }
                    if (rolesToImport != null) {
                        rolesToImport.sort(Comparator.naturalOrder());
                    }
                    boolean presentWithSameRole = memberToImport.getRoles() != null && !memberToImport.getRoles().isEmpty() && membersAlreadyPresent
                        .stream()
                        .anyMatch(m -> {
                            m.getRoles().sort(Comparator.naturalOrder());
                            return
                                m.getRoles().equals(memberToImport.getRoles())
                                    && (m.getSourceId().equals(memberToImport.getSourceId())
                                    && m.getSource().equals(memberToImport.getSource()));
                        });

                    // add/update members if :
                    //  - not already present with the same role
                    //  - not the new PO
                    //  - not the current PO
                    if (!presentWithSameRole
                        && (memberToImport.getRoles() != null && !memberToImport.getRoles().isEmpty() && !memberToImport.getRoles().contains(poRoleId))
                        && !(memberToImport.getSourceId().equals(currentPo.getSourceId())
                        && memberToImport.getSource().equals(currentPo.getSource()))) {
                        try {
                            UserEntity userEntity = userService.findBySource(memberToImport.getSource(), memberToImport.getSourceId(), false);

                            rolesToImport.forEach(role ->
                                membershipService.addRoleToMemberOnReference(
                                    MembershipReferenceType.API,
                                    createdOrUpdatedApiEntity.getId(),
                                    MembershipMemberType.USER,
                                    userEntity.getId(),
                                    role)
                            );
                        } catch (UserNotFoundException unfe) {

                        }
                    }

                    // get the future role of the current PO
                    if (currentPo.getSourceId().equals(memberToImport.getSourceId())
                        && currentPo.getSource().equals(memberToImport.getSource())
                        && !rolesToImport.contains(poRoleId)) {
                        roleUsedInTransfert = rolesToImport;
                    }

                    if (rolesToImport.contains(poRoleId)) {
                        futurePO = memberToImport;
                    }
                }

                // transfer the ownership
                if (futurePO != null
                    && !(currentPo.getSource().equals(futurePO.getSource())
                    && currentPo.getSourceId().equals(futurePO.getSourceId()))) {
                    try {
                        UserEntity userEntity = userService.findBySource(futurePO.getSource(), futurePO.getSourceId(), false);
                        List<RoleEntity> roleEntity = null;
                        if (roleUsedInTransfert != null && !roleUsedInTransfert.isEmpty()) {
                            roleEntity = roleUsedInTransfert.stream().map(roleService::findById).collect(Collectors.toList());
                        }
                        membershipService.transferApiOwnership(
                            createdOrUpdatedApiEntity.getId(),
                            new MembershipService.MembershipMember(userEntity.getId(), null, MembershipMemberType.USER),
                            roleEntity);
                    } catch (UserNotFoundException unfe) {

                    }
                }
            }
        }

        //Pages
        final JsonNode pagesDefinition = jsonNode.path("pages");
        if (pagesDefinition != null && pagesDefinition.isArray()) {
            List<PageEntity> pagesList = objectMapper.readValue(pagesDefinition.toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, PageEntity.class));
            PageEntityTreeNode documentationTree = new PageEntityTreeNode(new PageEntity());
            documentationTree.appendListToTree(pagesList);
            createOrUpdateChildrenPages(createdOrUpdatedApiEntity.getId(), null, documentationTree.children);
        }

        //Plans
        final JsonNode plansDefinition = jsonNode.path("plans");
        if (plansDefinition != null && plansDefinition.isArray()) {
            for (JsonNode planNode : plansDefinition) {
                PlanQuery query = new PlanQuery.Builder().
                    api(createdOrUpdatedApiEntity.getId()).
                    name(planNode.get("name").asText()).
                    security(PlanSecurityType.valueOf(planNode.get("security").asText().toUpperCase())).
                    build();
                List<PlanEntity> planEntities = planService.search(query).stream()
                    .filter(planEntity -> !PlanStatus.CLOSED.equals(planEntity.getStatus()))
                    .collect(toList());
                if (planEntities.isEmpty()) {
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
        // Metadata
        final JsonNode metadataDefinition = jsonNode.path("metadata");
        if (metadataDefinition != null && metadataDefinition.isArray()) {
            try {
                for (JsonNode metadataNode : metadataDefinition) {
                    UpdateApiMetadataEntity updateApiMetadataEntity = objectMapper.readValue(metadataNode.toString(), UpdateApiMetadataEntity.class);
                    updateApiMetadataEntity.setApiId(createdOrUpdatedApiEntity.getId());
                    apiMetadataService.update(updateApiMetadataEntity);
                }
            } catch (Exception ex) {
                LOGGER.error("An error occurs while creating API metadata", ex);
                throw new TechnicalManagementException("An error occurs while creating API Metadata", ex);
            }
        }
    }

    private String fetchApiDefinitionContentFromURL(String apiDefinitionOrURL) {
        if (apiDefinitionOrURL.toUpperCase().startsWith("HTTP")) {
            UrlSanitizerUtils.checkAllowed(apiDefinitionOrURL, importConfiguration.getImportWhitelist(), importConfiguration.isAllowImportFromPrivate());
            Buffer buffer = httpClientService.request(HttpMethod.GET, apiDefinitionOrURL, null, null, null);
            return buffer.toString();
        }
        return apiDefinitionOrURL;
    }

    class PageEntityTreeNode {

        PageEntity data;
        PageEntityTreeNode parent;
        List<PageEntityTreeNode> children;

        public PageEntityTreeNode(PageEntity data) {
            this.data = data;
            this.children = new LinkedList<>();
        }

        public PageEntityTreeNode addChild(PageEntity child) {
            PageEntityTreeNode childNode = new PageEntityTreeNode(child);
            childNode.parent = this;
            this.children.add(childNode);
            return childNode;
        }

        private PageEntityTreeNode findById(String id) {
            if (id.equals(data.getId())) {
                return this;
            }
            for (PageEntityTreeNode child : children) {
                PageEntityTreeNode result = child.findById(id);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        public void appendListToTree(List<PageEntity> pagesList) {
            List<PageEntity> orphans = new ArrayList<>();
            for (PageEntity newPage : pagesList) {
                if (newPage.getParentId() == null || newPage.getParentId().isEmpty()) {
                    this.addChild(newPage);
                } else {
                    PageEntityTreeNode parentNode = this.findById(newPage.getParentId());
                    if (parentNode != null) {
                        parentNode.addChild(newPage);
                    } else {
                        orphans.add(newPage);
                    }
                }
            }
            if (!orphans.isEmpty() && orphans.size() < pagesList.size()) {
                appendListToTree(orphans);
            }
        }
    }

    private void createOrUpdateChildrenPages(String apiId, String parentId, List<PageEntityTreeNode> children) {
        for (final PageEntityTreeNode child : children) {
            PageEntity pageEntityToImport = child.data;
            pageEntityToImport.setParentId(parentId);

            PageEntity createdOrUpdatedPage = pageEntityToImport.getId() != null ? pageService.findById(pageEntityToImport.getId()) : null;

            if (createdOrUpdatedPage == null) {
                NewPageEntity newPage = new NewPageEntity();
                newPage.setConfiguration(pageEntityToImport.getConfiguration());
                newPage.setContent(pageEntityToImport.getContent());
                newPage.setExcludedGroups(pageEntityToImport.getExcludedGroups());
                newPage.setHomepage(pageEntityToImport.isHomepage());
                newPage.setLastContributor(pageEntityToImport.getLastContributor());
                newPage.setName(pageEntityToImport.getName());
                newPage.setOrder(pageEntityToImport.getOrder());
                newPage.setParentId(pageEntityToImport.getParentId());
                newPage.setPublished(pageEntityToImport.isPublished());
                newPage.setSource(pageEntityToImport.getSource());
                newPage.setType(PageType.valueOf(pageEntityToImport.getType()));

                createdOrUpdatedPage = pageService.createPage(apiId, newPage);
            } else {
                UpdatePageEntity updatePageEntity = new UpdatePageEntity();
                updatePageEntity.setConfiguration(pageEntityToImport.getConfiguration());
                updatePageEntity.setContent(pageEntityToImport.getContent());
                updatePageEntity.setExcludedGroups(pageEntityToImport.getExcludedGroups());
                updatePageEntity.setHomepage(pageEntityToImport.isHomepage());
                updatePageEntity.setLastContributor(pageEntityToImport.getLastContributor());
                updatePageEntity.setName(pageEntityToImport.getName());
                updatePageEntity.setOrder(pageEntityToImport.getOrder());
                updatePageEntity.setParentId(pageEntityToImport.getParentId());
                updatePageEntity.setPublished(pageEntityToImport.isPublished());
                updatePageEntity.setSource(pageEntityToImport.getSource());

                createdOrUpdatedPage = pageService.update(pageEntityToImport.getId(), updatePageEntity);
            }

            if (child.children != null && !child.children.isEmpty()) {
                this.createOrUpdateChildrenPages(apiId, createdOrUpdatedPage.getId(), child.children);
            }
        }
    }

    @Override
    public InlinePictureEntity getPicture(String apiId) {
        ApiEntity apiEntity = findById(apiId);
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        if (apiEntity.getPicture() != null) {
            String[] parts = apiEntity.getPicture().split(";", 2);
            imageEntity.setType(parts[0].split(":")[1]);
            String base64Content = apiEntity.getPicture().split(",", 2)[1];
            imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
        }

        return imageEntity;
    }

    @Override
    public void deleteCategoryFromAPIs(final String categoryId) {
        findAll().forEach(api -> {
            if (api.getCategories() != null && api.getCategories().contains(categoryId)) {
                removeCategory(api.getId(), categoryId);
            }
        });
    }

    private void removeCategory(String apiId, String categoryId) throws TechnicalManagementException {
        try {
            Optional<Api> optApi = apiRepository.findById(apiId);
            if (optApi.isPresent()) {
                Api api = optApi.get();
                Api previousApi = new Api(api);
                api.getCategories().remove(categoryId);
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
            LOGGER.error("An error occurs while removing category from API: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while removing category from API: " + apiId, ex);
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
    public ApiModelEntity findByIdForTemplates(String apiId, boolean decodeTemplate) {
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
        apiModelEntity.setCategories(apiEntity.getCategories());
        apiModelEntity.setVersion(apiEntity.getVersion());
        apiModelEntity.setState(apiEntity.getState());
        apiModelEntity.setTags(apiEntity.getTags());
        apiModelEntity.setServices(apiEntity.getServices());
        apiModelEntity.setPaths(apiEntity.getPaths());
        apiModelEntity.setPicture(apiEntity.getPicture());
        apiModelEntity.setPrimaryOwner(apiEntity.getPrimaryOwner());
        apiModelEntity.setProperties(apiEntity.getProperties());
        apiModelEntity.setProxy(convert(apiEntity.getProxy()));
        apiModelEntity.setLifecycleState(apiEntity.getLifecycleState());
        apiModelEntity.setDisableMembershipNotifications(apiEntity.isDisableMembershipNotifications());

        final List<ApiMetadataEntity> metadataList = apiMetadataService.findAllByApi(apiId);

        if (metadataList != null) {
            final Map<String, String> mapMetadata = new HashMap<>(metadataList.size());
            metadataList.forEach(metadata -> mapMetadata.put(metadata.getKey(),
                metadata.getValue() == null ? metadata.getDefaultValue() : metadata.getValue()));
            apiModelEntity.setMetadata(mapMetadata);
            if (decodeTemplate) {
                try {
                    Template apiMetadataTemplate = new Template(apiModelEntity.getId(), new StringReader(mapMetadata.toString()), freemarkerConfiguration);
                    String decodedValue = FreeMarkerTemplateUtils.processTemplateIntoString(apiMetadataTemplate, Collections.singletonMap("api", apiModelEntity));
                    Map<String, String> metadataDecoded = Arrays
                        .stream(decodedValue.substring(1, decodedValue.length() - 1).split(", "))
                        .map(entry -> entry.split("="))
                        .collect(Collectors.toMap(entry -> entry[0], entry -> entry.length > 1 ? entry[1] : ""));
                    apiModelEntity.setMetadata(metadataDecoded);
                } catch (Exception ex) {
                    throw new TechnicalManagementException("An error occurs while evaluating API metadata", ex);
                }
            }
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
            final SwaggerApiEntity swaggerApiEntity = swaggerService.createAPI(importSwaggerDescriptorEntity);
            apiEntity.getPathMappings().addAll(swaggerApiEntity.getPathMappings());
        }

        return update(apiEntity.getId(), ApiService.convert(apiEntity));
    }

    public Collection<ApiEntity> search(final ApiQuery query) {
        try {
            LOGGER.debug("Search APIs by {}", query);
            return filterApiByQuery(this.convert(apiRepository.search(queryToCriteria(query).build())).stream(), query)
                .collect(toList());
        } catch (TechnicalException ex) {
            final String errorMessage = "An error occurs while trying to search for APIs: " + query;
            LOGGER.error(errorMessage, ex);
            throw new TechnicalManagementException(errorMessage, ex);
        }
    }

    @Override
    public Collection<String> searchIds(ApiQuery query) {
        try {
            LOGGER.debug("Search API ids by {}", query);
            return apiRepository.search(queryToCriteria(query).build()).stream().map(Api::getId).collect(toList());
        } catch (Exception ex) {
            final String errorMessage = "An error occurs while trying to search for API ids: " + query;
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
        return entities.stream()
            .filter(apiHeaderEntity -> apiHeaderEntity.getValue() != null && !apiHeaderEntity.getValue().isEmpty())
            .collect(Collectors.toList());
    }

    @Override
    public ApiEntity askForReview(final String apiId, final String userId, final ReviewEntity reviewEntity) {
        LOGGER.debug("Ask for review API {}", apiId);
        return updateWorkflowReview(apiId, userId, ApiHook.ASK_FOR_REVIEW, WorkflowState.IN_REVIEW, reviewEntity.getMessage());
    }

    @Override
    public ApiEntity acceptReview(final String apiId, final String userId, final ReviewEntity reviewEntity) {
        LOGGER.debug("Accept review API {}", apiId);
        return updateWorkflowReview(apiId, userId, ApiHook.REVIEW_OK, WorkflowState.REVIEW_OK, reviewEntity.getMessage());
    }

    @Override
    public ApiEntity rejectReview(final String apiId, final String userId, final ReviewEntity reviewEntity) {
        LOGGER.debug("Reject review API {}", apiId);
        return updateWorkflowReview(apiId, userId, ApiHook.REQUEST_FOR_CHANGES, WorkflowState.REQUEST_FOR_CHANGES, reviewEntity.getMessage());
    }

    @Override
    public ApiEntity duplicate(final String apiId, final DuplicateApiEntity duplicateApiEntity) {
        LOGGER.debug("Duplicate API {}", apiId);
        final ApiEntity apiEntity = findById(apiId);

        final UpdateApiEntity newApiEntity = convert(apiEntity);
        final Proxy proxy = apiEntity.getProxy();
        proxy.setVirtualHosts(singletonList(new VirtualHost(duplicateApiEntity.getContextPath())));
        newApiEntity.setProxy(proxy);
        newApiEntity.setVersion(duplicateApiEntity.getVersion() == null ? apiEntity.getVersion() : duplicateApiEntity.getVersion());

        if (duplicateApiEntity.getFilteredFields().contains("groups")) {
            newApiEntity.setGroups(null);
        } else {
            newApiEntity.setGroups(apiEntity.getGroups());
        }
        final ApiEntity duplicatedApi = create0(newApiEntity, getAuthenticatedUsername(), false);

        if (!duplicateApiEntity.getFilteredFields().contains("members")) {
            final Set<MembershipEntity> membershipsToDuplicate =
                membershipService.getMembershipsByReference(io.gravitee.rest.api.model.MembershipReferenceType.API, apiId);
            Optional<RoleEntity> optPrimaryOwnerRole = roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name());
            if (optPrimaryOwnerRole.isPresent()) {
                String primaryOwnerRoleId = optPrimaryOwnerRole.get().getId();
                membershipsToDuplicate.forEach(membership -> {
                    String roleId = membership.getRoleId();
                    if (!primaryOwnerRoleId.equals(roleId)) {
                        membershipService.addRoleToMemberOnReference(io.gravitee.rest.api.model.MembershipReferenceType.API, duplicatedApi.getId(), membership.getMemberType(), membership.getMemberId(), roleId);
                    }
                });
            }
        }

        if (!duplicateApiEntity.getFilteredFields().contains("pages")) {
            final List<PageEntity> pages = pageService.search(new PageQuery.Builder().api(apiId).build(), true);
            pages.forEach(page -> pageService.create(duplicatedApi.getId(), page));
        }

        if (!duplicateApiEntity.getFilteredFields().contains("plans")) {
            final Set<PlanEntity> plans = planService.findByApi(apiId);
            plans.forEach(plan -> planService.create(duplicatedApi.getId(), plan));
        }

        return duplicatedApi;
    }

    private UpdateApiEntity convert(final ApiEntity apiEntity) {
        final UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDescription(apiEntity.getDescription());
        updateApiEntity.setName(apiEntity.getName());
        updateApiEntity.setVersion(apiEntity.getVersion());
        updateApiEntity.setGroups(apiEntity.getGroups());
        updateApiEntity.setLabels(apiEntity.getLabels());
        updateApiEntity.setLifecycleState(apiEntity.getLifecycleState());
        updateApiEntity.setPicture(apiEntity.getPicture());
        updateApiEntity.setProperties(apiEntity.getProperties());
        updateApiEntity.setProxy(apiEntity.getProxy());
        updateApiEntity.setResources(apiEntity.getResources());
        updateApiEntity.setResponseTemplates(apiEntity.getResponseTemplates());
        updateApiEntity.setServices(apiEntity.getServices());
        updateApiEntity.setTags(apiEntity.getTags());
        updateApiEntity.setCategories(apiEntity.getCategories());
        updateApiEntity.setVisibility(apiEntity.getVisibility());
        updateApiEntity.setPaths(apiEntity.getPaths());
        updateApiEntity.setPathMappings(apiEntity.getPathMappings());
        updateApiEntity.setDisableMembershipNotifications(apiEntity.isDisableMembershipNotifications());
        return updateApiEntity;
    }

    private ApiEntity updateWorkflowReview(final String apiId, final String userId, final ApiHook hook,
                                           final WorkflowState workflowState, final String workflowMessage) {
        workflowService.create(WorkflowReferenceType.API, apiId, REVIEW, userId, workflowState, workflowMessage);
        final ApiEntity apiEntity = findById(apiId);
        apiEntity.setWorkflowState(workflowState);

        notifierService.trigger(hook, apiId,
            new NotificationParamsBuilder()
                .api(apiEntity)
                .user(userService.findById(userId))
                .build());
        return apiEntity;
    }

    private ApiCriteria.Builder queryToCriteria(ApiQuery query) {
        final ApiCriteria.Builder builder = new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment());
        if (query == null) {
            return builder;
        }
        builder.label(query.getLabel())
            .name(query.getName())
            .version(query.getVersion());

        if (!isBlank(query.getCategory())) {
            builder.category(categoryService.findById(query.getCategory()).getId());
        }
        if (query.getGroups() != null && !query.getGroups().isEmpty()) {
            builder.groups(query.getGroups().toArray(new String[0]));
        }
        if (!isBlank(query.getState())) {
            builder.state(LifecycleState.valueOf(query.getState()));
        }
        if (query.getVisibility() != null) {
            builder.visibility(Visibility.valueOf(query.getVisibility().name()));
        }
        if (query.getLifecycleStates() != null) {
            builder.lifecycleStates(query.getLifecycleStates().stream()
                .map(apiLifecycleState -> ApiLifecycleState.valueOf(apiLifecycleState.name()))
                .collect(toList()));
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
            ApiEntity apiEntity = convert(apiRepository.update(api), getPrimaryOwner(api), null);
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
                    && Objects.equals(loggingToUpdate.getCondition(), loggingUpdated.getCondition()))) {
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
        Optional<RoleEntity> optPrimaryOwnerRole = roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name());
        if (!optPrimaryOwnerRole.isPresent()) {
            throw new RoleNotFoundException("API_PRIMARY_OWNER");
        }
        //find primary owners usernames of each apis
        final List<String> apiIds = apis.stream().map(Api::getId).collect(toList());

        Set<MemberEntity> memberships = membershipService.getMembersByReferencesAndRole(MembershipReferenceType.API, apiIds, optPrimaryOwnerRole.get().getId());
        int poMissing = apis.size() - memberships.size();
        Stream<Api> streamApis = apis.stream();
        if (poMissing > 0) {
            Set<String> apiMembershipsIds = memberships.stream().map(MemberEntity::getReferenceId).collect(toSet());

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
        memberships.forEach(membership -> apiToUser.put(membership.getReferenceId(), membership.getId()));

        Map<String, UserEntity> userIdToUserEntity = new HashMap<>(memberships.size());
        userService.findByIds(memberships.stream().map(MemberEntity::getId).collect(toList()))
            .forEach(userEntity -> userIdToUserEntity.put(userEntity.getId(), userEntity));

        final List<CategoryEntity> categories = categoryService.findAll();
        return streamApis
            .map(publicApi -> this.convert(publicApi, userIdToUserEntity.get(apiToUser.get(publicApi.getId())), categories))
            .collect(toSet());
    }

    private ApiEntity convert(Api api) {
        return convert(api, null, null);
    }

    private ApiEntity convert(Api api, UserEntity primaryOwner, List<CategoryEntity> categories) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setName(api.getName());
        apiEntity.setDeployedAt(api.getDeployedAt());
        apiEntity.setCreatedAt(api.getCreatedAt());
        apiEntity.setGroups(api.getGroups());
        apiEntity.setDisableMembershipNotifications(api.isDisableMembershipNotifications());

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

                // Issue https://github.com/gravitee-io/issues/issues/3356
                if (apiDefinition.getProxy().getVirtualHosts() != null &&
                    !apiDefinition.getProxy().getVirtualHosts().isEmpty()) {
                    apiEntity.setContextPath(apiDefinition.getProxy().getVirtualHosts().get(0).getPath());
                }

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
        apiEntity.setLabels(api.getLabels());

        final Set<String> apiCategories = api.getCategories();
        if (apiCategories != null) {
            if (categories == null) {
                categories = categoryService.findAll();
            }
            final Set<String> newApiCategories = new HashSet<>(apiCategories.size());
            for (final String apiView : apiCategories) {
                final Optional<CategoryEntity> optionalView = categories.stream().filter(c -> apiView.equals(c.getId())).findAny();
                optionalView.ifPresent(category -> newApiCategories.add(category.getKey()));
            }
            apiEntity.setCategories(newApiCategories);
        }
        final LifecycleState state = api.getLifecycleState();
        if (state != null) {
            apiEntity.setState(Lifecycle.State.valueOf(state.name()));
        }
        if (api.getVisibility() != null) {
            apiEntity.setVisibility(io.gravitee.rest.api.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        if (primaryOwner != null) {
            apiEntity.setPrimaryOwner(new PrimaryOwnerEntity(primaryOwner));
        }
        final ApiLifecycleState lifecycleState = api.getApiLifecycleState();
        if (lifecycleState != null) {
            apiEntity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.valueOf(lifecycleState.name()));
        }

        if (parameterService.findAsBoolean(Key.API_REVIEW_ENABLED)) {
            final List<Workflow> workflows = workflowService.findByReferenceAndType(API, api.getId(), REVIEW);
            if (workflows != null && !workflows.isEmpty()) {
                apiEntity.setWorkflowState(WorkflowState.valueOf(workflows.get(0).getState()));
            }
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

        final Set<String> apiCategories = updateApiEntity.getCategories();
        if (apiCategories != null) {
            final List<CategoryEntity> categories = categoryService.findAll();
            final Set<String> newApiCategories = new HashSet<>(apiCategories.size());
            for (final String apiCategory : apiCategories) {
                final Optional<CategoryEntity> optionalCategory =
                    categories.stream().filter(c -> apiCategory.equals(c.getKey()) || apiCategory.equals(c.getId())).findAny();
                optionalCategory.ifPresent(category -> newApiCategories.add(category.getId()));
            }
            api.setCategories(newApiCategories);
        }

        if (updateApiEntity.getLabels() != null) {
            api.setLabels(new ArrayList(new LinkedHashSet<>(updateApiEntity.getLabels())));
        }

        api.setGroups(updateApiEntity.getGroups());
        api.setDisableMembershipNotifications(updateApiEntity.isDisableMembershipNotifications());

        try {
            io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
            apiDefinition.setId(apiId);
            apiDefinition.setName(updateApiEntity.getName());
            apiDefinition.setVersion(updateApiEntity.getVersion());
            apiDefinition.setProxy(updateApiEntity.getProxy());

            apiDefinition.setPaths(updateApiEntity.getPaths());
            if (updateApiEntity.getPathMappings() != null) {
                apiDefinition.setPathMappings(updateApiEntity.getPathMappings().stream()
                    .collect(toMap(pathMapping -> pathMapping, pathMapping -> Pattern.compile(""))));
            }

            apiDefinition.setServices(updateApiEntity.getServices());
            apiDefinition.setResources(updateApiEntity.getResources());
            apiDefinition.setProperties(updateApiEntity.getProperties());
            apiDefinition.setTags(updateApiEntity.getTags());

            apiDefinition.setResponseTemplates(updateApiEntity.getResponseTemplates());

            String definition = objectMapper.writeValueAsString(apiDefinition);
            api.setDefinition(definition);
            if (updateApiEntity.getLifecycleState() != null) {
                api.setApiLifecycleState(ApiLifecycleState.valueOf(updateApiEntity.getLifecycleState().name()));
            }
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
        private List<String> roles; // After v3
        private String role; // Before v3

        public MemberToImport() {
        }

        public MemberToImport(String source, String sourceId, List<String> roles, String role) {
            this.source = source;
            this.sourceId = sourceId;
            this.roles = roles;
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

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
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

    private ProxyModelEntity convert(Proxy proxy) {
        ProxyModelEntity proxyModelEntity = new ProxyModelEntity();

        proxyModelEntity.setCors(proxy.getCors());
        proxyModelEntity.setFailover(proxy.getFailover());
        proxyModelEntity.setGroups(proxy.getGroups());
        proxyModelEntity.setLogging(proxy.getLogging());
        proxyModelEntity.setPreserveHost(proxy.isPreserveHost());
        proxyModelEntity.setStripContextPath(proxy.isStripContextPath());
        proxyModelEntity.setVirtualHosts(proxy.getVirtualHosts());

        //add a default context-path to preserve compatibility on old templates
        if (proxy.getVirtualHosts() != null && !proxy.getVirtualHosts().isEmpty()) {
            proxyModelEntity.setContextPath(proxy.getVirtualHosts().get(0).getPath());
        }

        return proxyModelEntity;
    }
}
