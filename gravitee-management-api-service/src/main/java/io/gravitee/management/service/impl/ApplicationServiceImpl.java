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
import io.gravitee.management.service.IdentityService;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.EmailService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.exceptions.ApplicationAlreadyExistsException;
import io.gravitee.management.service.exceptions.ApplicationNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class ApplicationServiceImpl extends TransactionalService implements ApplicationService {

    private final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private MembershipRepository membershipRepository;

    @Override
    public ApplicationEntity findById(String applicationId) {
        try {
            LOGGER.debug("Find application by ID: {}", applicationId);

            Optional<Application> application = applicationRepository.findById(applicationId);

            if (application.isPresent()) {
                Optional<Membership> primaryOwnerMembership = membershipRepository.findByReferenceAndMembershipType(
                        MembershipReferenceType.APPLICATION,
                        applicationId,
                        MembershipType.PRIMARY_OWNER.name())
                        .stream()
                        .findFirst();
                if (!primaryOwnerMembership.isPresent()) {
                    LOGGER.error("The Application {} doesn't have any primary owner.", applicationId);
                    throw new TechnicalException("The Application " + applicationId + " doesn't have any primary owner.");
                }
                return convert(application.get(), userService.findByName(primaryOwnerMembership.get().getUserId()));
            }

            throw new ApplicationNotFoundException(applicationId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an application using its ID {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an application using its ID " + applicationId, ex);
        }
    }

    @Override
    public Set<ApplicationEntity> findByUser(String username) {
        try {
            LOGGER.debug("Find applications for user {}", username);

            List<String> appIds = membershipRepository.findByUserAndReferenceType(username, MembershipReferenceType.APPLICATION).stream()
                    .map(Membership::getReferenceId).collect(Collectors.toList());
            final Set<Application> applications = applicationRepository.findByIds(appIds);

            if (applications == null || applications.isEmpty()) {
                return emptySet();
            }

            return this.convert(applications);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find applications for user {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for user " + username, ex);
        }
    }

    @Override
    public Set<ApplicationEntity> findAll() {
        try {
            LOGGER.debug("Find all applications");

            final Set<Application> applications = applicationRepository.findAll();

            if (applications == null || applications.isEmpty()) {
                return emptySet();
            }

            return this.convert(applications);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all applications", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all applications", ex);
        }
    }

    @Override
    public ApplicationEntity create(NewApplicationEntity newApplicationEntity, String username) {
        try {
            LOGGER.debug("Create {} for user {}", newApplicationEntity, username);

            String id = UUID.toString(UUID.random());

            Optional<Application> checkApplication = applicationRepository.findById(id);
            if (checkApplication.isPresent()) {
                throw new ApplicationAlreadyExistsException(id);
            }

            Application application = convert(newApplicationEntity);

            application.setId(id);

            // Set date fields
            application.setCreatedAt(new Date());
            application.setUpdatedAt(application.getCreatedAt());

            Application createdApplication = applicationRepository.create(application);

            // Add the primary owner of the newly created API
            Membership membership = new Membership(username, createdApplication.getId(), MembershipReferenceType.APPLICATION);
            membership.setType(MembershipType.PRIMARY_OWNER.name());
            membership.setCreatedAt(application.getCreatedAt());
            membership.setUpdatedAt(application.getCreatedAt());
            membershipRepository.create(membership);

            return convert(createdApplication, userService.findByName(username));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", newApplicationEntity, username, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + newApplicationEntity + " for user " + username, ex);
        }
    }

    @Override
    public ApplicationEntity update(String applicationId, UpdateApplicationEntity updateApplicationEntity) {
        try {
            LOGGER.debug("Update application {}", applicationId);

            Optional<Application> optApplicationToUpdate = applicationRepository.findById(applicationId);
            if (!optApplicationToUpdate.isPresent()) {
                throw new ApplicationNotFoundException(applicationId);
            }

            Application application = convert(updateApplicationEntity);
            application.setId(applicationId);
            application.setUpdatedAt(new Date());

            Application updatedApplication =  applicationRepository.update(application);
            return convert(Collections.singleton(updatedApplication)).iterator().next();
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update application {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to update application " + applicationId, ex);
        }
    }

    @Override
    public void delete(String applicationId) {
        try {
            LOGGER.debug("Delete application {}", applicationId);
            Set<ApiKey> keys = apiKeyRepository.findByApplication(applicationId);
            keys.forEach(apiKey -> {
                try {
                    apiKeyRepository.delete(apiKey.getKey());
                } catch (TechnicalException e) {
                    LOGGER.error("An error occurs while deleting API Key {}", apiKey.getKey(), e);
                }
            });

            applicationRepository.delete(applicationId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete application {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete application " + applicationId, ex);
        }
    }

    @Override
    public Set<ApplicationEntity> findByApi(String apiId) {
        try {
            LOGGER.debug("Find applications for api {}", apiId);
            final Set<ApiKey> applications = apiKeyRepository.findByApi(apiId);
            return applications.stream()
                .map(application -> findById(application.getApplication()))
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get applications for api {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get applications for api " + apiId, ex);
        }
    }

    @Override
    public Set<MemberEntity> getMembers(String applicationId, io.gravitee.management.model.MembershipType membershipType) {
        try {
            LOGGER.debug("Get members for application {}", applicationId);

            Set<Membership> memberships = membershipRepository.findByReferenceAndMembershipType(
                    MembershipReferenceType.APPLICATION,
                    applicationId,
                    (membershipType == null ) ? null : membershipType.name());

            return memberships.stream()
                    .map(this::convert)
                    .collect(Collectors.toSet());

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get members for application {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for application " + applicationId, ex);
        }
    }

    @Override
    public MemberEntity getMember(String applicationId, String username) {
        try {
            LOGGER.debug("Get membership for application {} and user {}", applicationId, username);

            Optional<Membership> optionalMembership = membershipRepository.findById(username, MembershipReferenceType.APPLICATION, applicationId);

            if (optionalMembership.isPresent()) {
                return convert(optionalMembership.get());
            }

            return null;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get membership for application {} and user", applicationId, username, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for application " + applicationId + " and user " + username, ex);
        }
    }

    @Override
    public void addOrUpdateMember(String applicationId, String username, io.gravitee.management.model.MembershipType membershipType) {
        try {
            LOGGER.debug("Add a new member for applicationId {}", applicationId);

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
                    membershipRepository.findById(username, MembershipReferenceType.APPLICATION, applicationId);
            Date updateDate = new Date();
            if (optionalMembership.isPresent()) {
                optionalMembership.get().setType(membershipType.name());
                optionalMembership.get().setUpdatedAt(updateDate);
                membershipRepository.update(optionalMembership.get());
            } else {
                Membership membership = new Membership(username, applicationId, MembershipReferenceType.APPLICATION);
                membership.setType(membershipType.name());
                membership.setCreatedAt(updateDate);
                membership.setUpdatedAt(updateDate);
                membershipRepository.create(membership);
            }

            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                emailService.sendEmailNotification(new EmailNotificationBuilder()
                        .to(user.getEmail())
                        .subject("Subscription to application " + applicationId)
                        .content("applicationMember.html")
                        .params(ImmutableMap.of("application", applicationId, "username", username))
                        .build()
                );
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to add member for applicationId {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to add member for applicationId " + applicationId, ex);
        }
    }

    @Override
    public void deleteMember(String applicationId, String username) {
        try {
            LOGGER.debug("Delete member {} for application {}", username, applicationId);

            membershipRepository.delete(new Membership(username, applicationId, MembershipReferenceType.APPLICATION));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete member {} for application {}", username, applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete member " + username + " for application " + applicationId, ex);
        }
    }

    private Set<ApplicationEntity> convert(Set<Application> applications) throws TechnicalException {
        //find primary owners usernames of each applications
        Set<Membership> memberships = membershipRepository.findByReferencesAndMembershipType(
                MembershipReferenceType.APPLICATION,
                applications.stream().map(Application::getId).collect(Collectors.toList()),
                MembershipType.PRIMARY_OWNER.name()
        );

        int poMissing = applications.size() - memberships.size();
        if (poMissing > 0) {
            Optional<String> optionalApplicationsAsString = applications.stream().map(Application::getId).reduce((a, b) -> a + " / " + b);
            String applicationsAsString = "?";
            if (optionalApplicationsAsString.isPresent())
                applicationsAsString = optionalApplicationsAsString.get();
            LOGGER.error("{} applications has no identified primary owners in this list {}.", poMissing , applicationsAsString);
            throw new TechnicalManagementException(poMissing + " applications has no identified primary owners in this list " + applicationsAsString + ".");
        }

        Map<String, String> applicationToUser = new HashMap<>(memberships.size());
        memberships.forEach(membership -> applicationToUser.put(membership.getReferenceId(), membership.getUserId()));

        Map<String, UserEntity> userIdToUserEntity = new HashMap<>(memberships.size());
        userService.findByNames(memberships.stream().map(Membership::getUserId).collect(Collectors.toList()))
                .forEach(userEntity -> userIdToUserEntity.put(userEntity.getUsername(), userEntity));

        return applications.stream()
                .map(publicApplication -> convert(publicApplication, userIdToUserEntity.get(applicationToUser.get(publicApplication.getId()))))
                .collect(Collectors.toSet());
    }

    private static ApplicationEntity convert(Application application, UserEntity primaryOwner) {
        ApplicationEntity applicationEntity = new ApplicationEntity();

        applicationEntity.setId(application.getId());
        applicationEntity.setName(application.getName());
        applicationEntity.setDescription(application.getDescription());
        applicationEntity.setType(application.getType());

        applicationEntity.setCreatedAt(application.getCreatedAt());
        applicationEntity.setUpdatedAt(application.getUpdatedAt());

        if (primaryOwner != null) {
            final PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
            primaryOwnerEntity.setUsername(primaryOwner.getUsername());
            primaryOwnerEntity.setLastname(primaryOwner.getLastname());
            primaryOwnerEntity.setFirstname(primaryOwner.getFirstname());
            primaryOwnerEntity.setEmail(primaryOwner.getEmail());
            applicationEntity.setPrimaryOwner(primaryOwnerEntity);
        }

        return applicationEntity;
    }

    private static Application convert(NewApplicationEntity newApplicationEntity) {
        Application application = new Application();

        application.setName(newApplicationEntity.getName().trim());
        application.setDescription(newApplicationEntity.getDescription().trim());

        if (newApplicationEntity.getType() != null) {
            application.setType(newApplicationEntity.getType().trim());
        }

        return application;
    }

    private static Application convert(UpdateApplicationEntity updateApplicationEntity) {
        Application application = new Application();

        application.setName(updateApplicationEntity.getName().trim());
        application.setDescription(updateApplicationEntity.getDescription().trim());

        if (updateApplicationEntity.getType() != null) {
            application.setType(updateApplicationEntity.getType().trim());
        }

        return application;
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

}
