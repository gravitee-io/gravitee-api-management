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

import static io.gravitee.repository.management.model.Application.AuditEvent.APPLICATION_ARCHIVED;
import static io.gravitee.repository.management.model.Application.AuditEvent.APPLICATION_CREATED;
import static io.gravitee.repository.management.model.Application.AuditEvent.APPLICATION_RESTORED;
import static io.gravitee.repository.management.model.Application.AuditEvent.APPLICATION_UPDATED;
import static io.gravitee.repository.management.model.Application.METADATA_ADDITIONAL_CLIENT_METADATA;
import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;
import static io.gravitee.repository.management.model.Application.METADATA_REGISTRATION_PAYLOAD;
import static io.gravitee.repository.management.model.Application.METADATA_TYPE;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.gravitee.apim.core.application_certificate.crud_service.ClientCertificateCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.Origin;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.search.ApplicationCriteria;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.application.ApplicationExcludeFilter;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.application.TlsSettings;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.ClientCertificateStatus;
import io.gravitee.rest.api.model.clientcertificate.CreateClientCertificate;
import io.gravitee.rest.api.model.clientcertificate.UpdateClientCertificate;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.configuration.application.ApplicationGrantTypeEntity;
import io.gravitee.rest.api.model.configuration.application.ApplicationTypeEntity;
import io.gravitee.rest.api.model.configuration.application.registration.ClientRegistrationProviderEntity;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.settings.ConsoleConfigEntity;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationAlertService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.configuration.application.ApplicationTypeService;
import io.gravitee.rest.api.service.configuration.application.ClientRegistrationService;
import io.gravitee.rest.api.service.converter.ApplicationConverter;
import io.gravitee.rest.api.service.exceptions.ApplicationActiveException;
import io.gravitee.rest.api.service.exceptions.ApplicationArchivedException;
import io.gravitee.rest.api.service.exceptions.ApplicationClientIdException;
import io.gravitee.rest.api.service.exceptions.ApplicationGrantTypesNotAllowedException;
import io.gravitee.rest.api.service.exceptions.ApplicationGrantTypesNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationRedirectUrisNotFound;
import io.gravitee.rest.api.service.exceptions.ApplicationRenewClientSecretException;
import io.gravitee.rest.api.service.exceptions.ApplicationTypeNotFoundException;
import io.gravitee.rest.api.service.exceptions.ClientIdAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationTypeException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotClosableException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotPausedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.ws.rs.BadRequestException;
import jakarta.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.cmp.CertificateStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class ApplicationServiceImpl extends AbstractService implements ApplicationService {

    @Lazy
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CloseSubscriptionDomainService closeSubscriptionDomainService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private GenericNotificationConfigService genericNotificationConfigService;

    @Autowired
    private PortalNotificationConfigService portalNotificationConfigService;

    @Autowired
    private ClientRegistrationService clientRegistrationService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private ApplicationTypeService applicationTypeService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    @Lazy
    private ApplicationAlertService applicationAlertService;

    @Autowired
    private ApplicationConverter applicationConverter;

    @Autowired
    private PlanSearchService planSearchService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ClientCertificateCrudService clientCertificateCrudService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ApplicationEntity findById(final ExecutionContext executionContext, String applicationId) {
        try {
            log.debug("Find application by ID: {}", applicationId);

            Optional<Application> applicationOptional = applicationRepository.findById(applicationId);

            if (executionContext.hasEnvironmentId()) {
                applicationOptional = applicationOptional.filter(result ->
                    result.getEnvironmentId().equals(executionContext.getEnvironmentId())
                );
            }

            if (applicationOptional.isPresent()) {
                return convertAndFillPrimaryOwner(executionContext, applicationOptional.get());
            }

            throw new ApplicationNotFoundException(applicationId);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find an application using its ID {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an application using its ID " + applicationId, ex);
        }
    }

    @Override
    public Set<ApplicationListItem> findByIds(ExecutionContext executionContext, Collection<String> applicationIds) {
        return findByIdsAndStatus(executionContext, applicationIds, null);
    }

    @Override
    public Set<ApplicationListItem> findByIdsAndStatus(
        final ExecutionContext executionContext,
        Collection<String> applicationIds,
        ApplicationStatus applicationStatus
    ) {
        try {
            log.debug("Find application by IDs: {}", applicationIds);

            if (applicationIds.isEmpty()) {
                return Collections.emptySet();
            }

            ApplicationCriteria.ApplicationCriteriaBuilder criteriaBuilder = ApplicationCriteria.builder().restrictedToIds(
                new HashSet<>(applicationIds)
            );

            if (applicationStatus != null) {
                criteriaBuilder.status(applicationStatus);
            }

            if (executionContext.hasEnvironmentId()) {
                criteriaBuilder.environmentIds(Set.of(executionContext.getEnvironmentId()));
            }

            Page<Application> applications = applicationRepository.search(criteriaBuilder.build(), null);

            if (applications.getContent().isEmpty()) {
                return emptySet();
            }
            return this.convertToList(executionContext, applications.getContent());
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find applications by ids {}", applicationIds, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications by ids {}" + applicationIds, ex);
        }
    }

    @Override
    public Set<String> findIdsByUser(ExecutionContext executionContext, String username, Sortable sortable) {
        log.debug("Find applications for user {}", username);

        ApplicationQuery applicationQuery = new ApplicationQuery();
        applicationQuery.setUser(username);
        applicationQuery.setStatus(ApplicationStatus.ACTIVE.name());
        applicationQuery.setExcludeFilters(Arrays.asList(ApplicationExcludeFilter.OWNER));

        return searchIds(executionContext, applicationQuery, sortable);
    }

    @Override
    public Set<String> findIdsByUserAndPermission(
        ExecutionContext executionContext,
        String username,
        Sortable sortable,
        RolePermission rolePermission,
        RolePermissionAction... acl
    ) {
        log.debug("Find applicationIds for user and permission {}, {}, {}", username, rolePermission, acl);
        ApplicationQuery applicationQuery = buildApplicationQueryForUserAndPermission(executionContext, rolePermission, acl, username);
        applicationQuery.setExcludeFilters(Arrays.asList(ApplicationExcludeFilter.OWNER));
        return searchIds(executionContext, applicationQuery, sortable);
    }

    @NotNull
    private ApplicationQuery buildApplicationQueryForUserAndPermission(
        ExecutionContext executionContext,
        RolePermission rolePermission,
        RolePermissionAction[] acl,
        String username
    ) {
        List<String> roleIdsWithPermission = roleService
            .findAllByOrganization(executionContext.getOrganizationId())
            .stream()
            .filter(roleEntity -> roleService.hasPermission(roleEntity.getPermissions(), rolePermission.getPermission(), acl))
            .map(RoleEntity::getId)
            .toList();

        Set<String> appIds = membershipService.getReferenceIdsByMemberAndReferenceAndRoleIn(
            MembershipMemberType.USER,
            username,
            MembershipReferenceType.APPLICATION,
            roleIdsWithPermission
        );

        ApplicationQuery applicationQuery = new ApplicationQuery();
        applicationQuery.setIds(appIds);
        applicationQuery.setUser(username);
        applicationQuery.setStatus(ApplicationStatus.ACTIVE.name());
        return applicationQuery;
    }

    @Override
    public Set<ApplicationListItem> findByUser(final ExecutionContext executionContext, String username, Sortable sortable) {
        log.debug("Find applications for user {}", username);
        ApplicationQuery applicationQuery = ApplicationQuery.builder().user(username).status(ApplicationStatus.ACTIVE.name()).build();
        Page<ApplicationListItem> applications = search(executionContext, applicationQuery, sortable, null);
        return new LinkedHashSet<>(applications.getContent());
    }

    @Override
    public Set<String> findIdsByOrganization(String organizationId) {
        log.debug("Find applications by organization {} ", organizationId);
        if (organizationId == null || organizationId.trim().isEmpty()) {
            return emptySet();
        }

        return this.searchIds(new ExecutionContext(organizationId, null), new ApplicationQuery(), null);
    }

    @Override
    public Set<String> findIdsByEnvironment(final ExecutionContext executionContext) {
        log.debug("Find applications by environment {} ", executionContext.getEnvironmentId());
        if (isBlank(executionContext.getEnvironmentId())) {
            return Set.of();
        }

        return this.searchIds(executionContext, null, null);
    }

    @Override
    public Set<ApplicationListItem> findByGroups(final ExecutionContext executionContext, List<String> groupIds) {
        return this.findByGroupsAndStatus(executionContext, groupIds, ApplicationStatus.ACTIVE.name());
    }

    @Override
    public Set<ApplicationListItem> findByGroupsAndStatus(final ExecutionContext executionContext, List<String> groupIds, String status) {
        log.debug("Find applications by groups {}", groupIds);
        try {
            ApplicationStatus requestedStatus = ApplicationStatus.valueOf(status.toUpperCase());
            Set<Application> applications = applicationRepository.findByGroups(groupIds, ApplicationStatus.valueOf(status.toUpperCase()));

            return ApplicationStatus.ACTIVE.equals(requestedStatus)
                ? convertToList(executionContext, applications)
                : convertToSimpleList(applications);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find applications for groups {}", groupIds, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for groups " + groupIds, ex);
        }
    }

    @Override
    public ApplicationEntity create(final ExecutionContext executionContext, NewApplicationEntity newApplicationEntity, String userId) {
        log.debug("Create {} for user {}", newApplicationEntity, userId);

        creationPreFlightChecks(executionContext, newApplicationEntity);

        // Create application metadata
        Map<String, String> metadata = new HashMap<>();

        // Create a simple "internal" application
        if (newApplicationEntity.getSettings().getApp() != null) {
            // If client registration is enabled, check that the simple type is allowed
            if (
                isClientRegistrationEnabled(executionContext, executionContext.getEnvironmentId()) &&
                !isApplicationTypeAllowed(executionContext, "simple", executionContext.getEnvironmentId())
            ) {
                throw new IllegalStateException("Application type 'simple' is not allowed");
            }

            // If clientId is set, check for uniqueness
            String clientId = newApplicationEntity.getSettings().getApp().getClientId();
            if (StringUtils.isNotBlank(clientId)) {
                checkClientIdIsUniqueForEnv(clientId, executionContext.getEnvironmentId());
            }
        } else {
            // Check that client registration is enabled
            checkClientRegistrationEnabled(executionContext, executionContext.getEnvironmentId());

            String appType = newApplicationEntity.getSettings().getOauth().getApplicationType();
            // Check that the application_type is allowed
            if (!isApplicationTypeAllowed(executionContext, appType, executionContext.getEnvironmentId())) {
                throw new IllegalStateException("Application type '" + appType + "' is not allowed");
            }
            checkAndSanitizeOAuthClientSettings(newApplicationEntity.getSettings().getOauth());

            // Create an OAuth client
            ClientRegistrationResponse registrationResponse = clientRegistrationService.register(executionContext, newApplicationEntity);
            try {
                metadata.put(METADATA_CLIENT_ID, registrationResponse.getClientId());
                metadata.put(METADATA_REGISTRATION_PAYLOAD, mapper.writeValueAsString(registrationResponse));
                metadata.put(
                    METADATA_ADDITIONAL_CLIENT_METADATA,
                    mapper.writeValueAsString(newApplicationEntity.getSettings().getOauth().getAdditionalClientMetadata())
                );
            } catch (JsonProcessingException e) {
                log.error("An error has occurred while serializing registration response", e);
            }
        }

        String clientCertificate = getClientCertificate(newApplicationEntity.getSettings());

        if (newApplicationEntity.getGroups() != null && !newApplicationEntity.getGroups().isEmpty()) {
            //throw a NotFoundException if the group doesn't exist
            groupService.findByIds(newApplicationEntity.getGroups());
        }

        Application application = applicationConverter.toApplication(newApplicationEntity);
        if (newApplicationEntity.getId() != null && !newApplicationEntity.getId().isEmpty()) {
            application.setId(newApplicationEntity.getId());
        } else {
            application.setId(UuidString.generateRandom());
        }
        application.setStatus(ApplicationStatus.ACTIVE);
        metadata.forEach((key, value) -> application.getMetadata().put(key, value));

        // Add Default groups
        Set<String> defaultGroups = groupService
            .findByEvent(executionContext.getEnvironmentId(), GroupEvent.APPLICATION_CREATE)
            .stream()
            .map(GroupEntity::getId)
            .collect(toSet());
        if (!defaultGroups.isEmpty()) {
            application.addAllGroups(defaultGroups);
        }

        // Set date fields
        application.setCreatedAt(new Date());
        application.setUpdatedAt(application.getCreatedAt());

        return createApplicationForEnvironment(executionContext, userId, application, clientCertificate);
    }

    private static String getClientCertificate(ApplicationSettings settings) {
        if (settings.getTls() != null && StringUtils.isNotBlank(settings.getTls().getClientCertificate())) {
            return settings.getTls().getClientCertificate();
        }
        return null;
    }

    private void creationPreFlightChecks(ExecutionContext executionContext, NewApplicationEntity newApplicationEntity) {
        // Check that only one settings is defined
        if (newApplicationEntity.getSettings().getApp() != null && newApplicationEntity.getSettings().getOauth() != null) {
            throw new InvalidApplicationTypeException();
        }

        // Check that a type is defined
        if (newApplicationEntity.getSettings().getApp() == null && newApplicationEntity.getSettings().getOauth() == null) {
            throw new InvalidApplicationTypeException();
        }

        // Check that shared API Key mode is enabled
        if (
            newApplicationEntity.getApiKeyMode() == ApiKeyMode.SHARED &&
            !parameterService.findAsBoolean(executionContext, Key.PLAN_SECURITY_APIKEY_SHARED_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            throw new InvalidApplicationApiKeyModeException(
                "Can't create application with SHARED API Key mode cause environment setting is disabled"
            );
        }
    }

    @NotNull
    private ApplicationEntity createApplicationForEnvironment(
        final ExecutionContext executionContext,
        String userId,
        Application application,
        String clientCertificate
    ) {
        try {
            application.setEnvironmentId(executionContext.getEnvironmentId());

            // Create client certificate if provided
            if (clientCertificate != null) {
                clientCertificateCrudService.create(
                    application.getId(),
                    new CreateClientCertificate(application.getName(), null, null, clientCertificate)
                );
            }

            Application createdApplication = applicationRepository.create(application);

            // Audit
            createAuditLog(executionContext, APPLICATION_CREATED, createdApplication.getCreatedAt(), null, createdApplication);

            // Add the primary owner of the newly created Application
            membershipService.addRoleToMemberOnReference(
                executionContext,
                new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, createdApplication.getId()),
                new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, SystemRole.PRIMARY_OWNER.name())
            );

            // create the default mail notification
            UserEntity userEntity = userService.findById(executionContext, userId);
            if (userEntity.getEmail() != null && !userEntity.getEmail().isEmpty()) {
                GenericNotificationConfigEntity notificationConfigEntity = new GenericNotificationConfigEntity();
                notificationConfigEntity.setName("Default Mail Notifications");
                notificationConfigEntity.setReferenceType(HookScope.APPLICATION.name());
                notificationConfigEntity.setReferenceId(createdApplication.getId());
                notificationConfigEntity.setHooks(Arrays.stream(ApplicationHook.values()).map(Enum::name).toList());
                notificationConfigEntity.setNotifier(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID);
                notificationConfigEntity.setConfig(userEntity.getEmail());
                genericNotificationConfigService.create(notificationConfigEntity);
            }
            return convert(executionContext, createdApplication, userEntity);
        } catch (TechnicalException ex) {
            log.error(
                "An error occurs while trying to create {} for user {} in environment {}",
                application,
                userId,
                executionContext.getEnvironmentId(),
                ex
            );
            throw new TechnicalManagementException(
                "An error occurs while trying create " +
                    application +
                    " for user " +
                    userId +
                    " in environment " +
                    executionContext.getEnvironmentId(),
                ex
            );
        }
    }

    private void checkAndSanitizeOAuthClientSettings(OAuthClientSettings oAuthClientSettings) {
        if (oAuthClientSettings.getGrantTypes() == null || oAuthClientSettings.getGrantTypes().isEmpty()) {
            throw new ApplicationGrantTypesNotFoundException();
        }

        if (oAuthClientSettings.getApplicationType() == null || oAuthClientSettings.getApplicationType().isEmpty()) {
            throw new ApplicationTypeNotFoundException(null);
        }

        ApplicationTypeEntity applicationTypeEntity = applicationTypeService.getApplicationType(oAuthClientSettings.getApplicationType());

        List<String> targetGrantTypes = oAuthClientSettings.getGrantTypes();
        List<String> allowedGrantTypes = applicationTypeEntity
            .getAllowed_grant_types()
            .stream()
            .map(ApplicationGrantTypeEntity::getType)
            .toList();
        if (!allowedGrantTypes.containsAll(targetGrantTypes)) {
            throw new ApplicationGrantTypesNotAllowedException(oAuthClientSettings.getApplicationType(), targetGrantTypes);
        }

        List<String> redirectUris = oAuthClientSettings.getRedirectUris();
        if (Boolean.TRUE.equals(applicationTypeEntity.getRequires_redirect_uris()) && (redirectUris == null || redirectUris.isEmpty())) {
            throw new ApplicationRedirectUrisNotFound();
        }

        List<String> responseTypes = applicationTypeEntity
            .getAllowed_grant_types()
            .stream()
            .filter(applicationGrantTypeEntity -> targetGrantTypes.contains(applicationGrantTypeEntity.getType()))
            .map(ApplicationGrantTypeEntity::getResponse_types)
            .flatMap(Collection::stream)
            .distinct()
            .toList();

        oAuthClientSettings.setResponseTypes(responseTypes);
    }

    @Override
    public ApplicationEntity update(
        final ExecutionContext executionContext,
        String applicationId,
        UpdateApplicationEntity updateApplicationEntity
    ) {
        try {
            log.debug("Update application {}", applicationId);

            validateApplicationClientId(executionContext, applicationId, updateApplicationEntity);
            Set<String> groups = updateApplicationEntity.getGroups();
            validateUserGroups(executionContext, groups);

            Application applicationToUpdate = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

            updatePreFlightChecks(updateApplicationEntity, applicationToUpdate, groups);

            // Retro-compatibility : If input API Key mode is not specified, get it from existing application
            if (updateApplicationEntity.getApiKeyMode() == null && applicationToUpdate.getApiKeyMode() != null) {
                updateApplicationEntity.setApiKeyMode(ApiKeyMode.valueOf(applicationToUpdate.getApiKeyMode().name()));
            } else {
                // Check that application API Key mode is valid
                checkApiKeyModeUpdate(executionContext, updateApplicationEntity.getApiKeyMode(), applicationToUpdate);
            }

            // Update application metadata
            Map<String, String> metadata = new HashMap<>();

            // Handle TLS certificate changes via ClientCertificateCrudService
            String newCertificateToCreate = null;
            if (
                updateApplicationEntity.getSettings().getTls() != null &&
                !StringUtils.isBlank(updateApplicationEntity.getSettings().getTls().getClientCertificate())
            ) {
                String newCertificate = updateApplicationEntity.getSettings().getTls().getClientCertificate().trim();

                // Check if certificate has changed by comparing with the most recent active certificate
                Optional<ClientCertificate> existingCertOpt = clientCertificateCrudService.findMostRecentActiveByApplicationId(
                    applicationId
                );

                boolean certificateChanged = existingCertOpt
                    .map(existingCert -> !newCertificate.equals(existingCert.certificate()))
                    .orElse(true);

                if (certificateChanged) {
                    // Expire the current active certificate if exists
                    existingCertOpt.ifPresent(existingCert ->
                        clientCertificateCrudService.update(
                            existingCert.id(),
                            new UpdateClientCertificate(existingCert.name(), existingCert.startsAt(), new Date())
                        )
                    );
                    // Mark new certificate for creation after application update
                    newCertificateToCreate = newCertificate;
                }
            }

            // Update a simple application
            if (applicationToUpdate.getType() == ApplicationType.SIMPLE && updateApplicationEntity.getSettings().getApp() != null) {
                // If clientId is set, check for uniqueness
                checkClientIdUniqueness(executionContext, updateApplicationEntity, applicationToUpdate);
            } else {
                // Check that client registration is enabled
                checkClientRegistrationEnabled(executionContext, executionContext.getEnvironmentId());
                // Some IdPs do not support the optional application_type parameter. However, the DCR implementation expects this parameter to never be empty
                if (updateApplicationEntity.getSettings().getOauth().getApplicationType() == null) {
                    updateApplicationEntity.getSettings().getOauth().setApplicationType(applicationToUpdate.getType().name());
                }
                checkAndSanitizeOAuthClientSettings(updateApplicationEntity.getSettings().getOauth());

                // Update an OAuth client
                final String registrationPayload = applicationToUpdate.getMetadata().get(METADATA_REGISTRATION_PAYLOAD);
                if (registrationPayload != null) {
                    updateClientRegistration(executionContext, updateApplicationEntity, registrationPayload, metadata, applicationToUpdate);
                }
            }

            // Create new certificate if certificate was changed
            final Optional<String> clientCertificate = createNewCertificateIfNecessary(
                applicationId,
                updateApplicationEntity,
                newCertificateToCreate
            );

            Application application = applicationConverter.toApplication(updateApplicationEntity);
            application.setId(applicationId);
            application.setHrid(applicationToUpdate.getHrid());
            application.setEnvironmentId(applicationToUpdate.getEnvironmentId());
            application.setStatus(ApplicationStatus.ACTIVE);
            application.setType(applicationToUpdate.getType());
            application.setCreatedAt(applicationToUpdate.getCreatedAt());
            application.setUpdatedAt(new Date());
            application.setOrigin(applicationToUpdate.getOrigin() != null ? applicationToUpdate.getOrigin() : Origin.MANAGEMENT);
            metadata.forEach((key, value) -> application.getMetadata().put(key, value));

            Application updatedApplication = applicationRepository.update(application);

            createAuditLog(
                executionContext,
                APPLICATION_UPDATED,
                updatedApplication.getUpdatedAt(),
                applicationToUpdate,
                updatedApplication
            );

            updateActiveSubscriptions(executionContext, applicationId, application, clientCertificate);
            return convertApplication(executionContext, Collections.singleton(updatedApplication)).iterator().next();
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to update application %s", applicationId),
                ex
            );
        }
    }

    private void createAuditLog(
        ExecutionContext executionContext,
        Application.AuditEvent applicationUpdated,
        Date updatedApplication,
        Application applicationToUpdate,
        Application updatedApplication1
    ) {
        // Audit
        auditService.createApplicationAuditLog(
            executionContext,
            AuditService.AuditLogData.builder()
                .properties(Collections.emptyMap())
                .event(applicationUpdated)
                .createdAt(updatedApplication)
                .oldValue(applicationToUpdate)
                .newValue(updatedApplication1)
                .build(),
            updatedApplication1.getId()
        );
    }

    private void updateActiveSubscriptions(
        ExecutionContext executionContext,
        String applicationId,
        Application application,
        Optional<String> clientCertificate
    ) {
        // Set correct client_id for all active subscriptions
        SubscriptionQuery subQuery = new SubscriptionQuery();
        subQuery.setApplication(applicationId);
        subQuery.setStatuses(Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED, SubscriptionStatus.PENDING));

        String clientId = application.getMetadata().get(METADATA_CLIENT_ID);

        final String subscriptionClientCertificate = clientCertificate
            .map(cert -> Base64.getEncoder().encodeToString(cert.getBytes()))
            .orElse(null);

        Consumer<Subscription> clientIdSubscriptionModifier = s -> s.setClientId(clientId);
        Consumer<Subscription> clientCertificateSubscriptionModifier = s -> s.setClientCertificate(subscriptionClientCertificate);
        Consumer<Subscription> applicationNameSubscriptionModifier = s -> s.setApplicationName(application.getName());

        subscriptionService
            .search(executionContext, subQuery)
            .forEach(subscriptionEntity -> {
                Consumer<Subscription> subscriptionModifier = null;
                if (areNotEmptyAndDifferent(clientId, subscriptionEntity.getClientId())) {
                    subscriptionModifier = clientIdSubscriptionModifier;
                }
                if (areNotEmptyAndDifferent(subscriptionClientCertificate, subscriptionEntity.getClientCertificate())) {
                    subscriptionModifier = subscriptionModifier == null
                        ? clientCertificateSubscriptionModifier
                        : subscriptionModifier.andThen(clientCertificateSubscriptionModifier);
                }
                if (areNotEmptyAndDifferent(application.getName(), subscriptionEntity.getApplicationName())) {
                    subscriptionModifier = subscriptionModifier == null
                        ? applicationNameSubscriptionModifier
                        : subscriptionModifier.andThen(applicationNameSubscriptionModifier);
                }
                if (subscriptionModifier != null) {
                    UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
                    updateSubscriptionEntity.setId(subscriptionEntity.getId());
                    updateSubscriptionEntity.setStartingAt(subscriptionEntity.getStartingAt());
                    updateSubscriptionEntity.setEndingAt(subscriptionEntity.getEndingAt());
                    updateSubscriptionEntity.setConfiguration(subscriptionEntity.getConfiguration());
                    subscriptionService.update(executionContext, updateSubscriptionEntity, subscriptionModifier);
                }
            });
    }

    private Optional<String> createNewCertificateIfNecessary(
        String applicationId,
        UpdateApplicationEntity updateApplicationEntity,
        String newCertificateToCreate
    ) {
        if (newCertificateToCreate != null) {
            clientCertificateCrudService.create(
                applicationId,
                new CreateClientCertificate(updateApplicationEntity.getName(), null, null, newCertificateToCreate)
            );
            return Optional.of(newCertificateToCreate);
        }
        return Optional.empty();
    }

    private void checkClientIdUniqueness(
        ExecutionContext executionContext,
        UpdateApplicationEntity updateApplicationEntity,
        Application applicationToUpdate
    ) throws TechnicalException {
        String clientId = updateApplicationEntity.getSettings().getApp().getClientId();

        if (!StringUtils.isBlank(clientId)) {
            log.debug("Check that client_id is unique among all applications");
            final Set<Application> applications = applicationRepository.findAllByEnvironment(
                executionContext.getEnvironmentId(),
                ApplicationStatus.ACTIVE
            );
            final Optional<Application> byClientId = applications
                .stream()
                .filter(app -> app.getMetadata() != null)
                .filter(app -> clientId.equals(app.getMetadata().get(METADATA_CLIENT_ID)))
                .findAny();
            if (byClientId.map(app -> !app.getId().equals(applicationToUpdate.getId())).orElse(false)) {
                log.error("An application already exists with the same client_id");
                throw new ClientIdAlreadyExistsException(clientId);
            }
        }
    }

    private void updatePreFlightChecks(
        UpdateApplicationEntity updateApplicationEntity,
        Application applicationToUpdate,
        Set<String> groups
    ) {
        if (groups != null && !groups.isEmpty()) {
            //throw a NotFoundException if the group doesn't exist
            groupService.findByIds(groups);
        }

        if (ApplicationStatus.ARCHIVED.equals(applicationToUpdate.getStatus())) {
            throw new ApplicationArchivedException(applicationToUpdate.getName());
        }

        // Check that only one settings is defined
        if (updateApplicationEntity.getSettings().getApp() != null && updateApplicationEntity.getSettings().getOauth() != null) {
            throw new InvalidApplicationTypeException();
        }

        // Check that a type is defined
        if (updateApplicationEntity.getSettings().getApp() == null && updateApplicationEntity.getSettings().getOauth() == null) {
            throw new InvalidApplicationTypeException();
        }
    }

    private void updateClientRegistration(
        ExecutionContext executionContext,
        UpdateApplicationEntity updateApplicationEntity,
        String registrationPayload,
        Map<String, String> metadata,
        Application applicationToUpdate
    ) {
        try {
            ClientRegistrationResponse registrationResponse = clientRegistrationService.update(
                executionContext,
                registrationPayload,
                updateApplicationEntity
            );
            metadata.put(METADATA_CLIENT_ID, registrationResponse.getClientId());
            metadata.put(METADATA_REGISTRATION_PAYLOAD, mapper.writeValueAsString(registrationResponse));
            metadata.put(
                METADATA_ADDITIONAL_CLIENT_METADATA,
                mapper.writeValueAsString(updateApplicationEntity.getSettings().getOauth().getAdditionalClientMetadata())
            );
        } catch (Exception e) {
            log.error("Failed to update OAuth client data from client registration. Keeping old OAuth client data.", e);
            metadata.put(METADATA_CLIENT_ID, applicationToUpdate.getMetadata().get(METADATA_CLIENT_ID));
            metadata.put(METADATA_REGISTRATION_PAYLOAD, applicationToUpdate.getMetadata().get(METADATA_REGISTRATION_PAYLOAD));
        }
    }

    private void validateUserGroups(ExecutionContext executionContext, Set<String> groups) {
        ConsoleConfigEntity consoleConfig = configService.getConsoleConfig(executionContext);

        if (consoleConfig.getUserGroup().getRequired().isEnabled() && CollectionUtils.isEmpty(groups)) {
            throw new BadRequestException("Updating an application is not allowed as at least one group is required on the application.");
        }
    }

    private static boolean areNotEmptyAndDifferent(String applicationField, String subscriptionField) {
        return (
            StringUtils.isNotEmpty(subscriptionField) &&
            StringUtils.isNotEmpty(applicationField) &&
            !subscriptionField.equals(applicationField)
        );
    }

    @Override
    public ApplicationEntity updateApiKeyMode(final ExecutionContext executionContext, String applicationId, ApiKeyMode apiKeyMode) {
        try {
            log.debug("Update application {} with apiKeyMode {}", applicationId, apiKeyMode);

            Application applicationToUpdate = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

            if (ApplicationStatus.ARCHIVED.equals(applicationToUpdate.getStatus())) {
                throw new ApplicationArchivedException(applicationToUpdate.getName());
            }

            // Check that application Api Key mode is valid
            checkApiKeyModeUpdate(executionContext, apiKeyMode, applicationToUpdate);

            applicationToUpdate.setApiKeyMode(io.gravitee.repository.management.model.ApiKeyMode.valueOf(apiKeyMode.name()));
            Application updatedApplication = applicationRepository.update(applicationToUpdate);

            // Audit
            createAuditLog(
                executionContext,
                APPLICATION_UPDATED,
                updatedApplication.getUpdatedAt(),
                applicationToUpdate,
                updatedApplication
            );

            return convertApplication(executionContext, Collections.singleton(updatedApplication)).iterator().next();
        } catch (TechnicalException ex) {
            String error = String.format(
                "An error occurs while trying to update application %s with apiKeyMode %s",
                applicationId,
                apiKeyMode
            );
            log.error(error, ex);
            throw new TechnicalManagementException(error, ex);
        }
    }

    private void validateApplicationClientId(
        ExecutionContext executionContext,
        String applicationId,
        UpdateApplicationEntity updateApplicationEntity
    ) {
        if (
            null != updateApplicationEntity.getSettings() &&
            null != updateApplicationEntity.getSettings().getApp() &&
            StringUtils.isEmpty(updateApplicationEntity.getSettings().getApp().getClientId())
        ) {
            Set<String> planIds = subscriptionService
                .findByApplicationAndPlan(executionContext, applicationId, null)
                .stream()
                .filter(subscriptionEntity ->
                    Set.of(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED, SubscriptionStatus.PENDING).contains(
                        subscriptionEntity.getStatus()
                    )
                )
                .map(SubscriptionEntity::getPlan)
                .collect(Collectors.toSet());

            if (!planIds.isEmpty()) {
                Set<GenericPlanEntity> plans = this.planSearchService.findByIdIn(executionContext, planIds)
                    .stream()
                    .filter(planEntity -> PlanMode.STANDARD.equals(planEntity.getPlanMode()))
                    .filter(planEntity -> {
                        PlanSecurityType security = PlanSecurityType.valueOfLabel(planEntity.getPlanSecurity().getType());
                        return security == PlanSecurityType.JWT || security == PlanSecurityType.OAUTH2;
                    })
                    .collect(toSet());

                if (!plans.isEmpty()) {
                    throw new ApplicationClientIdException(
                        "Can't update application because client_id is missing and it has subscriptions"
                    );
                }
            }
        }
    }

    @Override
    public ApplicationEntity renewClientSecret(final ExecutionContext executionContext, String applicationId) {
        try {
            log.debug("Renew client secret for application {}", applicationId);

            Application applicationToUpdate = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

            if (ApplicationStatus.ARCHIVED.equals(applicationToUpdate.getStatus())) {
                throw new ApplicationArchivedException(applicationToUpdate.getName());
            }

            // Check that client registration is enabled
            checkClientRegistrationEnabled(executionContext, executionContext.getEnvironmentId());

            ApplicationEntity applicationEntity = findById(executionContext, applicationId);

            // Check that the application can be updated with a new client secret
            if (
                applicationEntity.getSettings().getOauth() != null &&
                applicationEntity.getSettings().getOauth().isRenewClientSecretSupported()
            ) {
                ClientRegistrationResponse registrationResponse = clientRegistrationService.renewClientSecret(
                    executionContext,
                    applicationToUpdate.getMetadata().get(METADATA_REGISTRATION_PAYLOAD)
                );

                // Update application metadata
                Map<String, String> metadata = new HashMap<>();

                try {
                    metadata.put(METADATA_CLIENT_ID, registrationResponse.getClientId());
                    metadata.put(METADATA_REGISTRATION_PAYLOAD, mapper.writeValueAsString(registrationResponse));
                } catch (JsonProcessingException e) {
                    log.error("An error has occurred while writing registration response", e);
                }

                applicationToUpdate.setUpdatedAt(new Date());

                metadata.forEach((key, value) -> applicationToUpdate.getMetadata().put(key, value));

                Application updatedApplication = applicationRepository.update(applicationToUpdate);

                // Audit
                createAuditLog(
                    executionContext,
                    APPLICATION_UPDATED,
                    updatedApplication.getUpdatedAt(),
                    applicationToUpdate,
                    updatedApplication
                );

                return convertApplication(executionContext, Collections.singleton(updatedApplication)).iterator().next();
            }

            throw new ApplicationRenewClientSecretException(applicationToUpdate.getName());
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to renew client secret {}", applicationId, ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to renew client secret %s", applicationId),
                ex
            );
        }
    }

    @Override
    public ApplicationEntity restore(final ExecutionContext executionContext, String applicationId) {
        try {
            log.debug("Restore application {}", applicationId);

            Application application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

            if (!ApplicationStatus.ARCHIVED.equals(application.getStatus())) {
                throw new ApplicationActiveException(application.getName());
            }

            if (application.getMetadata() != null && application.getMetadata().containsKey(METADATA_CLIENT_ID)) {
                checkClientIdIsUniqueForEnv(application.getMetadata().get(METADATA_CLIENT_ID), application.getEnvironmentId());
            }

            application.setStatus(ApplicationStatus.ACTIVE);
            application.setUpdatedAt(new Date());

            String userId = getAuthenticatedUsername();
            membershipService.deleteReference(executionContext, MembershipReferenceType.APPLICATION, applicationId);
            // Add the primary owner of the newly created Application
            membershipService.addRoleToMemberOnReference(
                executionContext,
                new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, applicationId),
                new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, SystemRole.PRIMARY_OWNER.name())
            );

            // delete notifications
            genericNotificationConfigService.deleteReference(NotificationReferenceType.APPLICATION, applicationId);
            portalNotificationConfigService.deleteReference(NotificationReferenceType.APPLICATION, applicationId);

            Application restoredApplication = applicationRepository.update(application);

            Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApplicationAndPlan(
                executionContext,
                applicationId,
                null
            );

            subscriptions.forEach(subscription -> {
                try {
                    subscriptionService.restore(executionContext, subscription.getId());
                } catch (SubscriptionNotPausedException snce) {
                    // Subscription cannot be closed because it is already closed or not yet accepted
                    log.debug("The subscription cannot be closed: {}", snce.getMessage());
                }
            });

            UserEntity userEntity = userService.findById(executionContext, userId);

            // Audit
            createAuditLog(executionContext, APPLICATION_RESTORED, restoredApplication.getUpdatedAt(), application, restoredApplication);

            return convert(executionContext, restoredApplication, userEntity);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to restore {}", applicationId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to restore %s", applicationId), ex);
        }
    }

    private void checkClientRegistrationEnabled(ExecutionContext executionContext, String environmentId) {
        if (!isClientRegistrationEnabled(executionContext, environmentId)) {
            throw new IllegalStateException("The client registration is disabled");
        }
    }

    private boolean isClientRegistrationEnabled(ExecutionContext executionContext, String environmentId) {
        return parameterService.findAsBoolean(
            executionContext,
            Key.APPLICATION_REGISTRATION_ENABLED,
            environmentId,
            ParameterReferenceType.ENVIRONMENT
        );
    }

    private boolean isApplicationTypeAllowed(ExecutionContext executionContext, String applicationType, String environmentId) {
        Key key = Key.valueOf("APPLICATION_TYPE_" + applicationType.toUpperCase() + "_ENABLED");
        return parameterService.findAsBoolean(executionContext, key, environmentId, ParameterReferenceType.ENVIRONMENT);
    }

    @Override
    public void archive(final ExecutionContext executionContext, String applicationId) {
        try {
            log.debug("Delete application {}", applicationId);

            Application application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
            Application previousApplication = new Application(application);
            Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApplicationAndPlan(
                executionContext,
                applicationId,
                null
            );

            subscriptions.forEach(subscription -> {
                List<ApiKeyEntity> apiKeys = apiKeyService.findBySubscription(executionContext, subscription.getId());
                apiKeys.forEach(apiKey -> {
                    try {
                        apiKeyService.delete(apiKey.getKey());
                    } catch (TechnicalManagementException tme) {
                        log.error("An error occurs while deleting API Key with id {}", apiKey.getId(), tme);
                    }
                });

                try {
                    closeSubscriptionDomainService.closeSubscription(
                        subscription.getId(),
                        AuditInfo.builder()
                            .organizationId(executionContext.getOrganizationId())
                            .environmentId(executionContext.getEnvironmentId())
                            .actor(getAuthenticatedUserAsAuditActor())
                            .build()
                    );
                } catch (SubscriptionNotClosableException snce) {
                    // Subscription cannot be closed because it is already closed or not yet accepted
                    log.debug("The subscription cannot be closed: {}", snce.getMessage());
                }
            });

            // Archive the application
            application.setUpdatedAt(new Date());
            application.setStatus(ApplicationStatus.ARCHIVED);
            applicationRepository.update(application);
            // remove notifications
            genericNotificationConfigService.deleteReference(NotificationReferenceType.APPLICATION, applicationId);
            // delete memberships
            membershipService.deleteReference(executionContext, MembershipReferenceType.APPLICATION, applicationId);
            // delete alerts
            applicationAlertService.deleteAll(applicationId);
            // delete client certificates
            clientCertificateCrudService.deleteByApplicationId(applicationId);
            // Audit
            createAuditLog(executionContext, APPLICATION_ARCHIVED, application.getUpdatedAt(), previousApplication, application);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to delete application {}", applicationId, ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to delete application %s", applicationId),
                ex
            );
        }
    }

    private Set<ApplicationEntity> convertApplication(ExecutionContext executionContext, Collection<Application> applications) {
        if (applications == null || applications.isEmpty()) {
            return Collections.emptySet();
        }
        RoleEntity primaryOwnerRole = roleService.findPrimaryOwnerRoleByOrganization(
            executionContext.getOrganizationId(),
            RoleScope.APPLICATION
        );
        if (primaryOwnerRole == null) {
            throw new RoleNotFoundException("APPLICATION_PRIMARY_OWNER");
        }

        //find primary owners usernames of each application
        final List<String> activeApplicationIds = applications
            .stream()
            .filter(app -> ApplicationStatus.ACTIVE.equals(app.getStatus()))
            .map(Application::getId)
            .collect(Collectors.toList());

        Set<MembershipEntity> memberships = membershipService.getMembershipsByReferencesAndRole(
            io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION,
            activeApplicationIds,
            primaryOwnerRole.getId()
        );
        int poMissing = activeApplicationIds.size() - memberships.size();
        if (poMissing > 0) {
            Set<String> appMembershipsIds = memberships.stream().map(MembershipEntity::getReferenceId).collect(toSet());

            activeApplicationIds.removeAll(appMembershipsIds);
            Optional<String> optionalApplicationsAsString = activeApplicationIds.stream().reduce((a, b) -> a + " / " + b);

            String applicationsAsString = "?";
            if (optionalApplicationsAsString.isPresent()) applicationsAsString = optionalApplicationsAsString.get();
            throw new TechnicalManagementException(
                poMissing + " applications has no identified primary owners in this list " + applicationsAsString + "."
            );
        }

        Map<String, String> applicationToUser = HashMap.newHashMap(memberships.size());
        Map<String, UserEntity> userIdToUserEntity = HashMap.newHashMap(memberships.size());

        if (!memberships.isEmpty()) {
            memberships.forEach(membership -> applicationToUser.put(membership.getReferenceId(), membership.getMemberId()));

            // We don't need user metadata, only global information
            userService
                .findByIds(executionContext, memberships.stream().map(MembershipEntity::getMemberId).toList(), false)
                .forEach(userEntity -> userIdToUserEntity.put(userEntity.getId(), userEntity));
        }

        return applications
            .stream()
            .map(publicApplication ->
                convert(executionContext, publicApplication, userIdToUserEntity.get(applicationToUser.get(publicApplication.getId())))
            )
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<ApplicationListItem> convertToSimpleList(Collection<Application> applications) {
        return applications
            .stream()
            .map(application -> {
                ApplicationListItem item = new ApplicationListItem();
                item.setId(application.getId());
                item.setName(application.getName());
                item.setDescription(application.getDescription());
                item.setDomain(application.getDomain());
                item.setCreatedAt(application.getCreatedAt());
                item.setUpdatedAt(application.getUpdatedAt());
                item.setType(application.getType().name());
                item.setStatus(application.getStatus().name());
                item.setPicture(application.getPicture());
                item.setBackground(application.getBackground());
                item.setDisableMembershipNotifications(application.isDisableMembershipNotifications());
                item.setOrigin(application.getOrigin());
                return item;
            })
            .collect(toSet());
    }

    private Page<ApplicationListItem> convertToSimpleList(Page<Application> applications) {
        Set<ApplicationListItem> applicationListItems = convertToSimpleList(applications.getContent());
        return new Page<>(
            List.copyOf(applicationListItems),
            applications.getPageNumber(),
            applicationListItems.size(),
            applications.getTotalElements()
        );
    }

    private Page<ApplicationListItem> convertToList(final ExecutionContext executionContext, Page<Application> applications)
        throws TechnicalException {
        Set<ApplicationListItem> applicationListItems = convertToList(executionContext, applications.getContent());
        return new Page<>(
            List.copyOf(applicationListItems),
            applications.getPageNumber(),
            applicationListItems.size(),
            applications.getTotalElements()
        );
    }

    private Set<ApplicationListItem> convertToList(final ExecutionContext executionContext, Collection<Application> applications) {
        Set<ApplicationEntity> entities = convertApplication(executionContext, applications);

        Set<ApplicationListItem> apps = entities
            .stream()
            .map(applicationEntity -> {
                ApplicationListItem item = new ApplicationListItem();
                item.setId(applicationEntity.getId());
                item.setName(applicationEntity.getName());
                item.setDescription(applicationEntity.getDescription());
                item.setDomain(applicationEntity.getDomain());
                item.setCreatedAt(applicationEntity.getCreatedAt());
                item.setUpdatedAt(applicationEntity.getUpdatedAt());
                item.setGroups(applicationEntity.getGroups());
                item.setPrimaryOwner(applicationEntity.getPrimaryOwner());
                item.setType(applicationEntity.getType());
                item.setStatus(applicationEntity.getStatus());
                item.setPicture(applicationEntity.getPicture());
                item.setBackground(applicationEntity.getBackground());
                item.setApiKeyMode(applicationEntity.getApiKeyMode());
                item.setDisableMembershipNotifications(applicationEntity.isDisableMembershipNotifications());
                item.setOrigin(applicationEntity.getOrigin());

                final Application app = applications
                    .stream()
                    .filter(application -> application.getId().equals(applicationEntity.getId()))
                    .findFirst()
                    .get();
                item.setSettings(getSettings(executionContext, app, false));
                return item;
            })
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<ClientCertificate> certificates = clientCertificateCrudService.findByApplicationIdsAndStatuses(
            apps.stream().map(ApplicationListItem::getId).toList(),
            ClientCertificateStatus.ACTIVE,
            ClientCertificateStatus.ACTIVE_WITH_END
        );

        // Group by applicationId and find most recent certificate
        Map<String, ClientCertificate> mostRecentCertificates = certificates
            .stream()
            .collect(
                Collectors.groupingBy(
                    ClientCertificate::applicationId,
                    Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparing(ClientCertificate::createdAt)), opt ->
                        opt.orElse(null)
                    )
                )
            );

        // Set TLS settings with most recent certificate for each app
        apps.forEach(app -> {
            ClientCertificate mostRecent = mostRecentCertificates.get(app.getId());
            if (mostRecent != null) {
                app.getSettings().setTls(TlsSettings.builder().clientCertificate(mostRecent.certificate()).build());
            }
        });

        return apps;
    }

    private ApplicationEntity convert(final ExecutionContext executionContext, Application application, UserEntity primaryOwner) {
        if (primaryOwner == null) {
            // add a default unknown user
            primaryOwner = new UserEntity();
            primaryOwner.setId("0");
            primaryOwner.setFirstname("Unknown");
            primaryOwner.setLastname("User");
        }

        ApplicationEntity applicationEntity = new ApplicationEntity();

        applicationEntity.setId(application.getId());
        applicationEntity.setHrid(application.getHrid());
        applicationEntity.setEnvironmentId(application.getEnvironmentId());
        applicationEntity.setName(application.getName());
        applicationEntity.setDescription(application.getDescription());
        applicationEntity.setDomain(application.getDomain());
        if (application.getType() != null) {
            applicationEntity.setType(application.getType().name());
        }
        applicationEntity.setStatus(application.getStatus().toString());
        applicationEntity.setPicture(application.getPicture());
        applicationEntity.setBackground(application.getBackground());
        applicationEntity.setGroups(application.getGroups());
        applicationEntity.setCreatedAt(application.getCreatedAt());
        applicationEntity.setUpdatedAt(application.getUpdatedAt());
        applicationEntity.setPrimaryOwner(new PrimaryOwnerEntity(primaryOwner));

        applicationEntity.setSettings(getSettings(executionContext, application, true));
        applicationEntity.setDisableMembershipNotifications(application.isDisableMembershipNotifications());
        if (application.getApiKeyMode() != null) {
            applicationEntity.setApiKeyMode(ApiKeyMode.valueOf(application.getApiKeyMode().name()));
        }
        applicationEntity.setOrigin(application.getOrigin() != null ? application.getOrigin() : Origin.MANAGEMENT);
        return applicationEntity;
    }

    private ApplicationSettings getSettings(final ExecutionContext executionContext, Application application, boolean fetchCertificate) {
        final ApplicationSettings settings = new ApplicationSettings();
        if (application.getType() == ApplicationType.SIMPLE) {
            setSimpleAppSettings(application, settings);
        } else {
            setOAuthSettings(executionContext, application, settings);
        }

        if (fetchCertificate) {
            // Fetch the most recent active certificate from ClientCertificateCrudService
            clientCertificateCrudService
                .findMostRecentActiveByApplicationId(application.getId())
                .ifPresent(cert -> settings.setTls(TlsSettings.builder().clientCertificate(cert.certificate()).build()));
        }

        return settings;
    }

    private void setOAuthSettings(ExecutionContext executionContext, Application application, ApplicationSettings settings) {
        OAuthClientSettings clientSettings = new OAuthClientSettings();
        if (application.getMetadata() != null) {
            try {
                final String registrationPayload = application.getMetadata().get(METADATA_REGISTRATION_PAYLOAD);
                if (registrationPayload != null) {
                    final ClientRegistrationResponse registrationResponse = mapper.readValue(
                        registrationPayload,
                        ClientRegistrationResponse.class
                    );
                    clientSettings.setClientId(registrationResponse.getClientId());
                    clientSettings.setClientSecret(registrationResponse.getClientSecret());
                    clientSettings.setClientUri(registrationResponse.getClientUri());
                    clientSettings.setApplicationType(registrationResponse.getApplicationType());
                    clientSettings.setLogoUri(registrationResponse.getLogoUri());
                    clientSettings.setResponseTypes(registrationResponse.getResponseTypes());
                    clientSettings.setRedirectUris(registrationResponse.getRedirectUris());
                    clientSettings.setGrantTypes(registrationResponse.getGrantTypes());
                }

                final String additionalClientMetadata = application.getMetadata().get(METADATA_ADDITIONAL_CLIENT_METADATA);
                if (additionalClientMetadata != null) {
                    clientSettings.setAdditionalClientMetadata(mapper.readValue(additionalClientMetadata, new TypeReference<>() {}));
                }

                Iterator<ClientRegistrationProviderEntity> clientRegistrationProviderIte = clientRegistrationService
                    .findAll(executionContext)
                    .iterator();
                if (clientRegistrationProviderIte.hasNext()) {
                    clientSettings.setRenewClientSecretSupported(clientRegistrationProviderIte.next().isRenewClientSecretSupport());
                }
            } catch (IOException e) {
                log.error("An error occurred while reading client settings");
            }
        }
        settings.setOauth(clientSettings);
    }

    private static void setSimpleAppSettings(Application application, ApplicationSettings settings) {
        SimpleApplicationSettings simpleSettings = new SimpleApplicationSettings();
        if (application.getMetadata() != null) {
            if (application.getMetadata().get(METADATA_CLIENT_ID) != null) {
                simpleSettings.setClientId(application.getMetadata().get(METADATA_CLIENT_ID));
            }
            if (application.getMetadata().get(METADATA_TYPE) != null) {
                simpleSettings.setType(application.getMetadata().get(METADATA_TYPE));
            }
        }
        settings.setApp(simpleSettings);
    }

    @Override
    public InlinePictureEntity getPicture(final ExecutionContext executionContext, String applicationId) {
        ApplicationEntity applicationEntity = findById(executionContext, applicationId);
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        if (applicationEntity.getPicture() != null && !applicationEntity.getPicture().isEmpty()) {
            String[] parts = applicationEntity.getPicture().split(";", 2);
            encodeImage(imageEntity, parts, applicationEntity.getPicture());
        }
        return imageEntity;
    }

    @Override
    public InlinePictureEntity getBackground(final ExecutionContext executionContext, String applicationId) {
        ApplicationEntity applicationEntity = findById(executionContext, applicationId);
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        if (applicationEntity.getBackground() != null) {
            String[] parts = applicationEntity.getBackground().split(";", 2);
            encodeImage(imageEntity, parts, applicationEntity.getBackground());
        }
        return imageEntity;
    }

    private static void encodeImage(InlinePictureEntity imageEntity, String[] parts, String applicationEntity) {
        imageEntity.setType(parts[0].split(":")[1]);
        String base64Content = applicationEntity.split(",", 2)[1];
        imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
    }

    @Override
    public Map<String, Object> findByIdAsMap(String id) throws TechnicalException {
        Application application = applicationRepository.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));

        ExecutionContext executionContext = new ExecutionContext(environmentService.findById(application.getEnvironmentId()));
        ApplicationEntity applicationEntity = convertAndFillPrimaryOwner(executionContext, application);

        Map<String, Object> dataAsMap = mapper.convertValue(applicationEntity, Map.class);
        dataAsMap.remove("picture");
        return dataAsMap;
    }

    public Set<String> searchIds(ExecutionContext executionContext, ApplicationQuery applicationQuery, Sortable sortable) {
        try {
            ApplicationCriteria searchCriteria = buildSearchCriteria(executionContext, applicationQuery);

            if (searchCriteria == null) {
                return Collections.emptySet();
            }

            return applicationRepository.searchIds(searchCriteria, convert(sortable));
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to search applications for query {}", applicationQuery, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for query " + applicationQuery, ex);
        }
    }

    @Override
    public Page<ApplicationListItem> search(
        ExecutionContext executionContext,
        ApplicationQuery applicationQuery,
        Sortable sortable,
        Pageable pageable
    ) {
        try {
            ApplicationCriteria searchCriteria = buildSearchCriteria(executionContext, applicationQuery);

            if (searchCriteria == null) {
                return new Page<>(Collections.emptyList(), 1, 0, 0);
            }

            Page<Application> applications = applicationRepository.search(searchCriteria, convert(pageable), convert(sortable));

            // An archived doesn't have owner
            if (!ApplicationStatus.ARCHIVED.name().equals(applicationQuery.getStatus()) && applicationQuery.includeOwner()) {
                return this.convertToList(executionContext, applications);
            }
            return this.convertToSimpleList(applications);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to search applications for query {}", applicationQuery, ex);
            throw new TechnicalManagementException("An error occurs while trying to find applications for query " + applicationQuery, ex);
        }
    }

    ApplicationCriteria buildSearchCriteria(ExecutionContext executionContext, ApplicationQuery applicationQuery) {
        ApplicationCriteria.ApplicationCriteriaBuilder criteriaBuilder = ApplicationCriteria.builder();

        if (executionContext.hasEnvironmentId()) {
            criteriaBuilder.environmentIds(Sets.newHashSet(executionContext.getEnvironmentId()));
        } else {
            final Set<String> environmentIds = environmentService
                .findByOrganization(executionContext.getOrganizationId())
                .stream()
                .map(EnvironmentEntity::getId)
                .collect(toSet());
            criteriaBuilder.environmentIds(environmentIds);
        }

        if (applicationQuery != null) {
            if (applicationQuery.getQuery() != null) {
                criteriaBuilder.query(applicationQuery.getQuery());
            }

            if (CollectionUtils.isNotEmpty(applicationQuery.getGroups())) {
                criteriaBuilder.groups(applicationQuery.getGroups());
            }

            if (StringUtils.isNotBlank(applicationQuery.getName())) {
                criteriaBuilder.name(applicationQuery.getName());
            }

            ApplicationStatus applicationStatus = null;
            if (applicationQuery.getStatus() != null && !applicationQuery.getStatus().isBlank()) {
                applicationStatus = ApplicationStatus.valueOf(applicationQuery.getStatus().toUpperCase());
                criteriaBuilder.status(applicationStatus);
            }

            if (CollectionUtils.isNotEmpty(applicationQuery.getIds())) {
                criteriaBuilder.restrictedToIds(applicationQuery.getIds());
            }

            if (StringUtils.isNotBlank(applicationQuery.getUser())) {
                Set<String> userApplicationsIds = findUserApplicationsIds(executionContext, applicationQuery.getUser(), applicationStatus);
                if (userApplicationsIds.isEmpty()) {
                    return null;
                }
                if (CollectionUtils.isNotEmpty(applicationQuery.getIds())) {
                    Set<String> authorizedApps = userApplicationsIds.stream().filter(applicationQuery.getIds()::contains).collect(toSet());
                    if (authorizedApps.isEmpty()) {
                        return null;
                    }
                    criteriaBuilder.restrictedToIds(authorizedApps);
                } else {
                    criteriaBuilder.restrictedToIds(userApplicationsIds);
                }
            }
        }
        return criteriaBuilder.build();
    }

    public Set<String> findUserApplicationsIds(ExecutionContext executionContext, String username, ApplicationStatus status) {
        //find applications where the user is a member
        Set<String> appIds = membershipService.getReferenceIdsByMemberAndReference(
            MembershipMemberType.USER,
            username,
            MembershipReferenceType.APPLICATION
        );

        //find user groups
        List<String> groupIds = membershipService
            .getMembershipsByMemberAndReference(MembershipMemberType.USER, username, MembershipReferenceType.GROUP)
            .stream()
            .filter(m -> m.getRoleId() != null && roleService.findById(m.getRoleId()).getScope().equals(RoleScope.APPLICATION))
            .map(MembershipEntity::getReferenceId)
            .toList();

        if (!groupIds.isEmpty()) {
            ApplicationQuery applicationQueryWithGroupsAndStatus = new ApplicationQuery();
            applicationQueryWithGroupsAndStatus.setGroups(new HashSet<>(groupIds));
            if (status != null) {
                applicationQueryWithGroupsAndStatus.setStatus(status.name());
            }
            appIds.addAll(this.searchIds(executionContext, applicationQueryWithGroupsAndStatus, null));
        }

        return appIds;
    }

    private void checkClientIdIsUniqueForEnv(String clientId, String environmentId) {
        if (applicationRepository.existsMetadataEntryForEnv(METADATA_CLIENT_ID, clientId, environmentId)) {
            throw new ClientIdAlreadyExistsException(clientId);
        }
    }

    private void checkApiKeyModeUpdate(ExecutionContext executionContext, ApiKeyMode apiKeyMode, Application applicationToUpdate) {
        if (
            applicationToUpdate.getApiKeyMode() != null &&
            !applicationToUpdate.getApiKeyMode().isUpdatable() &&
            !applicationToUpdate.getApiKeyMode().name().equals(apiKeyMode.name())
        ) {
            throw new InvalidApplicationApiKeyModeException(
                String.format(
                    "Can't update application %s API Key mode cause current API Key Mode %s is not updatable",
                    applicationToUpdate.getId(),
                    applicationToUpdate.getApiKeyMode()
                )
            );
        } else if (
            apiKeyMode == ApiKeyMode.SHARED &&
            applicationToUpdate.getApiKeyMode() != io.gravitee.repository.management.model.ApiKeyMode.SHARED &&
            !parameterService.findAsBoolean(executionContext, Key.PLAN_SECURITY_APIKEY_SHARED_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            throw new InvalidApplicationApiKeyModeException(
                String.format(
                    "Can't update application %s API Key mode to SHARED cause environment setting is disabled",
                    applicationToUpdate.getId()
                )
            );
        }
    }

    private ApplicationEntity convertAndFillPrimaryOwner(ExecutionContext executionContext, Application application) {
        MembershipEntity primaryOwnerMemberEntity = membershipService.getPrimaryOwner(
            executionContext.getOrganizationId(),
            MembershipReferenceType.APPLICATION,
            application.getId()
        );
        if (primaryOwnerMemberEntity == null) {
            if (!ApplicationStatus.ARCHIVED.equals(application.getStatus())) {
                log.error("The Application {} doesn't have any primary owner.", application.getId());
            }
            return convert(executionContext, application, null);
        }

        return convert(executionContext, application, userService.findById(executionContext, primaryOwnerMemberEntity.getMemberId()));
    }
}
