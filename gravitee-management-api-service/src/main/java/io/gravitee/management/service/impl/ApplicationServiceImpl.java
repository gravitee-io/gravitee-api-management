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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.*;
import io.gravitee.management.model.application.*;
import io.gravitee.management.model.notification.GenericNotificationConfigEntity;
import io.gravitee.management.model.parameters.Key;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.model.subscription.SubscriptionQuery;
import io.gravitee.management.service.*;
import io.gravitee.management.service.configuration.application.ClientRegistrationService;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import io.gravitee.management.service.notification.ApplicationHook;
import io.gravitee.management.service.notification.HookScope;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Application.AuditEvent.*;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApplicationServiceImpl extends AbstractService implements ApplicationService {

    private final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private GroupService groupService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private GenericNotificationConfigService genericNotificationConfigService;

    @Autowired
    private ClientRegistrationService clientRegistrationService;

    @Autowired
    private ParameterService parameterService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ApplicationEntity findById(String applicationId) {
        try {
            LOGGER.debug("Find application by ID: {}", applicationId);

            Optional<Application> application = applicationRepository.findById(applicationId);

            if (application.isPresent()) {
                Optional<Membership> primaryOwnerMembership = membershipRepository.findByReferenceAndRole(
                        MembershipReferenceType.APPLICATION,
                        applicationId,
                        RoleScope.APPLICATION,
                        SystemRole.PRIMARY_OWNER.name())
                        .stream()
                        .findFirst();
                if (! primaryOwnerMembership.isPresent()) {
                    LOGGER.error("The Application {} doesn't have any primary owner.", applicationId);
                    return convert(application.get(), null);
                }
                return convert(application.get(), userService.findById(primaryOwnerMembership.get().getUserId()));
            }

            throw new ApplicationNotFoundException(applicationId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an application using its ID {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an application using its ID " + applicationId, ex);
        }
    }

    @Override
    public Set<ApplicationListItem> findByUser(String username) {
        try {
            LOGGER.debug("Find applications for user {}", username);

            //find applications where the user is a member
            List<String> appIds = membershipRepository.findByUserAndReferenceType(username, MembershipReferenceType.APPLICATION).stream()
                    .map(Membership::getReferenceId).collect(Collectors.toList());
            final Set<Application> applications =
                    applicationRepository.findByIds(appIds).stream().
                            filter(app -> ApplicationStatus.ACTIVE.equals(app.getStatus())).
                            collect(Collectors.toSet());

            //find applications which be part of the same group as the user
            List<String> groupIds = membershipRepository.findByUserAndReferenceType(username, MembershipReferenceType.GROUP).stream()
                    .filter(m -> m.getRoles().keySet().contains(RoleScope.APPLICATION.getId()))
                    .map(Membership::getReferenceId).collect(Collectors.toList());
            applications.addAll(applicationRepository.findByGroups(groupIds, ApplicationStatus.ACTIVE));

            if (applications.isEmpty()) {
                return emptySet();
            }

            return this.convertToList(applications);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find applications for user {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for user " + username, ex);
        }
    }

    @Override
    public Set<ApplicationListItem> findByName(String name) {
        LOGGER.debug("Find applications by name {}", name);
        try {
            if (name == null || name.trim().isEmpty()) {
                return emptySet();
            }
            Set<Application> applications = applicationRepository.
                    findByName(name.trim()).stream().
                    filter(app -> ApplicationStatus.ACTIVE.equals(app.getStatus())).
                    collect(Collectors.toSet());
            return convertToList(applications);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find applications for name {}", name, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for name " + name, ex);
        }
    }

    @Override
    public Set<ApplicationListItem> findByGroups(List<String> groupIds) {
        LOGGER.debug("Find applications by groups {}", groupIds);
        try {
            return convertToList(applicationRepository.findByGroups(groupIds, ApplicationStatus.ACTIVE));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find applications for groups {}", groupIds, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for groups " + groupIds, ex);
        }
    }

    @Override
    public Set<ApplicationListItem> findAll() {
        try {
            LOGGER.debug("Find all applications");

            final Set<Application> applications = applicationRepository.findAll(ApplicationStatus.ACTIVE);

            if (applications == null || applications.isEmpty()) {
                return emptySet();
            }

            return this.convertToList(applications);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all applications", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all applications", ex);
        }
    }

    @Override
    public ApplicationEntity create(NewApplicationEntity newApplicationEntity, String userId) {
        try {
            LOGGER.debug("Create {} for user {}", newApplicationEntity, userId);

            // Check that only one settings is defined
            if (newApplicationEntity.getSettings().getApp() != null && newApplicationEntity.getSettings().getoAuthClient() != null) {
                throw new InvalidApplicationTypeException();
            }

            // Check that a type is defined
            if (newApplicationEntity.getSettings().getApp() == null && newApplicationEntity.getSettings().getoAuthClient() == null) {
                throw new InvalidApplicationTypeException();
            }

            // Create application metadata
            Map<String, String> metadata = new HashMap<>();

            // Create a simple "internal" application
            if (newApplicationEntity.getSettings().getApp() != null)  {
                // If clientId is set, check for uniqueness
                String clientId = newApplicationEntity.getSettings().getApp().getClientId();

                if (clientId != null && ! clientId.trim().isEmpty()) {
                    LOGGER.debug("Check that client_id is unique among all applications");
                    final Set<Application> applications = applicationRepository.findAll(ApplicationStatus.ACTIVE);
                    final boolean alreadyExistingApp = applications.stream().anyMatch(app ->
                            app.getMetadata() != null && clientId.equals(app.getMetadata().get("client_id")));
                    if (alreadyExistingApp) {
                        LOGGER.error("An application already exists with the same client_id");
                        throw new ClientIdAlreadyExistsException(clientId);
                    }
                }
            }  else {
                // Check that client registration is enabled
                checkClientRegistrationEnabled();

                // Create an OAuth client
                ClientRegistrationResponse registrationResponse = clientRegistrationService.register(newApplicationEntity);
                try {
                    metadata.put("client_id", registrationResponse.getClientId());
                    metadata.put("registration_payload", mapper.writeValueAsString(registrationResponse));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            if (newApplicationEntity.getGroups() != null && !newApplicationEntity.getGroups().isEmpty()) {
                //throw a NotFoundException if the group doesn't exist
                groupService.findByIds(newApplicationEntity.getGroups());
            }

            Application application = convert(newApplicationEntity);
            application.setId( UUID.toString(UUID.random()));
            application.setStatus(ApplicationStatus.ACTIVE);

            metadata.forEach((key, value) -> application.getMetadata().put(key, value));

            // Add Default groups
            Set<String> defaultGroups = groupService.findByEvent(GroupEvent.APPLICATION_CREATE).
                    stream().
                    map(GroupEntity::getId).
                    collect(Collectors.toSet());
            if (!defaultGroups.isEmpty() && application.getGroups() == null) {
                application.setGroups(defaultGroups);
            } else if (!defaultGroups.isEmpty()) {
                application.getGroups().addAll(defaultGroups);
            }

            // Set date fields
            application.setCreatedAt(new Date());
            application.setUpdatedAt(application.getCreatedAt());

            Application createdApplication = applicationRepository.create(application);
            // Audit
            auditService.createApplicationAuditLog(
                    createdApplication.getId(),
                    Collections.emptyMap(),
                    APPLICATION_CREATED,
                    isAuthenticated()?getAuthenticatedUsername():userId,
                    createdApplication.getCreatedAt(),
                    null,
                    createdApplication);

            // Add the primary owner of the newly created API
            Membership membership = new Membership(userId, createdApplication.getId(), MembershipReferenceType.APPLICATION);
            membership.setRoles(singletonMap(RoleScope.APPLICATION.getId(), SystemRole.PRIMARY_OWNER.name()));
            membership.setCreatedAt(application.getCreatedAt());
            membership.setUpdatedAt(application.getCreatedAt());
            membershipRepository.create(membership);
            // create the default mail notification
            UserEntity userEntity = userService.findById(userId);
            if (userEntity.getEmail() != null && !userEntity.getEmail().isEmpty()) {
                GenericNotificationConfigEntity notificationConfigEntity = new GenericNotificationConfigEntity();
                notificationConfigEntity.setName("Default Mail Notifications");
                notificationConfigEntity.setReferenceType(HookScope.APPLICATION.name());
                notificationConfigEntity.setReferenceId(createdApplication.getId());
                notificationConfigEntity.setHooks(Arrays.stream(ApplicationHook.values()).map(Enum::name).collect(Collectors.toList()));
                notificationConfigEntity.setNotifier(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID);
                notificationConfigEntity.setConfig(userEntity.getEmail());
                genericNotificationConfigService.create(notificationConfigEntity);
            }

            return convert(createdApplication, userEntity);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {} for user {}", newApplicationEntity, userId, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + newApplicationEntity + " for user " + userId, ex);
        }
    }

    @Override
    public ApplicationEntity update(String applicationId, UpdateApplicationEntity updateApplicationEntity) {
        try {
            LOGGER.debug("Update application {}", applicationId);
            if (updateApplicationEntity.getGroups() != null && !updateApplicationEntity.getGroups().isEmpty()) {
                //throw a NotFoundException if the group doesn't exist
                groupService.findByIds(updateApplicationEntity.getGroups());
            }

            Optional<Application> optApplicationToUpdate = applicationRepository.findById(applicationId);
            if (!optApplicationToUpdate.isPresent()) {
                throw new ApplicationNotFoundException(applicationId);
            }

            // Check that only one settings is defined
            if (updateApplicationEntity.getSettings().getApp() != null && updateApplicationEntity.getSettings().getoAuthClient() != null) {
                throw new InvalidApplicationTypeException();
            }

            // Check that a type is defined
            if (updateApplicationEntity.getSettings().getApp() == null && updateApplicationEntity.getSettings().getoAuthClient() == null) {
                throw new InvalidApplicationTypeException();
            }

            // Update application metadata
            Map<String, String> metadata = new HashMap<>();

            // Update a simple application
            if (optApplicationToUpdate.get().getType() == ApplicationType.SIMPLE &&
                    updateApplicationEntity.getSettings().getApp() != null) {
                // If clientId is set, check for uniqueness
                String clientId = updateApplicationEntity.getSettings().getApp().getClientId();

                if (clientId != null && !clientId.trim().isEmpty()) {
                    LOGGER.debug("Check that client_id is unique among all applications");
                    final Set<Application> applications = applicationRepository.findAll(ApplicationStatus.ACTIVE);
                    final Optional<Application> byClientId = applications.stream().filter(
                            app -> app.getMetadata() != null && clientId.equals(app.getMetadata().get("client_id"))).findAny();
                    if (byClientId.isPresent() && !byClientId.get().getId().equals(optApplicationToUpdate.get().getId())) {
                        LOGGER.error("An application already exists with the same client_id");
                        throw new ClientIdAlreadyExistsException(clientId);
                    }
                }
            } else {
                // Check that client registration is enabled
                checkClientRegistrationEnabled();

                // Update an OAuth client
                ClientRegistrationResponse registrationResponse = clientRegistrationService.update(
                        optApplicationToUpdate.get().getMetadata().get("registration_payload"), updateApplicationEntity);

                try {
                    metadata.put("client_id", registrationResponse.getClientId());
                    metadata.put("registration_payload", mapper.writeValueAsString(registrationResponse));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }

            Application application = convert(updateApplicationEntity);
            application.setId(applicationId);
            application.setStatus(ApplicationStatus.ACTIVE);
            application.setType(optApplicationToUpdate.get().getType());
            application.setCreatedAt(optApplicationToUpdate.get().getCreatedAt());
            application.setUpdatedAt(new Date());

            metadata.forEach((key, value) -> application.getMetadata().put(key, value));

            Application updatedApplication =  applicationRepository.update(application);

            // Audit
            auditService.createApplicationAuditLog(
                    updatedApplication.getId(),
                    Collections.emptyMap(),
                    APPLICATION_UPDATED,
                    updatedApplication.getUpdatedAt(),
                    optApplicationToUpdate.get(),
                    updatedApplication);

            // Set correct client_id for all subscriptions
            SubscriptionQuery subQuery = new SubscriptionQuery();
            subQuery.setApplication(applicationId);
            subQuery.setStatuses(Collections.singleton(SubscriptionStatus.ACCEPTED));
            subscriptionService.search(subQuery).forEach(new Consumer<SubscriptionEntity>() {
                @Override
                public void accept(SubscriptionEntity subscriptionEntity) {
                    UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                    updateSubscriptionEntity.setId(subscriptionEntity.getId());
                    updateSubscriptionEntity.setStartingAt(subscriptionEntity.getStartingAt());
                    updateSubscriptionEntity.setEndingAt(subscriptionEntity.getEndingAt());

                    subscriptionService.update(updateSubscriptionEntity, application.getMetadata().get("client_id"));
                }
            });
            return convert(Collections.singleton(updatedApplication)).iterator().next();
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update application {}", applicationId, ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to update application %s", applicationId), ex);
        }
    }

    private void checkClientRegistrationEnabled() {
        if (!parameterService.findAsBoolean(Key.APPLICATION_REGISTRATION_ENABLED)) {
            throw new IllegalStateException("The client registration is disabled");
        }
    }

    @Override
    public void archive(String applicationId) {
        try {
            LOGGER.debug("Delete application {}", applicationId);
            Optional<Application> optApplication = applicationRepository.findById(applicationId);

            if(!optApplication.isPresent()) {
                throw new ApplicationNotFoundException(applicationId);
            }
            Application application = optApplication.get();
            Application previousApplication = new Application(application);
            Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApplicationAndPlan(applicationId, null);

            subscriptions.forEach(subscription -> {
                Set<ApiKeyEntity> apiKeys = apiKeyService.findBySubscription(subscription.getId());
                apiKeys.forEach(apiKey -> {
                    try {
                        apiKeyService.delete(apiKey.getKey());
                    } catch (TechnicalManagementException tme) {
                        LOGGER.error("An error occurs while deleting API Key {}", apiKey.getKey(), tme);
                    }
                });

                try {
                    subscriptionService.close(subscription.getId());
                } catch (SubscriptionNotClosableException snce) {
                    // Subscription can not be closed because it is already closed or not yet accepted
                    LOGGER.debug("The subscription can not be closed: {}", snce.getMessage());
                }
            });

            // Archive the application
            application.setUpdatedAt(new Date());
            application.setStatus(ApplicationStatus.ARCHIVED);
            applicationRepository.update(application);
            // Audit
            auditService.createApplicationAuditLog(
                    application.getId(),
                    Collections.emptyMap(),
                    APPLICATION_ARCHIVED,
                    application.getUpdatedAt(),
                    previousApplication,
                    application);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete application {}", applicationId, ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to delete application %s", applicationId), ex);
        }
    }

    private Set<ApplicationEntity> convert(Set<Application> applications) throws TechnicalException {
        if (applications == null || applications.isEmpty()){
            return Collections.emptySet();
        }
        //find primary owners usernames of each applications
        Set<Membership> memberships = membershipRepository.findByReferencesAndRole(
                MembershipReferenceType.APPLICATION,
                applications.stream().map(Application::getId).collect(Collectors.toList()),
                RoleScope.APPLICATION,
                SystemRole.PRIMARY_OWNER.name()
        );

        int poMissing = applications.size() - memberships.size();
        if (poMissing > 0) {
            Set<String> appIds = applications.stream().map(Application::getId).collect(Collectors.toSet());
            Set<String> appMembershipsIds = memberships.stream().map(Membership::getReferenceId).collect(Collectors.toSet());

            appIds.removeAll(appMembershipsIds);
            Optional<String> optionalApplicationsAsString = appIds.stream().reduce((a, b) -> a + " / " + b);

            String applicationsAsString = "?";
            if (optionalApplicationsAsString.isPresent())
                applicationsAsString = optionalApplicationsAsString.get();
            LOGGER.error("{} applications has no identified primary owners in this list {}.", poMissing , applicationsAsString);
            throw new TechnicalManagementException(poMissing + " applications has no identified primary owners in this list " + applicationsAsString + ".");
        }

        Map<String, String> applicationToUser = new HashMap<>(memberships.size());
        memberships.forEach(membership -> applicationToUser.put(membership.getReferenceId(), membership.getUserId()));

        Map<String, UserEntity> userIdToUserEntity = new HashMap<>(memberships.size());
        userService.findByIds(memberships.stream().map(Membership::getUserId).collect(Collectors.toList()))
                .forEach(userEntity -> userIdToUserEntity.put(userEntity.getId(), userEntity));

        return applications.stream()
                .map(publicApplication -> convert(publicApplication, userIdToUserEntity.get(applicationToUser.get(publicApplication.getId()))))
                .collect(Collectors.toSet());
    }

    private Set<ApplicationListItem> convertToList(Set<Application> applications) throws TechnicalException {
        Set<ApplicationEntity> entities = convert(applications);

        return entities.stream().map(new Function<ApplicationEntity, ApplicationListItem>() {
            @Override
            public ApplicationListItem apply(ApplicationEntity applicationEntity) {
                ApplicationListItem item = new ApplicationListItem();

                item.setId(applicationEntity.getId());
                item.setName(applicationEntity.getName());
                item.setDescription(applicationEntity.getDescription());
                item.setCreatedAt(applicationEntity.getCreatedAt());
                item.setUpdatedAt(applicationEntity.getUpdatedAt());
                item.setGroups(applicationEntity.getGroups());
                item.setPrimaryOwner(applicationEntity.getPrimaryOwner());
                item.setType(applicationEntity.getType());
                item.setStatus(applicationEntity.getStatus());

                ApplicationListItemSettings settings = new ApplicationListItemSettings();
                if (applicationEntity.getSettings().getApp() != null) {
                    settings.setType(applicationEntity.getSettings().getApp().getType());
                    settings.setClientId(applicationEntity.getSettings().getApp().getClientId());
                } else if (applicationEntity.getSettings().getoAuthClient() != null) {
                    settings.setClientId(applicationEntity.getSettings().getoAuthClient().getClientId());
                }
                item.setSettings(settings);

                return item;
            }
        }).collect(Collectors.toSet());
    }

    private ApplicationEntity convert(Application application, UserEntity primaryOwner) {
        if (primaryOwner == null) {
            // add a default unknown user
            primaryOwner = new UserEntity();
            primaryOwner.setId("0");
            primaryOwner.setFirstname("Unknown");
            primaryOwner.setLastname("User");
        }

        ApplicationEntity applicationEntity = new ApplicationEntity();

        applicationEntity.setId(application.getId());
        applicationEntity.setName(application.getName());
        applicationEntity.setDescription(application.getDescription());
        if (application.getType() != null) {
            applicationEntity.setType(application.getType().name());
        }
        applicationEntity.setStatus(application.getStatus().toString());
        applicationEntity.setGroups(application.getGroups());
        applicationEntity.setCreatedAt(application.getCreatedAt());
        applicationEntity.setUpdatedAt(application.getUpdatedAt());
        applicationEntity.setPrimaryOwner(new PrimaryOwnerEntity(primaryOwner));

        ApplicationSettings settings = new ApplicationSettings();
        if (application.getType() == ApplicationType.SIMPLE) {
            SimpleApplicationSettings simpleSettings = new SimpleApplicationSettings();
            if (application.getMetadata() != null) {
                if (application.getMetadata().get("client_id") != null) {
                    simpleSettings.setClientId(application.getMetadata().get("client_id"));
                }
                if (application.getMetadata().get("type") != null) {
                    simpleSettings.setType(application.getMetadata().get("type"));
                }
            }
            settings.setApp(simpleSettings);
        } else {
            OAuthClientSettings clientSettings = new OAuthClientSettings();
            if (application.getMetadata() != null) {
                try {
                    final String registrationPayload = application.getMetadata().get("registration_payload");
                    if (registrationPayload != null) {
                        final ClientRegistrationResponse registrationResponse = mapper.readValue(
                                registrationPayload, ClientRegistrationResponse.class);
                        clientSettings.setClientId(registrationResponse.getClientId());
                        clientSettings.setClientSecret(registrationResponse.getClientSecret());
                        clientSettings.setClientUri(registrationResponse.getClientUri());
                        clientSettings.setApplicationType(registrationResponse.getApplicationType());
                        clientSettings.setLogoUri(registrationResponse.getLogoUri());
                        clientSettings.setResponseTypes(registrationResponse.getResponseTypes());
                        clientSettings.setRedirectUris(registrationResponse.getRedirectUris());
                        clientSettings.setGrantTypes(registrationResponse.getGrantTypes());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            settings.setoAuthClient(clientSettings);
        }
        applicationEntity.setSettings(settings);

        return applicationEntity;
    }

    private static Application convert(NewApplicationEntity newApplicationEntity) {
        Application application = new Application();

        application.setName(newApplicationEntity.getName().trim());
        application.setDescription(newApplicationEntity.getDescription().trim());
        application.setGroups(newApplicationEntity.getGroups());

        Map<String, String> metadata = new HashMap<>();
        if (newApplicationEntity.getSettings().getApp() != null) {
            application.setType(ApplicationType.SIMPLE);
            if (newApplicationEntity.getSettings().getApp().getClientId() != null) {
                metadata.put("client_id", newApplicationEntity.getSettings().getApp().getClientId());
            }
            if (newApplicationEntity.getSettings().getApp().getType() != null) {
                metadata.put("type", newApplicationEntity.getSettings().getApp().getType());
            }
        } else {
            application.setType(ApplicationType.valueOf(newApplicationEntity.getSettings().getoAuthClient().getApplicationType().toUpperCase()));
        }

        application.setMetadata(metadata);

        return application;
    }

    private static Application convert(UpdateApplicationEntity updateApplicationEntity) {
        Application application = new Application();

        application.setName(updateApplicationEntity.getName().trim());
        application.setDescription(updateApplicationEntity.getDescription().trim());
        application.setGroups(updateApplicationEntity.getGroups());
        Map<String, String> metadata = new HashMap<>();

        if (updateApplicationEntity.getSettings().getApp() != null) {
            if (updateApplicationEntity.getSettings().getApp().getClientId() != null) {
                metadata.put("client_id", updateApplicationEntity.getSettings().getApp().getClientId());
            }
            if (updateApplicationEntity.getSettings().getApp().getType() != null) {
                metadata.put("type", updateApplicationEntity.getSettings().getApp().getType());
            }
        }

        application.setMetadata(metadata);

        return application;
    }
}
