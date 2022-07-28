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

import static io.gravitee.rest.api.model.permissions.RolePermission.API_DEFINITION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.DuplicateApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PlanConverter;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final RoleService roleService;
    private final PageService pageService;
    private final PlanService planService;
    private final GroupService groupService;
    private final UserService userService;
    private final ApiService apiService;
    private final ApiConverter apiConverter;
    private final PlanConverter planConverter;
    private final PermissionService permissionService;

    public ApiDuplicatorServiceImpl(
        HttpClientService httpClientService,
        ImportConfiguration importConfiguration,
        MediaService mediaService,
        ObjectMapper objectMapper,
        ApiMetadataService apiMetadataService,
        MembershipService membershipService,
        RoleService roleService,
        PageService pageService,
        PlanService planService,
        GroupService groupService,
        UserService userService,
        ApiService apiService,
        ApiConverter apiConverter,
        PlanConverter planConverter,
        PermissionService permissionService
    ) {
        this.httpClientService = httpClientService;
        this.importConfiguration = importConfiguration;
        this.mediaService = mediaService;
        this.objectMapper = objectMapper;
        this.apiMetadataService = apiMetadataService;
        this.membershipService = membershipService;
        this.roleService = roleService;
        this.pageService = pageService;
        this.planService = planService;
        this.groupService = groupService;
        this.userService = userService;
        this.apiService = apiService;
        this.apiConverter = apiConverter;
        this.planConverter = planConverter;
        this.permissionService = permissionService;
    }

    @Override
    public ApiEntity createWithImportedDefinition(String apiDefinitionOrURL, String organizationId, String environmentId) {
        String apiDefinition = fetchApiDefinitionContentFromURL(apiDefinitionOrURL);
        try {
            // Read the whole input API definition, and recalculate its ID
            ImportApiJsonNode apiJsonNode = recalculateApiDefinitionIds(
                new ImportApiJsonNode(objectMapper.readTree(apiDefinition)),
                environmentId
            );

            // check API consistency before import
            checkApiJsonConsistency(apiJsonNode);

            // import
            UpdateApiEntity importedApi = convertToEntity(apiJsonNode.toString(), apiJsonNode, environmentId);
            ApiEntity createdApiEntity = apiService.createWithApiDefinition(
                importedApi,
                getAuthenticatedUsername(),
                apiJsonNode.getJsonNode()
            );
            createOrUpdateApiNestedEntities(createdApiEntity.getId(), apiJsonNode, organizationId, environmentId);
            createPageAndMedia(createdApiEntity, apiJsonNode, environmentId);
            return createdApiEntity;
        } catch (IOException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", apiDefinition, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the API definition.");
        }
    }

    @Override
    public ApiEntity updateWithImportedDefinition(String urlApiId, String apiDefinitionOrURL, String organizationId, String environmentId) {
        String apiDefinition = fetchApiDefinitionContentFromURL(apiDefinitionOrURL);

        try {
            // Read the whole input API definition, and recalculate its ID
            ImportApiJsonNode apiJsonNode = recalculateApiDefinitionIds(
                new ImportApiJsonNode(objectMapper.readTree(apiDefinition)),
                environmentId,
                urlApiId
            );

            // ensure user has required permission to update target API
            if (!isAuthenticated() || !(isAdmin() || permissionService.hasPermission(API_DEFINITION, apiJsonNode.getId(), UPDATE))) {
                throw new ForbiddenAccessException();
            }

            // check API consistency before import
            checkApiJsonConsistency(apiJsonNode, urlApiId, environmentId);

            // import
            UpdateApiEntity importedApi = convertToEntity(apiJsonNode.toString(), apiJsonNode, environmentId);
            ApiEntity updatedApiEntity = apiService.update(apiJsonNode.getId(), importedApi);
            createOrUpdateApiNestedEntities(updatedApiEntity.getId(), apiJsonNode, organizationId, environmentId);
            return updatedApiEntity;
        } catch (IOException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", apiDefinition, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the API definition.");
        }
    }

    @Override
    public ApiEntity duplicate(
        final ApiEntity apiEntity,
        final DuplicateApiEntity duplicateApiEntity,
        String organizationId,
        String environmentId
    ) {
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
                .forEach(
                    plan -> {
                        String newPlanId = UuidString.generateRandom();
                        plansIdsMap.put(plan.getId(), newPlanId);
                        plan.setId(newPlanId);
                    }
                );
        }

        final ApiEntity duplicatedApi = apiService.createWithApiDefinition(newApiEntity, getAuthenticatedUsername(), null);

        if (!duplicateApiEntity.getFilteredFields().contains(API_DEFINITION_FIELD_MEMBERS)) {
            final Set<MembershipEntity> membershipsToDuplicate = membershipService.getMembershipsByReference(
                io.gravitee.rest.api.model.MembershipReferenceType.API,
                apiId
            );
            RoleEntity primaryOwnerRole = roleService.findPrimaryOwnerRoleByOrganization(organizationId, RoleScope.API);
            if (primaryOwnerRole != null) {
                String primaryOwnerRoleId = primaryOwnerRole.getId();
                membershipsToDuplicate.forEach(
                    membership -> {
                        String roleId = membership.getRoleId();
                        if (!primaryOwnerRoleId.equals(roleId)) {
                            membershipService.addRoleToMemberOnReference(
                                organizationId,
                                environmentId,
                                io.gravitee.rest.api.model.MembershipReferenceType.API,
                                duplicatedApi.getId(),
                                membership.getMemberType(),
                                membership.getMemberId(),
                                roleId
                            );
                        }
                    }
                );
            }
        }

        if (!duplicateApiEntity.getFilteredFields().contains(API_DEFINITION_FIELD_PAGES)) {
            final List<PageEntity> pages = pageService.search(new PageQuery.Builder().api(apiId).build(), true, environmentId);
            pageService.duplicatePages(pages, environmentId, duplicatedApi.getId());
        }

        if (!duplicateApiEntity.getFilteredFields().contains(API_DEFINITION_FIELD_PLANS)) {
            planService
                .findByApi(apiId)
                .forEach(
                    plan -> {
                        plan.setId(plansIdsMap.get(plan.getId()));
                        plan.setApi(duplicatedApi.getId());
                        planService.create(planConverter.toNewPlanEntity(plan, true));
                    }
                );
        }

        return duplicatedApi;
    }

    private UpdateApiEntity convertToEntity(String apiDefinition, ImportApiJsonNode apiJsonNode, final String environmentId)
        throws JsonProcessingException {
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
                List<GroupEntity> groupEntities = groupService.findByName(environmentId, name);
                GroupEntity group;
                if (groupEntities.isEmpty()) {
                    NewGroupEntity newGroupEntity = new NewGroupEntity();
                    newGroupEntity.setName(name);
                    group = groupService.create(environmentId, newGroupEntity);
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

        return importedApi;
    }

    private void createPageAndMedia(ApiEntity createdApiEntity, ImportApiJsonNode apiJsonNode, String environmentId)
        throws JsonProcessingException {
        for (ImportJsonNode media : apiJsonNode.getMedia()) {
            mediaService.createWithDefinition(createdApiEntity.getId(), media.toString());
        }

        List<PageEntity> search = pageService.search(
            new PageQuery.Builder()
                .api(createdApiEntity.getId())
                .name(SystemFolderType.ASIDE.folderName())
                .type(PageType.SYSTEM_FOLDER)
                .build(),
            environmentId
        );

        if (search.isEmpty()) {
            pageService.createAsideFolder(createdApiEntity.getId(), environmentId);
        }
    }

    private String fetchApiDefinitionContentFromURL(String apiDefinitionOrURL) {
        if (apiDefinitionOrURL.toUpperCase().startsWith("HTTP")) {
            UrlSanitizerUtils.checkAllowed(
                apiDefinitionOrURL,
                importConfiguration.getImportWhitelist(),
                importConfiguration.isAllowImportFromPrivate()
            );
            Buffer buffer = httpClientService.request(HttpMethod.GET, apiDefinitionOrURL, null, null, null);
            return buffer.toString();
        }
        return apiDefinitionOrURL;
    }

    private void createOrUpdateApiNestedEntities(String apiId, ImportApiJsonNode apiJsonNode, String organizationId, String environmentId)
        throws IOException {
        createOrUpdateMembers(apiId, apiJsonNode, organizationId, environmentId);
        createOrUpdatePages(apiId, apiJsonNode, environmentId);
        createOrUpdatePlans(apiId, apiJsonNode, environmentId);
        createOrUpdateMetadata(apiId, apiJsonNode);
    }

    private void createOrUpdateMembers(String apiId, ImportApiJsonNode apiJsonNode, String organizationId, String environmentId)
        throws JsonProcessingException {
        if (apiJsonNode.hasMembers()) {
            // get current members of the api
            Set<MemberToImport> membersAlreadyPresent = getAPICurrentMembers(apiId);
            // get the current PO
            RoleEntity poRole = roleService.findPrimaryOwnerRoleByOrganization(organizationId, RoleScope.API);
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

                List<String> roleIdsToImport = getRoleIdsToImport(memberToImport);
                addOrUpdateMembers(
                    apiId,
                    organizationId,
                    environmentId,
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
            transferOwnership(apiId, organizationId, environmentId, currentPo, roleUsedInTransfert, futurePo);
        }
    }

    @NotNull
    protected Set<MemberToImport> getAPICurrentMembers(String apiId) {
        return membershipService
            .getMembersByReference(MembershipReferenceType.API, apiId)
            .stream()
            .filter(member -> member.getType() == MembershipMemberType.USER)
            .map(
                member -> {
                    UserEntity userEntity = userService.findById(member.getId());
                    return new MemberToImport(
                        userEntity.getSource(),
                        userEntity.getSourceId(),
                        member.getRoles().stream().map(RoleEntity::getId).collect(toList()),
                        null
                    );
                }
            )
            .collect(toSet());
    }

    protected boolean isPresentWithSameRole(Set<MemberToImport> membersAlreadyPresent, MemberToImport memberToImport) {
        return (
            memberToImport.getRoles() != null &&
            !memberToImport.getRoles().isEmpty() &&
            membersAlreadyPresent
                .stream()
                .anyMatch(
                    m -> {
                        m.getRoles().sort(Comparator.naturalOrder());
                        return (
                            m.getRoles().equals(memberToImport.getRoles()) &&
                            (m.getSourceId().equals(memberToImport.getSourceId()) && m.getSource().equals(memberToImport.getSource()))
                        );
                    }
                )
        );
    }

    protected List<String> getRoleIdsToImport(MemberToImport memberToImport) {
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
            Optional<RoleEntity> optRoleToAddEntity = roleService.findByScopeAndName(RoleScope.API, roleNameToAdd);
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
        String apiId,
        String organizationId,
        String environmentId,
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
                UserEntity userEntity = userService.findBySource(memberToImport.getSource(), memberToImport.getSourceId(), false);

                rolesToImport.forEach(
                    role -> {
                        try {
                            membershipService.addRoleToMemberOnReference(
                                organizationId,
                                environmentId,
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
                    }
                );
            } catch (UserNotFoundException unfe) {}
        }
    }

    private void transferOwnership(
        String apiId,
        String organizationId,
        String environmentId,
        MemberToImport currentPo,
        List<String> roleUsedInTransfert,
        MemberToImport futurePo
    ) {
        if (
            futurePo != null &&
            !(currentPo.getSource().equals(futurePo.getSource()) && currentPo.getSourceId().equals(futurePo.getSourceId()))
        ) {
            try {
                UserEntity userEntity = userService.findBySource(futurePo.getSource(), futurePo.getSourceId(), false);
                List<RoleEntity> roleEntity = null;
                if (roleUsedInTransfert != null && !roleUsedInTransfert.isEmpty()) {
                    roleEntity = roleUsedInTransfert.stream().map(roleService::findById).collect(toList());
                }
                membershipService.transferApiOwnership(
                    organizationId,
                    environmentId,
                    apiId,
                    new MembershipService.MembershipMember(userEntity.getId(), null, MembershipMemberType.USER),
                    roleEntity
                );
            } catch (UserNotFoundException unfe) {}
        }
    }

    protected void createOrUpdateMetadata(String apiId, ImportApiJsonNode apiJsonNode) {
        try {
            for (ImportJsonNode metadataNode : apiJsonNode.getMetadata()) {
                UpdateApiMetadataEntity updateApiMetadataEntity = objectMapper.readValue(
                    metadataNode.toString(),
                    UpdateApiMetadataEntity.class
                );
                updateApiMetadataEntity.setApiId(apiId);
                apiMetadataService.update(updateApiMetadataEntity);
            }
        } catch (Exception ex) {
            LOGGER.error("An error occurs while creating API metadata", ex);
            throw new TechnicalManagementException("An error occurs while creating API Metadata", ex);
        }
    }

    protected void createOrUpdatePlans(String apiId, ImportApiJsonNode apiJsonNode, String environmentId) throws IOException {
        if (apiJsonNode.hasPlans()) {
            Map<String, PlanEntity> existingPlans = planService
                .findByApi(apiId)
                .stream()
                .collect(toMap(PlanEntity::getId, Function.identity()));

            List<PlanEntity> plansToImport = readPlansToImportFromDefinition(apiJsonNode, existingPlans);

            findRemovedPlansIds(existingPlans.values(), plansToImport).forEach(planService::delete);

            plansToImport.forEach(
                planEntity -> {
                    planEntity.setApi(apiId);
                    planService.createOrUpdatePlan(planEntity, environmentId);
                }
            );
        }
    }

    protected void createOrUpdatePages(String apiId, ImportApiJsonNode apiJsonNode, String environmentId) throws JsonProcessingException {
        if (apiJsonNode.hasPages() && !apiJsonNode.getPages().isEmpty()) {
            List<PageEntity> pagesList = objectMapper.readValue(
                apiJsonNode.getPages().toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, PageEntity.class)
            );
            pageService.createOrUpdatePages(pagesList, environmentId, apiId);
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

    private Stream<String> findRemovedPlansIds(Collection<PlanEntity> existingPlans, Collection<PlanEntity> importedPlans) {
        return existingPlans.stream().filter(existingPlan -> !importedPlans.contains(existingPlan)).map(PlanEntity::getId);
    }

    private List<PlanEntity> readPlansToImportFromDefinition(ImportApiJsonNode apiJsonNode, Map<String, PlanEntity> existingPlans)
        throws IOException {
        List<PlanEntity> plansToImport = new ArrayList<>();
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

    private void checkApiJsonConsistency(ImportApiJsonNode apiJsonNode, String urlApiId, String environmentId) {
        if (urlApiId != null && !urlApiId.equals(apiJsonNode.getId())) {
            throw new ApiImportException(
                String.format(
                    "Can't update API [%s] cause crossId [%s] already belongs to another API in environment [%s]",
                    urlApiId,
                    apiJsonNode.getCrossId(),
                    environmentId
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

    protected ImportApiJsonNode recalculateApiDefinitionIds(ImportApiJsonNode apiJsonNode, String environmentId) {
        return recalculateApiDefinitionIds(apiJsonNode, environmentId, null);
    }

    /*
     * If the API definition is imported from another environment we need to match the targeted
     * entities (API, plans, pages) from the input IDs.
     *
     * If the API definition holds a cross ID, the entities will be matched using this unique ID.
     *
     * If the API definition does not hold a cross ID, the matching entity IDs will be matched using
     * a predictable ID generation based on the target environment ID, the source API ID and the source entity ID
     */
    protected ImportApiJsonNode recalculateApiDefinitionIds(ImportApiJsonNode apiJsonNode, String environmentId, String urlApiId) {
        /*
         * In case of an update, if the API definition ID is the same as the resource ID targeted by the update,
         * we don't apply any kind of ID transformation so that we don't break previous exports that don't hold
         * a cross ID
         */
        if (!apiJsonNode.hasId() || !apiJsonNode.getId().equals(urlApiId)) {
            findApiByEnvironmentAndCrossId(environmentId, apiJsonNode.getCrossId())
                .ifPresentOrElse(
                    api -> recalculateIdsFromCrossId(apiJsonNode, api),
                    () -> recalculateIdsFromDefinitionIds(environmentId, apiJsonNode, urlApiId)
                );
        }
        return generateEmptyIds(apiJsonNode);
    }

    private Optional<ApiEntity> findApiByEnvironmentAndCrossId(String environmentId, String crossId) {
        return crossId == null ? Optional.empty() : apiService.findByEnvironmentIdAndCrossId(environmentId, crossId);
    }

    private void recalculateIdsFromCrossId(ImportApiJsonNode apiJsonNode, ApiEntity api) {
        apiJsonNode.setId(api.getId());
        recalculatePlanIdsFromCrossIds(api, apiJsonNode.getPlans());
        recalculatePageIdsFromCrossIds(api, apiJsonNode.getPages());
    }

    private void recalculatePlanIdsFromCrossIds(ApiEntity api, List<ImportJsonNodeWithIds> plansNodes) {
        Map<String, PlanEntity> plansByCrossId = planService
            .findByApi(api.getId())
            .stream()
            .filter(plan -> plan.getCrossId() != null)
            .collect(toMap(PlanEntity::getCrossId, Function.identity()));

        plansNodes
            .stream()
            .filter(ImportJsonNodeWithIds::hasCrossId)
            .forEach(
                plan -> {
                    PlanEntity matchingPlan = plansByCrossId.get(plan.getCrossId());
                    plan.setApi(api.getId());
                    plan.setId(matchingPlan != null ? matchingPlan.getId() : UuidString.generateRandom());
                }
            );
    }

    private void recalculatePageIdsFromCrossIds(ApiEntity api, List<ImportJsonNodeWithIds> pagesNodes) {
        Map<String, PageEntity> pagesByCrossId = pageService
            .findByApi(api.getId())
            .stream()
            .filter(page -> page.getCrossId() != null)
            .collect(toMap(PageEntity::getCrossId, Function.identity()));

        pagesNodes
            .stream()
            .filter(ImportJsonNodeWithIds::hasCrossId)
            .forEach(
                page -> {
                    String pageId = page.hasId() ? page.getId() : null;
                    PageEntity matchingPage = pagesByCrossId.get(page.getCrossId());
                    page.setApi(api.getId());
                    if (matchingPage != null) {
                        page.setId(matchingPage.getId());
                        updatePagesHierarchy(pagesNodes, pageId, matchingPage.getId());
                    } else {
                        String newPageId = UuidString.generateRandom();
                        page.setId(newPageId);
                        updatePagesHierarchy(pagesNodes, pageId, newPageId);
                    }
                }
            );
    }

    private void recalculateIdsFromDefinitionIds(String environmentId, ImportApiJsonNode apiJsonNode, String urlApiId) {
        String targetApiId = urlApiId == null ? UuidString.generateForEnvironment(environmentId, apiJsonNode.getId()) : urlApiId;
        apiJsonNode.setId(targetApiId);
        recalculatePlanIdsFromDefinitionIds(apiJsonNode.getPlans(), environmentId, targetApiId);
        recalculatePageIdsFromDefinitionIds(apiJsonNode.getPages(), environmentId, targetApiId);
    }

    private void recalculatePlanIdsFromDefinitionIds(List<ImportJsonNodeWithIds> plansNodes, String environmentId, String apiId) {
        plansNodes
            .stream()
            .filter(ImportJsonNodeWithIds::hasId)
            .forEach(
                plan -> {
                    plan.setId(UuidString.generateForEnvironment(environmentId, apiId, plan.getId()));
                    plan.setApi(apiId);
                }
            );
    }

    private void recalculatePageIdsFromDefinitionIds(List<ImportJsonNodeWithIds> pagesNodes, String environmentId, String apiId) {
        pagesNodes
            .stream()
            .filter(ImportJsonNodeWithIds::hasId)
            .forEach(
                page -> {
                    String pageId = page.getId();
                    String generatedPageId = UuidString.generateForEnvironment(environmentId, apiId, pageId);
                    page.setId(generatedPageId);
                    page.setApi(apiId);
                    updatePagesHierarchy(pagesNodes, pageId, generatedPageId);
                }
            );
    }

    private void updatePagesHierarchy(List<ImportJsonNodeWithIds> pagesNodes, String parentId, String newParentId) {
        pagesNodes.stream().filter(child -> isChildPageOf(child, parentId)).forEach(child -> child.setParentId(newParentId));
    }

    private boolean isChildPageOf(ImportJsonNodeWithIds pageNode, String parentPageId) {
        return pageNode.hasParentId() && pageNode.getParentId().equals(parentPageId);
    }

    private ImportApiJsonNode generateEmptyIds(ImportApiJsonNode apiJsonNode) {
        Stream
            .concat(apiJsonNode.getPlans().stream(), apiJsonNode.getPages().stream())
            .filter(node -> !node.hasId())
            .forEach(node -> node.setId(UuidString.generateRandom()));
        return apiJsonNode;
    }
}
