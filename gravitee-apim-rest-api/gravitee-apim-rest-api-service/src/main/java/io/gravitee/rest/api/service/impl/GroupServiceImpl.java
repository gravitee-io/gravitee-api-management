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

import static io.gravitee.repository.management.model.Audit.AuditProperties.GROUP;
import static io.gravitee.repository.management.model.Group.AuditEvent.GROUP_CREATED;
import static io.gravitee.repository.management.model.Group.AuditEvent.GROUP_DELETED;
import static io.gravitee.repository.management.model.Group.AuditEvent.GROUP_UPDATED;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.common.event.EventManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.PageCriteria;
import io.gravitee.repository.management.model.AccessControl;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.repository.management.model.GroupEventRule;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.Page;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.AccessControlReferenceType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.GroupEventRuleEntity;
import io.gravitee.rest.api.model.GroupSimpleEntity;
import io.gravitee.rest.api.model.InvitationReferenceType;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewGroupEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateGroupEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.alert.ApplicationAlertMembershipEvent;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.InvitationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.GroupNameAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.GroupsNotFoundException;
import io.gravitee.rest.api.service.exceptions.StillPrimaryOwnerException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.reactivex.rxjava3.functions.Action;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GroupServiceImpl extends AbstractService implements GroupService {

    private final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Lazy
    @Autowired
    private GroupRepository groupRepository;

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
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

    @Lazy
    @Autowired
    private PageRepository pageRepository;

    @Lazy
    @Autowired
    private PlanRepository planRepository;

    @Lazy
    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private NotifierService notifierService;

    @Autowired
    private ApiConverter apiConverter;

    @Override
    public List<GroupEntity> findAll(ExecutionContext executionContext) {
        try {
            logger.debug("Find all groups");
            Set<Group> all = groupRepository.findAllByEnvironment(executionContext.getEnvironmentId());
            logger.debug("Find all groups - DONE");
            final List<GroupEntity> groups = all
                .stream()
                .map(this::map)
                .sorted(Comparator.comparing(GroupEntity::getName))
                .collect(Collectors.toList());

            populateGroupFlags(executionContext, groups);

            if (
                permissionService.hasPermission(
                    executionContext,
                    RolePermission.ENVIRONMENT_GROUP,
                    executionContext.getEnvironmentId(),
                    CREATE,
                    UPDATE,
                    DELETE
                )
            ) {
                groups.forEach(groupEntity -> groupEntity.setManageable(true));
            } else {
                Optional<RoleEntity> optGroupAdminSystemRole = roleService.findByScopeAndName(
                    RoleScope.GROUP,
                    SystemRole.ADMIN.name(),
                    executionContext.getOrganizationId()
                );
                if (optGroupAdminSystemRole.isPresent()) {
                    List<String> groupIds = membershipService
                        .getMembershipsByMemberAndReferenceAndRole(
                            MembershipMemberType.USER,
                            getAuthenticatedUsername(),
                            MembershipReferenceType.GROUP,
                            optGroupAdminSystemRole.get().getId()
                        )
                        .stream()
                        .map(MembershipEntity::getReferenceId)
                        .toList();
                    groups.forEach(groupEntity -> groupEntity.setManageable(groupIds.contains(groupEntity.getId())));
                }
            }
            return groups;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find all groups", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all groups", ex);
        }
    }

    @Override
    public List<GroupSimpleEntity> findAllByOrganization(String organizationId) {
        try {
            logger.debug("Find all groups for organization {}", organizationId);
            Set<Group> groups = groupRepository.findAllByOrganization(organizationId);
            logger.debug("Find all groups for organization {} - DONE", organizationId);
            return groups
                .stream()
                .map(this::mapToSimple)
                .sorted(Comparator.comparing(GroupSimpleEntity::getName))
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find all groups", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all groups", ex);
        }
    }

    private void populateGroupFlags(ExecutionContext executionContext, final List<GroupEntity> groups) {
        RoleEntity apiPORole = roleService
            .findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name(), executionContext.getOrganizationId())
            .orElseThrow(() -> new TechnicalManagementException("API System Role 'PRIMARY_OWNER' not found."));

        groups.forEach(group -> {
            final boolean isApiPO = !membershipService
                .getMembershipsByMemberAndReferenceAndRole(
                    MembershipMemberType.GROUP,
                    group.getId(),
                    MembershipReferenceType.API,
                    apiPORole.getId()
                )
                .isEmpty();

            group.setPrimaryOwner(isApiPO);
        });
    }

    @Override
    public List<GroupEntity> findByName(final String environmentId, String name) {
        try {
            logger.debug("findByUsername : {}", name);
            if (name == null) {
                return Collections.emptyList();
            }
            List<GroupEntity> groupEntities = groupRepository
                .findAllByEnvironment(environmentId)
                .stream()
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
    public GroupEntity create(ExecutionContext executionContext, NewGroupEntity group) {
        try {
            logger.debug("create {}", group);
            if (!this.findByName(executionContext.getEnvironmentId(), group.getName()).isEmpty()) {
                throw new GroupNameAlreadyExistsException(group.getName());
            }
            Group newGroup = this.map(group);
            newGroup.setId(UuidString.generateRandom());
            newGroup.setEnvironmentId(executionContext.getEnvironmentId());
            newGroup.setCreatedAt(new Date());
            newGroup.setUpdatedAt(newGroup.getCreatedAt());
            GroupEntity grp = this.map(groupRepository.create(newGroup));
            // Audit
            auditService.createAuditLog(
                executionContext,
                Collections.singletonMap(GROUP, newGroup.getId()),
                GROUP_CREATED,
                newGroup.getCreatedAt(),
                null,
                newGroup
            );
            logger.debug("create {} - DONE", grp);
            return grp;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to create a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to create a group", ex);
        }
    }

    @Override
    public GroupEntity update(ExecutionContext executionContext, String groupId, UpdateGroupEntity group) {
        try {
            logger.debug("update {}", group);
            GroupEntity updatedGroupEntity = this.findById(executionContext, groupId);
            Group previousGroup = this.map(updatedGroupEntity, executionContext.getEnvironmentId());
            updatedGroupEntity.setName(group.getName());
            updatedGroupEntity.setUpdatedAt(new Date());
            updatedGroupEntity.setEventRules(group.getEventRules());
            updatedGroupEntity.setMaxInvitation(group.getMaxInvitation());
            updatedGroupEntity.setLockApiRole(group.isLockApiRole());
            updatedGroupEntity.setLockApplicationRole(group.isLockApplicationRole());
            updatedGroupEntity.setSystemInvitation(group.isSystemInvitation());
            updatedGroupEntity.setEmailInvitation(group.isEmailInvitation());
            updatedGroupEntity.setDisableMembershipNotifications(group.isDisableMembershipNotifications());

            Group updatedGroup = this.map(updatedGroupEntity, executionContext.getEnvironmentId());
            GroupEntity grp = this.map(groupRepository.update(updatedGroup));
            logger.debug("update {} - DONE", grp);

            updateDefaultRoles(executionContext, groupId, updatedGroupEntity.getRoles(), group.getRoles());

            // Audit
            auditService.createAuditLog(
                executionContext,
                Collections.singletonMap(GROUP, groupId),
                GROUP_UPDATED,
                updatedGroupEntity.getUpdatedAt(),
                previousGroup,
                updatedGroup
            );
            return findById(executionContext, groupId);
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to update a group";
            logger.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private void updateDefaultRoles(
        ExecutionContext executionContext,
        String groupId,
        Map<RoleScope, String> formerRoles,
        Map<RoleScope, String> newRoles
    ) throws TechnicalException {
        RoleScope[] groupRoleScopes = { RoleScope.API, RoleScope.APPLICATION };
        for (RoleScope roleScope : groupRoleScopes) {
            if (
                formerRoles != null &&
                formerRoles.get(roleScope) != null &&
                (
                    newRoles == null ||
                    (
                        newRoles != null &&
                        !formerRoles.get(roleScope).equals(newRoles.get(roleScope)) &&
                        !SystemRole.PRIMARY_OWNER.name().equals(newRoles.get(roleScope))
                    )
                )
            ) {
                removeOldDefaultRole(executionContext, groupId, MembershipReferenceType.valueOf(roleScope.name()));
            }

            if (
                newRoles != null &&
                newRoles.get(roleScope) != null &&
                !SystemRole.PRIMARY_OWNER.name().equals(newRoles.get(roleScope)) &&
                (formerRoles == null || (formerRoles != null && !newRoles.get(roleScope).equals(formerRoles.get(roleScope))))
            ) {
                addNewDefaultRole(executionContext, groupId, newRoles.get(roleScope), roleScope);
            }
        }
    }

    private void removeOldDefaultRole(ExecutionContext executionContext, String groupId, MembershipReferenceType referenceType) {
        membershipService.deleteReferenceMember(executionContext, referenceType, null, MembershipMemberType.GROUP, groupId);
    }

    private void addNewDefaultRole(ExecutionContext executionContext, String groupId, String newRole, RoleScope roleScope) {
        membershipService.addRoleToMemberOnReference(
            executionContext,
            new MembershipService.MembershipReference(MembershipReferenceType.valueOf(roleScope.name()), null),
            new MembershipService.MembershipMember(groupId, null, MembershipMemberType.GROUP),
            new MembershipService.MembershipRole(roleScope, newRole)
        );
    }

    @Override
    @NotNull
    public GroupEntity findById(ExecutionContext executionContext, String groupId) {
        try {
            logger.debug("findById {}", groupId);
            Optional<Group> group = groupRepository
                .findById(groupId)
                .filter(g ->
                    !executionContext.hasEnvironmentId() || g.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId())
                );
            if (!group.isPresent()) {
                throw new GroupNotFoundException(groupId);
            }
            logger.debug("findById {} - DONE", group.get());
            GroupEntity groupEntity = this.map(executionContext, group.get());

            if (groupEntity == null) {
                logger.error("An error occurs while trying to find a group {}", groupId);
                throw new TechnicalManagementException("An error occurs while trying to find a group " + groupId);
            }

            if (
                executionContext.hasEnvironmentId() &&
                permissionService.hasPermission(
                    executionContext,
                    RolePermission.ENVIRONMENT_GROUP,
                    executionContext.getEnvironmentId(),
                    CREATE,
                    UPDATE,
                    DELETE
                )
            ) {
                groupEntity.setManageable(true);
            } else {
                Optional<RoleEntity> optGroupAdminSystemRole = roleService.findByScopeAndName(
                    RoleScope.GROUP,
                    SystemRole.ADMIN.name(),
                    executionContext.getOrganizationId()
                );
                if (optGroupAdminSystemRole.isPresent()) {
                    List<String> groupIds = membershipService
                        .getMembershipsByMemberAndReferenceAndRole(
                            MembershipMemberType.USER,
                            getAuthenticatedUsername(),
                            MembershipReferenceType.GROUP,
                            optGroupAdminSystemRole.get().getId()
                        )
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
    public void associate(final ExecutionContext executionContext, String groupId, String associationType) {
        try {
            switch (associationType.toLowerCase(Locale.ROOT)) {
                case "api":
                    apiRepository
                        .search(
                            new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId()).build(),
                            null,
                            ApiFieldFilter.allFields()
                        )
                        .filter(api -> api.addGroup(groupId))
                        .forEach(api -> {
                            runAndManageTechnicalException(() -> apiRepository.update(api));
                            triggerUpdateNotification(executionContext, api);
                        });
                    break;
                case "application":
                    applicationRepository
                        .findAllByEnvironment(executionContext.getEnvironmentId())
                        .stream()
                        .filter(application -> application.addGroup(groupId))
                        .forEach(application -> runAndManageTechnicalException(() -> applicationRepository.update(application)));

                    eventManager.publishEvent(
                        ApplicationAlertEventType.APPLICATION_MEMBERSHIP_UPDATE,
                        new ApplicationAlertMembershipEvent(executionContext.getOrganizationId(), Set.of(), Set.of(groupId))
                    );
                    break;
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to associate group to all {}", associationType, ex);
            throw new TechnicalManagementException("An error occurs while trying to associate group to all " + associationType, ex);
        }
    }

    private void runAndManageTechnicalException(Action action) {
        try {
            action.run();
        } catch (Throwable e) {
            logger.error("An error occurs while trying to update group", e);
        }
    }

    @Override
    public Set<GroupEntity> findByIds(Set<String> groupIds) {
        try {
            logger.debug("findByIds {}", groupIds);
            Set<Group> groups = groupRepository.findByIds(groupIds);
            if (groups == null || groups.size() != groupIds.size()) {
                List<String> groupsFound = groups == null
                    ? Collections.emptyList()
                    : groups.stream().map(Group::getId).collect(Collectors.toList());
                Set<String> groupIdsNotFound = new HashSet<>(groupIds);
                groupIdsNotFound.removeAll(groupsFound);
                throw new GroupsNotFoundException(groupIdsNotFound);
            }
            logger.debug("findByIds {} - DONE", groups);
            return groups
                .stream()
                .map(this::map)
                .sorted(Comparator.comparing(GroupEntity::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups", ex);
        }
    }

    @Override
    public Set<GroupEntity> findByEvent(final String environmentId, GroupEvent event) {
        try {
            logger.debug("findByEvent : {}", event);
            Set<GroupEntity> set = groupRepository
                .findAllByEnvironment(environmentId)
                .stream()
                .filter(g ->
                    g.getEventRules() != null &&
                    g.getEventRules().stream().map(GroupEventRule::getEvent).collect(Collectors.toList()).contains(event)
                )
                .map(this::map)
                .sorted(Comparator.comparing(GroupEntity::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            logger.debug("findByEvent : {} - DONE", set);
            return set;
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find groups by event", ex);
            throw new TechnicalManagementException("An error occurs while trying to find groups by event", ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String groupId) {
        try {
            logger.debug("delete {}", groupId);
            Optional<Group> group = groupRepository
                .findById(groupId)
                .filter(g -> g.getEnvironmentId().equalsIgnoreCase(executionContext.getEnvironmentId()));
            if (!group.isPresent()) {
                throw new GroupNotFoundException(groupId);
            }

            RoleEntity apiPORole = roleService
                .findByScopeAndName(RoleScope.API, SystemRole.PRIMARY_OWNER.name(), executionContext.getOrganizationId())
                .orElseThrow(() -> new TechnicalManagementException("API System Role 'PRIMARY_OWNER' not found."));

            final long apiCount = membershipService
                .getMembershipsByMemberAndReferenceAndRole(
                    MembershipMemberType.GROUP,
                    groupId,
                    MembershipReferenceType.API,
                    apiPORole.getId()
                )
                .size();

            if (apiCount > 0) {
                throw new StillPrimaryOwnerException(apiCount, ApiPrimaryOwnerMode.GROUP);
            }

            //remove all members
            membershipService.deleteReference(executionContext, MembershipReferenceType.GROUP, groupId);

            //remove default group API role
            membershipService.deleteReferenceMember(
                executionContext,
                MembershipReferenceType.API,
                null,
                MembershipMemberType.GROUP,
                groupId
            );

            //remove default group APPLICATION role
            membershipService.deleteReferenceMember(
                executionContext,
                MembershipReferenceType.APPLICATION,
                null,
                MembershipMemberType.GROUP,
                groupId
            );

            //remove all applications or apis
            Date updatedDate = new Date();
            apiRepository
                .search(
                    new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId()).groups(groupId).build(),
                    null,
                    ApiFieldFilter.allFields()
                )
                .forEach(api -> {
                    api.getGroups().remove(groupId);
                    api.setUpdatedAt(updatedDate);
                    try {
                        apiRepository.update(api);
                        triggerUpdateNotification(executionContext, api);
                    } catch (TechnicalException ex) {
                        logger.error("An error occurs while trying to delete a group", ex);
                        throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
                    }

                    //remove from API plans
                    removeFromAPIPlans(groupId, updatedDate, api.getId());

                    //remove from API pages
                    PageCriteria apiPageCriteria = new PageCriteria.Builder()
                        .referenceId(api.getId())
                        .referenceType(PageReferenceType.API.name())
                        .build();
                    removeGroupFromPages(groupId, updatedDate, apiPageCriteria);

                    //remove idp group mapping using this group
                    removeIDPGroupMapping(groupId, updatedDate);
                });
            Set<String> applicationIds = new HashSet<>();
            applicationRepository
                .findByGroups(Collections.singletonList(groupId))
                .forEach(application -> {
                    application.getGroups().remove(groupId);
                    application.setUpdatedAt(updatedDate);
                    try {
                        applicationRepository.update(application);
                        applicationIds.add(application.getId());
                    } catch (TechnicalException ex) {
                        logger.error("An error occurs while trying to delete a group", ex);
                        throw new TechnicalManagementException("An error occurs while trying to delete a group", ex);
                    }
                });

            eventManager.publishEvent(
                ApplicationAlertEventType.APPLICATION_MEMBERSHIP_UPDATE,
                new ApplicationAlertMembershipEvent(executionContext.getOrganizationId(), applicationIds, Collections.emptySet())
            );

            //remove from portal pages
            PageCriteria environmentPageCriteria = new PageCriteria.Builder()
                .referenceId(executionContext.getEnvironmentId())
                .referenceType(PageReferenceType.ENVIRONMENT.name())
                .build();
            removeGroupFromPages(groupId, updatedDate, environmentPageCriteria);

            //remove group
            groupRepository.delete(groupId);

            // Audit
            auditService.createAuditLog(
                executionContext,
                Collections.singletonMap(GROUP, groupId),
                GROUP_DELETED,
                new Date(),
                group.get(),
                null
            );

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

    private void removeGroupFromPages(String groupId, Date updatedDate, PageCriteria pageCriteria) {
        try {
            final List<Page> pages = this.pageRepository.search(pageCriteria);
            for (Page page : pages) {
                if (page.getAccessControls() != null) {
                    Set<AccessControl> accessControlsToRemove = page
                        .getAccessControls()
                        .stream()
                        .filter(accessControl ->
                            (
                                AccessControlReferenceType.GROUP.name().equals(accessControl.getReferenceType()) &&
                                accessControl.getReferenceId().equals(groupId)
                            )
                        )
                        .collect(Collectors.toSet());

                    if (!accessControlsToRemove.isEmpty()) {
                        page.setUpdatedAt(updatedDate);
                        page.getAccessControls().removeAll(accessControlsToRemove);
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
    public boolean isUserAuthorizedToAccessApiData(GenericApiEntity api, List<String> excludedGroups, String username) {
        // in anonymous mode
        if (username == null) {
            // only public API without restrictions are authorized
            return (excludedGroups == null || excludedGroups.isEmpty()) && (Visibility.PUBLIC.equals(api.getVisibility()));
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
            return excludedGroups
                .stream()
                .allMatch(group ->
                    this.membershipService.getRoles(MembershipReferenceType.GROUP, group, MembershipMemberType.USER, username)
                        .stream()
                        .noneMatch(role -> role.getScope() == RoleScope.API)
                );
        }

        // for private apis
        // user must be in at least one attached group which is not also an exclusion group.
        if (Visibility.PRIVATE.equals(api.getVisibility()) && api.getGroups() != null && !api.getGroups().isEmpty()) {
            Set<String> authorizedGroups = new HashSet<>(api.getGroups());
            authorizedGroups.removeAll(excludedGroups);

            return authorizedGroups
                .stream()
                .anyMatch(group ->
                    this.membershipService.getRoles(MembershipReferenceType.GROUP, group, MembershipMemberType.USER, username)
                        .stream()
                        .anyMatch(role -> role.getScope() == RoleScope.API)
                );
        }

        return false;
    }

    @Override
    public Set<GroupEntity> findByUser(String user) {
        Set<String> userGroups = membershipService
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, user, MembershipReferenceType.GROUP)
            .stream()
            .map(MembershipEntity::getReferenceId)
            .collect(Collectors.toSet());
        try {
            return groupRepository.findByIds(userGroups).stream().map(this::map).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find all user groups", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all user groups", ex);
        }
    }

    @Override
    public List<ApiEntity> getApis(final String environmentId, String groupId) {
        return apiRepository
            .search(new ApiCriteria.Builder().environmentId(environmentId).groups(groupId).build(), null, ApiFieldFilter.defaultFields())
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
            return applicationRepository
                .findByGroups(Collections.singletonList(groupId), ApplicationStatus.ACTIVE)
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

    private Group map(GroupEntity entity, final String environmentId) {
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
        group.setEnvironmentId(environmentId);
        group.setDisableMembershipNotifications(entity.isDisableMembershipNotifications());
        group.setApiPrimaryOwner(entity.getApiPrimaryOwner());

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

    private GroupSimpleEntity mapToSimple(Group group) {
        if (group == null) {
            return null;
        }

        GroupSimpleEntity entity = new GroupSimpleEntity();
        entity.setId(group.getId());
        entity.setName(group.getName());

        return entity;
    }

    private GroupEntity map(Group group) {
        return map(null, group);
    }

    private GroupEntity map(ExecutionContext executionContext, Group group) {
        if (group == null) {
            return null;
        }

        GroupEntity entity = new GroupEntity();
        entity.setId(group.getId());
        entity.setName(group.getName());
        if (group.getApiPrimaryOwner() != null && !group.getApiPrimaryOwner().isEmpty()) {
            entity.setApiPrimaryOwner(group.getApiPrimaryOwner());
            entity.setPrimaryOwner(true);
        }

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
    public void deleteUserFromGroup(ExecutionContext executionContext, String groupId, String username) {
        //check if user exist
        this.userService.findById(executionContext, username);

        eventManager.publishEvent(
            ApplicationAlertEventType.APPLICATION_MEMBERSHIP_UPDATE,
            new ApplicationAlertMembershipEvent(
                executionContext.getOrganizationId(),
                Collections.emptySet(),
                Collections.singleton(groupId)
            )
        );
        membershipService.deleteReferenceMember(
            executionContext,
            MembershipReferenceType.GROUP,
            groupId,
            MembershipMemberType.USER,
            username
        );

        GroupEntity existingGroup = this.findById(executionContext, groupId);
        if (existingGroup.getApiPrimaryOwner() != null && existingGroup.getApiPrimaryOwner().equals(username)) {
            updateApiPrimaryOwner(groupId, username);
        }
    }

    private RoleEntity getDefaultRole(String groupId, RoleScope scope) {
        Optional<RoleEntity> optDefaultRole = membershipService
            .getRoles(MembershipReferenceType.valueOf(scope.name()), null, MembershipMemberType.GROUP, groupId)
            .stream()
            .findFirst();
        if (optDefaultRole.isPresent()) {
            return optDefaultRole.get();
        }
        return null;
    }

    @Override
    public int getNumberOfMembers(ExecutionContext executionContext, String groupId) {
        return (
            membershipService.getMembersByReference(executionContext, MembershipReferenceType.GROUP, groupId).size() +
            invitationService.findByReference(InvitationReferenceType.GROUP, groupId).size()
        );
    }

    @Override
    public void updateApiPrimaryOwner(String groupId, String newApiPrimaryOwner) {
        try {
            Group group = groupRepository.findById(groupId).orElseThrow(() -> new GroupNotFoundException(groupId));
            group.setApiPrimaryOwner(newApiPrimaryOwner);
            groupRepository.update(group);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find or update a group", ex);
            throw new TechnicalManagementException("An error occurs while trying to find or update a group", ex);
        }
    }

    private void triggerUpdateNotification(ExecutionContext executionContext, Api api) {
        ApiEntity apiEntity = apiConverter.toApiEntity(api, null);
        notifierService.trigger(
            executionContext,
            ApiHook.API_UPDATED,
            api.getId(),
            new NotificationParamsBuilder().api(apiEntity).user(userService.findById(executionContext, getAuthenticatedUsername())).build()
        );
    }
}
