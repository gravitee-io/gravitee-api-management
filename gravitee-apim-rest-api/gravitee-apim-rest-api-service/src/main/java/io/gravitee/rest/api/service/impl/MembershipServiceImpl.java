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
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserMembership;
import io.gravitee.rest.api.model.alert.ApplicationAlertEventType;
import io.gravitee.rest.api.model.alert.ApplicationAlertMembershipEvent;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.providers.User;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApplicationAlertService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.v4.ApiGroupService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
public class MembershipServiceImpl extends AbstractService implements MembershipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MembershipServiceImpl.class);

    private static final String DEFAULT_SOURCE = "system";
    private final Cache<String, Set<RoleEntity>> roles = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    private final UserService userService;

    private final EmailService emailService;

    private final IdentityService identityService;

    private final MembershipRepository membershipRepository;

    private final RoleService roleService;

    private final ApplicationService applicationService;

    private final ApplicationAlertService applicationAlertService;

    private final ApiSearchService apiSearchService;

    private final ApiGroupService apiGroupService;

    private final GroupService groupService;

    private final AuditService auditService;

    private final ApiRepository apiRepository;

    private final ApplicationRepository applicationRepository;

    private final EventManager eventManager;

    private final PrimaryOwnerService primaryOwnerService;
    private ParameterService parameterService;

    public MembershipServiceImpl(
        @Autowired @Lazy IdentityService identityService,
        @Autowired @Lazy UserService userService,
        @Autowired @Lazy ApplicationRepository applicationRepository,
        @Autowired EventManager eventManager,
        @Autowired @Lazy PrimaryOwnerService primaryOwnerService,
        @Autowired EmailService emailService,
        @Autowired @Lazy MembershipRepository membershipRepository,
        @Autowired @Lazy RoleService roleService,
        @Autowired @Lazy ApplicationService applicationService,
        @Autowired @Lazy ApplicationAlertService applicationAlertService,
        @Autowired ApiSearchService apiSearchService,
        @Autowired @Lazy ApiGroupService apiGroupService,
        @Autowired @Lazy ApiRepository apiRepository,
        @Autowired @Lazy GroupService groupService,
        @Autowired AuditService auditService,
        @Autowired ParameterService parameterService
    ) {
        this.identityService = identityService;
        this.userService = userService;
        this.applicationRepository = applicationRepository;
        this.eventManager = eventManager;
        this.primaryOwnerService = primaryOwnerService;
        this.emailService = emailService;
        this.membershipRepository = membershipRepository;
        this.roleService = roleService;
        this.applicationService = applicationService;
        this.applicationAlertService = applicationAlertService;
        this.apiSearchService = apiSearchService;
        this.apiGroupService = apiGroupService;
        this.apiRepository = apiRepository;
        this.groupService = groupService;
        this.auditService = auditService;
        this.parameterService = parameterService;
    }

    @Override
    public MemberEntity addRoleToMemberOnReference(
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String role
    ) {
        return addRoleToMemberOnReference(executionContext, referenceType, referenceId, memberType, memberId, role, DEFAULT_SOURCE);
    }

    @Override
    public MemberEntity addRoleToMemberOnReference(
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String role,
        String source
    ) {
        RoleEntity roleToAdd = roleService.findById(role);
        return _addRoleToMemberOnReference(
            executionContext,
            new MembershipReference(referenceType, referenceId),
            new MembershipMember(memberId, null, memberType),
            new MembershipRole(roleToAdd.getScope(), roleToAdd.getName()),
            source,
            true,
            false
        );
    }

    @Override
    public MemberEntity addRoleToMemberOnReference(
        ExecutionContext executionContext,
        MembershipReference reference,
        MembershipMember member,
        MembershipRole role
    ) {
        return _addRoleToMemberOnReference(executionContext, reference, member, role, DEFAULT_SOURCE, true, false);
    }

    private MemberEntity _addRoleToMemberOnReference(
        ExecutionContext executionContext,
        MembershipReference reference,
        MembershipMember member,
        MembershipRole role,
        String source,
        boolean notify,
        boolean update
    ) {
        try {
            LOGGER.debug("Add a new member for {} {}", reference.getType(), reference.getId());

            Optional<RoleEntity> optRoleEntity = roleService.findByScopeAndName(
                role.getScope(),
                role.getName(),
                executionContext.getOrganizationId()
            );
            if (optRoleEntity.isPresent()) {
                RoleEntity roleEntity = optRoleEntity.get();

                assertRoleScopeAllowedForReference(reference, roleEntity);
                assertRoleNameAllowedForReference(reference, roleEntity);

                if (member.getMemberId() != null) {
                    Set<io.gravitee.repository.management.model.Membership> similarMemberships =
                        membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
                            member.getMemberId(),
                            convert(member.getMemberType()),
                            convert(reference.getType()),
                            reference.getId(),
                            roleEntity.getId()
                        );

                    if (!similarMemberships.isEmpty()) {
                        if (update) {
                            UserEntity userEntity = findUserFromMembershipMember(executionContext, member);
                            return getUserMember(executionContext, reference.getType(), reference.getId(), userEntity.getId());
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
                    UserEntity userEntity = findUserFromMembershipMember(executionContext, member);
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
                    createAuditLog(executionContext, MEMBERSHIP_CREATED, membership.getCreatedAt(), null, membership);

                    if (MembershipReferenceType.APPLICATION.equals(reference.getType())) {
                        applicationAlertService.addMemberToApplication(executionContext, reference.getId(), userEntity.getEmail());
                    }

                    Set<io.gravitee.repository.management.model.Membership> userRolesOnReference =
                        membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
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
                            final GroupEntity group = groupService.findById(executionContext, reference.getId());
                            shouldNotify = !group.isDisableMembershipNotifications();
                        } else if (MembershipReferenceType.API.equals(reference.getType())) {
                            final GenericApiEntity api = apiSearchService.findGenericById(executionContext, reference.getId());
                            shouldNotify = !api.isDisableMembershipNotifications();
                        } else if (MembershipReferenceType.APPLICATION.equals(reference.getType())) {
                            final ApplicationEntity application = applicationService.findById(executionContext, reference.getId());
                            shouldNotify = !application.isDisableMembershipNotifications();
                        }
                    }

                    if (shouldNotify) {
                        EmailNotification emailNotification = buildEmailNotification(
                            executionContext,
                            userEntity,
                            reference.getType(),
                            reference.getId()
                        );
                        if (emailNotification != null) {
                            final boolean isTrialInstance = parameterService.findAsBoolean(
                                executionContext,
                                Key.TRIAL_INSTANCE,
                                ParameterReferenceType.SYSTEM
                            );
                            if (!isTrialInstance || userEntity.optedIn()) {
                                try {
                                    emailService.sendAsyncEmailNotification(executionContext, emailNotification);
                                } catch (Exception e) {
                                    LOGGER.error(
                                        "An error occurs while trying to send email notification for {} {} {}",
                                        reference.getType(),
                                        reference.getId(),
                                        userEntity.getId(),
                                        e
                                    );
                                }
                            }
                        }
                    }

                    userMember = getUserMember(executionContext, reference.getType(), reference.getId(), userEntity.getId());
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
                    createAuditLog(executionContext, MEMBERSHIP_CREATED, membership.getCreatedAt(), null, membership);
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
        ExecutionContext executionContext,
        Audit.AuditEvent event,
        Date date,
        io.gravitee.repository.management.model.Membership oldValue,
        io.gravitee.repository.management.model.Membership newValue
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
                auditService.createApiAuditLog(executionContext, referenceId, properties, event, date, oldValue, newValue);
                break;
            case APPLICATION:
                auditService.createApplicationAuditLog(executionContext, referenceId, properties, event, date, oldValue, newValue);
                break;
            case GROUP:
                properties.put(Audit.AuditProperties.GROUP, referenceId);
                auditService.createAuditLog(executionContext, properties, event, date, oldValue, newValue);
                break;
            case ENVIRONMENT:
                auditService.createAuditLog(executionContext, properties, event, date, oldValue, newValue);
                break;
            case ORGANIZATION:
                auditService.createOrganizationAuditLog(
                    executionContext,
                    executionContext.getOrganizationId(),
                    properties,
                    event,
                    date,
                    oldValue,
                    newValue
                );
                break;
        }
    }

    private EmailNotification buildEmailNotification(
        ExecutionContext executionContext,
        UserEntity user,
        MembershipReferenceType referenceType,
        String referenceId
    ) {
        EmailNotificationBuilder.EmailTemplate template = null;
        Map<String, Object> params = null;
        GroupEntity groupEntity;
        NotificationParamsBuilder paramsBuilder = new NotificationParamsBuilder();
        switch (referenceType) {
            case APPLICATION:
                ApplicationEntity applicationEntity = applicationService.findById(executionContext, referenceId);
                template = EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_APPLICATION_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.application(applicationEntity).user(user).build();
                break;
            case API:
                GenericApiEntity indexableApi = apiSearchService.findGenericById(executionContext, referenceId);
                template = EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_API_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.api(indexableApi).user(user).build();
                break;
            case GROUP:
                groupEntity = groupService.findById(executionContext, referenceId);
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
        final ExecutionContext executionContext,
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
                userEntities.addAll(userService.findByIds(executionContext, userIds, false));
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

            members.forEach(m -> {
                Optional<io.gravitee.repository.management.model.Membership> membership = memberships
                    .stream()
                    .filter(ms -> ms.getMemberId().equals(m.getId()))
                    .findFirst();
                membership.ifPresent(ms -> {
                    if (ms.getMemberType() == io.gravitee.repository.management.model.MembershipMemberType.USER) {
                        Optional<UserEntity> user = userEntities.stream().filter(u -> u.getId().equals(ms.getMemberId())).findFirst();
                        user.ifPresent(u -> {
                            m.setDisplayName(u.getDisplayName());
                            m.setEmail(u.getEmail());
                        });
                    } else {
                        Optional<GroupEntity> group = groupEntities.stream().filter(u -> u.getId().equals(ms.getMemberId())).findFirst();
                        group.ifPresent(g -> m.setDisplayName(g.getName()));
                    }
                });
            });
        }
    }

    private UserEntity findUserFromMembershipMember(ExecutionContext executionContext, MembershipMember member) {
        UserEntity userEntity;
        if (member.getMemberId() != null) {
            userEntity = userService.findById(executionContext, member.getMemberId());
        } else {
            // We have a user reference, meaning that the user is coming from an external system
            // User does not exist so we are looking into defined providers
            Optional<io.gravitee.rest.api.model.providers.User> providerUser = identityService.findByReference(member.getReference());
            if (providerUser.isPresent()) {
                User identityUser = providerUser.get();
                userEntity = findOrCreateUser(executionContext, identityUser);
            } else {
                throw new UserNotFoundException(member.getReference());
            }
        }
        return userEntity;
    }

    private UserEntity findOrCreateUser(ExecutionContext executionContext, User identityUser) {
        UserEntity userEntity;
        try {
            userEntity =
                userService.findBySource(executionContext.getOrganizationId(), identityUser.getSource(), identityUser.getSourceId(), false);
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
                userEntity = userService.create(executionContext, newUser, true);
            } else {
                userEntity = userService.create(executionContext, newUser, false);
                for (Map.Entry<String, String> role : identityUser.getRoles().entrySet()) {
                    MembershipReferenceType membershipReferenceType = MembershipReferenceType.valueOf(role.getKey());
                    MembershipReference reference = null;
                    if (membershipReferenceType == MembershipReferenceType.ORGANIZATION) {
                        reference = new MembershipReference(membershipReferenceType, executionContext.getOrganizationId());
                    } else if (membershipReferenceType == MembershipReferenceType.ENVIRONMENT) {
                        reference = new MembershipReference(membershipReferenceType, executionContext.getEnvironmentId());
                    }

                    if (reference != null) {
                        this.addRoleToMemberOnReference(
                                executionContext,
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
    public void deleteMembership(ExecutionContext executionContext, String membershipId) {
        try {
            Optional<io.gravitee.repository.management.model.Membership> membership = membershipRepository.findById(membershipId);
            if (membership.isPresent()) {
                LOGGER.debug("Delete membership {}", membership.get());
                membershipRepository.delete(membershipId);
                createAuditLog(executionContext, MEMBERSHIP_DELETED, new Date(), membership.get(), null);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete membership {}", membershipId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete membership " + membershipId, ex);
        }
    }

    @Override
    public void deleteReference(ExecutionContext executionContext, MembershipReferenceType referenceType, String referenceId) {
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
                    createAuditLog(executionContext, MEMBERSHIP_DELETED, new Date(), membership, null);
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
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId
    ) {
        deleteReferenceMemberBySource(executionContext, referenceType, referenceId, memberType, memberId, null);
    }

    @Override
    public void deleteReferenceMemberBySource(
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId,
        String sourceId
    ) {
        try {
            RoleEntity apiPORole = roleService
                .findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), executionContext.getOrganizationId())
                .orElseThrow(() -> new TechnicalManagementException("Unable to find API Primary Owner role"));
            RoleEntity applicationPORole = roleService
                .findByScopeAndName(RoleScope.APPLICATION, PRIMARY_OWNER.name(), executionContext.getOrganizationId())
                .orElseThrow(() -> new TechnicalManagementException("Unable to find Application Primary Owner role"));
            Set<io.gravitee.repository.management.model.Membership> memberships =
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    memberId,
                    convert(memberType),
                    convert(referenceType),
                    referenceId
                );

            if (MembershipReferenceType.API.equals(referenceType)) {
                assertNoPrimaryOwnerRemoval(apiPORole, memberships);
            }
            if (MembershipReferenceType.APPLICATION.equals(referenceType)) {
                assertNoPrimaryOwnerRemoval(applicationPORole, memberships);
            }

            for (io.gravitee.repository.management.model.Membership membership : memberships) {
                if (sourceId == null || membership.getSource().equals(sourceId)) {
                    LOGGER.debug("Delete membership {}", membership.getId());
                    membershipRepository.delete(membership.getId());
                    createAuditLog(executionContext, MEMBERSHIP_DELETED, new Date(), membership, null);
                }

                if (MembershipReferenceType.APPLICATION.equals(referenceType) && MembershipMemberType.USER.equals(memberType)) {
                    UserEntity userEntity = findUserFromMembershipMember(
                        executionContext,
                        new MembershipMember(memberId, null, memberType)
                    );
                    applicationAlertService.deleteMemberFromApplication(executionContext, referenceId, userEntity.getEmail());
                }

                //if the API Primary owner of a group has been deleted, we must update the apiPrimaryOwnerField of this group
                if (
                    membership.getReferenceType() == io.gravitee.repository.management.model.MembershipReferenceType.GROUP &&
                    membership.getRoleId().equals(apiPORole.getId())
                ) {
                    groupService.updateApiPrimaryOwner(membership.getReferenceId(), null);
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

    private void assertNoPrimaryOwnerRemoval(RoleEntity apiPORole, Set<io.gravitee.repository.management.model.Membership> memberships) {
        memberships
            .stream()
            .filter(membership -> membership.getRoleId().equals(apiPORole.getId()))
            .findFirst()
            .ifPresent(membership -> {
                throw new PrimaryOwnerRemovalException();
            });
    }

    @Override
    public List<UserMembership> findUserMembershipBySource(
        ExecutionContext executionContext,
        MembershipReferenceType type,
        String userId,
        String sourceId
    ) {
        try {
            Map<String, RoleEntity> roleMap = roleService
                .findAllByOrganization(executionContext.getOrganizationId())
                .stream()
                .collect(Collectors.toMap(RoleEntity::getId, r -> r));
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
                .forEach(membership -> {
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
                });

            return new ArrayList<>(userMembershipMap.values());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove user {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove user " + userId, ex);
        }
    }

    @Override
    public List<UserMembership> findUserMembership(ExecutionContext executionContext, MembershipReferenceType type, String userId) {
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
                .findByScope(roleService.findScopeByMembershipReferenceType(type), executionContext.getOrganizationId())
                .stream()
                .collect(Collectors.toMap(RoleEntity::getId, r -> r));
            HashMap<Integer, UserMembership> userMembershipMap = new HashMap<>();
            membershipRepository
                .findByMemberIdAndMemberTypeAndReferenceType(
                    userId,
                    io.gravitee.repository.management.model.MembershipMemberType.USER,
                    convert(type)
                )
                .forEach(membership -> {
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
                });
            Set<UserMembership> userMemberships = new HashSet<>(userMembershipMap.values());

            // Find all application Ids and api Ids linked to all user groups
            if (type.equals(MembershipReferenceType.APPLICATION) || type.equals(MembershipReferenceType.API)) {
                String[] groupIds = groupService.findByUser(userId).stream().map(GroupEntity::getId).toArray(String[]::new);

                List<String> resourceIds = new ArrayList<>();

                if (type.equals(MembershipReferenceType.APPLICATION) && groupIds.length > 0) {
                    Set<Application> groupApplications = applicationRepository.findByGroups(List.of(groupIds));
                    groupApplications.forEach(application -> resourceIds.add(application.getId()));
                } else if (type.equals(MembershipReferenceType.API) && groupIds.length > 0) {
                    ApiCriteria criteria = new ApiCriteria.Builder().groups(groupIds).build();
                    List<String> groupApisIds = apiRepository
                        .searchIds(List.of(criteria), convert(new PageableImpl(1, Integer.MAX_VALUE)), null)
                        .getContent();
                    if (groupApisIds != null) {
                        resourceIds.addAll(groupApisIds);
                    }
                }

                if (!resourceIds.isEmpty()) {
                    userMemberships.addAll(
                        resourceIds
                            .stream()
                            .map(id -> {
                                UserMembership userMembership = new UserMembership();
                                userMembership.setType(type.name());
                                userMembership.setReference(id);
                                return userMembership;
                            })
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
                criteria.ids(memberships.stream().map(UserMembership::getReference).toArray(String[]::new));
                apiRepository
                    .search(criteria.build(), null, ApiFieldFilter.defaultFields())
                    .forEach(api -> {
                        metadata.put(api.getId(), "name", api.getName());
                        metadata.put(api.getId(), "version", api.getVersion());
                        metadata.put(api.getId(), "visibility", api.getVisibility());
                    });
            } else if (type.equals(MembershipReferenceType.APPLICATION)) {
                applicationRepository
                    .findByIds(memberships.stream().map(UserMembership::getReference).collect(Collectors.toList()))
                    .forEach(application -> {
                        metadata.put(application.getId(), "name", application.getName());
                    });
            }
            return metadata;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get user membership metadata", ex);
            throw new TechnicalManagementException("An error occurs while trying to get user membership metadata", ex);
        }
    }

    @Override
    public Page<MemberEntity> getMembersByReference(
        final ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        Pageable pageable
    ) {
        return getMembersByReference(executionContext, referenceType, referenceId, null, pageable);
    }

    @Override
    public Set<MemberEntity> getMembersByReference(
        final ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId
    ) {
        return this.getMembersByReferenceAndRole(executionContext, referenceType, referenceId, null);
    }

    @Override
    public Page<MemberEntity> getMembersByReference(
        final ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        String role,
        Pageable pageable
    ) {
        return this.getMembersByReferenceAndRole(executionContext, referenceType, referenceId, role, pageable);
    }

    @Override
    public Set<MemberEntity> getMembersByReference(
        final ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        String role
    ) {
        return this.getMembersByReferenceAndRole(executionContext, referenceType, referenceId, role);
    }

    @Override
    public Page<MemberEntity> getMembersByReferenceAndRole(
        final ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        String role,
        Pageable pageable
    ) {
        return this.getMembersByReferencesAndRole(executionContext, referenceType, Collections.singletonList(referenceId), role, pageable);
    }

    @Override
    public Set<MemberEntity> getMembersByReferenceAndRole(
        final ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        String role
    ) {
        return new HashSet<>(getMembersByReference(executionContext, referenceType, referenceId, role, null).getContent());
    }

    @Override
    public Set<MemberEntity> getMembersByReferencesAndRole(
        final ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        List<String> referenceIds,
        String role
    ) {
        return new HashSet<>(this.getMembersByReferencesAndRole(executionContext, referenceType, referenceIds, role, null).getContent());
    }

    @Override
    public Page<MemberEntity> getMembersByReferencesAndRole(
        final ExecutionContext executionContext,
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
                .forEach(member -> {
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
                });

            List<MemberEntity> members = new ArrayList<>(results.values());
            fillMemberUserInformation(executionContext, memberships, members);
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
    public Set<String> getReferenceIdsByMemberAndReference(
        MembershipMemberType memberType,
        String memberId,
        MembershipReferenceType referenceType
    ) {
        try {
            return membershipRepository
                .findRefIdsByMemberIdAndMemberTypeAndReferenceType(memberId, convert(memberType), convert(referenceType))
                .collect(toSet());
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
    public Set<String> getReferenceIdsByMemberAndReferenceAndRoleIn(
        MembershipMemberType memberType,
        String memberId,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    ) {
        try {
            return membershipRepository.findRefIdByMemberAndRefTypeAndRoleIdIn(
                memberId,
                convert(memberType),
                convert(referenceType),
                roleIds
            );
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
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        String userId
    ) {
        try {
            Set<io.gravitee.repository.management.model.Membership> userMemberships =
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                    userId,
                    convert(MembershipMemberType.USER),
                    convert(referenceType),
                    referenceId
                );

            //Get entity groups
            Set<String> entityGroups = new HashSet<>();
            switch (referenceType) {
                case API:
                    entityGroups = apiRepository.findById(referenceId).orElseThrow(() -> new ApiNotFoundException(referenceId)).getGroups();
                    break;
                case APPLICATION:
                    entityGroups =
                        applicationRepository
                            .findById(referenceId)
                            .orElseThrow(() -> new ApplicationNotFoundException(referenceId))
                            .getGroups();
                    break;
                default:
                    break;
            }

            if (userMemberships.isEmpty() && (entityGroups == null || entityGroups.isEmpty())) {
                return null;
            }

            MemberEntity memberEntity = new MemberEntity();
            UserEntity userEntity = userService.findById(executionContext, userId);
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
                            .map(role ->
                                role.isApiPrimaryOwner()
                                    ? mapApiPrimaryOwnerRoleToGroupRole(executionContext, referenceId, group, role)
                                    : role
                            )
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

    private RoleEntity mapApiPrimaryOwnerRoleToGroupRole(ExecutionContext executionContext, String apiId, String groupId, RoleEntity role) {
        PrimaryOwnerEntity apiPrimaryOwner = primaryOwnerService.getPrimaryOwner(executionContext.getOrganizationId(), apiId);
        if (apiPrimaryOwner.getId().equals(groupId)) {
            return role;
        }

        GroupEntity userGroup = groupService.findById(executionContext, groupId);
        String groupApiRole = userGroup.getRoles().get(RoleScope.API);

        return groupApiRole == null
            ? null
            : roleService.findByScopeAndName(RoleScope.API, groupApiRole, executionContext.getOrganizationId()).orElse(null);
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

    @Override
    public Map<String, char[]> getUserMemberPermissions(
        ExecutionContext executionContext,
        MembershipReferenceType referenceType,
        String referenceId,
        String userId
    ) {
        MemberEntity member = this.getUserMember(executionContext, referenceType, referenceId, userId);
        if (member != null) {
            return member.getPermissions();
        }
        return emptyMap();
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(ExecutionContext executionContext, GenericApiEntity api, String userId) {
        return getUserMemberPermissions(executionContext, MembershipReferenceType.API, api.getId(), userId);
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(ExecutionContext executionContext, ApplicationEntity application, String userId) {
        return getUserMemberPermissions(executionContext, MembershipReferenceType.APPLICATION, application.getId(), userId);
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(ExecutionContext executionContext, GroupEntity group, String userId) {
        return getUserMemberPermissions(executionContext, MembershipReferenceType.GROUP, group.getId(), userId);
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(ExecutionContext executionContext, EnvironmentEntity environment, String userId) {
        return getUserMemberPermissions(executionContext, MembershipReferenceType.ENVIRONMENT, environment.getId(), userId);
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
            Set<io.gravitee.repository.management.model.Membership> membershipsToDelete =
                membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
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
                Set<io.gravitee.repository.management.model.Membership> membershipsWithNewRole =
                    membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
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
    public void removeMemberMemberships(ExecutionContext executionContext, MembershipMemberType memberType, String memberId) {
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
                new ApplicationAlertMembershipEvent(executionContext.getOrganizationId(), applicationIds, groupIds)
            );
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove member {} {}", memberType, memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove " + memberType + " " + memberId, ex);
        }
    }

    @Override
    public void transferApiOwnership(
        ExecutionContext executionContext,
        String apiId,
        MembershipMember member,
        List<RoleEntity> newPrimaryOwnerRoles
    ) {
        if (
            MembershipMemberType.GROUP.equals(member.getMemberType()) &&
            !this.hasApiPrimaryOwnerMemberInGroup(executionContext, member.getMemberId())
        ) {
            throw new ApiOwnershipTransferException(apiId);
        }

        this.transferOwnership(executionContext, MembershipReferenceType.API, RoleScope.API, apiId, member, newPrimaryOwnerRoles);
    }

    @Override
    public void transferApplicationOwnership(
        ExecutionContext executionContext,
        String applicationId,
        MembershipMember member,
        List<RoleEntity> newPrimaryOwnerRoles
    ) {
        this.transferOwnership(
                executionContext,
                MembershipReferenceType.APPLICATION,
                RoleScope.APPLICATION,
                applicationId,
                member,
                newPrimaryOwnerRoles
            );
    }

    private void transferOwnership(
        ExecutionContext executionContext,
        MembershipReferenceType membershipReferenceType,
        RoleScope roleScope,
        String itemId,
        MembershipMember member,
        List<RoleEntity> newPrimaryOwnerRoles
    ) {
        List<RoleEntity> newRoles;
        if (newPrimaryOwnerRoles == null || newPrimaryOwnerRoles.isEmpty()) {
            newRoles = roleService.findDefaultRoleByScopes(executionContext.getOrganizationId(), roleScope);
        } else {
            newRoles = newPrimaryOwnerRoles;
        }

        MembershipEntity primaryOwner = this.getPrimaryOwner(executionContext.getOrganizationId(), membershipReferenceType, itemId);

        this.addRoleToMemberOnReference(
                executionContext,
                new MembershipReference(membershipReferenceType, itemId),
                new MembershipMember(member.getMemberId(), member.getReference(), member.getMemberType()),
                new MembershipRole(roleScope, PRIMARY_OWNER.name())
            );

        //If the new PO is a group and the reference is an API, add the group as a member of the API
        if (membershipReferenceType == MembershipReferenceType.API && member.getMemberType() == MembershipMemberType.GROUP) {
            apiGroupService.addGroup(executionContext, itemId, member.getMemberId());
        }

        RoleEntity poRoleEntity = roleService.findPrimaryOwnerRoleByOrganization(executionContext.getOrganizationId(), roleScope);
        if (poRoleEntity != null) {
            // if the new primary owner is a user, remove its previous role
            if (member.getMemberType() == MembershipMemberType.USER) this.getRoles(
                    membershipReferenceType,
                    itemId,
                    member.getMemberType(),
                    member.getMemberId()
                )
                .forEach(role -> {
                    if (!role.getId().equals(poRoleEntity.getId())) {
                        this.removeRole(membershipReferenceType, itemId, member.getMemberType(), member.getMemberId(), role.getId());
                    }
                });

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
                            executionContext,
                            new MembershipReference(membershipReferenceType, itemId),
                            new MembershipMember(primaryOwner.getMemberId(), null, primaryOwner.getMemberType()),
                            new MembershipRole(roleScope, newRole.getName())
                        );
                }
            } else if (primaryOwner.getMemberType() == MembershipMemberType.GROUP) {
                // remove this group from the api's group list
                apiGroupService.removeGroup(executionContext, itemId, primaryOwner.getId());
            }
        }
    }

    @Override
    public MemberEntity updateRoleToMemberOnReference(
        ExecutionContext executionContext,
        MembershipReference reference,
        MembershipMember member,
        MembershipRole role
    ) {
        return updateRolesToMemberOnReference(executionContext, reference, member, singleton(role), null, true)
            .stream()
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<MemberEntity> updateRolesToMemberOnReference(
        ExecutionContext executionContext,
        MembershipReference reference,
        MembershipMember member,
        Collection<MembershipRole> roles,
        String source,
        boolean notify
    ) {
        try {
            RoleEntity apiPORole = roleService
                .findByScopeAndName(RoleScope.API, PRIMARY_OWNER.name(), executionContext.getOrganizationId())
                .orElseThrow(() -> new TechnicalManagementException("Unable to find API Primary Owner role"));

            Set<io.gravitee.repository.management.model.Membership> existingMemberships =
                this.membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
                        member.getMemberId(),
                        convert(member.getMemberType()),
                        convert(reference.getType()),
                        reference.getId()
                    );

            // If new roles do not contain PRIMARY_OWNER, check we are not removing PRIMARY_OWNER membership
            if (roles.stream().filter(role -> role.getName().equals(PRIMARY_OWNER.name())).findAny().isEmpty()) {
                assertNoPrimaryOwnerRemoval(apiPORole, existingMemberships);
            }

            if (existingMemberships != null && !existingMemberships.isEmpty()) {
                existingMemberships.forEach(membership -> this.deleteMembership(executionContext, membership.getId()));
            }
            return roles
                .stream()
                .map(role -> _addRoleToMemberOnReference(executionContext, reference, member, role, source, notify, false))
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
        ExecutionContext executionContext,
        MembershipReference reference,
        MembershipMember member,
        Collection<MembershipRole> roles,
        String source
    ) {
        return roles
            .stream()
            .map(role -> _addRoleToMemberOnReference(executionContext, reference, member, role, source, false, true))
            .collect(Collectors.toList());
    }

    @Override
    public MemberEntity createNewMembershipForApi(
        ExecutionContext executionContext,
        String apiId,
        String userId,
        String externalReference,
        String roleName
    ) {
        MembershipService.MembershipReference reference = new MembershipService.MembershipReference(MembershipReferenceType.API, apiId);
        MembershipService.MembershipMember member = new MembershipService.MembershipMember(
            userId,
            externalReference,
            MembershipMemberType.USER
        );
        MembershipService.MembershipRole role = new MembershipService.MembershipRole(RoleScope.API, roleName);

        if (member.getMemberId() != null) {
            MemberEntity userMember = getUserMember(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.API,
                apiId,
                member.getMemberId()
            );
            if (userMember != null && userMember.getRoles() != null && !userMember.getRoles().isEmpty()) {
                throw new MembershipAlreadyExistsException(
                    member.getMemberId(),
                    MembershipMemberType.USER,
                    apiId,
                    MembershipReferenceType.API
                );
            }
        }
        return addRoleToMemberOnReference(GraviteeContext.getExecutionContext(), reference, member, role);
    }

    @Override
    public MemberEntity updateMembershipForApi(ExecutionContext executionContext, String apiId, String memberId, String roleName) {
        MemberEntity membership = null;
        MemberEntity userMember = getUserMember(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, apiId, memberId);

        MembershipService.MembershipReference reference = new MembershipService.MembershipReference(MembershipReferenceType.API, apiId);
        MembershipService.MembershipMember member = new MembershipService.MembershipMember(memberId, null, MembershipMemberType.USER);
        MembershipService.MembershipRole role = new MembershipService.MembershipRole(RoleScope.API, roleName);

        if (userMember != null && userMember.getRoles() != null && !userMember.getRoles().isEmpty()) {
            membership = updateRoleToMemberOnReference(GraviteeContext.getExecutionContext(), reference, member, role);
        }
        return membership;
    }

    @Override
    public void deleteMemberForApi(ExecutionContext executionContext, String apiId, String memberId) {
        deleteReferenceMember(executionContext, MembershipReferenceType.API, apiId, MembershipMemberType.USER, memberId);
    }

    @Override
    public void deleteMemberForApplication(ExecutionContext executionContext, String applicationId, String memberId) {
        deleteReferenceMember(executionContext, MembershipReferenceType.APPLICATION, applicationId, MembershipMemberType.USER, memberId);
    }

    private boolean hasApiPrimaryOwnerMemberInGroup(ExecutionContext executionContext, String groupId) {
        return this.getMembersByReference(executionContext, MembershipReferenceType.GROUP, groupId)
            .stream()
            .anyMatch(member -> member.getRoles().stream().anyMatch(RoleEntity::isApiPrimaryOwner));
    }
}
