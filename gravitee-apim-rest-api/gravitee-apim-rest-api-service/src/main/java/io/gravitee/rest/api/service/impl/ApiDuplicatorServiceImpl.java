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

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import io.gravitee.rest.api.service.exceptions.PageImportException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.jackson.ser.api.ApiSerializer;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.rest.api.service.spring.ImportConfiguration;
import io.vertx.core.buffer.Buffer;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        ApiService apiService
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
    }

    @Override
    public ApiEntity createWithImportedDefinition(String apiDefinitionOrURL, String userId, String organizationId, String environmentId) {
        String apiDefinition = fetchApiDefinitionContentFromURL(apiDefinitionOrURL);
        try {
            JsonNode apiJsonNode = handleApiDefinitionIds(objectMapper.readTree(apiDefinition), environmentId);

            if (apiJsonNode.has("pages") && apiJsonNode.get("pages").isArray()) {
                ArrayNode pagesDefinition = (ArrayNode) apiJsonNode.get("pages");
                checkPagesConsistency(pagesDefinition);
            }

            if (apiJsonNode.has("plans") && apiJsonNode.get("plans").isArray()) {
                ArrayNode plansDefinition = (ArrayNode) apiJsonNode.get("plans");
                checkPlansDefinitionOwnership(plansDefinition, apiJsonNode.get("id").asText());
            }

            UpdateApiEntity importedApi = convertToEntity(apiJsonNode.toString(), apiJsonNode, environmentId);
            ApiEntity createdApiEntity = apiService.createWithApiDefinition(importedApi, userId, apiJsonNode);
            updateApiReferences(createdApiEntity, apiJsonNode, organizationId, environmentId, false);
            createPageAndMedia(createdApiEntity, apiJsonNode, environmentId);
            return createdApiEntity;
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

        final UpdateApiEntity newApiEntity = ApiService.convert(apiEntity);
        final Proxy proxy = apiEntity.getProxy();
        proxy.setVirtualHosts(singletonList(new VirtualHost(duplicateApiEntity.getContextPath())));
        newApiEntity.setProxy(proxy);
        newApiEntity.setVersion(duplicateApiEntity.getVersion() == null ? apiEntity.getVersion() : duplicateApiEntity.getVersion());

        if (duplicateApiEntity.getFilteredFields().contains("groups")) {
            newApiEntity.setGroups(null);
        } else {
            newApiEntity.setGroups(apiEntity.getGroups());
        }

        Map<String, String> plansIdsMap = new HashMap<>();
        if (!duplicateApiEntity.getFilteredFields().contains("plans")) {
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

        if (!duplicateApiEntity.getFilteredFields().contains("members")) {
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

        if (!duplicateApiEntity.getFilteredFields().contains("pages")) {
            final List<PageEntity> pages = pageService.search(new PageQuery.Builder().api(apiId).build(), true, environmentId);
            pageService.duplicatePages(pages, environmentId, duplicatedApi.getId());
        }

        if (!duplicateApiEntity.getFilteredFields().contains("plans")) {
            planService
                .findByApi(apiId)
                .forEach(
                    plan -> {
                        plan.setId(plansIdsMap.get(plan.getId()));
                        plan.setApi(duplicatedApi.getId());
                        planService.create(NewPlanEntity.from(plan));
                    }
                );
        }

        return duplicatedApi;
    }

    @Override
    public String exportAsJson(final String apiId, String exportVersion, String... filteredFields) {
        ApiEntity apiEntity = apiService.findById(apiId);
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
    public ApiEntity updateWithImportedDefinition(
        String apiId,
        String apiDefinitionOrURL,
        String userId,
        String organizationId,
        String environmentId
    ) {
        String apiDefinition = fetchApiDefinitionContentFromURL(apiDefinitionOrURL);

        try {
            // Read the whole definition
            JsonNode apiJsonNode = handleApiDefinitionIds(objectMapper.readTree(apiDefinition), environmentId);

            if (apiJsonNode.has("pages") && apiJsonNode.get("pages").isArray()) {
                ArrayNode pagesDefinition = (ArrayNode) apiJsonNode.get("pages");
                checkPagesConsistency(pagesDefinition);
            }

            if (apiJsonNode.has("plans") && apiJsonNode.get("plans").isArray()) {
                ArrayNode plansDefinition = (ArrayNode) apiJsonNode.get("plans");
                checkPlansDefinitionOwnership(plansDefinition, apiId);
            }

            UpdateApiEntity importedApi = convertToEntity(apiDefinition, apiJsonNode, environmentId);
            ApiEntity updatedApiEntity = apiService.update(apiId, importedApi, false);
            updateApiReferences(updatedApiEntity, apiJsonNode, organizationId, environmentId, true);
            return updatedApiEntity;
        } catch (IOException e) {
            LOGGER.error("An error occurs while trying to JSON deserialize the API {}", apiDefinition, e);
            throw new TechnicalManagementException("An error occurs while trying to JSON deserialize the API definition.");
        }
    }

    private UpdateApiEntity convertToEntity(String apiDefinition, JsonNode jsonNode, final String environmentId)
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

    private void createPageAndMedia(ApiEntity createdApiEntity, JsonNode jsonNode, String environmentId) throws JsonProcessingException {
        final JsonNode apiMedia = jsonNode.path("apiMedia");
        if (apiMedia != null && apiMedia.isArray()) {
            for (JsonNode media : apiMedia) {
                mediaService.createWithDefinition(createdApiEntity.getId(), media.toString());
            }
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

    private void updateApiReferences(
        ApiEntity createdOrUpdatedApiEntity,
        JsonNode jsonNode,
        String organizationId,
        String environmentId,
        // FIXME: This whole method should be split in 2 (creation and update) and this flag should be removed
        boolean isUpdate
    ) throws IOException {
        // Members
        final JsonNode membersToImport = jsonNode.path("members");
        if (membersToImport != null && membersToImport.isArray()) {
            // get current members of the api
            Set<MemberToImport> membersAlreadyPresent = membershipService
                .getMembersByReference(MembershipReferenceType.API, createdOrUpdatedApiEntity.getId())
                .stream()
                .map(
                    member -> {
                        UserEntity userEntity = userService.findById(member.getId());
                        return new MemberToImport(
                            userEntity.getSource(),
                            userEntity.getSourceId(),
                            member.getRoles().stream().map(RoleEntity::getId).collect(Collectors.toList()),
                            null
                        );
                    }
                )
                .collect(toSet());
            // get the current PO
            RoleEntity poRole = roleService.findPrimaryOwnerRoleByOrganization(organizationId, RoleScope.API);
            if (poRole != null) {
                String poRoleId = poRole.getId();
                MemberToImport currentPo = membersAlreadyPresent
                    .stream()
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
                    boolean presentWithSameRole =
                        memberToImport.getRoles() != null &&
                        !memberToImport.getRoles().isEmpty() &&
                        membersAlreadyPresent
                            .stream()
                            .anyMatch(
                                m -> {
                                    m.getRoles().sort(Comparator.naturalOrder());
                                    return (
                                        m.getRoles().equals(memberToImport.getRoles()) &&
                                        (
                                            m.getSourceId().equals(memberToImport.getSourceId()) &&
                                            m.getSource().equals(memberToImport.getSource())
                                        )
                                    );
                                }
                            );

                    // add/update members if :
                    //  - not already present with the same role
                    //  - not the new PO
                    //  - not the current PO
                    if (
                        !presentWithSameRole &&
                        (
                            memberToImport.getRoles() != null &&
                            !memberToImport.getRoles().isEmpty() &&
                            !memberToImport.getRoles().contains(poRoleId)
                        ) &&
                        !(
                            memberToImport.getSourceId().equals(currentPo.getSourceId()) &&
                            memberToImport.getSource().equals(currentPo.getSource())
                        )
                    ) {
                        try {
                            UserEntity userEntity = userService.findBySource(
                                memberToImport.getSource(),
                                memberToImport.getSourceId(),
                                false
                            );

                            rolesToImport.forEach(
                                role -> {
                                    try {
                                        membershipService.addRoleToMemberOnReference(
                                            organizationId,
                                            environmentId,
                                            MembershipReferenceType.API,
                                            createdOrUpdatedApiEntity.getId(),
                                            MembershipMemberType.USER,
                                            userEntity.getId(),
                                            role
                                        );
                                    } catch (Exception e) {
                                        LOGGER.warn(
                                            "Unable to add role '{}' to member '{}' on API '{}' due to : {}",
                                            role,
                                            userEntity.getId(),
                                            createdOrUpdatedApiEntity.getId(),
                                            e.getMessage()
                                        );
                                    }
                                }
                            );
                        } catch (UserNotFoundException unfe) {}
                    }

                    // get the future role of the current PO
                    if (
                        currentPo.getSourceId().equals(memberToImport.getSourceId()) &&
                        currentPo.getSource().equals(memberToImport.getSource()) &&
                        !rolesToImport.contains(poRoleId)
                    ) {
                        roleUsedInTransfert = rolesToImport;
                    }

                    if (rolesToImport.contains(poRoleId)) {
                        futurePO = memberToImport;
                    }
                }

                // transfer the ownership
                if (
                    futurePO != null &&
                    !(currentPo.getSource().equals(futurePO.getSource()) && currentPo.getSourceId().equals(futurePO.getSourceId()))
                ) {
                    try {
                        UserEntity userEntity = userService.findBySource(futurePO.getSource(), futurePO.getSourceId(), false);
                        List<RoleEntity> roleEntity = null;
                        if (roleUsedInTransfert != null && !roleUsedInTransfert.isEmpty()) {
                            roleEntity = roleUsedInTransfert.stream().map(roleService::findById).collect(Collectors.toList());
                        }
                        membershipService.transferApiOwnership(
                            organizationId,
                            environmentId,
                            createdOrUpdatedApiEntity.getId(),
                            new MembershipService.MembershipMember(userEntity.getId(), null, MembershipMemberType.USER),
                            roleEntity
                        );
                    } catch (UserNotFoundException unfe) {}
                }
            }
        }

        //Pages
        final JsonNode pagesDefinition = jsonNode.path("pages");
        if (pagesDefinition != null && pagesDefinition.isArray()) {
            List<PageEntity> pagesList = objectMapper.readValue(
                pagesDefinition.toString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, PageEntity.class)
            );
            pageService.createOrUpdatePages(pagesList, environmentId, createdOrUpdatedApiEntity.getId());
        }

        //Plans
        final JsonNode plansDefinition = jsonNode.path("plans");
        if (plansDefinition != null && plansDefinition.isArray()) {
            Map<String, PlanEntity> existingPlans = isUpdate
                ? planService.findByApi(createdOrUpdatedApiEntity.getId()).stream().collect(toMap(PlanEntity::getId, plan -> plan))
                : Collections.emptyMap();

            List<PlanEntity> plansToImport = readPlansToImportFromDefinition(plansDefinition, existingPlans);

            findRemovedPlansIds(existingPlans.values(), plansToImport).forEach(planService::delete);

            plansToImport.forEach(
                planEntity -> {
                    planEntity.setApi(createdOrUpdatedApiEntity.getId());
                    planService.createOrUpdatePlan(planEntity, environmentId);
                }
            );
        }
        // Metadata
        final JsonNode metadataDefinition = jsonNode.path("metadata");
        if (metadataDefinition != null && metadataDefinition.isArray()) {
            try {
                for (JsonNode metadataNode : metadataDefinition) {
                    UpdateApiMetadataEntity updateApiMetadataEntity = objectMapper.readValue(
                        metadataNode.toString(),
                        UpdateApiMetadataEntity.class
                    );
                    updateApiMetadataEntity.setApiId(createdOrUpdatedApiEntity.getId());
                    apiMetadataService.update(updateApiMetadataEntity);
                }
            } catch (Exception ex) {
                LOGGER.error("An error occurs while creating API metadata", ex);
                throw new TechnicalManagementException("An error occurs while creating API Metadata", ex);
            }
        }
    }

    private static class MemberToImport {

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
        return existingPlans.stream().filter(existingPlan -> !importedPlans.contains(existingPlan)).map(plan -> plan.getId());
    }

    private List<PlanEntity> readPlansToImportFromDefinition(JsonNode plansDefinition, Map<String, PlanEntity> existingPlans)
        throws IOException {
        List<PlanEntity> plansToImport = new ArrayList<>();
        for (Iterator<JsonNode> it = plansDefinition.elements(); it.hasNext();) {
            JsonNode planDefinition = it.next();
            PlanEntity existingPlan = planDefinition.has("id") ? existingPlans.get(planDefinition.get("id").asText()) : null;
            if (existingPlan != null) {
                plansToImport.add(objectMapper.readerForUpdating(existingPlan).readValue(planDefinition));
            } else {
                plansToImport.add(objectMapper.readValue(planDefinition.toString(), PlanEntity.class));
            }
        }
        return plansToImport;
    }

    private void checkPlansDefinitionOwnership(ArrayNode plansDefinition, String apiId) {
        List<String> planIds = plansDefinition.findValuesAsText("id");
        if (planService.anyPlanMismatchWithApi(planIds, apiId)) {
            throw new TechnicalManagementException("Some inconsistencies were found in the API plans definition");
        }
    }

    private void checkPagesConsistency(ArrayNode pagesDefinition) {
        long systemFoldersCount = pagesDefinition
            .findValuesAsText("type")
            .stream()
            .filter(type -> PageType.SYSTEM_FOLDER.name().equals(type))
            .count();

        if (systemFoldersCount > 1) {
            throw new PageImportException("Only one system folder is allowed in the API pages definition");
        }
    }

    private void recalculatePromotedIds(String targetEnvId, JsonNode apiJsonNode) {
        String apiId = apiJsonNode.hasNonNull("id") ? apiJsonNode.get("id").asText() : null;
        String generatedApiId = UuidString.generateForEnvironment(targetEnvId, apiId);

        ((ObjectNode) apiJsonNode).put("id", generatedApiId);

        getPlansNodes(apiJsonNode)
            .stream()
            .filter(plan -> plan.hasNonNull("id"))
            .forEach(
                plan -> {
                    ((ObjectNode) plan).put("id", UuidString.generateForEnvironment(targetEnvId, apiId, plan.get("id").asText()));
                }
            );

        List<JsonNode> pagesNodes = getPagesNodes(apiJsonNode);

        pagesNodes
            .stream()
            .filter(page -> page.hasNonNull("id"))
            .forEach(
                page -> {
                    String pageId = page.get("id").asText();
                    String generatedPageId = UuidString.generateForEnvironment(targetEnvId, apiId, page.get("id").asText());
                    ((ObjectNode) page).put("id", generatedPageId);

                    pagesNodes.forEach(
                        childNode -> {
                            if (childNode.hasNonNull("parentId") && childNode.get("parentId").asText().equals(pageId)) {
                                ((ObjectNode) childNode).put("parentId", generatedPageId);
                            }
                        }
                    );
                }
            );
    }

    public JsonNode handleApiDefinitionIds(JsonNode apiJsonNode, String environmentId) {
        if (!apiJsonNode.hasNonNull("id")) {
            ((ObjectNode) apiJsonNode).put("id", UuidString.generateRandom());
        }

        boolean isPromote = apiJsonNode.hasNonNull("environment_id") && !apiJsonNode.get("environment_id").asText().equals(environmentId);
        if (isPromote) {
            recalculatePromotedIds(environmentId, apiJsonNode);
        }
        return generateEmptyIds(apiJsonNode);
    }

    private JsonNode generateEmptyIds(JsonNode apiJsonNode) {
        Stream
            .concat(getPlansNodes(apiJsonNode).stream(), getPagesNodes(apiJsonNode).stream())
            .filter(node -> !node.hasNonNull("id"))
            .forEach(node -> ((ObjectNode) node).put("id", UuidString.generateRandom()));
        return apiJsonNode;
    }

    private List<JsonNode> getPlansNodes(JsonNode apiJsonNode) {
        List<JsonNode> plansNodes = new ArrayList<>();
        if (apiJsonNode.has("plans") && apiJsonNode.get("plans").isArray()) {
            apiJsonNode.get("plans").forEach(plansNodes::add);
        }
        return plansNodes;
    }

    private List<JsonNode> getPagesNodes(JsonNode apiJsonNode) {
        List<JsonNode> pagesNodes = new ArrayList<>();
        if (apiJsonNode.has("pages") && apiJsonNode.get("pages").isArray()) {
            apiJsonNode.get("pages").forEach(pagesNodes::add);
        }
        return pagesNodes;
    }
}
