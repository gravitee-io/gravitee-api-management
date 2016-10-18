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
import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.*;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

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

    @Override
    public Set<MemberEntity> getMembers(MembershipReferenceType referenceType, String referenceId) {
        return getMembers(referenceType, referenceId, null);
    }

    @Override
    public Set<MemberEntity> getMembers(MembershipReferenceType referenceType, String referenceId, MembershipType membershipType) {
        try {
            LOGGER.debug("Get members for {} {}", referenceType, referenceId);

            Set<Membership> memberships = membershipRepository.findByReferenceAndMembershipType(
                    referenceType,
                    referenceId,
                    (membershipType == null ) ? null : membershipType.name());

            return memberships.stream()
                    .map(this::convert)
                    .collect(Collectors.toSet());

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get members for {} {}", referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for "+ referenceType +" " + referenceId, ex);
        }
    }

    @Override
    public MemberEntity getMember(MembershipReferenceType referenceType, String referenceId, String username) {
        try {
            LOGGER.debug("Get membership for {} {} and user {}", referenceType, referenceId, username);

            Optional<Membership> optionalMembership = membershipRepository.findById(username, referenceType, referenceId);

            if (optionalMembership.isPresent()) {
                return convert(optionalMembership.get());
            }

            return null;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for {} {} and user", referenceType, referenceId, username, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for " + referenceType + " " + referenceId + " and user " + username, ex);
        }
    }

    @Override
    public void addOrUpdateMember(MembershipReferenceType referenceType, String referenceId, String username, MembershipType membershipType) {
        try {
            LOGGER.debug("Add a new member for {} {}", referenceType, referenceId);

            if ((MembershipReferenceType.API_GROUP.equals(referenceType)|| MembershipReferenceType.APPLICATION_GROUP.equals(referenceType))
                    && MembershipType.PRIMARY_OWNER.equals(membershipType)) {
                LOGGER.error("PRIMARY_OWNER is not authorized on Groups");
                throw new NotAuthorizedMembershipException(membershipType.name());
            }

            UserEntity user;

            try {
                user = userService.findByName(username);
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

                    user = userService.create(newUser);
                } else {
                    throw new UserNotFoundException(username);
                }
            }

            Optional<Membership> optionalMembership =
                    membershipRepository.findById(username, referenceType, referenceId);
            Date updateDate = new Date();
            if (optionalMembership.isPresent()) {
                optionalMembership.get().setType(membershipType.name());
                optionalMembership.get().setUpdatedAt(updateDate);
                membershipRepository.update(optionalMembership.get());
            } else {
                Membership membership = new Membership(username, referenceId, referenceType);
                membership.setType(membershipType.name());
                membership.setCreatedAt(updateDate);
                membership.setUpdatedAt(updateDate);
                membershipRepository.create(membership);
            }

            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                emailService.sendAsyncEmailNotification(buildEmailNotification(user, referenceType, referenceId));
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to add member for {} {}", referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to add member for " + referenceType + " " + referenceId, ex);
        }
    }

    @Override
    public void deleteMember(MembershipReferenceType referenceType, String referenceId, String username) {
        try {
            LOGGER.debug("Delete member {} for {} {}", username, referenceType, referenceId);

            membershipRepository.delete(new Membership(username, referenceId, referenceType));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete member {} for {} {}", username, referenceType, referenceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete member " + username + " for "+ referenceType +" " + referenceId, ex);
        }
    }

    private MemberEntity convert(Membership membership) {
        MemberEntity member = new MemberEntity();

        UserEntity userEntity = userService.findByName(membership.getUserId());
        member.setUsername(userEntity.getUsername());
        member.setCreatedAt(membership.getCreatedAt());
        member.setUpdatedAt(membership.getUpdatedAt());
        member.setType(MembershipType.valueOf(membership.getType()));
        member.setFirstname(userEntity.getFirstname());
        member.setLastname(userEntity.getLastname());
        member.setEmail(userEntity.getEmail());

        return member;
    }

    private EmailNotification buildEmailNotification(UserEntity user, MembershipReferenceType referenceType, String referenceId) {
        String subject = null;
        String content = null;
        Map params = null;

        switch (referenceType) {
            case APPLICATION:
                subject = "Subscription to application " + referenceId;
                content = "applicationMember.html";
                params = ImmutableMap.of("application", referenceId, "username", user.getUsername());
                break;
            case API:
                subject = "Subscription to API " + referenceId;
                content = "apiMember.html";
                params = ImmutableMap.of("api", referenceId, "username", user.getUsername());
                break;
            case APPLICATION_GROUP:
                subject = "Subscription to application group " + referenceId;
                content = "applicationGroupMember.html";
                params = ImmutableMap.of("group", referenceId, "username", user.getUsername());
                break;
            case API_GROUP:
                subject = "Subscription to API group " + referenceId;
                content = "apiGroupMember.html";
                params = ImmutableMap.of("group", referenceId, "username", user.getUsername());
                break;
        }

        return new EmailNotificationBuilder()
                .to(user.getEmail())
                .subject(subject)
                .content(content)
                .params(params)
                .build();
    }
}
