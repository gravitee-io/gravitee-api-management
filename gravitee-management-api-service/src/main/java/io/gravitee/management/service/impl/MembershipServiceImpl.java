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

import io.gravitee.management.model.*;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.api.ApiQuery;
import io.gravitee.management.model.pagedresult.Metadata;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.model.providers.User;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.notification.NotificationParamsBuilder;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.management.model.permissions.SystemRole.PRIMARY_OWNER;
import static io.gravitee.management.service.notification.PortalHook.GROUP_INVITATION;
import static io.gravitee.repository.management.model.Membership.AuditEvent.*;
import static io.gravitee.repository.management.model.MembershipReferenceType.*;
import static java.util.Collections.singletonMap;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MembershipServiceImpl extends AbstractService implements MembershipService {

    private final Logger LOGGER = LoggerFactory.getLogger(MembershipServiceImpl.class);

    @Autowired
    private UserService userService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private RoleService roleService;
    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApiService apiService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private ApiRepository apiRepository;
    @Autowired
    private ApplicationRepository applicationRepository;
    @Autowired
    private NotifierService notifierService;
    @Autowired
    private InvitationService invitationService;

    @Override
    public Set<MemberEntity> getMembers(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope) {
        return getMembers(referenceType, referenceId, roleScope, null);
    }

    @Override
    public Set<MemberEntity> getMembers(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope, String roleName) {
        try {
            LOGGER.debug("Get members for {} {}", referenceType, referenceId);

            Set<Membership> memberships = membershipRepository.findByReferenceAndRole(
                    referenceType,
                    referenceId,
                    roleScope,
                    roleName);

            return memberships.stream()
                    .map(m -> convert(m, roleScope))
                    .collect(Collectors.toSet());

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get members for {} {}", referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referenceId, ex);
        }
    }

    @Override
    public MemberEntity getMember(MembershipReferenceType referenceType, String referenceId, String userId, RoleScope roleScope) {
        try {
            LOGGER.debug("Get membership for {} {} and user {}", referenceType, referenceId, userId);

            Optional<Membership> optionalMembership = membershipRepository.findById(userId, referenceType, referenceId);

            return optionalMembership.
                    map(m -> convert(m, roleScope)).
                    orElse(null);

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for {} {} and user", referenceType, referenceId, userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referenceId + " and user " + userId, ex);
        }
    }

    @Override
    public RoleEntity getRole(MembershipReferenceType referenceType, String referenceId, String userId, RoleScope roleScope) {
        try {
            if (referenceId == null) {
                throw new IllegalArgumentException("You must provide a referenceId !");
            }
            LOGGER.debug("Get role for {} {} and user {}", referenceType, referenceId, userId);

            Optional<Membership> optionalMembership = membershipRepository.findById(userId, referenceType, referenceId);

            return optionalMembership.
                    filter(m -> m.getRoles().get(roleScope.getId()) != null).
                    map(m -> roleService.findById(roleScope, m.getRoles().get(roleScope.getId()))).
                    orElse(null);

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for {} {} and user", referenceType, referenceId, userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referenceId + " and user " + userId, ex);
        }
    }

    @Override
    public Set<RoleEntity> getRoles(MembershipReferenceType referenceType, Set<String> referenceIds, String username, RoleScope roleScope) {
        try {
            if (referenceIds == null) {
                throw new IllegalArgumentException("You must provide referenceIds !");
            }
            LOGGER.debug("Get role for {} {} and user {}", referenceType, referenceIds, username);

            Set<Membership> memberships = membershipRepository.findByIds(username, referenceType, referenceIds);

            return memberships == null ? Collections.emptySet() :
                    memberships.
                            stream().
                            filter(m -> m.getRoles().get(roleScope.getId()) != null).
                            map(m -> roleService.findById(roleScope, m.getRoles().get(roleScope.getId()))).
                            collect(Collectors.toSet());

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for {} {} and user", referenceType, referenceIds, username, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referenceIds + " and user " + username, ex);
        }
    }

    /**
     * assert that the role's scope is allowed for the given reference
     */
    private void assertRoleScopeAllowedForReference(MembershipReference reference, MembershipRole role) {
        RoleEntity roleEntity = roleService.findById(role.getScope(), role.getName());
        if (API.equals(reference.getType())
                && !io.gravitee.management.model.permissions.RoleScope.API.equals(roleEntity.getScope())) {
            throw new NotAuthorizedMembershipException(role.getName());
        } else if (APPLICATION.equals(reference.getType())
                && !io.gravitee.management.model.permissions.RoleScope.APPLICATION.equals(roleEntity.getScope())) {
            throw new NotAuthorizedMembershipException(role.getName());
        } else if (GROUP.equals(reference.getType())
                && !io.gravitee.management.model.permissions.RoleScope.APPLICATION.equals(roleEntity.getScope())
                && !io.gravitee.management.model.permissions.RoleScope.API.equals(roleEntity.getScope())
                && !io.gravitee.management.model.permissions.RoleScope.GROUP.equals(roleEntity.getScope())) {
            throw new NotAuthorizedMembershipException(role.getName());
        } else if (GROUP.equals(reference.getType())
                && SystemRole.PRIMARY_OWNER.name().equals(role.getName())) {
            throw new NotAuthorizedMembershipException(role.getName());
        }
    }

    /**
     * assert that the role's name is allowed for the given reference
     */
    private void assertRoleNameAllowedForReference(MembershipReference reference, MembershipRole role) {
        if (GROUP.equals(reference.getType())
                && SystemRole.PRIMARY_OWNER.name().equals(role.getName())) {
            throw new NotAuthorizedMembershipException(role.getName());
        }
    }

    /**
     * assert that we do not try to override the primary owner
     */
    private void assertNotOverridePrimaryOwner(MembershipReference reference, String userId, MembershipRole role) {
        if (userId != null &&
                !SystemRole.PRIMARY_OWNER.name().equals(role.getName())
                && (API.equals(reference.getType()) || APPLICATION.equals(reference.getType()))) {
            MemberEntity member = this.getMember(reference.getType(), reference.getId(), userId, role.getScope());
            if (member != null && SystemRole.PRIMARY_OWNER.name().equals(member.getRole())) {
                throw new AlreadyPrimaryOwnerException(userId);
            }
        }
    }

    @Override
    public MemberEntity addOrUpdateMember(MembershipReference reference, MembershipUser user, MembershipRole role) {
        return addOrUpdateMember(reference, user, role, false);
    }

    public MemberEntity addOrUpdateMember(MembershipReference reference, MembershipUser user, MembershipRole role, boolean transferOwnership) {
        try {
            LOGGER.debug("Add a new member for {} {}", reference.getType(), reference.getId());

            assertRoleScopeAllowedForReference(reference, role);
            assertRoleNameAllowedForReference(reference, role);

            UserEntity userEntity;
            if (user.getId() != null) {
                userEntity = userService.findById(user.getId());
                // In the context of transfer_ownership, so we must accept primary_owner overriding.
                if (! transferOwnership) {
                    assertNotOverridePrimaryOwner(reference, user.getId(), role);
                }
            } else {
                // We have a user reference, meaning that the user is coming from an external system
                // User does not exist so we are looking into defined providers
                Optional<io.gravitee.management.model.providers.User> providerUser = identityService.findByReference(user.getReference());
                if (providerUser.isPresent()) {
                    User identityUser = providerUser.get();

                    try {
                        userEntity = userService.findBySource(identityUser.getSource(), identityUser.getSourceId(),false);
                        assertNotOverridePrimaryOwner(reference, userEntity.getId(), role);
                    } catch (UserNotFoundException unfe) {
                        // The user is not yet registered in repository
                        // Information will be updated after the first connection of the user
                        NewExternalUserEntity newUser = new NewExternalUserEntity();
                        newUser.setFirstname(identityUser.getFirstname());
                        newUser.setLastname(identityUser.getLastname());
                        newUser.setSource(identityUser.getSource());
                        newUser.setEmail(identityUser.getEmail());
                        newUser.setSourceId(identityUser.getSourceId());

                        userEntity = userService.create(newUser, true);
                    }
                } else {
                    throw new UserNotFoundException(user.getReference());
                }
            }

            Optional<Membership> optionalMembership =
                    membershipRepository.findById(userEntity.getId(), reference.getType(), reference.getId());
            Date updateDate = new Date();
            Membership returnedMembership;
            if (optionalMembership.isPresent()) {
                Membership updatedMembership = optionalMembership.get();
                Membership previousMembership = new Membership(updatedMembership);
                updatedMembership.getRoles().put(role.getScope().getId(), role.getName());
                updatedMembership.setUpdatedAt(updateDate);
                returnedMembership = membershipRepository.update(optionalMembership.get());
                createAuditLog(MEMBERSHIP_UPDATED, updatedMembership.getUpdatedAt(), previousMembership, updatedMembership);
            } else {
                Membership membership = new Membership(userEntity.getId(), reference.getId(), reference.getType());
                membership.setRoles(singletonMap(role.getScope().getId(), role.getName()));
                membership.setCreatedAt(updateDate);
                membership.setUpdatedAt(updateDate);
                returnedMembership = membershipRepository.create(membership);
                createAuditLog(MEMBERSHIP_CREATED, membership.getCreatedAt(), null, membership);
                if (userEntity.getEmail() != null && !userEntity.getEmail().isEmpty()) {
                    EmailNotification emailNotification = buildEmailNotification(userEntity, reference.getType(), reference.getId());
                    if (emailNotification != null) {
                        emailService.sendAsyncEmailNotification(emailNotification);
                    }
                }
                if (GROUP.equals(reference.getType())) {
                    notifierService.trigger(GROUP_INVITATION, singletonMap("group", groupService.findById(reference.getId())));
                }
            }

            return convert(returnedMembership, role.getScope());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to add member for {} {}", reference.getType(), reference.getId(), ex);
            throw new TechnicalManagementException("An error occurs while trying to add member for " + reference.getType() + " " + reference.getId(), ex);
        }
    }

    @Override
    public void deleteMember(MembershipReferenceType referenceType, String referenceId, String userId) {
        try {
            LOGGER.debug("Delete member {} for {} {}", userId, referenceType, referenceId);
            if (!GROUP.equals(referenceType)) {
                RoleScope roleScope = getScopeByMembershipReferenceType(referenceType);

                RoleEntity roleEntity = this.getRole(referenceType, referenceId, userId, roleScope);
                if (roleEntity != null && PRIMARY_OWNER.name().equals(roleEntity.getName())) {
                    throw new SinglePrimaryOwnerException(
                            referenceType.equals(API)
                                    ? RoleScope.API : RoleScope.APPLICATION
                    );
                }
            }
            Membership membership = new Membership(userId, referenceId, referenceType);
            membershipRepository.delete(membership);
            createAuditLog(MEMBERSHIP_DELETED, new Date(), membership, null);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete member {} for {} {}", userId, referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete member " + userId + " for " + referenceType + " " + referenceId, ex);
        }
    }

    @Override
    public void deleteMembers(MembershipReferenceType referenceType, String referenceId) {
        try {
            LOGGER.debug("Delete members for {} {}", referenceType, referenceId);
            membershipRepository.deleteMembers(referenceType, referenceId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete members for {} {}", referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete members for " + referenceType + " " + referenceId, ex);
        }
    }



    @Override
    public void transferApiOwnership(String apiId, MembershipUser user, RoleEntity newPrimaryOwnerRole) {
        this.transferOwnership(API, RoleScope.API, apiId, user, newPrimaryOwnerRole);
    }

    @Override
    public void transferApplicationOwnership(String applicationId, MembershipUser user, RoleEntity newPrimaryOwnerRole) {
        this.transferOwnership(APPLICATION, RoleScope.APPLICATION, applicationId, user, newPrimaryOwnerRole);
    }

    private void transferOwnership(MembershipReferenceType membershipReferenceType, RoleScope roleScope, String itemId, MembershipUser user, RoleEntity newPrimaryOwnerRole) {
        final RoleEntity newRole = (newPrimaryOwnerRole != null) ? newPrimaryOwnerRole : roleService.findDefaultRoleByScopes(roleScope).get(0);

        // Set the new primary owner
        MemberEntity newPrimaryOwner = this.addOrUpdateMember(
                new MembershipReference(membershipReferenceType, itemId),
                new MembershipUser(user.getId(), user.getReference()),
                new MembershipRole(roleScope, PRIMARY_OWNER.name()));

        // Update the role for previous primary_owner
        this.getMembers(membershipReferenceType, itemId, roleScope, PRIMARY_OWNER.name())
                .stream()
                .filter(memberEntity -> ! memberEntity.getId().equals(newPrimaryOwner.getId()))
                .forEach(m -> this.addOrUpdateMember(
                        new MembershipReference(membershipReferenceType, itemId),
                        new MembershipUser(m.getId(), null),
                        new MembershipRole(roleScope, newRole.getName()),
                        true));
    }

    @Override
    public Map<String, char[]> getMemberPermissions(ApiEntity api, String userId) {
        return getMemberPermissions(API,
                api.getId(),
                userId,
                api.getGroups(),
                RoleScope.API);
    }

    @Override
    public Map<String, char[]> getMemberPermissions(ApplicationEntity application, String userId) {
        return getMemberPermissions(APPLICATION,
                application.getId(),
                userId,
                application.getGroups(),
                RoleScope.APPLICATION);
    }

    @Override
    public Map<String, char[]> getMemberPermissions(GroupEntity group, String userId) {
        return getMemberPermissions(GROUP,
                group.getId(),
                userId,
                null,
                RoleScope.GROUP);
    }

    @Override
    public boolean removeRole(MembershipReferenceType referenceType, String referenceId, String userId, RoleScope roleScope) {
        try {
            Optional<Membership> optionalMembership = membershipRepository.findById(userId, referenceType, referenceId);
            if (optionalMembership.isPresent()) {
                Membership membership = optionalMembership.get();
                Membership previousMembership = new Membership(membership);
                membership.getRoles().remove(roleScope.getId());
                membership.setUpdatedAt(new Date());
                if (membership.getRoles().isEmpty()) {
                    throw new MemberWithoutRoleException(membership.getUserId());
                } else {
                    membershipRepository.update(membership);
                    createAuditLog(MEMBERSHIP_UPDATED, membership.getUpdatedAt(), previousMembership, membership);
                    return true;
                }
            }
            return false;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for {} {} and user", referenceType, referenceId, userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referenceId + " and user " + userId, ex);
        }
    }

    @Override
    public void removeRoleUsage(RoleScope roleScope, String roleName, String newRole) {
        try {
            Set<Membership> memberships = membershipRepository.findByRole(roleScope, roleName);
            for (Membership membership : memberships) {
                membership.getRoles().put(roleScope.getId(), newRole);
                membershipRepository.update(membership);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove role {} {}", roleScope, roleName, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove role " + roleScope + " " + roleName, ex);
        }
    }

    @Override
    public void removeUser(String userId) {
        try {
            for(Membership membership : membershipRepository.findByUser(userId)) {
                membershipRepository.delete(membership);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove user {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove user " + userId, ex);
        }
    }

    @Override
    public List<UserMembership> findUserMembership(String userId, MembershipReferenceType type) {

        // TODO only API, APPLICATION and GROUP are implemented
        if (type == null || (!type.equals(API) && !type.equals(APPLICATION) && !type.equals(GROUP))) {
            return Collections.emptyList();
        }

        try {
            Set<UserMembership> userMemberships = membershipRepository
                    .findByUserAndReferenceType(userId, type)
                    .stream()
                    .map(membership -> {
                        UserMembership userMembership = new UserMembership();
                        userMembership.setType(type.name());
                        userMembership.setReference(membership.getReferenceId());
                        userMembership.setRoles(membership.getRoles());
                        return userMembership;
                    })
                    .collect(Collectors.toSet());

            if (type.equals(APPLICATION) || type.equals(API)) {
                //get memberships by the user group
                final List<String> groupIds = membershipRepository
                        .findByUserAndReferenceType(userId, GROUP).stream()
                        .filter(m -> m.getRoles().keySet().contains(type.equals(API) ? RoleScope.API.getId() : RoleScope.APPLICATION.getId()))
                        .map(Membership::getReferenceId)
                        .collect(Collectors.toList());
                if (!groupIds.isEmpty() && type.equals(API)) {
                    ApiQuery apiQuery = new ApiQuery();
                    apiQuery.setGroups(groupIds);
                    userMemberships.addAll(apiService.search(apiQuery)
                            .stream()
                            .map(apiEntity -> {
                                UserMembership userMembership = new UserMembership();
                                userMembership.setReference(apiEntity.getId());
                                userMembership.setType(type.name());
                                return userMembership;
                            })
                            .collect(Collectors.toSet()));
                } else if (!groupIds.isEmpty() && type.equals(APPLICATION)) {
                    userMemberships.addAll(applicationService.findByGroups(groupIds).stream()
                            .map(applicationEntity -> {
                                UserMembership userMembership = new UserMembership();
                                userMembership.setReference(applicationEntity.getId());
                                userMembership.setType(type.name());
                                return userMembership;
                            })
                            .collect(Collectors.toSet()));
                }
            }

            return new ArrayList<>(userMemberships);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove user {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove user " + userId, ex);
        }
    }

    @Override
    public Metadata findUserMembershipMetadata(List<UserMembership> memberships, MembershipReferenceType type) {
        // TODO only API, APPLICATION and GROUP are implemented
        if (memberships == null || memberships.isEmpty() ||
                type == null || (!type.equals(API) && !type.equals(APPLICATION) && !type.equals(GROUP)) ) {
            return new Metadata();
        }
        try {
            Metadata metadata = new Metadata();
            if (type.equals(API)) {
                ApiCriteria.Builder criteria = new ApiCriteria.Builder();
                ApiFieldExclusionFilter filter = (new ApiFieldExclusionFilter.Builder()).excludeDefinition().excludePicture().build();
                criteria.ids(memberships.stream().map(UserMembership::getReference).toArray(String[]::new));
                apiRepository.search(criteria.build(), filter).forEach(api -> {
                    metadata.put(api.getId(), "name", api.getName());
                    metadata.put(api.getId(), "version", api.getVersion());
                    metadata.put(api.getId(), "visibility", api.getVisibility());
                });
            } else if (type.equals(APPLICATION)) {
                applicationRepository.findByIds(memberships.stream().map(UserMembership::getReference).collect(Collectors.toList())).forEach(application -> {
                    metadata.put(application.getId(), "name", application.getName());
                    metadata.put(application.getId(), "type", application.getType());
                    metadata.put(application.getId(), "status", application.getStatus());
                });
            }
            return metadata;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get user membership metadata", ex);
            throw new TechnicalManagementException("An error occurs while trying to get user membership metadata", ex);
        }
    }

    @Override
    public int getNumberOfMembers(final MembershipReferenceType referenceType, final String referenceId, final RoleScope roleScope) {
        return getMembers(referenceType, referenceId, roleScope).size() +
                invitationService.findByReference(InvitationReferenceType.valueOf(referenceType.name()), referenceId).size();
    }

    private Map<String, char[]> getMemberPermissions(MembershipReferenceType membershipReferenceType, String referenceId, String userId, Set<String> groups, RoleScope roleScope) {
        final Map<String, char[]> permissions = new HashMap<>();

        MemberEntity member = this.getMember(membershipReferenceType, referenceId, userId, roleScope);
        if (member != null) {
            permissions.putAll(member.getPermissions());
        }

        if (groups != null) {
            Map<String, Set<Character>> mergedPermissions = new HashMap<>();
            for (String groupid : groups) {
                member = this.getMember(GROUP, groupid, userId, roleScope);
                if (member != null) {
                    for (Map.Entry<String, char[]> perm : member.getPermissions().entrySet()) {
                        if (mergedPermissions.containsKey(perm.getKey())) {
                            Set<Character> previousCRUD = mergedPermissions.get(perm.getKey());
                            for (char c : perm.getValue()) {
                                previousCRUD.add(c);
                            }
                        } else {
                            HashSet<Character> crudAsSet = new HashSet<>();
                            for (char c : perm.getValue()) {
                                crudAsSet.add(c);
                            }
                            mergedPermissions.put(perm.getKey(), crudAsSet);
                        }
                    }
                }
            }

            mergedPermissions.forEach((String k, Set<Character> v) -> {
                Character[] characters = v.toArray(new Character[v.size()]);
                char[] chars = new char[characters.length];
                for (int i = 0; i < characters.length; i++) {
                    chars[i] = characters[i];
                }
                permissions.put(k, chars);
            });

        }

        return permissions;
    }

    private MemberEntity convert(Membership membership, RoleScope roleScope) {
        final MemberEntity member = new MemberEntity();

        final UserEntity userEntity = userService.findById(membership.getUserId());
        final RoleEntity role = getRole(
                membership.getReferenceType(),
                membership.getReferenceId(),
                membership.getUserId(),
                roleScope);
        // because API and APPLICATION RoleScope is not mandatory for a group,
        // role could be null
        if (role == null) {
            return null;
        }

        member.setPermissions(role.getPermissions());
        member.setId(membership.getUserId());
        member.setCreatedAt(membership.getCreatedAt());
        member.setUpdatedAt(membership.getUpdatedAt());
        member.setRole(role.getName());
        member.setDisplayName(userEntity.getDisplayName());
        member.setEmail(userEntity.getEmail());

        return member;
    }

    private EmailNotification buildEmailNotification(UserEntity user, MembershipReferenceType referenceType, String referenceId) {
        String subject = null;
        EmailNotificationBuilder.EmailTemplate template = null;
        Map<String, Object> params = null;
        GroupEntity groupEntity;
        NotificationParamsBuilder paramsBuilder = new NotificationParamsBuilder();
        switch (referenceType) {
            case APPLICATION:
                ApplicationEntity applicationEntity = applicationService.findById(referenceId);
                subject = "Subscription to application " + applicationEntity.getName();
                template = EmailNotificationBuilder.EmailTemplate.APPLICATION_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.application(applicationEntity).user(user).build();
                break;
            case API:
                ApiEntity apiEntity = apiService.findById(referenceId);
                subject = "Subscription to API " + apiEntity.getName();
                template = EmailNotificationBuilder.EmailTemplate.API_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.api(apiEntity).user(user).build();
                break;
            case GROUP:
                groupEntity = groupService.findById(referenceId);
                subject = "Subscription to group " + groupEntity.getName();
                template = EmailNotificationBuilder.EmailTemplate.GROUP_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.group(groupEntity).user(user).build();
                break;
        }

        if (template == null) {
            return null;
        }

        return new EmailNotificationBuilder()
                .to(user.getEmail())
                .subject(subject)
                .template(template)
                .params(params)
                .build();
    }

    private RoleScope getScopeByMembershipReferenceType(MembershipReferenceType type) {
        if (type == null) {
            return null;
        }
        if (MembershipReferenceType.PORTAL.equals(type)) {
            return RoleScope.PORTAL;
        } else if (MembershipReferenceType.MANAGEMENT.equals(type)) {
            return RoleScope.MANAGEMENT;
        } else if (APPLICATION.equals(type)) {
            return RoleScope.APPLICATION;
        } else if (API.equals(type)) {
            return RoleScope.API;
        } else {
            throw new TechnicalManagementException("the MembershipType is not associated to a RoleScope");
        }
    }

    private void createAuditLog(Audit.AuditEvent event, Date date, Membership oldValue, Membership newValue) {
        MembershipReferenceType referenceType = oldValue != null ? oldValue.getReferenceType() : newValue.getReferenceType();
        String referenceId = oldValue != null ? oldValue.getReferenceId() : newValue.getReferenceId();
        String username = oldValue != null ? oldValue.getUserId() : newValue.getUserId();

        Map<Audit.AuditProperties, String> properties = new HashMap<>();
        properties.put(Audit.AuditProperties.USER, username);
        switch (referenceType) {
            case API:
                auditService.createApiAuditLog(
                        referenceId,
                        properties,
                        event,
                        date,
                        oldValue,
                        newValue);
                break;
            case APPLICATION:
                auditService.createApplicationAuditLog(
                        referenceId,
                        properties,
                        event,
                        date,
                        oldValue,
                        newValue);
                break;
            case GROUP:
                properties.put(Audit.AuditProperties.GROUP, referenceId);
                auditService.createPortalAuditLog(
                        properties,
                        event,
                        date,
                        oldValue,
                        newValue);
                break;
            default:
                auditService.createPortalAuditLog(
                        properties,
                        event,
                        date,
                        oldValue,
                        newValue);
                break;
        }
    }
}
