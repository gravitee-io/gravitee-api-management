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
import io.gravitee.repository.management.model.MembershipType;
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

    @Override
    public ApplicationEntity findById(String applicationId) {
        try {
            LOGGER.debug("Find application by ID: {}", applicationId);

            Optional<Application> application = applicationRepository.findById(applicationId);

            if (application.isPresent()) {
                return convert(application.get());
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

            final Set<Application> applications = applicationRepository.findByUser(username, null);

            if (applications == null || applications.isEmpty()) {
                return emptySet();
            }

            final Set<ApplicationEntity> applicationEntities = new HashSet<>(applications.size());

            applicationEntities.addAll(applications.stream()
                    .map(ApplicationServiceImpl::convert)
                    .collect(Collectors.toSet())
            );

            return applicationEntities;
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

            final Set<ApplicationEntity> applicationEntities = new HashSet<>(applications.size());

            applicationEntities.addAll(applications.stream()
                    .map(ApplicationServiceImpl::convert)
                    .collect(Collectors.toSet())
            );

            return applicationEntities;
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
            applicationRepository.saveMember(createdApplication.getId(), username, MembershipType.PRIMARY_OWNER);

            return convert(createdApplication);
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
            return convert(updatedApplication);
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

            Collection<Membership> membersRepo = applicationRepository.getMembers(applicationId,
                    (membershipType == null ) ? null : MembershipType.valueOf(membershipType.toString()));

            final Set<MemberEntity> members = new HashSet<>(membersRepo.size());

            members.addAll(
                    membersRepo.stream()
                            .map(member -> convert(member))
                            .collect(Collectors.toSet())
            );

            return members;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get members for application {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to get members for application " + applicationId, ex);
        }
    }

    @Override
    public MemberEntity getMember(String applicationId, String username) {
        try {
            LOGGER.debug("Get membership for application {} and user {}", applicationId, username);

            Membership membership = applicationRepository.getMember(applicationId, username);

            if (membership != null) {
                return convert(membership);
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

            final UserEntity user = userService.findByName(username);

            applicationRepository.saveMember(applicationId, username,
                    MembershipType.valueOf(membershipType.toString()));

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

            applicationRepository.deleteMember(applicationId, username);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete member {} for application {}", username, applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete member " + username + " for application " + applicationId, ex);
        }
    }

    private static ApplicationEntity convert(Application application) {
        ApplicationEntity applicationEntity = new ApplicationEntity();

        applicationEntity.setId(application.getId());
        applicationEntity.setName(application.getName());
        applicationEntity.setDescription(application.getDescription());
        applicationEntity.setType(application.getType());

        applicationEntity.setCreatedAt(application.getCreatedAt());
        applicationEntity.setUpdatedAt(application.getUpdatedAt());

        if (application.getMembers() != null) {
            final Optional<Membership> member = application.getMembers().stream().filter(
                    membership -> MembershipType.PRIMARY_OWNER.equals(membership.getMembershipType())
            ).findFirst();
            if (member.isPresent()) {
                final User user = member.get().getUser();
                final PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
                primaryOwnerEntity.setUsername(user.getUsername());
                primaryOwnerEntity.setLastname(user.getLastname());
                primaryOwnerEntity.setFirstname(user.getFirstname());
                primaryOwnerEntity.setEmail(user.getEmail());
                applicationEntity.setPrimaryOwner(primaryOwnerEntity);
            }
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

        member.setUser(membership.getUser().getUsername());
        member.setCreatedAt(membership.getCreatedAt());
        member.setUpdatedAt(membership.getUpdatedAt());
        member.setType(io.gravitee.management.model.MembershipType.valueOf(membership.getMembershipType().toString()));

        return member;
    }

}
