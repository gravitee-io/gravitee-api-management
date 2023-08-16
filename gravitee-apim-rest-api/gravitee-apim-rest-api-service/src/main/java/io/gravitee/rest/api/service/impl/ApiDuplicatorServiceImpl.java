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
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.SystemFolderType;
import io.gravitee.rest.api.model.UpdateApiMetadataEntity;
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
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.imports.ImportApiJsonNode;
import io.gravitee.rest.api.service.imports.ImportJsonNode;
import io.gravitee.rest.api.service.imports.ImportJsonNodeWithIds;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
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
        ApiIdsCalculatorService apiIdsCalculatorService
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
            pagesIdsMap.putAll(pageDuplicateService.duplicatePages(executionContext, apiId, duplicatedApi.getId()));
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
        }

        // Views & Categories
        // Before 3.0.2, API 'categories' were called 'views'. This is for compatibility.
        final List<ImportJsonNode> viewsNodes = apiJsonNode.getViews();
        if (!viewsNodes.isEmpty()) {
            Set<String> categories = viewsNodes.stream().map(ImportJsonNode::asText).collect(toSet());
            importedApi.setCategories(categories);
        }

        // merge existing plans data with plans definition data
        // cause plans definition may contain less data than plans entities (for example when rollback an API from gateway event)
        Map<String, PlanEntity> existingPlans = readApiPlansById(executionContext, apiJsonNode.getId());
        importedApi.setPlans(readPlansToImportFromDefinition(apiJsonNode, existingPlans));

        return importedApi;
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
            for (final ImportJsonNode memberNode : apiJsonNode.getMembers()) {
                MemberToImport memberToImport = objectMapper.readValue(memberNode.toString(), MemberToImport.class);
                boolean presentWithSameRole = isPresentWithSameRole(membersAlreadyPresent, memberToImport);

                List<String> roleIdsToImport = getRoleIdsToImport(executionContext, memberToImport);
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
                    currentPo.getSourceId().equals(memberToImport.getSourceId()) &&
                    currentPo.getSource().equals(memberToImport.getSource()) &&
                    !roleIdsToImport.contains(poRoleId)
                ) {
                    roleUsedInTransfert = roleIdsToImport;
                }

                if (roleIdsToImport.contains(poRoleId)) {
                    futurePo = memberToImport;
                }
            }

            // transfer the ownership
            transferOwnership(executionContext, apiEntity.getId(), currentPo, roleUsedInTransfert, futurePo);
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
                    member.getRoles().stream().map(RoleEntity::getId).collect(toList()),
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
            memberToImport.setRoles(roleIdsToImport);
        } else {
            roleIdsToImport = new ArrayList<>(roleIdsToImport);
        }

        // Before v3, only one role per member could be imported and it was a role name.
        String roleNameToAdd = memberToImport.getRole();
        if (roleNameToAdd != null && !roleNameToAdd.isEmpty()) {
            Optional<RoleEntity> optRoleToAddEntity = roleService.findByScopeAndName(
                RoleScope.API,
                roleNameToAdd,
                executionContext.getOrganizationId()
            );
            if (optRoleToAddEntity.isPresent()) {
                roleIdsToImport.add(optRoleToAddEntity.get().getId());
            } else {
                LOGGER.warn("Role {} does not exist", roleNameToAdd);
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
                    executionContext,
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

    private void transferOwnership(
        ExecutionContext executionContext,
        String apiId,
        MemberToImport currentPo,
        List<String> roleUsedInTransfert,
        MemberToImport futurePo
    ) {
        if (
            futurePo != null &&
            !(currentPo.getSource().equals(futurePo.getSource()) && currentPo.getSourceId().equals(futurePo.getSourceId()))
        ) {
            try {
                UserEntity userEntity = userService.findBySource(executionContext, futurePo.getSource(), futurePo.getSourceId(), false);
                List<RoleEntity> roleEntity = null;
                if (roleUsedInTransfert != null && !roleUsedInTransfert.isEmpty()) {
                    roleEntity = roleUsedInTransfert.stream().map(roleService::findById).collect(toList());
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
                planService.createOrUpdatePlan(executionContext, planEntity);
                apiEntity.getPlans().add(planEntity);
            });
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
            pageService.createOrUpdatePages(executionContext, pagesList, apiEntity.getId());
        }
    }

    protected static class MemberToImport {

        private String source;

        private String sourceId;
        private List<String> roles; // After v3
        private String role; // Before v3

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
