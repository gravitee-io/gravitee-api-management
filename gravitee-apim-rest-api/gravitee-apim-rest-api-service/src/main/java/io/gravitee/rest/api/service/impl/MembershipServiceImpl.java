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

import static io.gravitee.repository.management.model.Membership.AuditEvent.MEMBERSHIP_CREATED;
import static io.gravitee.repository.management.model.Membership.AuditEvent.MEMBERSHIP_DELETED;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.alert.ApplicationAlertMembershipEvent;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.providers.User;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MembershipServiceImpl extends AbstractService implements MembershipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MembershipServiceImpl.class);

    private static final String DEFAULT_SOURCE = "system";

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
    private ApplicationAlertService applicationAlertService;

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
    private EventManager eventManager;

    private final Cache<String, Set<RoleEntity>> roles = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    @Override
    public MemberEntity addRoleToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String role
    ) {
        return addRoleToMemberOnReference(
            organizationId,
            environmentId,
            referenceType,
            referenceId,
            memberType,
            memberId,
            role,
            DEFAULT_SOURCE
        );
    }

    @Override
    public MemberEntity addRoleToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String role,
        String source
    ) {
        RoleEntity roleToAdd = roleService.findById(role);
        return _addRoleToMemberOnReference(
            new MembershipReference(referenceType, referenceId),
            new MembershipMember(memberId, null, memberType),
            new MembershipRole(roleToAdd.getScope(), roleToAdd.getName()),
            source,
            true,
            false,
            environmentId,
            organizationId
        );
    }

    @Override
    public MemberEntity addRoleToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReference reference,
        MembershipMember member,
        MembershipRole role
    ) {
        return _addRoleToMemberOnReference(reference, member, role, DEFAULT_SOURCE, true, false, environmentId, organizationId);
    }

    private MemberEntity _addRoleToMemberOnReference(
        MembershipReference reference,
        MembershipMember member,
        MembershipRole role,
        String source,
        boolean notify,
        boolean update,
        final String environmentId,
        final String organizationId
    ) {
        try {
            LOGGER.debug("Add a new member for {} {}", reference.getType(), reference.getId());

            Optional<RoleEntity> optRoleEntity = roleService.findByScopeAndName(role.getScope(), role.getName());
            if (optRoleEntity.isPresent()) {
                RoleEntity roleEntity = optRoleEntity.get();

                assertRoleScopeAllowedForReference(reference, roleEntity);
                assertRoleNameAllowedForReference(reference, roleEntity);

                if (member.getMemberId() != null) {
                    Set<io.gravitee.repository.management.model.Membership> similarMemberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
                        member.getMemberId(),
                        convert(member.getMemberType()),
                        convert(reference.getType()),
                        reference.getId(),
                        roleEntity.getId()
                    );

                    if (!similarMemberships.isEmpty()) {
                        if (update) {
                            UserEntity userEntity = findUserFromMembershipMember(member, organizationId, environmentId);
                            return getUserMember(environmentId, reference.getType(), reference.getId(), userEntity.getId());
                        } else {
                            throw new MembershipAlreadyExistsException(
                                member.getMemberId(),
                                member.getMemberType(),
                                reference.getId(),
                                reference.getType()
                            );
                        }
                    }
                }
                Date updateDate = new Date();
                MemberEntity userMember = null;
                if (member.getMemberType() == MembershipMemberType.USER) {
                    UserEntity userEntity = findUserFromMembershipMember(member, organizationId, environmentId);
                    io.gravitee.repository.management.model.Membership membership = new io.gravitee.repository.management.model.Membership(
                        UuidString.generateRandom(),
                        userEntity.getId(),
                        convert(member.getMemberType()),
                        reference.getId(),
                        convert(reference.getType()),
                        roleEntity.getId()
                    );
                    membership.setSource(source);
                    membership.setCreatedAt(updateDate);
                    membership.setUpdatedAt(updateDate);
                    membershipRepository.create(membership);
                    createAuditLog(MEMBERSHIP_CREATED, membership.getCreatedAt(), null, membership, environmentId, organizationId);

                    if (MembershipReferenceType.APPLICATION.equals(reference.getType())) {
                        applicationAlertService.addMemberToApplication(environmentId, reference.getId(), userEntity.getEmail());
                    }

                    Set<io.gravitee.repository.management.model.Membership> userRolesOnReference = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                        userEntity.getId(),
                        convert(member.getMemberType()),
                        convert(reference.getType()),
                        reference.getId()
                    );
                    boolean shouldNotify =
                        notify &&
                        userRolesOnReference != null &&
                        userRolesOnReference.size() == 1 &&
                        userEntity.getEmail() != null &&
                        !userEntity.getEmail().isEmpty();
                    if (shouldNotify) {
                        if (MembershipReferenceType.GROUP.equals(reference.getType())) {
                            final GroupEntity group = groupService.findById(environmentId, reference.getId());
                            shouldNotify = !group.isDisableMembershipNotifications();
                        } else if (MembershipReferenceType.API.equals(reference.getType())) {
                            final ApiEntity api = apiService.findById(reference.getId());
                            shouldNotify = !api.isDisableMembershipNotifications();
                        } else if (MembershipReferenceType.APPLICATION.equals(reference.getType())) {
                            final ApplicationEntity application = applicationService.findById(environmentId, reference.getId());
                            shouldNotify = !application.isDisableMembershipNotifications();
                        }
                    }

                    if (shouldNotify) {
                        EmailNotification emailNotification = buildEmailNotification(
                            userEntity,
                            reference.getType(),
                            reference.getId(),
                            environmentId
                        );
                        if (emailNotification != null) {
                            GraviteeContext.ReferenceContext context = environmentId != null
                                ? new GraviteeContext.ReferenceContext(environmentId, GraviteeContext.ReferenceContextType.ENVIRONMENT)
                                : new GraviteeContext.ReferenceContext(organizationId, GraviteeContext.ReferenceContextType.ORGANIZATION);
                            emailService.sendAsyncEmailNotification(emailNotification, context);
                        }
                    }

                    userMember = getUserMember(environmentId, reference.getType(), reference.getId(), userEntity.getId());
                } else {
                    io.gravitee.repository.management.model.Membership membership = new io.gravitee.repository.management.model.Membership(
                        UuidString.generateRandom(),
                        member.getMemberId(),
                        convert(member.getMemberType()),
                        reference.getId(),
                        convert(reference.getType()),
                        roleEntity.getId()
                    );
                    membership.setSource(source);
                    membership.setCreatedAt(updateDate);
                    membership.setUpdatedAt(updateDate);
                    membershipRepository.create(membership);
                    createAuditLog(MEMBERSHIP_CREATED, membership.getCreatedAt(), null, membership, environmentId, organizationId);
                }

                roles.invalidate(reference.getType().name() + reference.getId() + member.getMemberType() + member.getMemberId());

                return userMember;
            } else {
                throw new RoleNotFoundException(role.getScope().name() + "_" + role.getName());
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to add member for {} {}", reference.getType(), reference.getId(), ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to add member for " + reference.getType() + " " + reference.getId(),
                ex
            );
        }
    }

    private void createAuditLog(
        Audit.AuditEvent event,
        Date date,
        io.gravitee.repository.management.model.Membership oldValue,
        io.gravitee.repository.management.model.Membership newValue,
        final String environmentId,
        final String organizationId
    ) {
        io.gravitee.repository.management.model.MembershipReferenceType referenceType = oldValue != null
            ? oldValue.getReferenceType()
            : newValue.getReferenceType();
        String referenceId = oldValue != null ? oldValue.getReferenceId() : newValue.getReferenceId();
        String username = oldValue != null ? oldValue.getMemberId() : newValue.getMemberId();

        Map<Audit.AuditProperties, String> properties = new HashMap<>();
        properties.put(Audit.AuditProperties.USER, username);
        switch (referenceType) {
            case API:
                auditService.createApiAuditLog(referenceId, properties, event, date, oldValue, newValue);
                break;
            case APPLICATION:
                auditService.createApplicationAuditLog(referenceId, properties, event, date, oldValue, newValue);
                break;
            case GROUP:
                properties.put(Audit.AuditProperties.GROUP, referenceId);
                auditService.createEnvironmentAuditLog(environmentId, properties, event, date, oldValue, newValue);
                break;
            case ENVIRONMENT:
                auditService.createEnvironmentAuditLog(environmentId, properties, event, date, oldValue, newValue);
                break;
            case ORGANIZATION:
                auditService.createOrganizationAuditLog(organizationId, properties, event, date, oldValue, newValue);
                break;
        }
    }

    private EmailNotification buildEmailNotification(
        UserEntity user,
        MembershipReferenceType referenceType,
        String referenceId,
        final String environmentId
    ) {
        EmailNotificationBuilder.EmailTemplate template = null;
        Map<String, Object> params = null;
        GroupEntity groupEntity;
        NotificationParamsBuilder paramsBuilder = new NotificationParamsBuilder();
        switch (referenceType) {
            case APPLICATION:
                ApplicationEntity applicationEntity = applicationService.findById(environmentId, referenceId);
                template = EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_APPLICATION_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.application(applicationEntity).user(user).build();
                break;
            case API:
                ApiEntity apiEntity = apiService.findById(referenceId);
                template = EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_API_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.api(apiEntity).user(user).build();
                break;
            case GROUP:
                groupEntity = groupService.findById(environmentId, referenceId);
                template = EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_GROUP_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.group(groupEntity).user(user).build();
                break;
            default:
                break;
        }

        if (template == null) {
            return null;
        }

        return new EmailNotificationBuilder().to(user.getEmail()).template(template).params(params).build();
    }

    private MemberEntity convertToMemberEntity(io.gravitee.repository.management.model.Membership membership) {
        final MemberEntity member = new MemberEntity();
        member.setId(membership.getMemberId());
        member.setCreatedAt(membership.getCreatedAt());
        member.setUpdatedAt(membership.getUpdatedAt());
        member.setReferenceId(membership.getReferenceId());
        member.setReferenceType(convert(membership.getReferenceType()));
        if (membership.getRoleId() != null) {
            RoleEntity role = roleService.findById(membership.getRoleId());
            member.setPermissions(role.getPermissions());
            List<RoleEntity> roles = new ArrayList<>();
            roles.add(role);
            member.setRoles(roles);
        }
        member.setType(MembershipMemberType.valueOf(membership.getMemberType().name()));

        return member;
    }

    private void fillMemberUserInformation(
        Set<io.gravitee.repository.management.model.Membership> memberships,
        List<MemberEntity> members
    ) {
        if (memberships != null && !memberships.isEmpty()) {
            final Set<UserEntity> userEntities = new HashSet<>();
            final List<String> userIds = members
                .stream()
                .filter(m -> m.getType() == MembershipMemberType.USER)
                .map(MemberEntity::getId)
                .collect(toList());
            if (userIds != null && userIds.size() > 0) {
                userEntities.addAll(userService.findByIds(userIds, false));
            }

            final Set<GroupEntity> groupEntities = new HashSet<>();
            final Set<String> groupsIds = members
                .stream()
                .filter(m -> m.getType() == MembershipMemberType.GROUP)
                .map(MemberEntity::getId)
                .collect(toSet());
            if (groupsIds != null && groupsIds.size() > 0) {
                groupEntities.addAll(groupService.findByIds(groupsIds));
            }

            members.forEach(
                m -> {
                    Optional<io.gravitee.repository.management.model.Membership> membership = memberships
                        .stream()
                        .filter(ms -> ms.getMemberId().equals(m.getId()))
                        .findFirst();
                    membership.ifPresent(
                        ms -> {
                            if (ms.getMemberType() == io.gravitee.repository.management.model.MembershipMemberType.USER) {
                                Optional<UserEntity> user = userEntities
                                    .stream()
                                    .filter(u -> u.getId().equals(ms.getMemberId()))
                                    .findFirst();
                                user.ifPresent(
                                    u -> {
                                        m.setDisplayName(u.getDisplayName());
                                        m.setEmail(u.getEmail());
                                    }
                                );
                            } else {
                                Optional<GroupEntity> group = groupEntities
                                    .stream()
                                    .filter(u -> u.getId().equals(ms.getMemberId()))
                                    .findFirst();
                                group.ifPresent(g -> m.setDisplayName(g.getName()));
                            }
                        }
                    );
                }
            );
        }
    }

    private UserEntity findUserFromMembershipMember(MembershipMember member, final String organizationId, final String environmentId) {
        UserEntity userEntity;
        if (member.getMemberId() != null) {
            userEntity = userService.findById(member.getMemberId());
        } else {
            // We have a user reference, meaning that the user is coming from an external system
            // User does not exist so we are looking into defined providers
            Optional<io.gravitee.rest.api.model.providers.User> providerUser = identityService.findByReference(member.getReference());
            if (providerUser.isPresent()) {
                User identityUser = providerUser.get();
                userEntity = findOrCreateUser(identityUser, organizationId, environmentId);
            } else {
                throw new UserNotFoundException(member.getReference());
            }
        }
        return userEntity;
    }

    private UserEntity findOrCreateUser(User identityUser, final String organizationId, final String environmentId) {
        UserEntity userEntity;
        try {
            userEntity = userService.findBySource(identityUser.getSource(), identityUser.getSourceId(), false);
        } catch (UserNotFoundException unfe) {
            // The user is not yet registered in repository
            // Information will be updated after the first connection of the user
            NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setFirstname(identityUser.getFirstname());
            newUser.setLastname(identityUser.getLastname());
            newUser.setSource(identityUser.getSource());
            newUser.setEmail(identityUser.getEmail());
            newUser.setSourceId(identityUser.getSourceId());
            newUser.setPicture(identityUser.getPicture());
            if (identityUser.getRoles() == null || identityUser.getRoles().isEmpty()) {
                userEntity = userService.create(newUser, true);
            } else {
                userEntity = userService.create(newUser, false);
                for (Map.Entry<String, String> role : identityUser.getRoles().entrySet()) {
                    MembershipReferenceType membershipReferenceType = MembershipReferenceType.valueOf(role.getKey());
                    MembershipReference reference = null;
                    if (membershipReferenceType == MembershipReferenceType.ORGANIZATION) {
                        reference = new MembershipReference(membershipReferenceType, organizationId);
                    } else if (membershipReferenceType == MembershipReferenceType.ENVIRONMENT) {
                        reference = new MembershipReference(membershipReferenceType, environmentId);
                    }

                    if (reference != null) {
                        this.addRoleToMemberOnReference(
                                organizationId,
                                environmentId,
                                reference,
                                new MembershipMember(userEntity.getId(), null, MembershipMemberType.USER),
                                new MembershipRole(RoleScope.valueOf(role.getKey()), role.getValue())
                            );
                    }
                }
            }
        }
        return userEntity;
    }

    /**
     * assert that the role's scope is allowed for the given reference
     */
    private void assertRoleScopeAllowedForReference(MembershipReference reference, RoleEntity roleEntity) {
        if (
            (MembershipReferenceType.API == reference.getType() && RoleScope.API != roleEntity.getScope()) ||
            (MembershipReferenceType.APPLICATION == reference.getType() && RoleScope.APPLICATION != roleEntity.getScope()) ||
            (
                MembershipReferenceType.GROUP == reference.getType() &&
                RoleScope.GROUP != roleEntity.getScope() &&
                RoleScope.API != roleEntity.getScope() &&
                RoleScope.APPLICATION != roleEntity.getScope()
            )
        ) {
            throw new NotAuthorizedMembershipException(roleEntity.getName());
        }
    }

    /**
     * assert that the roleEntity's name is allowed for the given reference
     */
    public void assertRoleNameAllowedForReference(MembershipReference reference, RoleEntity roleEntity) throws TechnicalException {
        if (MembershipReferenceType.GROUP == reference.getType() && SystemRole.PRIMARY_OWNER.name().equals(roleEntity.getName())) {
            if (roleEntity.getScope() == RoleScope.APPLICATION) {
                throw new NotAuthorizedMembershipException(roleEntity.getName());
            } else if (roleEntity.getScope() == RoleScope.API) {
                if (
                    membershipRepository
                        .findByReferenceAndRoleId(
                            io.gravitee.repository.management.model.MembershipReferenceType.GROUP,
                            reference.getId(),
                            roleEntity.getId()
                        )
                        .size() >
                    0
                ) {
                    throw new NotAuthorizedMembershipException(roleEntity.getName());
                }
            }
        }
    }

    @Override
    public void deleteMembership(final String organizationId, final String environmentId, String membershipId) {
        try {
            Optional<io.gravitee.repository.management.model.Membership> membership = membershipRepository.findById(membershipId);
            if (membership.isPresent()) {
                LOGGER.debug("Delete membership {}", membership.get());
                membershipRepository.delete(membershipId);
                createAuditLog(MEMBERSHIP_DELETED, new Date(), membership.get(), null, environmentId, organizationId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete membership {}", membershipId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete membership " + membershipId, ex);
        }
    }

    @Override
    public void deleteReference(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId
    ) {
        try {
            Set<io.gravitee.repository.management.model.Membership> memberships = membershipRepository.findByReferenceAndRoleId(
                convert(referenceType),
                referenceId,
                null
            );
            if (!memberships.isEmpty()) {
                for (io.gravitee.repository.management.model.Membership membership : memberships) {
                    LOGGER.debug("Delete membership {}", membership.getId());
                    membershipRepository.delete(membership.getId());
                    createAuditLog(MEMBERSHIP_DELETED, new Date(), membership, null, environmentId, organizationId);
                }
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete memberships for {} {}", referenceType, referenceId, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to delete memberships for " + referenceType + " " + referenceId,
                ex
            );
        }
    }

    @Override
    public void deleteReferenceMember(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId
    ) {
        deleteReferenceMemberBySource(organizationId, environmentId, referenceType, referenceId, memberType, memberId, null);
    }

    @Override
    public void deleteReferenceMemberBySource(
        final String organizationId,
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String sourceId
    ) {
        try {
            final Optional<RoleEntity> optApiPORole = roleService.findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name());
            Set<io.gravitee.repository.management.model.Membership> memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                memberId,
                convert(memberType),
                convert(referenceType),
                referenceId
            );
            if (!memberships.isEmpty()) {
                for (io.gravitee.repository.management.model.Membership membership : memberships) {
                    if (sourceId == null || membership.getSource().equals(sourceId)) {
                        LOGGER.debug("Delete membership {}", membership.getId());
                        membershipRepository.delete(membership.getId());
                        createAuditLog(MEMBERSHIP_DELETED, new Date(), membership, null, environmentId, organizationId);
                    }

                    if (MembershipReferenceType.APPLICATION.equals(referenceType) && MembershipMemberType.USER.equals(memberType)) {
                        UserEntity userEntity = findUserFromMembershipMember(
                            new MembershipMember(memberId, null, memberType),
                            organizationId,
                            environmentId
                        );
                        applicationAlertService.deleteMemberFromApplication(environmentId, referenceId, userEntity.getEmail());
                    }

                    //if the API Primary owner of a group has been deleted, we must update the apiPrimaryOwnerField of this group
                    if (
                        optApiPORole.isPresent() &&
                        membership.getReferenceType() == io.gravitee.repository.management.model.MembershipReferenceType.GROUP &&
                        membership.getRoleId().equals(optApiPORole.get().getId())
                    ) {
                        groupService.updateApiPrimaryOwner(membership.getReferenceId(), null);
                    }
                }
            }
        } catch (TechnicalException ex) {
            LOGGER.error(
                "An error occurs while trying to delete memberships for {} {} {} {}",
                referenceType,
                referenceId,
                memberType,
                memberId,
                ex
            );
            throw new TechnicalManagementException(
                "An error occurs while trying to delete memberships for " +
                referenceType +
                " " +
                referenceId +
                " " +
                memberType +
                " " +
                memberId,
                ex
            );
        }
    }

    @Override
    public List<UserMembership> findUserMembershipBySource(MembershipReferenceType type, String userId, String sourceId) {
        try {
            Map<String, RoleEntity> roleMap = roleService.findAll().stream().collect(Collectors.toMap(RoleEntity::getId, r -> r));
            HashMap<Integer, UserMembership> userMembershipMap = new HashMap<>();
            membershipRepository
                .findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
                    userId,
                    io.gravitee.repository.management.model.MembershipMemberType.USER,
                    convert(type),
                    sourceId
                )
                .stream()
                .filter(membership -> sourceId != null && sourceId.equals(membership.getSource()))
                .forEach(
                    membership -> {
                        UserMembership userMembership = new UserMembership();
                        userMembership.setType(type.name());
                        userMembership.setReference(membership.getReferenceId());
                        userMembership.setSource(membership.getSource());
                        RoleEntity role = roleMap.get(membership.getRoleId());
                        if (role != null) {
                            int key = userMembership.hashCode();
                            if (userMembershipMap.containsKey(key)) {
                                userMembershipMap.get(key).getRoles().put(role.getScope().name(), role.getName());
                            } else {
                                HashMap<String, String> roles = new HashMap<>();
                                roles.put(role.getScope().name(), role.getName());
                                userMembership.setRoles(roles);
                                userMembershipMap.put(userMembership.hashCode(), userMembership);
                            }
                        }
                    }
                );

            return new ArrayList<>(userMembershipMap.values());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove user {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove user " + userId, ex);
        }
    }

    @Override
    public List<UserMembership> findUserMembership(MembershipReferenceType type, String userId) {
        if (
            type == null ||
            (
                !type.equals(MembershipReferenceType.API) &&
                !type.equals(MembershipReferenceType.APPLICATION) &&
                !type.equals(MembershipReferenceType.GROUP)
            )
        ) {
            return Collections.emptyList();
        }
        try {
            Map<String, RoleEntity> roleMap = roleService
                .findByScope(roleService.findScopeByMembershipReferenceType(type))
                .stream()
                .collect(Collectors.toMap(RoleEntity::getId, r -> r));
            HashMap<Integer, UserMembership> userMembershipMap = new HashMap<>();
            membershipRepository
                .findByMemberIdAndMemberTypeAndReferenceType(
                    userId,
                    io.gravitee.repository.management.model.MembershipMemberType.USER,
                    convert(type)
                )
                .forEach(
                    membership -> {
                        UserMembership userMembership = new UserMembership();
                        userMembership.setType(type.name());
                        userMembership.setReference(membership.getReferenceId());
                        userMembership.setSource(membership.getSource());
                        RoleEntity role = roleMap.get(membership.getRoleId());
                        if (role != null) {
                            int key = userMembership.hashCode();
                            if (userMembershipMap.containsKey(key)) {
                                userMembershipMap.get(key).getRoles().put(role.getScope().name(), role.getName());
                            } else {
                                HashMap<String, String> roles = new HashMap<>();
                                roles.put(role.getScope().name(), role.getName());
                                userMembership.setRoles(roles);
                                userMembershipMap.put(userMembership.hashCode(), userMembership);
                            }
                        }
                    }
                );
            Set<UserMembership> userMemberships = new HashSet<>(userMembershipMap.values());

            if (type.equals(MembershipReferenceType.APPLICATION) || type.equals(MembershipReferenceType.API)) {
                Set<GroupEntity> userGroups = groupService.findByUser(userId);
                for (GroupEntity group : userGroups) {
                    userMemberships.addAll(
                        membershipRepository
                            .findByMemberIdAndMemberTypeAndReferenceType(
                                group.getId(),
                                io.gravitee.repository.management.model.MembershipMemberType.GROUP,
                                convert(type)
                            )
                            .stream()
                            .map(
                                membership -> {
                                    UserMembership userMembership = new UserMembership();
                                    userMembership.setType(type.name());
                                    userMembership.setReference(membership.getReferenceId());
                                    return userMembership;
                                }
                            )
                            .collect(Collectors.toSet())
                    );
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
        if (
            memberships == null ||
            memberships.isEmpty() ||
            type == null ||
            (
                !type.equals(MembershipReferenceType.API) &&
                !type.equals(MembershipReferenceType.APPLICATION) &&
                !type.equals(MembershipReferenceType.GROUP)
            )
        ) {
            return new Metadata();
        }
        try {
            Metadata metadata = new Metadata();
            if (type.equals(MembershipReferenceType.API)) {
                ApiCriteria.Builder criteria = new ApiCriteria.Builder();
                ApiFieldExclusionFilter filter = (new ApiFieldExclusionFilter.Builder()).excludeDefinition().excludePicture().build();
                criteria.ids(memberships.stream().map(UserMembership::getReference).toArray(String[]::new));
                apiRepository
                    .search(criteria.build(), filter)
                    .forEach(
                        api -> {
                            metadata.put(api.getId(), "name", api.getName());
                            metadata.put(api.getId(), "version", api.getVersion());
                            metadata.put(api.getId(), "visibility", api.getVisibility());
                        }
                    );
            } else if (type.equals(MembershipReferenceType.APPLICATION)) {
                applicationRepository
                    .findByIds(memberships.stream().map(UserMembership::getReference).collect(Collectors.toList()))
                    .forEach(
                        application -> {
                            metadata.put(application.getId(), "name", application.getName());
                        }
                    );
            }
            return metadata;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get user membership metadata", ex);
            throw new TechnicalManagementException("An error occurs while trying to get user membership metadata", ex);
        }
    }

    @Override
    public Page<MemberEntity> getMembersByReference(MembershipReferenceType referenceType, String referenceId, Pageable pageable) {
        return this.getMembersByReferenceAndRole(referenceType, referenceId, null, pageable);
    }

    @Override
    public Set<MemberEntity> getMembersByReference(MembershipReferenceType referenceType, String referenceId) {
        return new HashSet<>(this.getMembersByReferenceAndRole(referenceType, referenceId, null, null).getContent());
    }

    @Override
    public Page<MemberEntity> getMembersByReference(
        MembershipReferenceType referenceType,
        String referenceId,
        String role,
        Pageable pageable
    ) {
        return this.getMembersByReferenceAndRole(referenceType, referenceId, role, pageable);
    }

    @Override
    public Set<MemberEntity> getMembersByReference(MembershipReferenceType referenceType, String referenceId, String role) {
        return new HashSet<>(this.getMembersByReferenceAndRole(referenceType, referenceId, role, null).getContent());
    }

    @Override
    public Page<MemberEntity> getMembersByReferenceAndRole(
        MembershipReferenceType referenceType,
        String referenceId,
        String role,
        Pageable pageable
    ) {
        return this.getMembersByReferencesAndRole(referenceType, Collections.singletonList(referenceId), role, pageable);
    }

    @Override
    public Set<MemberEntity> getMembersByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, String role) {
        return new HashSet<>(
            this.getMembersByReferencesAndRole(referenceType, Collections.singletonList(referenceId), role, null).getContent()
        );
    }

    @Override
    public Set<MemberEntity> getMembersByReferencesAndRole(MembershipReferenceType referenceType, List<String> referenceIds, String role) {
        return new HashSet<>(this.getMembersByReferencesAndRole(referenceType, referenceIds, role, null).getContent());
    }

    @Override
    public Page<MemberEntity> getMembersByReferencesAndRole(
        MembershipReferenceType referenceType,
        List<String> referenceIds,
        String role,
        Pageable pageable
    ) {
        try {
            LOGGER.debug("Get members for {} {}", referenceType, referenceIds);
            Set<io.gravitee.repository.management.model.Membership> memberships = membershipRepository.findByReferencesAndRoleId(
                convert(referenceType),
                referenceIds,
                role
            );
            Map<String, MemberEntity> results = new HashMap<>();
            memberships
                .stream()
                .map(this::convertToMemberEntity)
                .forEach(
                    member -> {
                        String key = member.getId() + member.getReferenceId();
                        MemberEntity existingEntity = results.get(key);
                        if (existingEntity == null) {
                            results.put(key, member);
                            existingEntity = member;
                        } else {
                            Set<RoleEntity> existingRoles = new HashSet<>(existingEntity.getRoles());
                            existingRoles.addAll(member.getRoles());
                            existingEntity.setRoles(new ArrayList<>(existingRoles));
                        }
                    }
                );

            List<MemberEntity> members = new ArrayList<>(results.values());
            fillMemberUserInformation(memberships, members);
            return paginate(results.values(), pageable);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get members for {} {}", referenceType, referenceIds, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to get members for " + referenceType + " " + referenceIds,
                ex
            );
        }
    }

    private Page<MemberEntity> paginate(Collection<MemberEntity> members, Pageable pageable) {
        // Pagination requires sorting members to be able to navigate through pages.
        Comparator<MemberEntity> comparator = Comparator.comparing(memberEntity -> memberEntity.getDisplayName().toLowerCase(Locale.ROOT));

        if (pageable == null) {
            pageable = new PageableImpl(1, Integer.MAX_VALUE);
        }

        int totalCount = members.size();
        int startIndex = (pageable.getPageNumber() - 1) * pageable.getPageSize();

        if (pageable.getPageNumber() < 1 || (totalCount > 0 && startIndex >= totalCount)) {
            throw new PaginationInvalidException();
        }

        List<MemberEntity> subsetApis = members
            .stream()
            .sorted(comparator)
            .skip(startIndex)
            .limit(pageable.getPageSize())
            .collect(toList());

        return new Page<>(subsetApis, pageable.getPageNumber(), pageable.getPageSize(), members.size());
    }

    private io.gravitee.repository.management.model.MembershipReferenceType convert(MembershipReferenceType referenceType) {
        return io.gravitee.repository.management.model.MembershipReferenceType.valueOf(referenceType.name());
    }

    private MembershipReferenceType convert(io.gravitee.repository.management.model.MembershipReferenceType referenceType) {
        return MembershipReferenceType.valueOf(referenceType.name());
    }

    private io.gravitee.repository.management.model.MembershipMemberType convert(MembershipMemberType memberType) {
        return io.gravitee.repository.management.model.MembershipMemberType.valueOf(memberType.name());
    }

    private MembershipMemberType convert(io.gravitee.repository.management.model.MembershipMemberType memberType) {
        return MembershipMemberType.valueOf(memberType.name());
    }

    private MembershipEntity convert(io.gravitee.repository.management.model.Membership membership) {
        MembershipEntity result = new MembershipEntity();
        result.setCreatedAt(membership.getCreatedAt());
        result.setId(membership.getId());
        result.setMemberId(membership.getMemberId());
        result.setMemberType(convert(membership.getMemberType()));
        result.setReferenceId(membership.getReferenceId());
        result.setReferenceType(convert(membership.getReferenceType()));
        result.setRoleId(membership.getRoleId());
        result.setUpdatedAt(membership.getUpdatedAt());
        return result;
    }

    @Override
    public Set<MembershipEntity> getMembershipsByMember(MembershipMemberType memberType, String memberId) {
        try {
            return membershipRepository
                .findByMemberIdAndMemberType(memberId, convert(memberType))
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} ", memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + memberId, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByMemberAndReference(
        MembershipMemberType memberType,
        String memberId,
        MembershipReferenceType referenceType
    ) {
        try {
            return membershipRepository
                .findByMemberIdAndMemberTypeAndReferenceType(memberId, convert(memberType), convert(referenceType))
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} ", memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + memberId, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByMemberAndReferenceAndRole(
        MembershipMemberType memberType,
        String memberId,
        MembershipReferenceType referenceType,
        String role
    ) {
        try {
            return membershipRepository
                .findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(memberId, convert(memberType), convert(referenceType), role)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} ", memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + memberId, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByMemberAndReferenceAndRoleIn(
        MembershipMemberType memberType,
        String memberId,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    ) {
        try {
            return membershipRepository
                .findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn(memberId, convert(memberType), convert(referenceType), roleIds)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} ", memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + memberId, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByMembersAndReference(
        MembershipMemberType memberType,
        List<String> memberIds,
        MembershipReferenceType referenceType
    ) {
        try {
            return membershipRepository
                .findByMemberIdsAndMemberTypeAndReferenceType(memberIds, convert(memberType), convert(referenceType))
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} ", memberIds, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + memberIds, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByReference(MembershipReferenceType referenceType, String referenceId) {
        try {
            return membershipRepository
                .findByReferenceAndRoleId(convert(referenceType), referenceId, null)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} {} ", referenceType, referenceId, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to get memberships for " + referenceType + " " + referenceId,
                ex
            );
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, String role) {
        try {
            return membershipRepository
                .findByReferenceAndRoleId(convert(referenceType), referenceId, role)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} {} and role", referenceType, referenceId, role, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to get memberships for " + referenceType + " " + referenceId + " and role " + role,
                ex
            );
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByReferencesAndRole(
        MembershipReferenceType referenceType,
        List<String> referenceIds,
        String role
    ) {
        try {
            return membershipRepository
                .findByReferencesAndRoleId(convert(referenceType), referenceIds, role)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} {} and role", referenceType, referenceIds, role, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to get memberships for " + referenceType + " " + referenceIds + " and role " + role,
                ex
            );
        }
    }

    @Override
    public MembershipEntity getPrimaryOwner(final String organizationId, MembershipReferenceType referenceType, String referenceId) {
        RoleScope poRoleScope;
        if (referenceType == MembershipReferenceType.API) {
            poRoleScope = RoleScope.API;
        } else if (referenceType == MembershipReferenceType.APPLICATION) {
            poRoleScope = RoleScope.APPLICATION;
        } else {
            throw new RoleNotFoundException(referenceType.name() + "_PRIMARY_OWNER");
        }
        RoleEntity poRole = roleService.findPrimaryOwnerRoleByOrganization(organizationId, poRoleScope);
        if (poRole != null) {
            try {
                Optional<io.gravitee.repository.management.model.Membership> poMember = membershipRepository
                    .findByReferenceAndRoleId(convert(referenceType), referenceId, poRole.getId())
                    .stream()
                    .findFirst();
                if (poMember.isPresent()) {
                    return convert(poMember.get());
                } else {
                    return null;
                }
            } catch (TechnicalException ex) {
                LOGGER.error("An error occurs while trying to get primary owner for {} {} and role", referenceType, referenceId, ex);
                throw new TechnicalManagementException(
                    "An error occurs while trying to get primary owner for " + referenceType + " " + referenceId,
                    ex
                );
            }
        } else {
            throw new RoleNotFoundException(referenceType.name() + "_PRIMARY_OWNER");
        }
    }

    @Override
    public Set<RoleEntity> getRoles(
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId
    ) {
        try {
            LOGGER.debug("Get role for {} {} and member {} {}", referenceType, referenceId, memberType, memberId);

            return roles.get(
                referenceType.name() + referenceId + memberType + memberId,
                () ->
                    membershipRepository
                        .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                            memberId,
                            convert(memberType),
                            convert(referenceType),
                            referenceId
                        )
                        .stream()
                        .map(io.gravitee.repository.management.model.Membership::getRoleId)
                        .map(roleService::findById)
                        .collect(Collectors.toSet())
            );
        } catch (Exception ex) {
            final String message =
                "An error occurs while trying to get roles for " + referenceType + " " + referenceId + " " + memberType + " " + memberId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public MemberEntity getUserMember(
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        String userId
    ) {
        try {
            Set<io.gravitee.repository.management.model.Membership> userMemberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                userId,
                convert(MembershipMemberType.USER),
                convert(referenceType),
                referenceId
            );

            //Get entity groups
            Set<String> entityGroups = new HashSet<>();
            switch (referenceType) {
                case API:
                    entityGroups = apiService.findById(referenceId).getGroups();
                    break;
                case APPLICATION:
                    entityGroups = applicationService.findById(environmentId, referenceId).getGroups();
                    break;
                default:
                    break;
            }

            if (userMemberships.isEmpty() && (entityGroups == null || entityGroups.isEmpty())) {
                return null;
            }

            MemberEntity memberEntity = new MemberEntity();
            UserEntity userEntity = userService.findById(userId);
            memberEntity.setCreatedAt(userEntity.getCreatedAt());
            memberEntity.setDisplayName(userEntity.getDisplayName());
            memberEntity.setEmail(userEntity.getEmail());
            memberEntity.setId(userEntity.getId());
            memberEntity.setUpdatedAt(userEntity.getUpdatedAt());

            Set<RoleEntity> userRoles = new HashSet<>();

            Set<RoleEntity> userDirectRoles = new HashSet<>();
            if (!userMemberships.isEmpty()) {
                userDirectRoles =
                    userMemberships
                        .stream()
                        .map(io.gravitee.repository.management.model.Membership::getRoleId)
                        .map(roleService::findById)
                        .collect(Collectors.toSet());

                userRoles.addAll(userDirectRoles);
            }
            memberEntity.setRoles(new ArrayList<>(userDirectRoles));

            if (entityGroups != null && !entityGroups.isEmpty()) {
                for (String group : entityGroups) {
                    userRoles.addAll(
                        membershipRepository
                            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                                userId,
                                convert(MembershipMemberType.USER),
                                convert(MembershipReferenceType.GROUP),
                                group
                            )
                            .stream()
                            .map(io.gravitee.repository.management.model.Membership::getRoleId)
                            .map(roleService::findById)
                            .filter(role -> role.getScope().name().equals(referenceType.name()))
                            .map(role -> role.isApiPrimaryOwner() ? mapApiPrimaryOwnerRoleToGroupRole(referenceId, group, role) : role)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet())
                    );
                }
            }

            Map<String, char[]> permissions = new HashMap<>();
            if (!userRoles.isEmpty()) {
                permissions = computeGlobalPermissions(userRoles);
            }
            memberEntity.setPermissions(permissions);

            return memberEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get user member for {} {} {} {}", referenceType, referenceId, userId, ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to get roles for " + referenceType + " " + referenceId + " " + userId,
                ex
            );
        }
    }

    private RoleEntity mapApiPrimaryOwnerRoleToGroupRole(String apiId, String groupId, RoleEntity role) {
        PrimaryOwnerEntity apiPrimaryOwner = apiService.getPrimaryOwner(apiId);
        if (apiPrimaryOwner.getId().equals(groupId)) {
            return role;
        }

        GroupEntity userGroup = groupService.findById(GraviteeContext.getCurrentEnvironment(), groupId);
        String groupApiRole = userGroup.getRoles().get(RoleScope.API);

        return groupApiRole == null ? null : roleService.findByScopeAndName(RoleScope.API, groupApiRole).orElse(null);
    }

    private Map<String, char[]> computeGlobalPermissions(Set<RoleEntity> userRoles) {
        Map<String, Set<Character>> mergedPermissions = new HashMap<>();
        for (RoleEntity role : userRoles) {
            for (Map.Entry<String, char[]> perm : role.getPermissions().entrySet()) {
                if (mergedPermissions.containsKey(perm.getKey())) {
                    Set<Character> previousCRUD = mergedPermissions.get(perm.getKey());
                    for (char c : perm.getValue()) {
                        previousCRUD.add(c);
                    }
                } else {
                    Set<Character> crudAsSet = new HashSet<>();
                    for (char c : perm.getValue()) {
                        crudAsSet.add(c);
                    }
                    mergedPermissions.put(perm.getKey(), crudAsSet);
                }
            }
        }
        Map<String, char[]> permissions = new HashMap<>(mergedPermissions.size());
        mergedPermissions.forEach(
            (String k, Set<Character> v) -> {
                Character[] characters = v.toArray(new Character[v.size()]);
                char[] chars = new char[characters.length];
                for (int i = 0; i < characters.length; i++) {
                    chars[i] = characters[i];
                }
                permissions.put(k, chars);
            }
        );
        return permissions;
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(
        final String environmentId,
        MembershipReferenceType referenceType,
        String referenceId,
        String userId
    ) {
        MemberEntity member = this.getUserMember(environmentId, referenceType, referenceId, userId);
        if (member != null) {
            return member.getPermissions();
        }
        return emptyMap();
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(final String environmentId, ApiEntity api, String userId) {
        return getUserMemberPermissions(environmentId, MembershipReferenceType.API, api.getId(), userId);
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(final String environmentId, ApplicationEntity application, String userId) {
        return getUserMemberPermissions(environmentId, MembershipReferenceType.APPLICATION, application.getId(), userId);
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(final String environmentId, GroupEntity group, String userId) {
        return getUserMemberPermissions(environmentId, MembershipReferenceType.GROUP, group.getId(), userId);
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(final String environmentId, EnvironmentEntity environment, String userId) {
        return getUserMemberPermissions(environmentId, MembershipReferenceType.ENVIRONMENT, environment.getId(), userId);
    }

    @Override
    public void removeRole(
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String roleId
    ) {
        try {
            Set<io.gravitee.repository.management.model.Membership> membershipsToDelete = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
                memberId,
                convert(memberType),
                convert(referenceType),
                referenceId,
                roleId
            );
            for (io.gravitee.repository.management.model.Membership m : membershipsToDelete) {
                membershipRepository.delete(m.getId());
            }
        } catch (TechnicalException ex) {
            LOGGER.error(
                "An error occurs while trying to remove role {} from member {} {} for {} {}",
                roleId,
                memberType,
                memberId,
                referenceType,
                referenceId,
                ex
            );
            throw new TechnicalManagementException(
                "An error occurs while trying to remove role " +
                roleId +
                " from member " +
                memberType +
                " " +
                memberId +
                " for " +
                referenceType +
                " " +
                referenceId,
                ex
            );
        }
    }

    @Override
    public void removeRoleUsage(String oldRoleId, String newRoleId) {
        try {
            Set<io.gravitee.repository.management.model.Membership> membershipsWithOldRole = membershipRepository.findByRoleId(oldRoleId);
            for (io.gravitee.repository.management.model.Membership membership : membershipsWithOldRole) {
                Set<io.gravitee.repository.management.model.Membership> membershipsWithNewRole = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
                    membership.getMemberId(),
                    membership.getMemberType(),
                    membership.getReferenceType(),
                    membership.getReferenceId(),
                    newRoleId
                );
                String oldMembershipId = membership.getId();
                if (membershipsWithNewRole.isEmpty()) {
                    membership.setId(UuidString.generateRandom());
                    membership.setRoleId(newRoleId);
                    membership.setSource(DEFAULT_SOURCE);
                    membershipRepository.create(membership);
                }
                membershipRepository.delete(oldMembershipId);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove role {} {}", oldRoleId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove role " + oldRoleId, ex);
        }
    }

    @Override
    public void removeMemberMemberships(MembershipMemberType memberType, String memberId) {
        Set<String> applicationIds = new HashSet<>();
        Set<String> groupIds = new HashSet<>();
        try {
            for (io.gravitee.repository.management.model.Membership membership : membershipRepository.findByMemberIdAndMemberType(
                memberId,
                convert(memberType)
            )) {
                if (convert(MembershipReferenceType.APPLICATION).equals(membership.getReferenceType())) {
                    applicationIds.add(membership.getReferenceId());
                }
                if (convert(MembershipReferenceType.GROUP).equals(membership.getReferenceType())) {
                    groupIds.add(membership.getReferenceId());
                }
                membershipRepository.delete(membership.getId());
            }

            eventManager.publishEvent(
                ApplicationAlertEventType.APPLICATION_MEMBERSHIP_UPDATE,
                new ApplicationAlertMembershipEvent(applicationIds, groupIds)
            );
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove member {} {}", memberType, memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove " + memberType + " " + memberId, ex);
        }
    }

    @Override
    public void transferApiOwnership(
        final String organizationId,
        final String environmentId,
        String apiId,
        MembershipMember member,
        List<RoleEntity> newPrimaryOwnerRoles
    ) {
        this.transferOwnership(
                MembershipReferenceType.API,
                RoleScope.API,
                apiId,
                member,
                newPrimaryOwnerRoles,
                organizationId,
                environmentId
            );
    }

    @Override
    public void transferApplicationOwnership(
        final String organizationId,
        final String environmentId,
        String applicationId,
        MembershipMember member,
        List<RoleEntity> newPrimaryOwnerRoles
    ) {
        this.transferOwnership(
                MembershipReferenceType.APPLICATION,
                RoleScope.APPLICATION,
                applicationId,
                member,
                newPrimaryOwnerRoles,
                organizationId,
                environmentId
            );
    }

    private void transferOwnership(
        MembershipReferenceType membershipReferenceType,
        RoleScope roleScope,
        String itemId,
        MembershipMember member,
        List<RoleEntity> newPrimaryOwnerRoles,
        final String organizationId,
        final String environmentId
    ) {
        List<RoleEntity> newRoles;
        if (newPrimaryOwnerRoles == null || newPrimaryOwnerRoles.isEmpty()) {
            newRoles = roleService.findDefaultRoleByScopes(roleScope);
        } else {
            newRoles = newPrimaryOwnerRoles;
        }

        MembershipEntity primaryOwner = this.getPrimaryOwner(organizationId, membershipReferenceType, itemId);
        // Set the new primary owner
        MemberEntity newPrimaryOwnerMember =
            this.addRoleToMemberOnReference(
                    organizationId,
                    environmentId,
                    new MembershipReference(membershipReferenceType, itemId),
                    new MembershipMember(member.getMemberId(), member.getReference(), member.getMemberType()),
                    new MembershipRole(roleScope, PRIMARY_OWNER.name())
                );

        //If the new PO is a group and the reference is an API, add the group as a member of the API
        if (membershipReferenceType == MembershipReferenceType.API && member.getMemberType() == MembershipMemberType.GROUP) {
            apiService.addGroup(itemId, member.getMemberId());
        }

        RoleEntity poRoleEntity = roleService.findPrimaryOwnerRoleByOrganization(organizationId, roleScope);
        if (poRoleEntity != null) {
            // if the new primary owner is a user, remove its previous role
            if (member.getMemberType() == MembershipMemberType.USER) this.getRoles(
                    membershipReferenceType,
                    itemId,
                    member.getMemberType(),
                    member.getMemberId()
                )
                .forEach(
                    role -> {
                        if (!role.getId().equals(poRoleEntity.getId())) {
                            this.removeRole(membershipReferenceType, itemId, member.getMemberType(), member.getMemberId(), role.getId());
                        }
                    }
                );

            // remove role of the previous  primary owner
            this.removeRole(
                    membershipReferenceType,
                    itemId,
                    primaryOwner.getMemberType(),
                    primaryOwner.getMemberId(),
                    poRoleEntity.getId()
                );

            // if the previous primary owner was a user
            if (primaryOwner.getMemberType() == MembershipMemberType.USER) {
                // set the new role
                for (RoleEntity newRole : newRoles) {
                    this.addRoleToMemberOnReference(
                            organizationId,
                            environmentId,
                            new MembershipReference(membershipReferenceType, itemId),
                            new MembershipMember(primaryOwner.getMemberId(), null, primaryOwner.getMemberType()),
                            new MembershipRole(roleScope, newRole.getName())
                        );
                }
            } else if (primaryOwner.getMemberType() == MembershipMemberType.GROUP) {
                // remove this group from the api's group list
                apiService.removeGroup(itemId, primaryOwner.getId());
            }
        }
    }

    @Override
    public MemberEntity updateRoleToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReference reference,
        MembershipMember member,
        MembershipRole role
    ) {
        return updateRolesToMemberOnReference(organizationId, environmentId, reference, member, singleton(role), null, true)
            .stream()
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<MemberEntity> updateRolesToMemberOnReference(
        final String organizationId,
        final String environmentId,
        MembershipReference reference,
        MembershipMember member,
        Collection<MembershipRole> roles,
        String source,
        boolean notify
    ) {
        try {
            Set<io.gravitee.repository.management.model.Membership> existingMemberships =
                this.membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                        member.getMemberId(),
                        convert(member.getMemberType()),
                        convert(reference.getType()),
                        reference.getId()
                    );
            if (existingMemberships != null && !existingMemberships.isEmpty()) {
                existingMemberships.forEach(membership -> this.deleteMembership(organizationId, environmentId, membership.getId()));
            }
            return roles
                .stream()
                .map(role -> _addRoleToMemberOnReference(reference, member, role, source, notify, false, environmentId, organizationId))
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update member for {} {}", reference.getType(), reference.getId(), ex);
            throw new TechnicalManagementException(
                "An error occurs while trying to update member for " + reference.getType() + " " + reference.getId(),
                ex
            );
        }
    }

    @Override
    public List<MemberEntity> updateRolesToMemberOnReferenceBySource(
        final String organizationId,
        final String environmentId,
        MembershipReference reference,
        MembershipMember member,
        Collection<MembershipRole> roles,
        String source
    ) {
        return roles
            .stream()
            .map(role -> _addRoleToMemberOnReference(reference, member, role, source, false, true, environmentId, organizationId))
            .collect(Collectors.toList());
    }
}
