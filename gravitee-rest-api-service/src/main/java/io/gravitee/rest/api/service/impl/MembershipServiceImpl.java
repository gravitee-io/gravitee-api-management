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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.providers.User;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Membership.AuditEvent.MEMBERSHIP_CREATED;
import static io.gravitee.repository.management.model.Membership.AuditEvent.MEMBERSHIP_DELETED;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static java.util.Collections.emptyMap;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MembershipServiceImpl extends AbstractService implements MembershipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MembershipServiceImpl.class);

    private final static String DEFAULT_SOURCE = "system";

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

    private final Cache<String, Set<RoleEntity>> roles = CacheBuilder
            .newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    @Override
    public MemberEntity addRoleToMemberOnReference(MembershipReferenceType referenceType, String referenceId, MembershipMemberType memberType, String memberId, String role) {
        return addRoleToMemberOnReference(referenceType, referenceId, memberType, memberId, role, DEFAULT_SOURCE);
    }

    @Override
    public MemberEntity addRoleToMemberOnReference(MembershipReferenceType referenceType, String referenceId, MembershipMemberType memberType, String memberId, String role, String source) {
        RoleEntity roleToAdd = roleService.findById(role);
        return _addRoleToMemberOnReference(
                new MembershipReference(referenceType, referenceId), 
                new MembershipMember(memberId,  null, memberType), 
                new MembershipRole(roleToAdd.getScope(), roleToAdd.getName()), source, true);
    }

    @Override
    public MemberEntity addRoleToMemberOnReference(MembershipReference reference, MembershipMember member, MembershipRole role) {
        return _addRoleToMemberOnReference(reference, member, role, DEFAULT_SOURCE, true);
    }

    private MemberEntity _addRoleToMemberOnReference(MembershipReference reference, MembershipMember member, MembershipRole role,
                                                    String source, boolean notify) {
        try {
            LOGGER.debug("Add a new member for {} {}", reference.getType(), reference.getId());

            assertRoleScopeAllowedForReference(reference, role);
            assertRoleNameAllowedForReference(reference, role);

            Optional<RoleEntity> optRole = roleService.findByScopeAndName(role.getScope(), role.getName());
            if(optRole.isPresent()) {
                if (member.getMemberId() != null) {
                    Set<io.gravitee.repository.management.model.Membership> similarMemberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(member.getMemberId(), convert(member.getMemberType()), convert(reference.getType()), reference.getId(), optRole.get().getId());
                    if(!similarMemberships.isEmpty()) {
                        throw new MembershipAlreadyExistsException(member.getMemberId(), member.getMemberType(), reference.getId(), reference.getType());
                    }
                }
                Date updateDate = new Date();
                MemberEntity userMember = null;
                if (member.getMemberType() == MembershipMemberType.USER) {
                    UserEntity userEntity = findUserFromMembershipMember(member);
                    io.gravitee.repository.management.model.Membership membership = new io.gravitee.repository.management.model.Membership(
                            RandomString.generate(), 
                            userEntity.getId(), 
                            convert(member.getMemberType()), 
                            reference.getId(), 
                            convert(reference.getType()),
                            optRole.get().getId());
                    membership.setSource(source);
                    membership.setCreatedAt(updateDate);
                    membership.setUpdatedAt(updateDate);
                    membershipRepository.create(membership);
                    createAuditLog(MEMBERSHIP_CREATED, membership.getCreatedAt(), null, membership);

                    Set<io.gravitee.repository.management.model.Membership> userRolesOnReference = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(userEntity.getId(), convert(member.getMemberType()), convert(reference.getType()), reference.getId());
                    boolean shouldNotify = notify && userRolesOnReference != null && userRolesOnReference.size() == 1 &&
                            userEntity.getEmail() != null && !userEntity.getEmail().isEmpty();
                    if (shouldNotify) {
                        if (MembershipReferenceType.GROUP.equals(reference.getType())) {
                            final GroupEntity group = groupService.findById(reference.getId());
                            shouldNotify = !group.isDisableMembershipNotifications();
                        } else if (MembershipReferenceType.API.equals(reference.getType())) {
                            final ApiEntity api = apiService.findById(reference.getId());
                            shouldNotify = !api.isDisableMembershipNotifications();
                        } else if (MembershipReferenceType.APPLICATION.equals(reference.getType())) {
                            final ApplicationEntity application = applicationService.findById(reference.getId());
                            shouldNotify = !application.isDisableMembershipNotifications();
                        }
                    }

                    if (shouldNotify) {
                        EmailNotification emailNotification = buildEmailNotification(userEntity, reference.getType(), reference.getId());
                        if (emailNotification != null) {
                            emailService.sendAsyncEmailNotification(emailNotification, GraviteeContext.getCurrentContext());
                        }
                    }
                    
                    userMember = getUserMember(reference.getType(), reference.getId(), userEntity.getId());
                } else {
                    io.gravitee.repository.management.model.Membership membership = new io.gravitee.repository.management.model.Membership(
                            RandomString.generate(), 
                            member.getMemberId(), 
                            convert(member.getMemberType()), 
                            reference.getId(), 
                            convert(reference.getType()),
                            optRole.get().getId());
                    membership.setSource(source);
                    membership.setCreatedAt(updateDate);
                    membership.setUpdatedAt(updateDate);
                    membershipRepository.create(membership);
                    createAuditLog(MEMBERSHIP_CREATED, membership.getCreatedAt(), null, membership);
                }

                roles.invalidate(reference.getType().name() + reference.getId() + member.getMemberType() + member.getMemberId());

                return userMember;
            }
            
            return null;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to add member for {} {}", reference.getType(), reference.getId(), ex);
            throw new TechnicalManagementException("An error occurs while trying to add member for " + reference.getType() + " " + reference.getId(), ex);
        }
    }

    private void createAuditLog(Audit.AuditEvent event, Date date, io.gravitee.repository.management.model.Membership oldValue, io.gravitee.repository.management.model.Membership newValue) {
        io.gravitee.repository.management.model.MembershipReferenceType referenceType = oldValue != null ? oldValue.getReferenceType() : newValue.getReferenceType();
        String referenceId = oldValue != null ? oldValue.getReferenceId() : newValue.getReferenceId();
        String username = oldValue != null ? oldValue.getMemberId() : newValue.getMemberId();

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
                auditService.createEnvironmentAuditLog(
                        properties,
                        event,
                        date,
                        oldValue,
                        newValue);
                break;
            case ENVIRONMENT:
                auditService.createEnvironmentAuditLog(
                        properties,
                        event,
                        date,
                        oldValue,
                        newValue);
                break;
            case ORGANIZATION:
                auditService.createOrganizationAuditLog(
                        properties,
                        event,
                        date,
                        oldValue,
                        newValue);
                break;
        }
    }

    private EmailNotification buildEmailNotification(UserEntity user, MembershipReferenceType referenceType, String referenceId) {
        EmailNotificationBuilder.EmailTemplate template = null;
        Map<String, Object> params = null;
        GroupEntity groupEntity;
        NotificationParamsBuilder paramsBuilder = new NotificationParamsBuilder();
        switch (referenceType) {
            case APPLICATION:
                ApplicationEntity applicationEntity = applicationService.findById(referenceId);
                template = EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_APPLICATION_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.application(applicationEntity).user(user).build();
                break;
            case API:
                ApiEntity apiEntity = apiService.findById(referenceId);
                template = EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_API_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.api(apiEntity).user(user).build();
                break;
            case GROUP:
                groupEntity = groupService.findById(referenceId);
                template = EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_GROUP_MEMBER_SUBSCRIPTION;
                params = paramsBuilder.group(groupEntity).user(user).build();
                break;
            default:
                break;
        }

        if (template == null) {
            return null;
        }

        return new EmailNotificationBuilder()
                .to(user.getEmail())
                .template(template)
                .params(params)
                .build();
    }

    private MemberEntity convertToMemberEntity(io.gravitee.repository.management.model.Membership membership) {
        final MemberEntity member = new MemberEntity();
        final UserEntity userEntity = userService.findById(membership.getMemberId());
        member.setId(membership.getMemberId());
        member.setCreatedAt(membership.getCreatedAt());
        member.setUpdatedAt(membership.getUpdatedAt());
        member.setDisplayName(userEntity.getDisplayName());
        member.setEmail(userEntity.getEmail());
        member.setReferenceId(membership.getReferenceId());
        member.setReferenceType(convert(membership.getReferenceType()));
        if (membership.getRoleId() != null) {
            RoleEntity role = roleService.findById(membership.getRoleId());
            member.setPermissions(role.getPermissions());
            List<RoleEntity> roles = new ArrayList<>();
            roles.add(role);
            member.setRoles(roles);
        }

        return member;
    }

    private UserEntity findUserFromMembershipMember(MembershipMember member) {
        UserEntity userEntity;
        if (member.getMemberId() != null) {
            userEntity = userService.findById(member.getMemberId());
        } else {
            // We have a user reference, meaning that the user is coming from an external system
            // User does not exist so we are looking into defined providers
            Optional<io.gravitee.rest.api.model.providers.User> providerUser = identityService.findByReference(member.getReference());
            if (providerUser.isPresent()) {
                User identityUser = providerUser.get();
                userEntity = findOrCreateUser(identityUser);
            } else {
                throw new UserNotFoundException(member.getReference());
            }
        }
        return userEntity;
    }

    private UserEntity findOrCreateUser(User identityUser) {
        UserEntity userEntity;
        try {
            userEntity = userService.findBySource(identityUser.getSource(), identityUser.getSourceId(),false);
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
                for(Map.Entry<String, String> role : identityUser.getRoles().entrySet()) {
                    MembershipReferenceType membershipReferenceType = MembershipReferenceType.valueOf(role.getKey());
                    MembershipReference reference = null;
                    if(membershipReferenceType == MembershipReferenceType.ORGANIZATION) {
                        reference = new MembershipReference(membershipReferenceType, GraviteeContext.getCurrentOrganization());
                    } else if(membershipReferenceType == MembershipReferenceType.ENVIRONMENT) {
                        reference = new MembershipReference(membershipReferenceType, GraviteeContext.getCurrentEnvironment());
                    }

                    if (reference != null) {
                        this.addRoleToMemberOnReference(
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
    private void assertRoleScopeAllowedForReference(MembershipReference reference, MembershipRole role) {
        Optional<RoleEntity> optRoleEntity = roleService.findByScopeAndName(role.getScope(), role.getName());
        if(optRoleEntity.isPresent()) {
            RoleEntity roleEntity = optRoleEntity.get();
            if (
                    (MembershipReferenceType.API == reference.getType() && RoleScope.API != roleEntity.getScope())
                    || (MembershipReferenceType.APPLICATION == reference.getType() && RoleScope.APPLICATION != roleEntity.getScope())
                    || (MembershipReferenceType.GROUP == reference.getType() && RoleScope.GROUP != roleEntity.getScope() && RoleScope.API != roleEntity.getScope() && RoleScope.APPLICATION != roleEntity.getScope())
                    || (MembershipReferenceType.GROUP == reference.getType() && SystemRole.PRIMARY_OWNER.name().equals(role.getName()))
               ) {
                throw new NotAuthorizedMembershipException(role.getName());
            }
        } else {
            throw new RoleNotFoundException(role.getScope().name() + "_" + role.getName());
        }
    }

    /**
     * assert that the role's name is allowed for the given reference
     */
    private void assertRoleNameAllowedForReference(MembershipReference reference, MembershipRole role) {
        if (MembershipReferenceType.GROUP == reference.getType() && SystemRole.PRIMARY_OWNER.name().equals(role.getName())) {
            throw new NotAuthorizedMembershipException(role.getName());
        }
    }

    @Override
    public void deleteMembership(String membershipId) {
        try {
            Optional<io.gravitee.repository.management.model.Membership> membership = membershipRepository.findById(membershipId);
            if (membership.isPresent()) {
                LOGGER.debug("Delete membership {}", membership.get());
                membershipRepository.delete(membershipId);
                createAuditLog(MEMBERSHIP_DELETED, new Date(), membership.get(), null);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete membership {}", membershipId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete membership " + membershipId, ex);
        }
    }

    @Override
    public void deleteReference(MembershipReferenceType referenceType, String referenceId) {
        try {
            Set<io.gravitee.repository.management.model.Membership> memberships = membershipRepository.findByReferenceAndRoleId(convert(referenceType), referenceId, null);
            if (!memberships.isEmpty()) {
                for(io.gravitee.repository.management.model.Membership membership: memberships) {
                    LOGGER.debug("Delete membership {}", membership.getId());
                    membershipRepository.delete(membership.getId());
                    createAuditLog(MEMBERSHIP_DELETED, new Date(), membership, null);
                }
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete memberships for {} {}", referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete memberships for " + referenceType + " " + referenceId, ex);
        }
        
    }

    @Override
    public void deleteReferenceMember(MembershipReferenceType referenceType, String referenceId, MembershipMemberType memberType, String memberId) {
        try {
            Set<io.gravitee.repository.management.model.Membership> memberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(memberId, convert(memberType), convert(referenceType), referenceId);
            if (!memberships.isEmpty()) {
                for(io.gravitee.repository.management.model.Membership membership: memberships) {
                    LOGGER.debug("Delete membership {}", membership.getId());
                    membershipRepository.delete(membership.getId());
                    createAuditLog(MEMBERSHIP_DELETED, new Date(), membership, null);
                };
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete memberships for {} {} {} {}", referenceType, referenceId, memberType, memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete memberships for " + referenceType + " " + referenceId + " " + memberType + " " + memberId, ex);
        }
    }

    @Override
    public List<UserMembership> findUserMembership(MembershipReferenceType type, String userId) {
        if (type == null || (!type.equals(MembershipReferenceType.API) && !type.equals(MembershipReferenceType.APPLICATION) && !type.equals(MembershipReferenceType.GROUP))) {
            return Collections.emptyList();
        }
        try {
            Map<String, RoleEntity> roleMap = roleService.findByScope(roleService.findScopeByMembershipReferenceType(type)).stream().collect(Collectors.toMap(RoleEntity::getId, r -> r));
            HashMap<Integer, UserMembership> userMembershipMap = new HashMap<>();
            membershipRepository
                    .findByMemberIdAndMemberTypeAndReferenceType(
                            userId, 
                            io.gravitee.repository.management.model.MembershipMemberType.USER, 
                            convert(type))
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


            if (type.equals(MembershipReferenceType.APPLICATION) || type.equals(MembershipReferenceType.API)) {
                Set<GroupEntity> userGroups = groupService.findByUser(userId);
                for(GroupEntity group: userGroups) {
                    userMemberships.addAll(
                            membershipRepository
                                    .findByMemberIdAndMemberTypeAndReferenceType(
                                            group.getId(),
                                            io.gravitee.repository.management.model.MembershipMemberType.GROUP,
                                            convert(type)
                                    )
                                    .stream()
                                    .map(membership -> {
                                        UserMembership userMembership = new UserMembership();
                                        userMembership.setType(type.name());
                                        userMembership.setReference(membership.getReferenceId());
                                        return userMembership;
                                    })
                                    .collect(Collectors.toSet())
                    );
                };
            }

            return new ArrayList<>(userMemberships);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove user {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove user " + userId, ex);
        }
    }

    @Override
    public Metadata findUserMembershipMetadata(List<UserMembership> memberships, MembershipReferenceType type) {
        if (memberships == null || memberships.isEmpty() ||
                type == null || (!type.equals(MembershipReferenceType.API) && !type.equals(MembershipReferenceType.APPLICATION) && !type.equals(MembershipReferenceType.GROUP)) ) {
            return new Metadata();
        }
        try {
            Metadata metadata = new Metadata();
            if (type.equals(MembershipReferenceType.API)) {
                ApiCriteria.Builder criteria = new ApiCriteria.Builder();
                ApiFieldExclusionFilter filter = (new ApiFieldExclusionFilter.Builder()).excludeDefinition().excludePicture().build();
                criteria.ids(memberships.stream().map(UserMembership::getReference).toArray(String[]::new));
                apiRepository.search(criteria.build(), filter).forEach(api -> {
                    metadata.put(api.getId(), "name", api.getName());
                    metadata.put(api.getId(), "version", api.getVersion());
                    metadata.put(api.getId(), "visibility", api.getVisibility());
                });
            } else if (type.equals(MembershipReferenceType.APPLICATION)) {
                applicationRepository.findByIds(memberships.stream().map(UserMembership::getReference).collect(Collectors.toList())).forEach(application -> {
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
    public Set<MemberEntity> getMembersByReference(MembershipReferenceType referenceType, String referenceId) {
        return this.getMembersByReferenceAndRole(referenceType, referenceId, null);
    }

    @Override
    public Set<MemberEntity> getMembersByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, String role) {
        return this.getMembersByReferencesAndRole(referenceType, Arrays.asList(referenceId), role);
    }

    @Override
    public Set<MemberEntity> getMembersByReferencesAndRole(MembershipReferenceType referenceType, List<String> referencesId, String role) {
        try {
            LOGGER.debug("Get members for {} {}", referenceType, referencesId);
            Set<io.gravitee.repository.management.model.Membership> memberships = membershipRepository.findByReferencesAndRoleId(
                    convert(referenceType),
                    referencesId,
                    role);
            Map<String, MemberEntity> results = new HashMap<>();
            memberships.stream()
            .filter(member -> member.getMemberType() == io.gravitee.repository.management.model.MembershipMemberType.USER)
            .map(this::convertToMemberEntity)
            .forEach(member -> {
                String key = member.getId()+member.getReferenceId();
                MemberEntity existingEntity = results.get(key);
                if(existingEntity == null) {
                    results.put(key, member);
                    existingEntity = member;
                } else {
                    Set<RoleEntity> existingRoles = new HashSet<>(existingEntity.getRoles());
                    existingRoles.addAll(member.getRoles());
                    existingEntity.setRoles(new ArrayList<>(existingRoles));
                }
            });
            return new HashSet<>(results.values());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get members for {} {}", referenceType, referencesId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referencesId, ex);
        }
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
            return membershipRepository.findByMemberIdAndMemberType(memberId, convert(memberType))
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
                    ;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} ", memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + memberId, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByMemberAndReference(MembershipMemberType memberType, String memberId,
            MembershipReferenceType referenceType) {
        try {
            return membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(memberId, convert(memberType), convert(referenceType))
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
                    ;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} ", memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + memberId, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByMemberAndReferenceAndRole(MembershipMemberType memberType,
            String memberId, MembershipReferenceType referenceType, String role) {
        try {
            return membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(memberId, convert(memberType), convert(referenceType), role)
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
                    ;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} ", memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + memberId, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByMembersAndReference(MembershipMemberType memberType,
            List<String> memberIds, MembershipReferenceType referenceType) {
        try {
            
            return membershipRepository.findByMemberIdsAndMemberTypeAndReferenceType(memberIds, convert(memberType), convert(referenceType))
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
                    ;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} ", memberIds, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + memberIds, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByReference(MembershipReferenceType referenceType, String referenceId) {
        try {
            return membershipRepository.findByReferenceAndRoleId(convert(referenceType), referenceId, null)
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
                    ;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} {} ", referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + referenceType + " " + referenceId, ex);
        }
    }

    @Override
    public Set<MembershipEntity> getMembershipsByReferenceAndRole(MembershipReferenceType referenceType,
            String referenceId, String role) {
        try {
            return membershipRepository.findByReferenceAndRoleId(convert(referenceType), referenceId, role)
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
                    ;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} {} and role", referenceType, referenceId, role, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + referenceType + " " + referenceId + " and role " + role, ex);
        }
    }
    
    @Override
    public Set<MembershipEntity> getMembershipsByReferencesAndRole(MembershipReferenceType referenceType,
            List<String> referenceIds, String role) {
        try {
            return membershipRepository.findByReferencesAndRoleId(convert(referenceType), referenceIds, role)
                    .stream()
                    .map(this::convert)
                    .collect(Collectors.toSet())
                    ;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get memberships for {} {} and role", referenceType, referenceIds, role, ex);
            throw new TechnicalManagementException("An error occurs while trying to get memberships for " + referenceType + " " + referenceIds + " and role " + role, ex);
        }
    }

    @Override
    public MembershipEntity getPrimaryOwner(MembershipReferenceType referenceType, String referenceId) {
        RoleScope poRoleScope;
        if(referenceType == MembershipReferenceType.API) {
            poRoleScope = RoleScope.API;
        } else if(referenceType == MembershipReferenceType.APPLICATION) {
            poRoleScope = RoleScope.APPLICATION;
        } else {
            throw new RoleNotFoundException(referenceType.name() + "_PRIMARY_OWNER");
        }
        Optional<RoleEntity> poRole = roleService.findByScopeAndName(poRoleScope, SystemRole.PRIMARY_OWNER.name());
        if(poRole.isPresent()) {
            try {
                Optional<io.gravitee.repository.management.model.Membership> poMember = membershipRepository.findByReferenceAndRoleId(convert(referenceType), referenceId, poRole.get().getId())
                    .stream()
                    .findFirst();
                if(poMember.isPresent()) {
                    return convert(poMember.get());
                } else {
                    return null;
                }
            } catch (TechnicalException ex) {
                LOGGER.error("An error occurs while trying to get primary owner for {} {} and role", referenceType, referenceId, ex);
                throw new TechnicalManagementException("An error occurs while trying to get primary owner for " + referenceType + " " + referenceId, ex);
            }
        } else {
            throw new RoleNotFoundException(referenceType.name() + "_PRIMARY_OWNER");
        }
    }

    @Override
    public Set<RoleEntity> getRoles(MembershipReferenceType referenceType, String referenceId, MembershipMemberType memberType,
            String memberId) {
        try {
            LOGGER.debug("Get role for {} {} and member {} {}", referenceType, referenceId, memberType, memberId);

            return roles.get(referenceType.name() + referenceId + memberType + memberId, () ->
                    membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(memberId, convert(memberType), convert(referenceType), referenceId)
                        .stream()
                        .map(io.gravitee.repository.management.model.Membership::getRoleId)
                        .map(roleService::findById)
                        .collect(Collectors.toSet()));
        } catch (Exception ex) {
            final String message = "An error occurs while trying to get roles for " + referenceType + " " + referenceId + " " + memberType + " " + memberId;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }
    
    @Override
    public MemberEntity getUserMember(MembershipReferenceType referenceType, String referenceId, String userId) {
        try {
            Set<io.gravitee.repository.management.model.Membership> userMemberships = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(userId, convert(MembershipMemberType.USER), convert(referenceType), referenceId);
            
            //Get entity groups
            Set<String> entityGroups = new HashSet<>();
            switch(referenceType) {
                case API:
                    entityGroups = apiService.findById(referenceId).getGroups();
                    break;
                case APPLICATION:
                    entityGroups = applicationService.findById(referenceId).getGroups();
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
            if(!userMemberships.isEmpty()) {
                userDirectRoles = userMemberships.stream()
                        .map(io.gravitee.repository.management.model.Membership::getRoleId)
                        .map(roleService::findById)
                        .collect(Collectors.toSet());
                
                userRoles.addAll(userDirectRoles);
            }
            memberEntity.setRoles(new ArrayList<>(userDirectRoles));
            
            if(entityGroups != null && !entityGroups.isEmpty()) {
                for(String group: entityGroups) {
                    userRoles.addAll(membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(userId, convert(MembershipMemberType.USER), convert(MembershipReferenceType.GROUP), group)
                            .stream()
                            .map(io.gravitee.repository.management.model.Membership::getRoleId)
                            .map(roleService::findById)
                            .filter(role -> role.getScope().name().equals(referenceType.name()))
                            .collect(Collectors.toSet()));
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
            throw new TechnicalManagementException("An error occurs while trying to get roles for " + referenceType + " " + referenceId + " " + userId, ex);
        }
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
            for(int i = 0; i< characters.length; i++) {
                chars[i] = characters[i];
            }
            permissions.put(k, chars);
        });
        return permissions;
    }
    
    @Override
    public Map<String, char[]> getUserMemberPermissions(MembershipReferenceType referenceType, String referenceId, String userId) {
        MemberEntity member = this.getUserMember(referenceType, referenceId, userId);
        if (member != null) {
         return member.getPermissions();
        }
        return emptyMap();
    }
    
    @Override
    public Map<String, char[]> getUserMemberPermissions(ApiEntity api, String userId) {
        return getUserMemberPermissions(MembershipReferenceType.API, api.getId(), userId);
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(ApplicationEntity application, String userId) {
        return getUserMemberPermissions(MembershipReferenceType.APPLICATION, application.getId(), userId);
    }

    @Override
    public Map<String, char[]> getUserMemberPermissions(GroupEntity group, String userId) {
        return getUserMemberPermissions(MembershipReferenceType.GROUP, group.getId(), userId);
    }

    @Override
    public void removeRole(MembershipReferenceType referenceType, String referenceId, MembershipMemberType memberType,
            String memberId, String roleId) {
        try {
            Set<io.gravitee.repository.management.model.Membership> membershipsToDelete = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(memberId, convert(memberType), convert(referenceType), referenceId, roleId);
            for(io.gravitee.repository.management.model.Membership m: membershipsToDelete) {
                membershipRepository.delete(m.getId());
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove role {} from member {} {} for {} {}", roleId, memberType, memberId, referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove role " + roleId + " from member " + memberType + " " + memberId + " for " + referenceType + " " + referenceId, ex);
        }

        
    }    
    @Override
    public void removeRoleUsage(String oldRoleId, String newRoleId) {
        try {
            Set<io.gravitee.repository.management.model.Membership> membershipsWithOldRole = membershipRepository.findByRoleId(oldRoleId);
            for (io.gravitee.repository.management.model.Membership membership : membershipsWithOldRole) {
                Set<io.gravitee.repository.management.model.Membership> membershipsWithNewRole = membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(membership.getMemberId(), membership.getMemberType(), membership.getReferenceType(), membership.getReferenceId(), newRoleId);
                String oldMembershipId = membership.getId();
                if(membershipsWithNewRole.isEmpty()) {
                    membership.setId(RandomString.generate());
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
        try {
            for(io.gravitee.repository.management.model.Membership membership : membershipRepository.findByMemberIdAndMemberType(memberId, convert(memberType))) {
                membershipRepository.delete(membership.getId());
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to remove member {} {}", memberType, memberId, ex);
            throw new TechnicalManagementException("An error occurs while trying to remove " + memberType + " " + memberId, ex);
        }
    }
    
    @Override
    public void transferApiOwnership(String apiId, MembershipMember member, List<RoleEntity> newPrimaryOwnerRoles) {
        this.transferOwnership(MembershipReferenceType.API, RoleScope.API, apiId, member, newPrimaryOwnerRoles);
    }

    @Override
    public void transferApplicationOwnership(String applicationId, MembershipMember member, List<RoleEntity> newPrimaryOwnerRoles) {
        this.transferOwnership(MembershipReferenceType.APPLICATION, RoleScope.APPLICATION, applicationId, member, newPrimaryOwnerRoles);
    }

    private void transferOwnership(MembershipReferenceType membershipReferenceType, RoleScope roleScope, String itemId, MembershipMember member, List<RoleEntity> newPrimaryOwnerRoles) {
        List<RoleEntity> newRoles;
        if (newPrimaryOwnerRoles == null || newPrimaryOwnerRoles.isEmpty()) {
            newRoles = roleService.findDefaultRoleByScopes(roleScope);
        } else {
            newRoles = newPrimaryOwnerRoles;
        }

        MembershipEntity primaryOwner = this.getPrimaryOwner(membershipReferenceType, itemId);
        // Set the new primary owner
        MemberEntity newPrimaryOwnerMember = this.addRoleToMemberOnReference(
                new MembershipReference(membershipReferenceType, itemId),
                new MembershipMember(member.getMemberId(), member.getReference(), member.getMemberType()),
                new MembershipRole(roleScope, PRIMARY_OWNER.name()));

        Optional<RoleEntity> optPoRoleEntity = roleService.findByScopeAndName(roleScope, PRIMARY_OWNER.name());
        if(optPoRoleEntity.isPresent()) {
            RoleEntity poRoleEntity = optPoRoleEntity.get();
            // remove previous role of the new primary owner
            this.getRoles(membershipReferenceType, itemId, member.getMemberType(), newPrimaryOwnerMember.getId()).forEach(role -> {
                if (!role.getId().equals(poRoleEntity.getId())) {
                    this.removeRole(membershipReferenceType, itemId, MembershipMemberType.USER, newPrimaryOwnerMember.getId(), role.getId());
                }
            });
        
            // Update the role for previous primary_owner
            this.removeRole(membershipReferenceType, itemId, MembershipMemberType.USER, primaryOwner.getMemberId(), poRoleEntity.getId());

            for(RoleEntity newRole : newRoles) {
                this.addRoleToMemberOnReference(
                        new MembershipReference(membershipReferenceType, itemId),
                        new MembershipMember(primaryOwner.getMemberId(), null, MembershipMemberType.USER),
                        new MembershipRole(roleScope, newRole.getName()));
            }
        }
    }

    @Override
    public MemberEntity updateRoleToMemberOnReference(MembershipReference reference, MembershipMember member,
                                                      MembershipRole role) {
        return updateRoleToMemberOnReference(reference, member, role, null, true);
    }

    @Override
    public MemberEntity updateRoleToMemberOnReference(MembershipReference reference, MembershipMember member,
            MembershipRole role, String source, boolean notify) {
        try {
            Set<io.gravitee.repository.management.model.Membership> existingMemberships = this.membershipRepository.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(member.getMemberId(), convert(member.getMemberType()), convert(reference.getType()), reference.getId());
            if (existingMemberships != null && !existingMemberships.isEmpty()) {
                existingMemberships.forEach(membership -> this.deleteMembership(membership.getId()));
            }
            return _addRoleToMemberOnReference(reference, member, role, source, false);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update member for {} {}", reference.getType(), reference.getId(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update member for " + reference.getType() + " " + reference.getId(), ex);
        }

    }
}
