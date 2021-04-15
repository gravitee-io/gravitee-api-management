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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.InvitationReferenceType;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Audit.AuditProperties.GROUP;
import static io.gravitee.repository.management.model.Group.AuditEvent.*;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GroupServiceImpl extends AbstractService implements GroupService {
    private final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private ApiRepository apiRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private MembershipService membershipService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private InvitationService invitationService;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Override
    public List<GroupEntity> findAll() {
        try {
            logger.debug("Find all groups");
            Set<Group> all = groupRepository.findAllByEnvironment(GraviteeContext.getCurrentEnvironment());
            logger.debug("Find all groups - DONE");
            final List<GroupEntity> groups = all.stream()
                .map(this::map)
                .sorted(Comparator.comparing(GroupEntity::getName))
                .collect(Collectors.toList());

            populateGroupFlags(groups);

            if (permissionService.hasPermission(RolePermission.ENVIRONMENT_GROUP, GraviteeContext.getCurrentEnvironment(), CREATE, UPDATE, DELETE)) {
                groups.forEach(groupEntity -> groupEntity.setManageable(true));
            } else {
                Optional<RoleEntity> optGroupAdminSystemRole = roleService.findByScopeAndName(RoleScope.GROUP, SystemRole.ADMIN.name());
                if (optGroupAdminSystemRole.isPresent()) {
                    List<String> groupIds = membershipService.getMembershipsByMemberAndReferenceAndRole(
                        MembershipMemberType.USER,
                        getAuthenticatedUsername(),
                        MembershipReferenceType.GROUP,
                        optGroupAdminSystemRole.get().getId())
                        .stream()
                        .map(MembershipEntity::getReferenceId)
                        .collect(Collectors.toList());
                    groups.forEach(groupEntity -> groupEntity.setManageable(groupIds.contains(groupEntity.getId())));
                }
            }
            return groups;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find all groups", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all groups", ex);
        }
    }

    private void populateGroupFlags(final List<GroupEntity> groups) {
        RoleEntity apiPORole = roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            .orElseThrow(() -> new TechnicalManagementException("API System Role 'PRIMARY_OWNER' not found."));

        groups.forEach(group -> {
            final boolean isApiPO = !membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.GROUP, group.getId(), MembershipReferenceType.API, apiPORole.getId())
                .isEmpty();

            group.setPrimaryOwner(isApiPO);
        });
    }

    @Override
    public List<GroupEntity> findByName(String name) {
        try {
            logger.debug("findByUsername : {}", name);
            if (name == null) {
                return Collections.emptyList();
            }
            List<GroupEntity> groupEntities = groupRepository.findAllByEnvironment(GraviteeContext.getCurrentEnvironment()).stream()
                .filter(group -> group.getName().equals(name))
                .map(this::map)
                .sorted(Comparator.comparing(GroupEntity::getName))
                .collect(Collectors.toList());
            logger.debug("findByUsername : {} - DONE", name);
            return groupEntities;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups by name", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups by name", ex);
        }
    }

    @Override
    public GroupEntity create(NewGroupEntity group) {
        try {
            logger.debug("create {}", group);
            if (!this.findByName(group.getName()).isEmpty()) {
                throw new GroupNameAlreadyExistsException(group.getName());
            }
            Group newGroup = this.map(group);
            newGroup.setId(RandomString.generate());
            newGroup.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
            newGroup.setCreatedAt(new Date());
            newGroup.setUpdatedAt(newGroup.getCreatedAt());
            GroupEntity grp = this.map(groupRepository.create(newGroup));
            // Audit
            auditService.createEnvironmentAuditLog(
                Collections.singletonMap(GROUP, newGroup.getId()),
                GROUP_CREATED,
                newGroup.getCreatedAt(),
                null,
                newGroup);
            logger.debug("create {} - DONE", grp);
            return grp;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to create a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to create a group", ex);
        }
    }

    @Override
    public GroupEntity update(String groupId, UpdateGroupEntity group) {
        try {
            logger.debug("update {}", group);
            GroupEntity updatedGroupEntity = this.findById(groupId);
            Group previousGroup = this.map(updatedGroupEntity);
            updatedGroupEntity.setName(group.getName());
            updatedGroupEntity.setUpdatedAt(new Date());
            updatedGroupEntity.setEventRules(group.getEventRules());
            updatedGroupEntity.setMaxInvitation(group.getMaxInvitation());
            updatedGroupEntity.setLockApiRole(group.isLockApiRole());
            updatedGroupEntity.setLockApplicationRole(group.isLockApplicationRole());
            updatedGroupEntity.setSystemInvitation(group.isSystemInvitation());
            updatedGroupEntity.setEmailInvitation(group.isEmailInvitation());
            updatedGroupEntity.setDisableMembershipNotifications(group.isDisableMembershipNotifications());

            Group updatedGroup = this.map(updatedGroupEntity);
            GroupEntity grp = this.map(groupRepository.update(updatedGroup));
            logger.debug("update {} - DONE", grp);

            updateDefaultRoles(groupId, updatedGroupEntity.getRoles(), group.getRoles());

            // Audit
            auditService.createEnvironmentAuditLog(
                Collections.singletonMap(GROUP, groupId),
                GROUP_UPDATED,
                updatedGroupEntity.getUpdatedAt(),
                previousGroup,
                updatedGroup);
            return findById(groupId);
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to update a group";
            logger.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private void updateDefaultRoles(String groupId, Map<RoleScope, String> formerRoles, Map<RoleScope, String> newRoles) throws TechnicalException {
        RoleScope[] groupRoleScopes = {RoleScope.API, RoleScope.APPLICATION};
        for (RoleScope roleScope : groupRoleScopes) {
            if (
                formerRoles != null &&
                    formerRoles.get(roleScope) != null &&
                    (newRoles == null ||
                        (newRoles != null &&
                            !formerRoles.get(roleScope).equals(newRoles.get(roleScope)) &&
                            !SystemRole.PRIMARY_OWNER.name().equals(newRoles.get(roleScope))))
            ) {
                removeOldDefaultRole(groupId, MembershipReferenceType.valueOf(roleScope.name()));
            }

            if (
                newRoles != null &&
                    newRoles.get(roleScope) != null &&
                    !SystemRole.PRIMARY_OWNER.name().equals(newRoles.get(roleScope)) &&
                    (formerRoles == null ||
                        (formerRoles != null &&
                            !newRoles.get(roleScope).equals(formerRoles.get(roleScope))))
            ) {
                addNewDefaultRole(groupId, newRoles.get(roleScope), roleScope);
            }
        }
    }

    private void removeOldDefaultRole(String groupId, MembershipReferenceType referenceType) {
        membershipService.deleteReferenceMember(referenceType, null, MembershipMemberType.GROUP, groupId);
    }

    private void addNewDefaultRole(String groupId, String newRole, RoleScope roleScope) {
        membershipService.addRoleToMemberOnReference(
            new MembershipService.MembershipReference(MembershipReferenceType.valueOf(roleScope.name()), null),
            new MembershipService.MembershipMember(groupId, null, MembershipMemberType.GROUP),
            new MembershipService.MembershipRole(roleScope, newRole));

    }

    @Override
    public GroupEntity findById(String groupId) {
        try {
            logger.debug("findById {}", groupId);
            Optional<Group> group = groupRepository.findById(groupId);
            if (!group.isPresent()) {
                throw new GroupNotFoundException(groupId);
            }
            logger.debug("findById {} - DONE", group.get());
            GroupEntity groupEntity = this.map(group.get());

            if (permissionService.hasPermission(RolePermission.ENVIRONMENT_GROUP, GraviteeContext.getCurrentEnvironment(), CREATE, UPDATE, DELETE)) {
                groupEntity.setManageable(true);
            } else {
                Optional<RoleEntity> optGroupAdminSystemRole = roleService.findByScopeAndName(RoleScope.GROUP, SystemRole.ADMIN.name());
                if (optGroupAdminSystemRole.isPresent()) {
                    List<String> groupIds = membershipService.getMembershipsByMemberAndReferenceAndRole(
                        MembershipMemberType.USER,
                        getAuthenticatedUsername(),
                        MembershipReferenceType.GROUP,
                        optGroupAdminSystemRole.get().getId())
                        .stream()
                        .map(MembershipEntity::getReferenceId)
                        .collect(Collectors.toList());
                    groupEntity.setManageable(groupIds.contains(groupEntity.getId()));
                }
            }

            return groupEntity;

        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to find a group", ex);
        }
    }

    @Override
    public void associate(String groupId, String associationType) {
        try {
            if ("api".equalsIgnoreCase(associationType)) {
                apiRepository.search(null)
                    .forEach(new Consumer<Api>() {
                        @Override
                        public void accept(Api api) {
                            if (api.getGroups() == null) {
                                api.setGroups(new HashSet<>());
                            }

                            if (!api.getGroups().contains(groupId)) {
                                api.getGroups().add(groupId);
                                try {
                                    apiRepository.update(api);
                                } catch (TechnicalException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
            } else if ("application".equalsIgnoreCase(associationType)) {
                applicationRepository.findAll()
                    .forEach(new Consumer<Application>() {
                        @Override
                        public void accept(Application application) {
                            if (application.getGroups() == null) {
                                application.setGroups(new HashSet<>());
                            }

                            if (!application.getGroups().contains(groupId)) {
                                application.getGroups().add(groupId);
                                try {
                                    applicationRepository.update(application);
                                } catch (TechnicalException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to associate group to all {}", associationType, ex);
            throw new TechnicalManagementException("An error occurs while trying to associate group to all " + associationType, ex);
        }
    }

    @Override
    public Set<GroupEntity> findByIds(Set<String> groupIds) {
        try {
            logger.debug("findByIds {}", groupIds);
            Set<Group> groups = groupRepository.findByIds(groupIds);
            if (groups == null || groups.size() != groupIds.size()) {
                List<String> groupsFound = groups == null ? Collections.emptyList()
                    : groups.stream().map(Group::getId).collect(Collectors.toList());
                Set<String> groupIdsNotFound = new HashSet<>(groupIds);
                groupIdsNotFound.removeAll(groupsFound);
                throw new GroupsNotFoundException(groupIdsNotFound);
            }
            logger.debug("findByIds {} - DONE", groups);
            return groups.
                stream().
                map(this::map).
                sorted(Comparator.comparing(GroupEntity::getName)).
                collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups", ex);
        }
    }

    @Override
    public Set<GroupEntity> findByEvent(GroupEvent event) {
        try {
            logger.debug("findByEvent : {}", event);
            Set<GroupEntity> set = groupRepository.findAllByEnvironment(GraviteeContext.getCurrentEnvironment()).
                stream().
                filter(g -> g.getEventRules() != null && g.getEventRules().
                    stream().
                    map(GroupEventRule::getEvent).
                    collect(Collectors.toList()).
                    contains(event)).
                map(this::map).
                sorted(Comparator.comparing(GroupEntity::getName)).
                collect(Collectors.toCollection(LinkedHashSet::new));
            logger.debug("findByEvent : {} - DONE", set);
            return set;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups by event", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups by event", ex);
        }
    }

    @Override
    public void delete(String groupId) {
        try {
            logger.debug("delete {}", groupId);
            Optional<Group> group = groupRepository.findById(groupId);
            if (!group.isPresent()) {
                throw new GroupNotFoundException(groupId);
            }

            RoleEntity apiPORole = roleService.findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
                .orElseThrow(() -> new TechnicalManagementException("API System Role 'PRIMARY_OWNER' not found."));


            final long apiCount = membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.GROUP, groupId, MembershipReferenceType.API, apiPORole.getId())
                .size();

            if (apiCount > 0) {
                throw new StillPrimaryOwnerException(apiCount, ApiPrimaryOwnerMode.GROUP);
            }

            //remove all members
            membershipService.deleteReference(MembershipReferenceType.GROUP, groupId);

            //remove all applications or apis
            Date updatedDate = new Date();
            apiRepository.search(new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).groups(groupId).build()).forEach(api -> {
                api.getGroups().remove(groupId);
                api.setUpdatedAt(updatedDate);
                try {
                    apiRepository.update(api);
                } catch (TechnicalException ex) {
                    logger.error("An error occurs while trying to delete a group", ex);
                    throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
                }

                //remove from API plans
                removeFromAPIPlans(groupId, updatedDate, api.getId());

                //remove from API pages
                removeGroupFromPages(groupId, updatedDate, api.getId());

                //remove idp group mapping using this group
                removeIDPGroupMapping(groupId, updatedDate);
            });
            applicationRepository.findByGroups(Collections.singletonList(groupId)).forEach(application -> {
                application.getGroups().remove(groupId);
                application.setUpdatedAt(updatedDate);
                try {
                    applicationRepository.update(application);
                } catch (TechnicalException ex) {
                    logger.error("An error occurs while trying to delete a group", ex);
                    throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
                }
            });

            //remove from portal pages
            removeGroupFromPages(groupId, updatedDate, null);

            //remove group
            groupRepository.delete(groupId);

            // Audit
            auditService.createEnvironmentAuditLog(
                Collections.singletonMap(GROUP, groupId),
                GROUP_DELETED,
                new Date(),
                group.get(),
                null);

            logger.debug("delete {} - DONE", groupId);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
        }
    }

    private void removeIDPGroupMapping(String groupId, Date updatedDate) {
        try {
            final Set<IdentityProvider> allIdp = this.identityProviderRepository.findAll();
            boolean idpHasBeenModified;
            for (IdentityProvider idp : allIdp) {
                idpHasBeenModified = false;
                Map<String, String[]> groupMappings = idp.getGroupMappings();
                if (groupMappings != null && !groupMappings.isEmpty()) {
                    for (Map.Entry<String, String[]> mapping : groupMappings.entrySet()) {
                        if (mapping.getValue() != null && mapping.getValue().length > 0) {
                            List<String> groups = new ArrayList<>(Arrays.asList(mapping.getValue()));
                            if (groups.contains(groupId)) {
                                groups.remove(groupId);
                                if (groups.isEmpty()) {
                                    groupMappings.remove(mapping.getKey());
                                } else {
                                    groupMappings.put(mapping.getKey(), groups.toArray(new String[groups.size()]));
                                }
                                idpHasBeenModified = true;
                            }
                        }
                    }
                    if (idpHasBeenModified) {
                        idp.setUpdatedAt(updatedDate);
                        this.identityProviderRepository.update(idp);
                    }
                }
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
        }
    }

    private void removeFromAPIPlans(String groupId, Date updatedDate, String apiId) {
        try {
            final Set<Plan> apiPlans = this.planRepository.findByApi(apiId);
            for (Plan plan : apiPlans) {
                if (plan.getExcludedGroups() != null && plan.getExcludedGroups().contains(groupId)) {
                    plan.getExcludedGroups().remove(groupId);
                    plan.setUpdatedAt(updatedDate);
                    this.planRepository.update(plan);
                }
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
        }
    }

    private void removeGroupFromPages(String groupId, Date updatedDate, String apiId) {
        try {
            PageCriteria.Builder criteriaBuilder = new PageCriteria.Builder();
            if (apiId != null) {
                criteriaBuilder.referenceId(apiId);
            }
            final List<Page> apiPages = this.pageRepository.search(criteriaBuilder.build());
            for (Page page : apiPages) {

                if (page.isExcludedAccessControls()) {

                    List<AccessControl> accessControlsWithoutGroup = page.getAccessControls()
                        .stream()
                        .filter(accessControl -> accessControl.getReferenceId().equals(groupId) &&
                            !AccessControlReferenceType.GROUP.name().equals(accessControl.getReferenceType())).collect(Collectors.toList());

                    if (accessControlsWithoutGroup.size() != page.getAccessControls().size()) {
                        page.setUpdatedAt(updatedDate);
                        this.pageRepository.update(page);
                    }

                }

            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
        }
    }

    @Override
    public boolean isUserAuthorizedToAccessApiData(ApiEntity api, List<String> excludedGroups, String username) {
        // in anonymous mode
        if (username == null) {
            // only public API without restrictions are authorized
            return (excludedGroups == null || excludedGroups.isEmpty())
                && (Visibility.PUBLIC.equals(api.getVisibility()));
        }

        // in connected mode,

        // if no restriction defined
        if (excludedGroups == null || excludedGroups.isEmpty()) {
            return true;
        }

        // if user is a direct member of the API
        if (!membershipService.getRoles(MembershipReferenceType.API, api.getId(), MembershipMemberType.USER, username).isEmpty()) {
            return true;
        }

        // for public apis
        // user must not be a member of any exclusion group.
        // That is user must not have no API role on each of the exclusion groups
        if (Visibility.PUBLIC.equals(api.getVisibility())) {
            return excludedGroups.stream()
                .allMatch(group -> this.membershipService.getRoles(MembershipReferenceType.GROUP, group, MembershipMemberType.USER, username)
                    .stream()
                    .noneMatch(role -> role.getScope() == RoleScope.API)
                );
        }

        // for private apis
        // user must be in at least one attached group which is not also an exclusion group.
        if (Visibility.PRIVATE.equals(api.getVisibility()) && api.getGroups() != null && !api.getGroups().isEmpty()) {
            Set<String> authorizedGroups = new HashSet<>(api.getGroups());
            authorizedGroups.removeAll(excludedGroups);

            return authorizedGroups.stream()
                .anyMatch(group -> this.membershipService.getRoles(MembershipReferenceType.GROUP, group, MembershipMemberType.USER, username)
                    .stream()
                    .anyMatch(role -> role.getScope() == RoleScope.API)
                );
        }

        return false;
    }

    @Override
    public Set<GroupEntity> findByUser(String user) {
        Set<String> userGroups = membershipService.getMembershipsByMemberAndReference(MembershipMemberType.USER, user, MembershipReferenceType.GROUP).stream()
            .map(MembershipEntity::getReferenceId)
            .collect(Collectors.toSet());
        try {
            return groupRepository.findByIds(userGroups)
                .stream()
                .map(this::map)
                .collect(Collectors.toSet())
                ;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find all user groups", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all user groups", ex);
        }
    }

    @Override
    public List<ApiEntity> getApis(String groupId) {
        return apiRepository.search(
            new ApiCriteria.Builder().environmentId(GraviteeContext.getCurrentEnvironment()).groups(groupId).build(),
            new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build())
            .stream()
            .map(api -> {
                ApiEntity apiEntity = new ApiEntity();
                apiEntity.setId(api.getId());
                apiEntity.setName(api.getName());
                apiEntity.setVersion(api.getVersion());
                apiEntity.setVisibility(Visibility.valueOf(api.getVisibility().name()));
                return apiEntity;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<ApplicationEntity> getApplications(String groupId) {
        try {
            return applicationRepository.findByGroups(Collections.singletonList(groupId), ApplicationStatus.ACTIVE)
                .stream()
                .map(application -> {
                    ApplicationEntity applicationEntity = new ApplicationEntity();
                    applicationEntity.setId(application.getId());
                    applicationEntity.setName(application.getName());
                    //applicationEntity.setType(application.getType());
                    return applicationEntity;
                })
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find all application of group {}", groupId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find all application of group " + groupId, ex);
        }
    }

    private Group map(GroupEntity entity) {
        if (entity == null) {
            return null;
        }

        Group group = new Group();
        group.setId(entity.getId());
        group.setName(entity.getName());

        if (entity.getEventRules() != null && !entity.getEventRules().isEmpty()) {
            List<GroupEventRule> groupEventRules = new ArrayList<>();
            for (GroupEventRuleEntity groupEventRuleEntity : entity.getEventRules()) {
                GroupEventRule eventRule = new GroupEventRule();
                eventRule.setEvent(GroupEvent.valueOf(groupEventRuleEntity.getEvent()));
                groupEventRules.add(eventRule);
            }
            group.setEventRules(groupEventRules);
        }

        group.setCreatedAt(entity.getCreatedAt());
        group.setUpdatedAt(entity.getUpdatedAt());
        group.setMaxInvitation(entity.getMaxInvitation());
        group.setLockApiRole(entity.isLockApiRole());
        group.setLockApplicationRole(entity.isLockApplicationRole());
        group.setSystemInvitation(entity.isSystemInvitation());
        group.setEmailInvitation(entity.isEmailInvitation());
        group.setEnvironmentId(GraviteeContext.getCurrentEnvironment());
        group.setDisableMembershipNotifications(entity.isDisableMembershipNotifications());

        return group;
    }

    private Group map(NewGroupEntity entity) {
        if (entity == null) {
            return null;
        }

        Group group = new Group();
        group.setName(entity.getName());
        if (entity.getEventRules() != null && !entity.getEventRules().isEmpty()) {
            List<GroupEventRule> groupEventRules = new ArrayList<>();
            for (GroupEventRuleEntity groupEventRuleEntity : entity.getEventRules()) {
                GroupEventRule eventRule = new GroupEventRule();
                eventRule.setEvent(GroupEvent.valueOf(groupEventRuleEntity.getEvent()));
                groupEventRules.add(eventRule);
            }
            group.setEventRules(groupEventRules);
        }
        group.setMaxInvitation(entity.getMaxInvitation());
        group.setLockApiRole(entity.isLockApiRole());
        group.setLockApplicationRole(entity.isLockApplicationRole());
        group.setSystemInvitation(entity.isSystemInvitation());
        group.setEmailInvitation(entity.isEmailInvitation());
        group.setDisableMembershipNotifications(entity.isDisableMembershipNotifications());
        return group;
    }

    private GroupEntity map(Group group) {
        if (group == null) {
            return null;
        }

        GroupEntity entity = new GroupEntity();
        entity.setId(group.getId());
        entity.setName(group.getName());
        entity.setApiPrimaryOwner(group.getApiPrimaryOwner());

        if (group.getEventRules() != null && !group.getEventRules().isEmpty()) {
            List<GroupEventRuleEntity> groupEventRules = new ArrayList<>();
            for (GroupEventRule groupEventRule : group.getEventRules()) {
                GroupEventRuleEntity eventRuleEntity = new GroupEventRuleEntity();
                eventRuleEntity.setEvent(groupEventRule.getEvent().name());
                groupEventRules.add(eventRuleEntity);
            }
            entity.setEventRules(groupEventRules);
        }

        Map<RoleScope, String> roles = new HashMap<>();
        RoleEntity defaultApiRole = getDefaultRole(group.getId(), RoleScope.API);
        if (defaultApiRole != null) {
            roles.put(RoleScope.API, defaultApiRole.getName());
        }
        RoleEntity defaultApplicationRole = getDefaultRole(group.getId(), RoleScope.APPLICATION);
        if (defaultApplicationRole != null) {
            roles.put(RoleScope.APPLICATION, defaultApplicationRole.getName());
        }
        entity.setRoles(roles);

        entity.setCreatedAt(group.getCreatedAt());
        entity.setUpdatedAt(group.getUpdatedAt());
        entity.setMaxInvitation(group.getMaxInvitation());
        entity.setLockApiRole(group.isLockApiRole());
        entity.setLockApplicationRole(group.isLockApplicationRole());
        entity.setSystemInvitation(group.isSystemInvitation());
        entity.setEmailInvitation(group.isEmailInvitation());
        entity.setDisableMembershipNotifications(group.isDisableMembershipNotifications());

        return entity;
    }

    @Override
    public void deleteUserFromGroup(String groupId, String username) {
        //check if user exist
        this.userService.findById(username);
        membershipService.deleteReferenceMember(MembershipReferenceType.GROUP, groupId, MembershipMemberType.USER, username);

        GroupEntity existingGroup = this.findById(groupId);
        if (existingGroup.getApiPrimaryOwner() != null && existingGroup.getApiPrimaryOwner().equals(username)) {
            updateApiPrimaryOwner(groupId, username);
        }
    }

    private RoleEntity getDefaultRole(String groupId, RoleScope scope) {
        Optional<RoleEntity> optDefaultRole = membershipService.getRoles(MembershipReferenceType.valueOf(scope.name()), null, MembershipMemberType.GROUP, groupId).stream().findFirst();
        if (optDefaultRole.isPresent()) {
            return optDefaultRole.get();
        }
        return null;
    }

    @Override
    public int getNumberOfMembers(String groupId) {
        return membershipService.getMembersByReference(MembershipReferenceType.GROUP, groupId).size() +
            invitationService.findByReference(InvitationReferenceType.GROUP, groupId).size();
    }

    @Override
    public void updateApiPrimaryOwner(String groupId, String newApiPrimaryOwner) {
        try {
            Optional<Group> group = groupRepository.findById(groupId);
            if (!group.isPresent()) {
                throw new GroupNotFoundException(groupId);
            }
            final Group foundGroup = group.get();
            foundGroup.setApiPrimaryOwner(newApiPrimaryOwner);
            groupRepository.update(foundGroup);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find or update a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to find or update a group", ex);
        }
    }

}
