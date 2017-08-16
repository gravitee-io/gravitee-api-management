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

import com.google.common.collect.ImmutableMap;
import io.gravitee.management.model.*;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
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

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MembershipServiceImpl extends TransactionalService implements MembershipService {

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
    public MemberEntity getMember(MembershipReferenceType referenceType, String referenceId, String username, RoleScope roleScope) {
        try {
            LOGGER.debug("Get membership for {} {} and user {}", referenceType, referenceId, username);

            Optional<Membership> optionalMembership = membershipRepository.findById(username, referenceType, referenceId);

            return optionalMembership.
                    map(m -> convert(m, roleScope)).
                    orElse(null);

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for {} {} and user", referenceType, referenceId, username, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referenceId + " and user " + username, ex);
        }
    }

    @Override
    public RoleEntity getRole(MembershipReferenceType referenceType, String referenceId, String username, RoleScope roleScope) {
        try {
            if (referenceId == null) {
                throw new IllegalArgumentException("You must provide a referenceId !");
            }
            LOGGER.debug("Get role for {} {} and user {}", referenceType, referenceId, username);

            Optional<Membership> optionalMembership = membershipRepository.findById(username, referenceType, referenceId);

            return optionalMembership.
                    filter(m -> m.getRoles().get(roleScope.getId()) != null).
                    map(m -> roleService.findById(roleScope, m.getRoles().get(roleScope.getId()))).
                    orElse(null);

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for {} {} and user", referenceType, referenceId, username, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referenceId + " and user " + username, ex);
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

    @Override
    public MemberEntity addOrUpdateMember(MembershipReferenceType referenceType, String referenceId, String username, RoleScope roleScope, String roleName) {
        try {
            LOGGER.debug("Add a new member for {} {}", referenceType, referenceId);

            RoleEntity roleEntity = roleService.findById(roleScope, roleName);
            if (MembershipReferenceType.API.equals(referenceType)
                    && !io.gravitee.management.model.permissions.RoleScope.API.equals(roleEntity.getScope())) {
                throw new NotAuthorizedMembershipException(roleName);
            } else if (MembershipReferenceType.APPLICATION.equals(referenceType)
                    && !io.gravitee.management.model.permissions.RoleScope.APPLICATION.equals(roleEntity.getScope())) {
                throw new NotAuthorizedMembershipException(roleName);
            } else if (MembershipReferenceType.GROUP.equals(referenceType)
                    && !io.gravitee.management.model.permissions.RoleScope.APPLICATION.equals(roleEntity.getScope())
                    && !io.gravitee.management.model.permissions.RoleScope.API.equals(roleEntity.getScope())) {
                throw new NotAuthorizedMembershipException(roleName);
            }
            UserEntity user;

            try {
                user = userService.findByName(username, false);
            } catch (UserNotFoundException unfe) {
                // User does not exist so we are looking into defined providers
                io.gravitee.management.model.providers.User providerUser = identityService.findOne(username);
                if (providerUser != null) {
                    // Information will be updated after the first connection of the user
                    NewExternalUserEntity newUser = new NewExternalUserEntity();
                    newUser.setUsername(username);
                    newUser.setFirstname(providerUser.getFirstname());
                    newUser.setLastname(providerUser.getLastname());
                    newUser.setEmail(providerUser.getEmail());
                    newUser.setSource(providerUser.getSource());
                    newUser.setSourceId(providerUser.getSourceId());

                    user = userService.create(newUser, true);
                } else {
                    throw new UserNotFoundException(username);
                }
            }

            Optional<Membership> optionalMembership =
                    membershipRepository.findById(username, referenceType, referenceId);
            Date updateDate = new Date();
            Membership returnedMembership;
            if (optionalMembership.isPresent()) {
                optionalMembership.get().getRoles().put(roleScope.getId(), roleName);
                optionalMembership.get().setUpdatedAt(updateDate);
                returnedMembership = membershipRepository.update(optionalMembership.get());
            } else {
                Membership membership = new Membership(username, referenceId, referenceType);
                membership.setRoles(Collections.singletonMap(roleScope.getId(), roleName));
                membership.setCreatedAt(updateDate);
                membership.setUpdatedAt(updateDate);
                returnedMembership = membershipRepository.create(membership);
            }

            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                EmailNotification emailNotification = buildEmailNotification(user, referenceType, referenceId);
                if (emailNotification != null) {
                    emailService.sendAsyncEmailNotification(emailNotification);
                }
            }

            return convert(returnedMembership, roleScope);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to add member for {} {}", referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to add member for " + referenceType + " " + referenceId, ex);
        }
    }

    @Override
    public void deleteMember(MembershipReferenceType referenceType, String referenceId, String username) {
        try {
            LOGGER.debug("Delete member {} for {} {}", username, referenceType, referenceId);
            if (!MembershipReferenceType.GROUP.equals(referenceType)) {
                RoleScope roleScope = getScopeByMembershipReferenceType(referenceType);

                RoleEntity roleEntity = this.getRole(referenceType, referenceId, username, roleScope);
                if (roleEntity != null && PRIMARY_OWNER.name().equals(roleEntity.getName())) {
                    throw new SinglePrimaryOwnerException(
                            referenceType.equals(MembershipReferenceType.API)
                                    ? RoleScope.API : RoleScope.APPLICATION
                    );
                }
            }
            membershipRepository.delete(new Membership(username, referenceId, referenceType));

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete member {} for {} {}", username, referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete member " + username + " for " + referenceType + " " + referenceId, ex);
        }
    }

    @Override
    public void transferApiOwnership(String apiId, String username) {
        RoleEntity defaultRole = roleService.findDefaultRoleByScopes(RoleScope.API).get(0);
        this.getMembers(MembershipReferenceType.API, apiId, RoleScope.API, PRIMARY_OWNER.name())
                .forEach(m -> this.addOrUpdateMember(MembershipReferenceType.API, apiId, m.getUsername(), RoleScope.API, defaultRole.getName()));
        // set the new primary owner
        this.addOrUpdateMember(MembershipReferenceType.API, apiId, username, RoleScope.API, PRIMARY_OWNER.name());
    }

    @Override
    public Map<String, char[]> getMemberPermissions(ApiEntity api, String username) {
        return getMemberPermissions(MembershipReferenceType.API,
                api.getId(),
                username,
                api.getGroups(),
                RoleScope.API);
    }

    @Override
    public Map<String, char[]> getMemberPermissions(ApplicationEntity application, String username) {
        return getMemberPermissions(MembershipReferenceType.APPLICATION,
                application.getId(),
                username,
                application.getGroups(),
                RoleScope.APPLICATION);
    }

    @Override
    public boolean removeRole(MembershipReferenceType referenceType, String referenceId, String username, RoleScope roleScope) {
        try {
            Optional<Membership> optionalMembership = membershipRepository.findById(username, referenceType, referenceId);
            if (optionalMembership.isPresent()) {
                Membership membership = optionalMembership.get();
                membership.getRoles().remove(roleScope.getId());
                if (membership.getRoles().isEmpty()) {
                    throw new MemberWithoutRoleException(membership.getUserId());
                } else {
                    membershipRepository.update(membership);
                    return true;
                }
            }
            return false;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for {} {} and user", referenceType, referenceId, username, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referenceId + " and user " + username, ex);
        }
    }

    private Map<String, char[]> getMemberPermissions(MembershipReferenceType membershipReferenceType, String referenceId, String username, Set<String> groups, RoleScope roleScope) {
        MemberEntity member = this.getMember(membershipReferenceType, referenceId, username, roleScope);
        if (member != null) {
            return member.getPermissions();
        } else if (groups != null) {
            Map<String, Set<Character>> mergedPermissions = new HashMap<>();
            for (String groupid : groups) {
                member = this.getMember(MembershipReferenceType.GROUP, groupid, username, roleScope);
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
            Map<String, char[]> permissions = new HashMap<>(mergedPermissions.size());
            mergedPermissions.forEach((String k, Set<Character> v) -> {
                Character[] characters = v.toArray(new Character[v.size()]);
                char[] chars = new char[characters.length];
                for (int i = 0; i < characters.length; i++) {
                    chars[i] = characters[i];
                }
                permissions.put(k, chars);
            });
            return permissions;
        }
        return Collections.emptyMap();
    }

    private MemberEntity convert(Membership membership, RoleScope roleScope) {
        final MemberEntity member = new MemberEntity();

        final UserEntity userEntity = userService.findByName(membership.getUserId(), false);
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
        member.setUsername(userEntity.getUsername());
        member.setCreatedAt(membership.getCreatedAt());
        member.setUpdatedAt(membership.getUpdatedAt());
        member.setRole(role.getName());
        member.setFirstname(userEntity.getFirstname());
        member.setLastname(userEntity.getLastname());
        member.setEmail(userEntity.getEmail());

        return member;
    }

    private EmailNotification buildEmailNotification(UserEntity user, MembershipReferenceType referenceType, String referenceId) {
        String subject = null;
        EmailNotificationBuilder.EmailTemplate template = null;
        Map params = null;
        GroupEntity groupEntity;
        switch (referenceType) {
            case APPLICATION:
                ApplicationEntity applicationEntity = applicationService.findById(referenceId);
                subject = "Subscription to application " + applicationEntity.getName();
                template = EmailNotificationBuilder.EmailTemplate.APPLICATION_MEMBER_SUBSCRIPTION;
                params = ImmutableMap.of("application", applicationEntity, "username", user.getUsername());
                break;
            case API:
                ApiEntity apiEntity = apiService.findById(referenceId);
                subject = "Subscription to API " + apiEntity.getName();
                template = EmailNotificationBuilder.EmailTemplate.API_MEMBER_SUBSCRIPTION;
                params = ImmutableMap.of("api", apiEntity, "username", user.getUsername());
                break;
            case GROUP:
                groupEntity = groupService.findById(referenceId);
                subject = "Subscription to group " + groupEntity.getName();
                template = EmailNotificationBuilder.EmailTemplate.GROUP_MEMBER_SUBSCRIPTION;
                params = ImmutableMap.of("group", groupEntity, "username", user.getUsername());
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
        } else if (MembershipReferenceType.APPLICATION.equals(type)) {
            return RoleScope.APPLICATION;
        } else if (MembershipReferenceType.API.equals(type)) {
            return RoleScope.API;
        } else {
            throw new TechnicalManagementException("the MembershipType is not associated to a RoleScope");
        }
    }
}
