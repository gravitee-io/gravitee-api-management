/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.AccessControlReferenceType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.ImportPageEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.SystemFolderType;
import io.gravitee.rest.api.model.UpdateApiMetadataEntity;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.DuplicateApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiIdsCalculatorService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipDuplicateService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageDuplicateService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.ApiImportException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.imports.ImportApiJsonNode;
import io.gravitee.rest.api.service.imports.ImportJsonNode;
import io.gravitee.rest.api.service.imports.ImportJsonNodeWithIds;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minidev.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Gaëtan MAISSE (gaetan.maisse at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiDuplicatorServiceImpl extends AbstractService implements ApiDuplicatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiDuplicatorServiceImpl.class);
    public static final String API_DEFINITION_FIELD_GROUPS = "groups";
    public static final String API_DEFINITION_FIELD_PLANS = "plans";
    public static final String API_DEFINITION_FIELD_MEMBERS = "members";
    public static final String API_DEFINITION_FIELD_PAGES = "pages";

    private final HttpClientService httpClientService;
    private final ImportConfiguration importConfiguration;
    private final MediaService mediaService;
    private final ObjectMapper objectMapper;
    private final ApiMetadataService apiMetadataService;
    private final MembershipService membershipService;
    private final MembershipDuplicateService membershipDuplicateService;
    private final RoleService roleService;
    private final PageService pageService;
    private final PageDuplicateService pageDuplicateService;
    private final PlanService planService;
    private final GroupService groupService;
    private final UserService userService;
    private final ApiService apiService;
    private final ApiConverter apiConverter;
    private final PlanConverter planConverter;
    private final PermissionService permissionService;
    private final ApiIdsCalculatorService apiIdsCalculatorService;
    private final CategoryMapper categoryMapper;

    public ApiDuplicatorServiceImpl(
        HttpClientService httpClientService,
        ImportConfiguration importConfiguration,
        MediaService mediaService,
        ObjectMapper objectMapper,
        ApiMetadataService apiMetadataService,
        MembershipService membershipService,
        MembershipDuplicateService membershipDuplicateService,
        RoleService roleService,
        PageService pageService,
        PageDuplicateService pageDuplicateService,
        PlanService planService,
        GroupService groupService,
        UserService userService,
        @Lazy ApiService apiService,
        ApiConverter apiConverter,
        PlanConverter planConverter,
        PermissionService permissionService,
        ApiIdsCalculatorService apiIdsCalculatorService,
        CategoryMapper categoryMapper
    ) {
        this.httpClientService = httpClientService;
        this.importConfiguration = importConfiguration;
        this.mediaService = mediaService;
        this.objectMapper = objectMapper;
        this.apiMetadataService = apiMetadataService;
        this.membershipService = membershipService;
        this.membershipDuplicateService = membershipDuplicateService;
        this.roleService = roleService;
        this.pageService = pageService;
        this.pageDuplicateService = pageDuplicateService;
        this.planService = planService;
        this.groupService = groupService;
        this.userService = userService;
        this.apiService = apiService;
        this.apiConverter = apiConverter;
        this.planConverter = planConverter;
        this.permissionService = permissionService;
        this.apiIdsCalculatorService = apiIdsCalculatorService;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public ApiEntity createWithImportedDefinition(final ExecutionContext executionContext, Object apiDefinitionOrURL) {
        String apiDefinition = fetchApiDefinitionContentFromURL(apiDefinitionOrURL);
        try {
            // Read the whole input API definition, and recalculate its ID
            ImportApiJsonNode apiJsonNode = apiIdsCalculatorService.recalculateApiDefinitionIds(
                executionContext,
                new ImportApiJsonNode(objectMapper.readTree(apiDefinition))
            );

            // check API consistency before import
            checkApiJsonConsistency(apiJsonNode);

            // import
            UpdateApiEntity importedApi = convertToEntity(executionContext, apiJsonNode.toString(), apiJsonNode);

            ApiEntity createdApiEntity = apiService.createWithApiDefinition(
                executionContext,
                importedApi,
                getAuthenticatedUsername(),
                apiJsonNode.getJsonNode()
            );
            createOrUpdateApiNestedEntities(executionContext, createdApiEntity, apiJsonNode);
            createPageAndMedia(executionContext, createdApiEntity, apiJsonNode);
            return createdApiEntity;
        } catch (IOException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", apiDefinition, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the API definition.");
        }
    }

    @Override
    public ApiEntity updateWithImportedDefinition(final ExecutionContext executionContext, String urlApiId, Object apiDefinitionOrURL) {
        String apiDefinition = fetchApiDefinitionContentFromURL(apiDefinitionOrURL);

        try {
            // Read the whole input API definition, and recalculate its ID
            ImportApiJsonNode apiJsonNode = apiIdsCalculatorService.recalculateApiDefinitionIds(
                executionContext,
                new ImportApiJsonNode(objectMapper.readTree(apiDefinition)),
                urlApiId
            );

            UpdateApiEntity importedApi = convertToEntity(executionContext, apiJsonNode.toString(), apiJsonNode);

            if (DefinitionVersion.V1.equals(DefinitionVersion.valueOfLabel(importedApi.getGraviteeDefinitionVersion()))) {
                throw new ApiDefinitionVersionNotSupportedException(importedApi.getGraviteeDefinitionVersion());
            }

            // ensure user has required permission to update target API
            if (
                !isAuthenticated() ||
                !(isEnvironmentAdmin() || permissionService.hasPermission(executionContext, API_DEFINITION, apiJsonNode.getId(), UPDATE))
            ) {
                throw new ForbiddenAccessException();
            }

            // check API consistency before import
            checkApiJsonConsistency(executionContext, apiJsonNode, urlApiId);

            // import
            ApiEntity updatedApiEntity = apiService.update(executionContext, apiJsonNode.getId(), importedApi);
            createOrUpdateApiNestedEntities(executionContext, updatedApiEntity, apiJsonNode);
            return updatedApiEntity;
        } catch (IOException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", apiDefinition, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the API definition.");
        }
    }

    @Override
    public ApiEntity duplicate(ExecutionContext executionContext, final ApiEntity apiEntity, final DuplicateApiEntity duplicateApiEntity) {
        requireNonNull(apiEntity, "Missing ApiEntity");
        final String apiId = apiEntity.getId();
        LOGGER.debug("Duplicate API {}", apiId);

        final UpdateApiEntity newApiEntity = apiConverter.toUpdateApiEntity(apiEntity, true);
        final Proxy proxy = apiEntity.getProxy();
        proxy.setVirtualHosts(singletonList(new VirtualHost(duplicateApiEntity.getContextPath())));
        newApiEntity.setProxy(proxy);
        newApiEntity.setVersion(duplicateApiEntity.getVersion() == null ? apiEntity.getVersion() : duplicateApiEntity.getVersion());

        if (duplicateApiEntity.getFilteredFields().contains(API_DEFINITION_FIELD_GROUPS)) {
            newApiEntity.setGroups(null);
        } else {
            newApiEntity.setGroups(apiEntity.getGroups());
        }

        Map<String, String> plansIdsMap = new HashMap<>();
        if (!duplicateApiEntity.getFilteredFields().contains(API_DEFINITION_FIELD_PLANS)) {
            newApiEntity
                .getPlans()
                .forEach(plan -> {
                    String newPlanId = UuidString.generateRandom();
                    plansIdsMap.put(plan.getId(), newPlanId);
                    plan.setId(newPlanId);
                });
        }

        final ApiEntity duplicatedApi = apiService.createWithApiDefinition(
            executionContext,
            newApiEntity,
            getAuthenticatedUsername(),
            null
        );

        if (!duplicateApiEntity.getFilteredFields().contains(API_DEFINITION_FIELD_MEMBERS)) {
            membershipDuplicateService.duplicateMemberships(executionContext, apiId, duplicatedApi.getId(), getAuthenticatedUsername());
        }

        final Map<String, String> pagesIdsMap = new HashMap<>();

        if (!duplicateApiEntity.getFilteredFields().contains(API_DEFINITION_FIELD_PAGES)) {
            pagesIdsMap.putAll(
                pageDuplicateService.duplicatePages(executionContext, apiId, duplicatedApi.getId(), getAuthenticatedUsername())
            );
        }

        if (!duplicateApiEntity.getFilteredFields().contains(API_DEFINITION_FIELD_PLANS)) {
            planService
                .findByApi(executionContext, apiId)
                .forEach(plan -> {
                    plan.setId(plansIdsMap.get(plan.getId()));
                    plan.setApi(duplicatedApi.getId());
                    if (plan.getGeneralConditions() != null) {
                        plan.setGeneralConditions(pagesIdsMap.get(plan.getGeneralConditions()));
                    }
                    planService.create(executionContext, planConverter.toNewPlanEntity(plan, true));
                });
        }

        return duplicatedApi;
    }

    private UpdateApiEntity convertToEntity(final ExecutionContext executionContext, String apiDefinition, ImportApiJsonNode apiJsonNode)
        throws IOException {
        final UpdateApiEntity importedApi = objectMapper
            // because definition could contains other values than the api itself (pages, members)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(apiDefinition, UpdateApiEntity.class);

        // Initialize with a default path
        if (
            Objects.equals(importedApi.getGraviteeDefinitionVersion(), DefinitionVersion.V1.getLabel()) &&
            (importedApi.getPaths() == null || importedApi.getPaths().isEmpty())
        ) {
            importedApi.setPaths(Collections.singletonMap("/", new ArrayList<>()));
        }

        //create group if not exist & replace groupName by groupId
        if (importedApi.getGroups() != null) {
            Set<String> groupNames = new HashSet<>(importedApi.getGroups());
            importedApi.getGroups().clear();
            for (String name : groupNames) {
                List<GroupEntity> groupEntities = groupService.findByName(executionContext.getEnvironmentId(), name);
                GroupEntity group;
                if (groupEntities.isEmpty()) {
                    NewGroupEntity newGroupEntity = new NewGroupEntity();
                    newGroupEntity.setName(name);
                    group = groupService.create(executionContext, newGroupEntity);
                } else {
                    group = groupEntities.get(0);
                }
                importedApi.getGroups().add(group.getId());
            }
        } else {
            // Using a mutable Set, so we can add groups if necessary
            importedApi.setGroups(new HashSet<>());
        }

        // Views & Categories
        // Before 3.0.2, API 'categories' were called 'views'. This is for compatibility.
        final List<ImportJsonNode> viewsNodes = apiJsonNode.getViews();
        if (!viewsNodes.isEmpty()) {
            Set<String> categories = viewsNodes.stream().map(ImportJsonNode::asText).collect(toSet());
            importedApi.setCategories(categories);
        }

        if (apiJsonNode.isKubernetesOrigin()) {
            var categories = cleanDefinitionCategories(executionContext.getEnvironmentId(), apiJsonNode);
            importedApi.setCategories(categories);
        }

        // merge existing plans data with plans definition data
        // cause plans definition may contain less data than plans entities (for example when rollback an API from gateway event)
        Map<String, PlanEntity> existingPlans = readApiPlansById(executionContext, apiJsonNode.getId());
        importedApi.setPlans(readPlansToImportFromDefinition(apiJsonNode, existingPlans));

        return importedApi;
    }

    protected Set<String> cleanDefinitionCategories(String environmentId, ImportApiJsonNode apiNode) {
        if (!apiNode.hasCategories() || apiNode.getCategoriesArray().isEmpty()) {
            return Set.of();
        }
        var categoriesNode = apiNode.getCategoriesArray();
        var categories = new HashSet<String>();
        if (apiNode.isKubernetesOrigin()) {
            // In kubernetes resources, a category can be referenced by name. If we don't do this the
            // category is not found because the IdGenerator lower cases the category name to produce
            // the key when the category is created. We don't know what would be the implications
            // of applying this for non kubernetes resources, hence the condition.
            categoriesNode.forEach(category -> categories.add(IdGenerator.generate(category.asText())));
        } else {
            categoriesNode.forEach(category -> categories.add(category.asText()));
        }
        return categoryMapper.toCategoryId(environmentId, categories);
    }

    private void createPageAndMedia(final ExecutionContext executionContext, ApiEntity createdApiEntity, ImportApiJsonNode apiJsonNode)
        throws JsonProcessingException {
        for (ImportJsonNode media : apiJsonNode.getMedia()) {
            mediaService.createWithDefinition(createdApiEntity.getId(), media.toString());
        }

        List<PageEntity> search = pageService.search(
            executionContext.getEnvironmentId(),
            new PageQuery.Builder()
                .api(createdApiEntity.getId())
                .name(SystemFolderType.ASIDE.folderName())
                .type(PageType.SYSTEM_FOLDER)
                .build()
        );

        if (search.isEmpty()) {
            pageService.createAsideFolder(executionContext, createdApiEntity.getId());
        }
    }

    private String fetchApiDefinitionContentFromURL(Object apiDefinitionOrURL) {
        String definitionOrURL = stringifyApiDefinitionFromJackson(apiDefinitionOrURL);
        if (definitionOrURL.toUpperCase().startsWith("HTTP")) {
            UrlSanitizerUtils.checkAllowed(
                definitionOrURL,
                importConfiguration.getImportWhitelist(),
                importConfiguration.isAllowImportFromPrivate()
            );
            Buffer buffer = httpClientService.request(HttpMethod.GET, definitionOrURL, null, null, null);
            return buffer.toString();
        }
        return definitionOrURL;
    }

    private void createOrUpdateApiNestedEntities(
        final ExecutionContext executionContext,
        ApiEntity apiEntity,
        ImportApiJsonNode apiJsonNode
    ) throws IOException {
        createOrUpdateMembers(executionContext, apiEntity, apiJsonNode);
        createOrUpdatePages(executionContext, apiEntity, apiJsonNode);
        createOrUpdatePlans(executionContext, apiEntity, apiJsonNode);
        createOrUpdateMetadata(executionContext, apiEntity, apiJsonNode);
    }

    private void createOrUpdateMembers(final ExecutionContext executionContext, ApiEntity apiEntity, ImportApiJsonNode apiJsonNode)
        throws JsonProcessingException {
        if (apiJsonNode.hasMembers()) {
            // get current members of the api
            Set<MemberToImport> membersAlreadyPresent = getAPICurrentMembers(executionContext, apiEntity.getId());
            // get the current PO
            RoleEntity poRole = roleService.findPrimaryOwnerRoleByOrganization(executionContext.getOrganizationId(), RoleScope.API);
            assert (poRole != null);
            String poRoleId = poRole.getId();
            MemberToImport currentPo = membersAlreadyPresent
                .stream()
                .filter(memberToImport -> memberToImport.getRoles().contains(poRoleId))
                .findFirst()
                .orElse(new MemberToImport());

            List<String> roleUsedInTransfert = null;
            MemberToImport futurePo = null;

            // upsert members
            Optional<RoleEntity> apiUserRoleOpt = roleService.findByScopeAndName(
                RoleScope.API,
                "USER",
                executionContext.getOrganizationId()
            );
            for (final ImportJsonNode memberNode : apiJsonNode.getMembers()) {
                MemberToImport memberToImport = objectMapper.readValue(memberNode.toString(), MemberToImport.class);
                memberToImport.setRoles(getRoleIdsToImport(executionContext, memberToImport));
                boolean presentWithSameRole = isPresentWithSameRole(membersAlreadyPresent, memberToImport);

                List<String> roleIdsToImport = memberToImport
                    .getRoles()
                    .stream()
                    .filter(roleId -> {
                        var role = roleService.findByIdAndOrganizationId(roleId, executionContext.getOrganizationId());
                        var isValidRole = role.isPresent() && RoleScope.API.equals(role.get().getScope());
                        if (!isValidRole) {
                            LOGGER.warn(
                                "Role {} does not exist in organization {} or is not an API role",
                                roleId,
                                executionContext.getOrganizationId()
                            );
                        }
                        return isValidRole;
                    })
                    .collect(Collectors.toList());

                if (roleIdsToImport.isEmpty() && apiUserRoleOpt.isPresent()) {
                    roleIdsToImport.add(apiUserRoleOpt.get().getId());
                }

                addOrUpdateMembers(
                    executionContext,
                    apiEntity.getId(),
                    poRoleId,
                    currentPo,
                    memberToImport,
                    roleIdsToImport,
                    presentWithSameRole
                );

                // get the future role of the current PO
                if (
                    memberToImport.getSourceId().equals(currentPo.getSourceId()) &&
                    memberToImport.getSource().equals(currentPo.getSource()) &&
                    !roleIdsToImport.contains(poRoleId)
                ) {
                    roleUsedInTransfert = roleIdsToImport;
                }

                if (roleIdsToImport.contains(poRoleId)) {
                    futurePo = memberToImport;
                }

                /*
                 * Use by GKO
                 * When the API comes from Kubernetes, we remove the members which are not in use anymore
                 * If the same behaviour required for all APIs regardless of their origin, then the context check
                 * can be removed
                 */
                if (apiJsonNode.isKubernetesOrigin()) {
                    // by removing the member, we will eventually end up with a clean list of
                    // Members that can be removed from this API
                    removePreviousRoleIfExist(executionContext, apiEntity.getId(), memberToImport, roleIdsToImport);
                    membersAlreadyPresent.remove(memberToImport);
                }
            }

            // Use by GKO
            if (apiJsonNode.isKubernetesOrigin()) {
                // delete members that no longer referenced inside this API
                for (MemberToImport memberToDelete : membersAlreadyPresent) {
                    if (!memberToDelete.getRoles().contains(poRoleId)) {
                        deleteMembers(executionContext, apiEntity.getId(), memberToDelete);
                    }
                }
            }

            // transfer the ownership
            transferOwnership(executionContext, apiEntity.getId(), currentPo, roleUsedInTransfert, futurePo);
        } else if (!apiJsonNode.hasMembers() && apiJsonNode.isKubernetesOrigin()) { // Used by GKO
            // Remove all members if exist except the PO
            // get current members of the api
            Set<MemberToImport> membersAlreadyPresent = getAPICurrentMembers(executionContext, apiEntity.getId());
            if (!membersAlreadyPresent.isEmpty()) {
                // get the current PO
                RoleEntity poRole = roleService.findPrimaryOwnerRoleByOrganization(executionContext.getOrganizationId(), RoleScope.API);
                assert (poRole != null);
                String poRoleId = poRole.getId();

                // delete members that no longer referenced inside this API
                for (MemberToImport memberToDelete : membersAlreadyPresent) {
                    if (!memberToDelete.getRoles().contains(poRoleId)) {
                        deleteMembers(executionContext, apiEntity.getId(), memberToDelete);
                    }
                }
            }
        }
    }

    @NotNull
    protected Set<MemberToImport> getAPICurrentMembers(ExecutionContext executionContext, String apiId) {
        return membershipService
            .getMembersByReference(executionContext, MembershipReferenceType.API, apiId)
            .stream()
            .filter(member -> member.getType() == MembershipMemberType.USER)
            .map(member -> {
                UserEntity userEntity = userService.findById(executionContext, member.getId());
                return new MemberToImport(
                    userEntity.getSource(),
                    userEntity.getSourceId(),
                    member.getRoles().stream().map(RoleEntity::getId).collect(Collectors.toList()),
                    null
                );
            })
            .collect(toSet());
    }

    protected boolean isPresentWithSameRole(Set<MemberToImport> membersAlreadyPresent, MemberToImport memberToImport) {
        return (
            memberToImport.getRoles() != null &&
            !memberToImport.getRoles().isEmpty() &&
            membersAlreadyPresent
                .stream()
                .anyMatch(m -> {
                    m.getRoles().sort(Comparator.naturalOrder());
                    return (
                        m.getRoles().equals(memberToImport.getRoles()) &&
                        (m.getSourceId().equals(memberToImport.getSourceId()) && m.getSource().equals(memberToImport.getSource()))
                    );
                })
        );
    }

    protected List<String> getRoleIdsToImport(ExecutionContext executionContext, MemberToImport memberToImport) {
        // Starting with v3, multiple roles per member can be imported and it is a list of role Ids.
        List<String> roleIdsToImport = memberToImport.getRoles();
        if (roleIdsToImport == null) {
            roleIdsToImport = new ArrayList<>();
        } else {
            roleIdsToImport = new ArrayList<>(roleIdsToImport);
        }

        /*
         * Used by GKO
         * Before v3, only one role per member could be imported and it was a role name.
         * And now we use the same thing for the APIs created by GKO
         */
        String roleIdOrName = memberToImport.getRole();
        if (roleIdOrName != null && !roleIdOrName.isEmpty()) {
            try {
                UUID.fromString(roleIdOrName);
                roleIdsToImport.add(roleIdOrName);
            } catch (IllegalArgumentException e) {
                Optional<RoleEntity> optRoleToAddEntity = roleService.findByScopeAndName(
                    RoleScope.API,
                    roleIdOrName,
                    executionContext.getOrganizationId()
                );
                if (optRoleToAddEntity.isPresent()) {
                    roleIdsToImport.add(optRoleToAddEntity.get().getId());
                } else {
                    LOGGER.warn("Role {} does not exist", roleIdOrName);
                    // We still add it to the list, same as what has been done for the memberToImport.getRoles()
                    roleIdsToImport.add(roleIdOrName);
                }
            }
        }

        roleIdsToImport.sort(Comparator.naturalOrder());

        return roleIdsToImport;
    }

    protected void addOrUpdateMembers(
        ExecutionContext executionContext,
        String apiId,
        String poRoleId,
        MemberToImport currentPo,
        MemberToImport memberToImport,
        List<String> rolesToImport,
        boolean presentWithSameRole
    ) {
        // add/update members if :
        //  - not already present with the same role
        //  - not the new PO
        //  - not the current PO
        if (
            !presentWithSameRole &&
            (memberToImport.getRoles() != null && !memberToImport.getRoles().isEmpty() && !memberToImport.getRoles().contains(poRoleId)) &&
            !(memberToImport.getSourceId().equals(currentPo.getSourceId()) && memberToImport.getSource().equals(currentPo.getSource()))
        ) {
            try {
                UserEntity userEntity = userService.findBySource(
                    executionContext.getOrganizationId(),
                    memberToImport.getSource(),
                    memberToImport.getSourceId(),
                    false
                );

                rolesToImport.forEach(role -> {
                    try {
                        membershipService.addRoleToMemberOnReference(
                            executionContext,
                            MembershipReferenceType.API,
                            apiId,
                            MembershipMemberType.USER,
                            userEntity.getId(),
                            role
                        );
                    } catch (Exception e) {
                        LOGGER.warn(
                            "Unable to add role '{}' to member '{}' on API '{}' due to : {}",
                            role,
                            userEntity.getId(),
                            apiId,
                            e.getMessage()
                        );
                    }
                });
            } catch (UserNotFoundException unfe) {}
        }
    }

    private void removePreviousRoleIfExist(
        ExecutionContext executionContext,
        String apiId,
        MemberToImport memberToImport,
        List<String> roleIdsToImport
    ) {
        UserEntity userEntity = userService.findBySource(
            executionContext.getOrganizationId(),
            memberToImport.getSource(),
            memberToImport.getSourceId(),
            false
        );

        List<String> existingRoles = new ArrayList<>(
            membershipService
                .getRoles(MembershipReferenceType.API, apiId, MembershipMemberType.USER, userEntity.getId())
                .stream()
                .map(RoleEntity::getId)
                .toList()
        );

        roleIdsToImport.forEach(existingRoles::remove);

        existingRoles.forEach(roleId ->
            membershipService.removeRole(MembershipReferenceType.API, apiId, MembershipMemberType.USER, userEntity.getId(), roleId)
        );
    }

    protected void deleteMembers(ExecutionContext executionContext, String apiId, MemberToImport memberToImport) {
        try {
            UserEntity userEntity = userService.findBySource(
                executionContext.getOrganizationId(),
                memberToImport.getSource(),
                memberToImport.getSourceId(),
                false
            );

            membershipService.deleteMemberForApi(executionContext, apiId, userEntity.getId());
        } catch (UserNotFoundException unfe) {} catch (Exception e) {
            LOGGER.warn("Unable to delete membership from API '{}' due to : {}", apiId, e.getMessage());
        }
    }

    private void transferOwnership(
        ExecutionContext executionContext,
        String apiId,
        MemberToImport currentPo,
        List<String> roleUsedInTransfert,
        MemberToImport futurePo
    ) {
        if (
            futurePo != null &&
            !(futurePo.getSource().equals(currentPo.getSource()) && futurePo.getSourceId().equals(currentPo.getSourceId()))
        ) {
            try {
                UserEntity userEntity = userService.findBySource(
                    executionContext.getOrganizationId(),
                    futurePo.getSource(),
                    futurePo.getSourceId(),
                    false
                );
                List<RoleEntity> roleEntity = null;
                if (roleUsedInTransfert != null && !roleUsedInTransfert.isEmpty()) {
                    roleEntity = roleUsedInTransfert.stream().map(roleService::findById).toList();
                }
                membershipService.transferApiOwnership(
                    executionContext,
                    apiId,
                    new MembershipService.MembershipMember(userEntity.getId(), null, MembershipMemberType.USER),
                    roleEntity
                );
            } catch (UserNotFoundException unfe) {}
        }
    }

    protected void createOrUpdateMetadata(final ExecutionContext executionContext, ApiEntity apiEntity, ImportApiJsonNode apiJsonNode) {
        try {
            for (ImportJsonNode metadataNode : apiJsonNode.getMetadata()) {
                UpdateApiMetadataEntity updateApiMetadataEntity = objectMapper.readValue(
                    metadataNode.toString(),
                    UpdateApiMetadataEntity.class
                );
                updateApiMetadataEntity.setApiId(apiEntity.getId());
                apiMetadataService.update(executionContext, updateApiMetadataEntity);
            }
        } catch (Exception ex) {
            LOGGER.error("An error occurs while creating API metadata", ex);
            throw new TechnicalManagementException("An error occurs while creating API Metadata", ex);
        }
    }

    protected void createOrUpdatePlans(final ExecutionContext executionContext, ApiEntity apiEntity, ImportApiJsonNode apiJsonNode)
        throws IOException {
        if (apiJsonNode.hasPlans()) {
            Map<String, PlanEntity> existingPlans = readApiPlansById(executionContext, apiEntity.getId());
            Set<PlanEntity> plansToImport = readPlansToImportFromDefinition(apiJsonNode, existingPlans);

            findRemovedPlans(existingPlans.values(), plansToImport)
                .forEach(plan -> {
                    planService.delete(executionContext, plan.getId());
                    apiEntity.getPlans().remove(plan);
                });

            plansToImport.forEach(planEntity -> {
                planEntity.setApi(apiEntity.getId());
                replacePlanGroupNameById(executionContext, apiEntity, planEntity);
                planService.createOrUpdatePlan(executionContext, planEntity);
                apiEntity.getPlans().add(planEntity);
            });
        }
    }

    private void replacePlanGroupNameById(final ExecutionContext executionContext, ApiEntity apiEntity, PlanEntity planEntity) {
        if (apiEntity.getGroups() != null && planEntity.getExcludedGroups() != null) {
            Set<String> groupNames = new HashSet<>(planEntity.getExcludedGroups());
            planEntity.getExcludedGroups().clear();
            for (String name : groupNames) {
                List<GroupEntity> groupEntities = groupService.findByName(executionContext.getEnvironmentId(), name);
                GroupEntity group;
                if (!groupEntities.isEmpty()) {
                    group = groupEntities.get(0);
                    planEntity.getExcludedGroups().add(group.getId());
                } else {
                    LOGGER.warn(
                        "Group with name {} does not exist and can't be added to plan \"{}\" [{}]",
                        name,
                        planEntity.getName(),
                        planEntity.getId()
                    );
                }
            }
        }
    }

    private Map<String, PlanEntity> readApiPlansById(ExecutionContext executionContext, String apiId) {
        return planService.findByApi(executionContext, apiId).stream().collect(toMap(PlanEntity::getId, Function.identity()));
    }

    protected void createOrUpdatePages(final ExecutionContext executionContext, ApiEntity apiEntity, ImportApiJsonNode apiJsonNode)
        throws JsonProcessingException {
        if (apiJsonNode.hasPages() && !apiJsonNode.getPages().isEmpty()) {
            List<PageEntity> pagesList = objectMapper.readValue(
                apiJsonNode.getPages().toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, PageEntity.class)
            );

            replacePagesGroupNameById(executionContext, pagesList);

            if (apiJsonNode.isKubernetesOrigin()) {
                importKubernetesPages(executionContext, apiEntity.getId(), pagesList);
            } else {
                pageService.createOrUpdatePages(executionContext, pagesList, apiEntity.getId());
            }
        } else if (apiJsonNode.isKubernetesOrigin()) {
            pageService.deleteAllByApi(executionContext, apiEntity.getId());
        }
    }

    private void importKubernetesPages(ExecutionContext executionContext, String apiId, List<PageEntity> pages) {
        deleteRemovedPages(executionContext, apiId, pages);
        var rootPages = pages.stream().filter(PageEntity::isRoot).toList();
        importKubernetesRootPages(executionContext, apiId, rootPages);
        pages.removeAll(rootPages);
        pageService.createOrUpdatePages(executionContext, pages, apiId);
    }

    private void importKubernetesRootPages(ExecutionContext executionContext, String apiId, List<PageEntity> rootPages) {
        for (var rootPage : rootPages) {
            overrideAccessControls(
                executionContext,
                apiId,
                rootPage,
                pageService.importFiles(
                    executionContext,
                    apiId,
                    ImportPageEntity
                        .builder()
                        .type(PageType.ROOT)
                        .visibility(rootPage.getVisibility())
                        .published(rootPage.isPublished())
                        .source(rootPage.getSource())
                        .build()
                )
            );
        }
    }

    private void overrideAccessControls(
        ExecutionContext executionContext,
        String apiId,
        PageEntity rootPage,
        List<PageEntity> importedPages
    ) {
        pageService.createOrUpdatePages(
            executionContext,
            importedPages
                .stream()
                .map(page ->
                    page
                        .toBuilder()
                        .visibility(rootPage.getVisibility())
                        .accessControls(rootPage.getAccessControls())
                        .excludedGroups(rootPage.getExcludedGroups())
                        .excludedAccessControls(rootPage.isExcludedAccessControls())
                        .build()
                )
                .toList(),
            apiId
        );
    }

    private void deleteRemovedPages(ExecutionContext executionContext, String apiId, List<PageEntity> givenPages) {
        var givenPageIds = givenPages.stream().map(PageEntity::getId).collect(toSet());
        var existingPageIds = pageService
            .findByApi(executionContext.getEnvironmentId(), apiId)
            .stream()
            .map(PageEntity::getId)
            .collect(toSet());

        existingPageIds.removeIf(givenPageIds::contains);

        try {
            for (var id : existingPageIds) {
                pageService.delete(executionContext, id);
            }
        } catch (RuntimeException e) {
            LOGGER.error("An error as occurred while trying to remove a page with kubernetes origin");
        }
    }

    private void replacePagesGroupNameById(ExecutionContext executionContext, List<PageEntity> pagesList) {
        final Map<String, String> pageGroupEntities = new HashMap<>();

        pagesList.forEach(pageEntity -> {
            if (pageEntity.getAccessControls() != null) {
                pageEntity.setAccessControls(
                    pageEntity
                        .getAccessControls()
                        .stream()
                        .filter(accessControlEntity ->
                            accessControlEntity.getReferenceType().equals(AccessControlReferenceType.GROUP.name())
                        )
                        .peek(accessControlEntity -> {
                            String groupId = pageGroupEntities.computeIfAbsent(
                                accessControlEntity.getReferenceId(),
                                key -> {
                                    List<GroupEntity> groupEntities = groupService.findByName(executionContext.getEnvironmentId(), key);
                                    if (!groupEntities.isEmpty()) {
                                        return groupEntities.get(0).getId();
                                    } else {
                                        LOGGER.warn(
                                            "Group with name {} does not exist and can't be added to access control list of page \"{}\" [{}]",
                                            accessControlEntity.getReferenceId(),
                                            pageEntity.getName(),
                                            pageEntity.getId()
                                        );
                                        return null;
                                    }
                                }
                            );
                            if (groupId != null) {
                                accessControlEntity.setReferenceId(groupId);
                            }
                        })
                        .collect(toSet())
                );
            }
        });
    }

    protected static class MemberToImport {

        private String source;
        private String sourceId;
        private List<String> roles; // After v3
        private String role; // Before v3 but now it is used by GKO

        public MemberToImport() {}

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

    private Stream<PlanEntity> findRemovedPlans(Collection<PlanEntity> existingPlans, Collection<PlanEntity> importedPlans) {
        return existingPlans.stream().filter(existingPlan -> !importedPlans.contains(existingPlan));
    }

    private Set<PlanEntity> readPlansToImportFromDefinition(ImportApiJsonNode apiJsonNode, Map<String, PlanEntity> existingPlans)
        throws IOException {
        Set<PlanEntity> plansToImport = new HashSet<>();
        for (ImportJsonNodeWithIds planNode : apiJsonNode.getPlans()) {
            PlanEntity existingPlan = planNode.hasId() ? existingPlans.get(planNode.getId()) : null;
            if (existingPlan != null) {
                plansToImport.add(objectMapper.readerForUpdating(existingPlan).readValue(planNode.getJsonNode()));
            } else {
                plansToImport.add(objectMapper.readValue(planNode.toString(), PlanEntity.class));
            }
        }
        return plansToImport;
    }

    private void checkApiJsonConsistency(final ExecutionContext executionContext, ImportApiJsonNode apiJsonNode, String urlApiId) {
        if (urlApiId != null && !urlApiId.equals(apiJsonNode.getId())) {
            throw new ApiImportException(
                String.format(
                    "Can't update API [%s] cause crossId [%s] already belongs to another API in environment [%s]",
                    urlApiId,
                    apiJsonNode.getCrossId(),
                    executionContext.getEnvironmentId()
                )
            );
        }
        checkApiJsonConsistency(apiJsonNode);
    }

    private void checkApiJsonConsistency(ImportApiJsonNode apiJsonNode) {
        checkPagesConsistency(apiJsonNode);
        checkPlansConsistency(apiJsonNode);
    }

    private void checkPlansConsistency(ImportApiJsonNode apiJsonNode) {
        if (apiJsonNode.hasPlans()) {
            List<String> planIds = apiJsonNode.getPlans().stream().map(ImportJsonNodeWithIds::getId).collect(toList());
            if (planService.anyPlanMismatchWithApi(planIds, apiJsonNode.getId())) {
                throw new ApiImportException("Some inconsistencies were found in the API plans definition");
            }
        }
    }

    private void checkPagesConsistency(ImportApiJsonNode apiJsonNode) {
        if (apiJsonNode.hasPages()) {
            long systemFoldersCount = apiJsonNode
                .getPagesArray()
                .findValuesAsText("type")
                .stream()
                .filter(type -> PageType.SYSTEM_FOLDER.name().equals(type))
                .count();

            if (systemFoldersCount > 1) {
                throw new ApiImportException("Only one system folder is allowed in the API pages definition");
            }
        }
    }

    private static String stringifyApiDefinitionFromJackson(Object apiDefinitionOrUrl) {
        return apiDefinitionOrUrl instanceof Map ? new JSONObject((Map) apiDefinitionOrUrl).toString() : apiDefinitionOrUrl.toString();
    }
}
