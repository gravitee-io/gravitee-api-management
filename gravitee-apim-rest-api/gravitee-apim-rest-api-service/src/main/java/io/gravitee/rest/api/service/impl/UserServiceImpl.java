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

import static io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService.DEFAULT_CONSOLE_URL;
import static io.gravitee.repository.management.model.Audit.AuditProperties.USER;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.GROUP_INVITATION;
import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.RESET_PASSWORD;
import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.USER_CREATION;
import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.USER_REGISTRATION;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.REGISTRATION_PATH;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.RESET_PASSWORD_PATH;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.data.domain.Page;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.spel.function.json.JsonPathFunction;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.InvitationEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.NewPreRegisterUserEntity;
import io.gravitee.rest.api.model.NewUserMetadataEntity;
import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.RegisterUserEntity;
import io.gravitee.rest.api.model.ResetPasswordUserEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.UpdateUserMetadataEntity;
import io.gravitee.rest.api.model.UrlPictureEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserMetadataEntity;
import io.gravitee.rest.api.model.UserRoleEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.configuration.identity.GroupMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.RoleMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.EmailValidator;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.InvitationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NewsletterService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PasswordValidator;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserMetadataService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper.ACTION;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.converter.UserConverter;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import io.gravitee.rest.api.service.exceptions.DefaultRoleNotFoundException;
import io.gravitee.rest.api.service.exceptions.EmailFormatInvalidException;
import io.gravitee.rest.api.service.exceptions.EmailRequiredException;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.PasswordAlreadyResetException;
import io.gravitee.rest.api.service.exceptions.PasswordFormatInvalidException;
import io.gravitee.rest.api.service.exceptions.ServiceAccountNotManageableException;
import io.gravitee.rest.api.service.exceptions.StillPrimaryOwnerException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.UserAlreadyFinalizedException;
import io.gravitee.rest.api.service.exceptions.UserNotActiveException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.exceptions.UserNotInternallyManagedException;
import io.gravitee.rest.api.service.exceptions.UserRegistrationUnavailableException;
import io.gravitee.rest.api.service.exceptions.UserStateConflictException;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import jakarta.xml.bind.DatatypeConverter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserServiceImpl extends AbstractService implements UserService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    /**
     * A default source used for user registration.
     */
    private static final String IDP_SOURCE_GRAVITEE = "gravitee";
    private static final String TEMPLATE_ENGINE_PROFILE_ATTRIBUTE = "profile";

    // Dirty hack: only used to force class loading
    static {
        try {
            LOGGER.trace(
                "Loading class to initialize properly JsonPath Cache provider: {}",
                Class.forName(JsonPathFunction.class.getName())
            );
        } catch (ClassNotFoundException ignored) {
            LOGGER.trace("Loading class to initialize properly JsonPath Cache provider : fail");
        }
    }

    @Lazy
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MembershipService membershipService;

    @Lazy
    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotifierService notifierService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private SearchEngineService searchEngineService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private PortalNotificationService portalNotificationService;

    @Autowired
    private PortalNotificationConfigService portalNotificationConfigService;

    @Autowired
    private GenericNotificationConfigService genericNotificationConfigService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private NewsletterService newsletterService;

    @Autowired
    private PasswordValidator passwordValidator;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private UserMetadataService userMetadataService;

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @Value("${user.login.defaultApplication:true}")
    private boolean defaultApplicationForFirstConnection;

    @Value("${user.anonymize-on-delete.enabled:false}")
    private boolean anonymizeOnDelete;

    private List<String> portalWhitelist;
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void afterPropertiesSet() {
        int i = 0;
        portalWhitelist = new ArrayList<>();

        String whitelistUrl;

        while ((whitelistUrl = environment.getProperty("portal.whitelist[" + i + "]")) != null) {
            portalWhitelist.add(whitelistUrl);
            i++;
        }
    }

    @Override
    public UserEntity connect(ExecutionContext executionContext, String userId) {
        try {
            LOGGER.debug("Connection of {}", userId);
            Optional<User> checkUser = userRepository.findById(userId);
            if (!checkUser.isPresent()) {
                throw new UserNotFoundException(userId);
            }

            User user = checkUser.get();
            User previousUser = new User(user);

            // First connection: create default application for user & notify
            if (user.getLastConnectionAt() == null && user.getFirstConnectionAt() == null) {
                notifierService.trigger(
                    executionContext,
                    PortalHook.USER_FIRST_LOGIN,
                    new NotificationParamsBuilder().user(convert(user, false)).build()
                );
                user.setFirstConnectionAt(new Date());
                if (defaultApplicationForFirstConnection) {
                    LOGGER.debug("Create a default application for {}", userId);
                    NewApplicationEntity defaultApp = new NewApplicationEntity();
                    defaultApp.setName("Default application");
                    defaultApp.setDescription("My default application");

                    // To preserve backward compatibility, ensure that we have at least default settings for simple application type
                    ApplicationSettings settings = new ApplicationSettings();
                    SimpleApplicationSettings simpleAppSettings = new SimpleApplicationSettings();
                    settings.setApp(simpleAppSettings);
                    defaultApp.setSettings(settings);

                    try {
                        environmentService
                            .findByUser(executionContext.getOrganizationId(), userId)
                            .forEach(env ->
                                applicationService.create(
                                    new ExecutionContext(executionContext.getOrganizationId(), env.getId()),
                                    defaultApp,
                                    userId
                                )
                            );
                    } catch (IllegalStateException ex) {
                        //do not fail to create a user even if we are not able to create its default app
                        LOGGER.warn("Not able to create default app for user {}", userId);
                    }
                }
            }
            // Set date fields
            user.setLastConnectionAt(new Date());
            if (user.getFirstConnectionAt() == null) {
                user.setFirstConnectionAt(user.getLastConnectionAt());
            }
            user.setUpdatedAt(user.getLastConnectionAt());

            user.setLoginCount(user.getLoginCount() + 1);

            User updatedUser = userRepository.update(user);
            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(USER, userId),
                User.AuditEvent.USER_CONNECTED,
                user.getUpdatedAt(),
                previousUser,
                user
            );

            final UserEntity userEntity = convert(updatedUser, true);
            searchEngineService.index(executionContext, userEntity, false);
            return userEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to connect {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to connect " + userId, ex);
        }
    }

    @Override
    public UserEntity findById(ExecutionContext executionContext, String id, boolean defaultValue) {
        return GraviteeContext
            .getCurrentUsers()
            .computeIfAbsent(
                id,
                k -> {
                    try {
                        LOGGER.debug("Find user by ID: {}", k);

                        Optional<User> optionalUser = userRepository
                            .findById(k)
                            .filter(u -> u.getOrganizationId().equalsIgnoreCase(executionContext.getOrganizationId()));
                        if (optionalUser.isPresent()) {
                            return convert(optionalUser.get(), false, userMetadataService.findAllByUserId(k), true);
                        }

                        if (defaultValue) {
                            UserEntity unknownUser = new UserEntity();
                            unknownUser.setId(k);
                            unknownUser.setFirstname("Unknown user");
                            return unknownUser;
                        }

                        //should never happen
                        throw new UserNotFoundException(k);
                    } catch (TechnicalException ex) {
                        LOGGER.error("An error occurs while trying to find user using its ID {}", k, ex);
                        throw new TechnicalManagementException("An error occurs while trying to find user using its ID " + k, ex);
                    }
                }
            );
    }

    @Override
    public Optional<UserEntity> findByEmail(ExecutionContext executionContext, String email) {
        try {
            LOGGER.debug("Find user by Email: {}", email);
            Optional<User> optionalUser = userRepository.findByEmail(email, executionContext.getOrganizationId());
            return optionalUser.map(user -> convert(optionalUser.get(), false));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find user using its email", ex);
            throw new TechnicalManagementException("An error occurs while trying to find user using its email", ex);
        }
    }

    @Override
    public UserEntity findByIdWithRoles(ExecutionContext executionContext, String userId) {
        LOGGER.debug("Find user by ID: {}", userId);
        try {
            return userRepository
                .findById(userId)
                .filter(user -> user.getOrganizationId().equalsIgnoreCase(executionContext.getOrganizationId()))
                .map(user -> convertWithFlags(executionContext, user))
                .orElseThrow(() -> new UserNotFoundException(userId)); // should never happen
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find user using its ID {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find user using its ID " + userId, ex);
        }
    }

    @Override
    public UserEntity findBySource(String organizationId, String source, String sourceId, boolean loadRoles) {
        try {
            LOGGER.debug("Find user by source[{}] user[{}]", source, sourceId);

            return userRepository
                .findBySource(source, sourceId, organizationId)
                .map(user -> convert(user, loadRoles, emptyList(), false))
                .orElseThrow(() -> new UserNotFoundException(sourceId));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find user using source[{}], user[{}]", source, sourceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find user using source " + source + ':' + sourceId, ex);
        }
    }

    @Override
    public Set<UserEntity> findByIds(ExecutionContext executionContext, Collection<String> ids) {
        return this.findByIds(executionContext, ids, true);
    }

    @Override
    public Set<UserEntity> findByIds(ExecutionContext executionContext, Collection<String> ids, boolean withUserMetadata) {
        try {
            LOGGER.debug("Find users by ID: {}", ids);

            Set<User> users = userRepository.findByIds(ids);

            if (!users.isEmpty()) {
                return users
                    .stream()
                    .map(u -> this.convert(u, false, withUserMetadata ? userMetadataService.findAllByUserId(u.getId()) : emptyList(), true))
                    .collect(toSet());
            }

            Optional<String> idsAsString = ids.stream().reduce((a, b) -> a + '/' + b);
            if (idsAsString.isPresent()) {
                throw new UserNotFoundException(idsAsString.get());
            } else {
                throw new UserNotFoundException("?");
            }
        } catch (TechnicalException ex) {
            Optional<String> idsAsString = ids.stream().reduce((a, b) -> a + '/' + b);
            LOGGER.error("An error occurs while trying to find users using their ID {}", idsAsString, ex);
            throw new TechnicalManagementException("An error occurs while trying to find users using their ID " + idsAsString, ex);
        }
    }

    private void checkUserRegistrationEnabled(ExecutionContext executionContext) {
        boolean userCreationEnabled;
        ReferenceContext currentContext = executionContext.getReferenceContext();
        if (currentContext.getReferenceType().equals(ReferenceContext.Type.ORGANIZATION)) {
            userCreationEnabled =
                parameterService.findAsBoolean(
                    executionContext,
                    Key.CONSOLE_USERCREATION_ENABLED,
                    currentContext.getReferenceId(),
                    ParameterReferenceType.ORGANIZATION
                );
        } else {
            userCreationEnabled =
                parameterService.findAsBoolean(
                    executionContext,
                    Key.PORTAL_USERCREATION_ENABLED,
                    currentContext.getReferenceId(),
                    ParameterReferenceType.ENVIRONMENT
                );
        }

        if (!userCreationEnabled) {
            throw new UserRegistrationUnavailableException();
        }
    }

    /**
     * Allows to complete the creation of a user which is pre-created.
     *
     * @param executionContext
     * @param registerUserEntity a valid token and a password
     * @return the user
     */
    @Override
    public UserEntity finalizeRegistration(ExecutionContext executionContext, final RegisterUserEntity registerUserEntity) {
        try {
            DecodedJWT jwt = getDecodedJWT(registerUserEntity.getToken());

            final String action = jwt.getClaim(Claims.ACTION).asString();
            if (RESET_PASSWORD.name().equals(action)) {
                throw new UserStateConflictException("Reset password forbidden on this resource");
            }

            if (USER_REGISTRATION.name().equals(action)) {
                checkUserRegistrationEnabled(executionContext);
            } else if (GROUP_INVITATION.name().equals(action)) {
                // check invitations
                final String email = jwt.getClaim(Claims.EMAIL).asString();
                final List<InvitationEntity> invitations = invitationService.findAll();
                final List<InvitationEntity> userInvitations = invitations
                    .stream()
                    .filter(invitation -> invitation.getEmail().equals(email))
                    .collect(toList());
                if (userInvitations.isEmpty()) {
                    throw new IllegalStateException("Invitation has been canceled");
                }
            }

            // check password here to avoid user creation if password is invalid
            if (registerUserEntity.getPassword() != null) {
                if (!passwordValidator.validate(registerUserEntity.getPassword())) {
                    throw new PasswordFormatInvalidException();
                }
            }

            final Object subject = jwt.getSubject();
            User user;
            if (subject == null) {
                final NewExternalUserEntity externalUser = new NewExternalUserEntity();
                final String email = jwt.getClaim(Claims.EMAIL).asString();
                externalUser.setSource(IDP_SOURCE_GRAVITEE);
                externalUser.setSourceId(email);
                externalUser.setFirstname(registerUserEntity.getFirstname());
                externalUser.setLastname(registerUserEntity.getLastname());
                externalUser.setEmail(email);
                user = userConverter.toUser(create(executionContext, externalUser, true));
                user.setOrganizationId(executionContext.getOrganizationId());
            } else {
                final String username = subject.toString();
                LOGGER.debug("Create an internal user {}", username);
                Optional<User> checkUser = userRepository.findById(username);
                user = checkUser.orElseThrow(() -> new UserNotFoundException(username));
                if (StringUtils.isNotBlank(user.getPassword())) {
                    throw new UserAlreadyFinalizedException(executionContext.getOrganizationId());
                }
            }

            if (GROUP_INVITATION.name().equals(action)) {
                // check invitations
                final String email = user.getEmail();
                final String userId = user.getId();
                final List<InvitationEntity> invitations = invitationService.findAll();
                invitations
                    .stream()
                    .filter(invitation -> invitation.getEmail().equals(email))
                    .forEach(invitation -> {
                        invitationService.addMember(
                            executionContext,
                            invitation.getReferenceType().name(),
                            invitation.getReferenceId(),
                            userId,
                            invitation.getApiRole(),
                            invitation.getApplicationRole()
                        );
                        invitationService.delete(invitation.getId(), invitation.getReferenceId());
                    });
            }

            // Set date fields
            user.setUpdatedAt(new Date());

            // Encrypt password if internal user
            encryptPassword(user, registerUserEntity.getPassword());

            user = userRepository.update(user);
            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(USER, user.getId()),
                User.AuditEvent.USER_CREATED,
                user.getUpdatedAt(),
                null,
                user
            );

            // Do not send back the password
            user.setPassword(null);

            final UserEntity userEntity = convert(user, true);
            searchEngineService.index(executionContext, userEntity, false);
            return userEntity;
        } catch (AbstractManagementException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to create an internal user with the token {}", registerUserEntity.getToken(), ex);
            throw new TechnicalManagementException(ex.getMessage(), ex);
        }
    }

    @Override
    public UserEntity finalizeResetPassword(ExecutionContext executionContext, ResetPasswordUserEntity registerUserEntity) {
        try {
            DecodedJWT jwt = getDecodedJWT(registerUserEntity.getToken());

            final String action = jwt.getClaim(Claims.ACTION).asString();
            if (!RESET_PASSWORD.name().equals(action)) {
                throw new UserStateConflictException("Invalid action on reset password resource");
            }

            final Object subject = jwt.getSubject();
            User user;
            if (subject == null) {
                throw new UserNotFoundException("Subject missing from JWT token");
            } else {
                final String username = subject.toString();
                LOGGER.debug("Find user {} to update password", username);
                user =
                    userRepository
                        .findById(username)
                        .filter(u -> u.getOrganizationId().equalsIgnoreCase(executionContext.getOrganizationId()))
                        .orElseThrow(() -> new UserNotFoundException(username));
            }

            // Set date fields
            user.setUpdatedAt(new Date());

            // Encrypt password if internal user
            encryptPassword(user, registerUserEntity.getPassword());

            user = userRepository.update(user);

            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(USER, user.getId()),
                User.AuditEvent.PASSWORD_CHANGED,
                user.getUpdatedAt(),
                null,
                null
            );

            // Do not send back the password
            user.setPassword(null);

            return convert(user, true);
        } catch (AbstractManagementException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.error(
                "An error occurs while trying to change password of an internal user with the token {}",
                registerUserEntity.getToken(),
                ex
            );
            throw new TechnicalManagementException(ex.getMessage(), ex);
        }
    }

    private void encryptPassword(User user, String password) {
        if (password != null) {
            if (passwordValidator.validate(password)) {
                user.setPassword(passwordEncoder.encode(password));
            } else {
                throw new PasswordFormatInvalidException();
            }
        }
    }

    private DecodedJWT getDecodedJWT(String token) {
        final String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        JWTVerifier verifier = JWT.require(algorithm).withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER)).build();

        return verifier.verify(token);
    }

    @Override
    public PictureEntity getPicture(ExecutionContext executionContext, String id) {
        UserEntity user = findById(executionContext, id);

        if (user.getPicture() != null) {
            String picture = user.getPicture();

            if (picture.matches("^(http|https)://.*$")) {
                return new UrlPictureEntity(picture);
            } else {
                try {
                    InlinePictureEntity imageEntity = new InlinePictureEntity();
                    String[] parts = picture.split(";", 2);
                    imageEntity.setType(parts[0].split(":")[1]);
                    String base64Content = picture.split(",", 2)[1];
                    imageEntity.setContent(DatatypeConverter.parseBase64Binary(base64Content));
                    return imageEntity;
                } catch (Exception ex) {
                    LOGGER.warn("Unable to get user picture for id[{}]", id);
                }
            }
        }

        // Return empty image
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        imageEntity.setType("image/png");
        return imageEntity;
    }

    /**
     * Allows to create a user.
     *
     * @param executionContext
     * @param newExternalUserEntity
     * @return
     */
    @Override
    public UserEntity create(ExecutionContext executionContext, NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole) {
        return create(executionContext, newExternalUserEntity, addDefaultRole, true);
    }

    private UserEntity create(
        ExecutionContext executionContext,
        NewExternalUserEntity newExternalUserEntity,
        boolean addDefaultRole,
        boolean autoRegistrationEnabled
    ) {
        try {
            String organizationId = executionContext.getOrganizationId();

            // First we check that organization exist
            this.organizationService.findById(organizationId);

            LOGGER.debug("Create an external user {}", newExternalUserEntity);
            Optional<User> checkUser = userRepository.findBySource(
                newExternalUserEntity.getSource(),
                newExternalUserEntity.getSourceId(),
                organizationId
            );

            if (checkUser.isPresent()) {
                throw new UserAlreadyExistsException(
                    newExternalUserEntity.getSource(),
                    newExternalUserEntity.getSourceId(),
                    organizationId
                );
            }

            User user = userConverter.toUser(newExternalUserEntity);
            user.setId(UuidString.generateRandom());
            user.setOrganizationId(organizationId);
            user.setStatus(autoRegistrationEnabled ? UserStatus.ACTIVE : UserStatus.PENDING);

            // Set date fields
            user.setCreatedAt(new Date());
            user.setUpdatedAt(user.getCreatedAt());

            User createdUser = userRepository.create(user);
            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(USER, user.getId()),
                User.AuditEvent.USER_CREATED,
                user.getCreatedAt(),
                null,
                user
            );

            List<UserMetadataEntity> metadata = new ArrayList<>();
            if (newExternalUserEntity.getCustomFields() != null) {
                for (Map.Entry<String, Object> entry : newExternalUserEntity.getCustomFields().entrySet()) {
                    NewUserMetadataEntity metadataEntity = new NewUserMetadataEntity();
                    metadataEntity.setName(entry.getKey());
                    metadataEntity.setUserId(createdUser.getId());
                    metadataEntity.setFormat(MetadataFormat.STRING);
                    metadataEntity.setValue(String.valueOf(entry.getValue()));
                    metadata.add(userMetadataService.create(executionContext, metadataEntity));
                }
            }

            if (addDefaultRole) {
                addDefaultMembership(executionContext, createdUser);
            }

            final UserEntity userEntity = convert(createdUser, true, metadata, true);
            searchEngineService.index(executionContext, userEntity, false);
            return userEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create an external user {}", newExternalUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create an external user" + newExternalUserEntity, ex);
        }
    }

    private void addDefaultMembership(ExecutionContext executionContext, User user) {
        RoleScope[] scopes = { RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT };
        List<RoleEntity> defaultRoleByScopes = roleService.findDefaultRoleByScopes(executionContext.getOrganizationId(), scopes);
        if (defaultRoleByScopes == null || defaultRoleByScopes.isEmpty()) {
            throw new DefaultRoleNotFoundException(scopes);
        }
        for (RoleEntity defaultRoleByScope : defaultRoleByScopes) {
            switch (defaultRoleByScope.getScope()) {
                case ORGANIZATION:
                    membershipService.addRoleToMemberOnReference(
                        executionContext,
                        new MembershipService.MembershipReference(
                            MembershipReferenceType.ORGANIZATION,
                            executionContext.getOrganizationId()
                        ),
                        new MembershipService.MembershipMember(user.getId(), null, MembershipMemberType.USER),
                        new MembershipService.MembershipRole(RoleScope.ORGANIZATION, defaultRoleByScope.getName())
                    );
                    break;
                case ENVIRONMENT:
                    membershipService.addRoleToMemberOnReference(
                        executionContext,
                        new MembershipService.MembershipReference(
                            MembershipReferenceType.ENVIRONMENT,
                            executionContext.hasEnvironmentId()
                                ? executionContext.getEnvironmentId()
                                : GraviteeContext.getDefaultEnvironment()
                        ),
                        new MembershipService.MembershipMember(user.getId(), null, MembershipMemberType.USER),
                        new MembershipService.MembershipRole(RoleScope.ENVIRONMENT, defaultRoleByScope.getName())
                    );
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public UserEntity register(ExecutionContext executionContext, final NewExternalUserEntity newExternalUserEntity) {
        return register(executionContext, newExternalUserEntity, null);
    }

    @Override
    public UserEntity register(
        ExecutionContext executionContext,
        final NewExternalUserEntity newExternalUserEntity,
        final String confirmationPageUrl
    ) {
        final ReferenceContext currentContext = executionContext.getReferenceContext();

        if (confirmationPageUrl != null) {
            UrlSanitizerUtils.checkAllowed(confirmationPageUrl, portalWhitelist, true);
        }

        checkUserRegistrationEnabled(executionContext);
        boolean autoRegistrationEnabled = isAutoRegistrationEnabled(executionContext, currentContext);

        return createAndSendEmail(
            executionContext,
            newExternalUserEntity,
            USER_REGISTRATION,
            confirmationPageUrl,
            autoRegistrationEnabled,
            false
        );
    }

    private boolean isAutoRegistrationEnabled(ExecutionContext executionContext, ReferenceContext currentContext) {
        if (currentContext.getReferenceType().equals(ReferenceContext.Type.ORGANIZATION)) {
            return parameterService.findAsBoolean(
                executionContext,
                Key.CONSOLE_USERCREATION_AUTOMATICVALIDATION_ENABLED,
                currentContext.getReferenceId(),
                ParameterReferenceType.ORGANIZATION
            );
        }
        return parameterService.findAsBoolean(
            executionContext,
            Key.PORTAL_USERCREATION_AUTOMATICVALIDATION_ENABLED,
            currentContext.getReferenceId(),
            ParameterReferenceType.ENVIRONMENT
        );
    }

    @Override
    public UserEntity create(ExecutionContext executionContext, final NewPreRegisterUserEntity newPreRegisterUserEntity) {
        return createAndSendEmail(
            executionContext,
            newPreRegisterUserEntity,
            USER_CREATION,
            null,
            true,
            newPreRegisterUserEntity.isService()
        );
    }

    private boolean isServiceAccount(User user) {
        // A service account do not have a password and the sourceId is equals to the lastname or to the email address
        return (
            IDP_SOURCE_GRAVITEE.equals(user.getSource()) &&
            (user.getPassword() == null || user.getPassword().isEmpty()) &&
            (user.getSourceId().equalsIgnoreCase(user.getEmail()) || user.getSourceId().equalsIgnoreCase(user.getLastname()))
        );
    }

    /**
     * Allows to create an user and send an email notification to finalize its creation.
     */
    private UserEntity createAndSendEmail(
        ExecutionContext executionContext,
        final NewExternalUserEntity newExternalUserEntity,
        final ACTION action,
        final String confirmationPageUrl,
        final boolean autoRegistrationEnabled,
        final boolean isServiceUser
    ) {
        if (
            (!isServiceUser || StringUtils.isNotEmpty(newExternalUserEntity.getEmail())) &&
            !EmailValidator.isValid(newExternalUserEntity.getEmail())
        ) {
            throw new EmailFormatInvalidException(newExternalUserEntity.getEmail());
        }

        String organizationId = executionContext.getOrganizationId();

        if (isBlank(newExternalUserEntity.getSource())) {
            newExternalUserEntity.setSource(IDP_SOURCE_GRAVITEE);
        } else if (!IDP_SOURCE_GRAVITEE.equals(newExternalUserEntity.getSource())) {
            // check if IDP exists
            identityProviderService.findById(newExternalUserEntity.getSource());
        }

        if (isBlank(newExternalUserEntity.getSourceId())) {
            newExternalUserEntity.setSourceId(isServiceUser ? newExternalUserEntity.getLastname() : newExternalUserEntity.getEmail());
        }

        final Optional<User> optionalUser;
        try {
            optionalUser =
                userRepository.findBySource(newExternalUserEntity.getSource(), newExternalUserEntity.getSourceId(), organizationId);
            if (optionalUser.isPresent()) {
                throw new UserAlreadyExistsException(
                    newExternalUserEntity.getSource(),
                    newExternalUserEntity.getSourceId(),
                    organizationId
                );
            }
        } catch (final TechnicalException e) {
            LOGGER.error(
                "An error occurs while trying to create user {} / {}",
                newExternalUserEntity.getSource(),
                newExternalUserEntity.getSourceId(),
                e
            );
            throw new TechnicalManagementException(e.getMessage(), e);
        }

        final UserEntity userEntity = create(executionContext, newExternalUserEntity, true, autoRegistrationEnabled);

        if (userEntity == null) {
            LOGGER.error("An error occurs while trying to create user");
            throw new TechnicalManagementException("An error occurs while trying to create user");
        }

        if (!isServiceUser) {
            if (IDP_SOURCE_GRAVITEE.equals(newExternalUserEntity.getSource())) {
                final Map<String, Object> params = getTokenRegistrationParams(
                    executionContext,
                    userEntity,
                    REGISTRATION_PATH,
                    action,
                    confirmationPageUrl
                );
                emailService.sendAsyncEmailNotification(
                    executionContext,
                    new EmailNotificationBuilder()
                        .to(userEntity.getEmail())
                        .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_REGISTRATION)
                        .params(params)
                        .param("registrationAction", USER_REGISTRATION.equals(action) ? "registration" : "creation")
                        .build()
                );

                if (autoRegistrationEnabled) {
                    notifierService.trigger(
                        executionContext,
                        ACTION.USER_REGISTRATION.equals(action) ? PortalHook.USER_REGISTERED : PortalHook.USER_CREATED,
                        params
                    );
                } else {
                    notifierService.trigger(executionContext, PortalHook.USER_REGISTRATION_REQUEST, params);
                }
            }
        }

        if (!isServiceUser && newExternalUserEntity.getNewsletter() != null && newExternalUserEntity.getNewsletter()) {
            newsletterService.subscribe(newExternalUserEntity.getEmail());
        }

        return userEntity;
    }

    @Override
    public UserEntity processRegistration(ExecutionContext executionContext, String userId, boolean accepted) {
        UserEntity userToProcess = findById(executionContext, userId);
        UserEntity processedUser = this.changeUserStatus(executionContext, userId, accepted ? UserStatus.ACTIVE : UserStatus.REJECTED);
        final Map<String, Object> params = new NotificationParamsBuilder().user(processedUser).build();
        emailService.sendAsyncEmailNotification(
            executionContext,
            new EmailNotificationBuilder()
                .to(userToProcess.getEmail())
                .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_REGISTRATION_REQUEST_PROCESSED)
                .params(params)
                .param("registrationStatus", accepted ? "accepted" : "rejected")
                .build()
        );
        auditService.createAuditLog(
            executionContext,
            Collections.singletonMap(USER, processedUser.getId()),
            accepted ? User.AuditEvent.USER_CONFIRMED : User.AuditEvent.USER_REJECTED,
            processedUser.getUpdatedAt(),
            userToProcess,
            processedUser
        );

        return processedUser;
    }

    @NotNull
    private UserEntity changeUserStatus(ExecutionContext executionContext, String userId, UserStatus newStatus) {
        try {
            Optional<User> optionalUser = this.userRepository.findById(userId);
            if (optionalUser.isPresent()) {
                final User user = optionalUser.get();
                user.setStatus(newStatus);
                user.setUpdatedAt(new Date());
                if (newStatus == UserStatus.REJECTED) {
                    //so a new registration can be requested with the same email
                    user.setSourceId(newStatus.name().toLowerCase() + "-" + user.getSourceId());
                }
                final User updatedUser = this.userRepository.update(user);
                if (updatedUser == null) {
                    throw new TechnicalManagementException("An error occurs while trying to update user");
                }
                return convert(updatedUser, true);
            }
            throw new UserNotFoundException(userId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to validate user registration {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to create an external user" + userId, ex);
        }
    }

    @Override
    public Map<String, Object> getTokenRegistrationParams(
        ExecutionContext executionContext,
        final UserEntity userEntity,
        final String managementUri,
        final ACTION action
    ) {
        return getTokenRegistrationParams(executionContext, userEntity, managementUri, action, null);
    }

    @Override
    public Map<String, Object> getTokenRegistrationParams(
        ExecutionContext executionContext,
        final UserEntity userEntity,
        final String managementUri,
        final ACTION action,
        final String targetPageUrl
    ) {
        // generate a JWT to store user's information and for security purpose
        final String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));

        Date issueAt = new Date();
        Instant expireAt = issueAt
            .toInstant()
            .plus(
                Duration.ofSeconds(
                    environment.getProperty("user.creation.token.expire-after", Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER)
                )
            );

        final String token = JWT
            .create()
            .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
            .withIssuedAt(issueAt)
            .withExpiresAt(Date.from(expireAt))
            .withSubject(userEntity.getId())
            .withClaim(Claims.EMAIL, userEntity.getEmail())
            .withClaim(Claims.FIRSTNAME, userEntity.getFirstname())
            .withClaim(Claims.LASTNAME, userEntity.getLastname())
            .withClaim(Claims.ACTION, action.name())
            .sign(algorithm);

        String userURL = "";
        String managementURL = installationAccessQueryService.getConsoleUrl(executionContext.getOrganizationId());
        if (!StringUtils.isEmpty(managementURL)) {
            if (managementURL.endsWith("/")) {
                managementURL = managementURL.substring(0, managementURL.length() - 1);
            }
            userURL = managementURL + "/#!/_organization/users/" + userEntity.getId();
        }

        String registrationUrl = "";
        if (targetPageUrl != null && !targetPageUrl.isEmpty()) {
            registrationUrl += targetPageUrl;
            if (!targetPageUrl.endsWith("/")) {
                registrationUrl += "/";
            }
            registrationUrl += token;
        } else if (!StringUtils.isEmpty(managementURL) && !DEFAULT_CONSOLE_URL.equals(managementURL)) {
            registrationUrl = managementURL + managementUri + token;
        } else {
            // This value is used as a fallback when no Management URL has been configured by the platform admin.
            registrationUrl = DEFAULT_CONSOLE_URL + managementUri + token;
            LOGGER.warn(
                "An email will be sent with a default '" +
                managementUri.substring(4, managementUri.indexOf('/', 4)) +
                "' link. You may want to change this default configuration of the 'Management URL' in the Settings."
            );
        }

        // send a confirm email with the token
        return new NotificationParamsBuilder().user(userEntity).token(token).registrationUrl(registrationUrl).userUrl(userURL).build();
    }

    @Override
    public UserEntity update(ExecutionContext executionContext, String id, UpdateUserEntity updateUserEntity) {
        return this.update(executionContext, id, updateUserEntity, updateUserEntity.getEmail());
    }

    @Override
    public UserEntity update(ExecutionContext executionContext, String id, UpdateUserEntity updateUserEntity, String newsletterEmail) {
        try {
            LOGGER.debug("Updating {}", updateUserEntity);
            Optional<User> checkUser = userRepository.findById(id);
            if (!checkUser.isPresent()) {
                throw new UserNotFoundException(id);
            }

            User user = checkUser.get();
            User previousUser = new User(user);

            // Set date fields
            user.setUpdatedAt(new Date());

            // Set variant fields
            if (updateUserEntity.getPicture() != null) {
                user.setPicture(updateUserEntity.getPicture());
            }

            if (updateUserEntity.getFirstname() != null) {
                user.setFirstname(updateUserEntity.getFirstname());
            }
            if (updateUserEntity.getLastname() != null) {
                user.setLastname(updateUserEntity.getLastname());
            }
            if (updateUserEntity.getEmail() != null && !updateUserEntity.getEmail().equals(user.getEmail())) {
                if (isInternalUser(user)) {
                    if (StringUtils.isNotEmpty(updateUserEntity.getEmail()) && !EmailValidator.isValid(updateUserEntity.getEmail())) {
                        throw new EmailFormatInvalidException(updateUserEntity.getEmail());
                    }

                    // sourceId can be updated only for user registered into the Gravitee Repository
                    // in that case, check if the email is available before update sourceId
                    final Optional<User> optionalUser = userRepository.findBySource(
                        user.getSource(),
                        updateUserEntity.getEmail(),
                        user.getOrganizationId()
                    );
                    if (optionalUser.isPresent()) {
                        throw new UserAlreadyExistsException(user.getSource(), updateUserEntity.getEmail(), user.getOrganizationId());
                    }
                    user.setSourceId(updateUserEntity.getEmail());
                }
                user.setEmail(updateUserEntity.getEmail());
            }
            if (updateUserEntity.getStatus() != null) {
                user.setStatus(UserStatus.valueOf(updateUserEntity.getStatus()));
            }

            if (updateUserEntity.isNewsletter() != null) {
                user.setNewsletterSubscribed(updateUserEntity.isNewsletter());
                if (updateUserEntity.isNewsletter() && newsletterEmail != null) {
                    newsletterService.subscribe(newsletterEmail);
                }
            }

            User updatedUser = userRepository.update(user);
            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(USER, user.getId()),
                User.AuditEvent.USER_UPDATED,
                user.getUpdatedAt(),
                previousUser,
                user
            );

            List<UserMetadataEntity> updatedMetadata = new ArrayList<>();
            if (updateUserEntity.getCustomFields() != null && !updateUserEntity.getCustomFields().isEmpty()) {
                List<UserMetadataEntity> metadata = userMetadataService.findAllByUserId(user.getId());
                for (Map.Entry<String, Object> entry : updateUserEntity.getCustomFields().entrySet()) {
                    Optional<UserMetadataEntity> existingMeta = metadata
                        .stream()
                        .filter(meta -> meta.getKey().equals(entry.getKey()))
                        .findFirst();
                    if (existingMeta.isPresent()) {
                        UserMetadataEntity meta = existingMeta.get();
                        UpdateUserMetadataEntity metadataEntity = new UpdateUserMetadataEntity();
                        metadataEntity.setName(meta.getName());
                        metadataEntity.setKey(meta.getKey());
                        metadataEntity.setValue(String.valueOf(entry.getValue()));
                        metadataEntity.setUserId(meta.getUserId());
                        metadataEntity.setFormat(meta.getFormat());
                        updatedMetadata.add(userMetadataService.update(executionContext, metadataEntity));
                    } else {
                        // some additional fields may have been added after the user registration
                        NewUserMetadataEntity metadataEntity = new NewUserMetadataEntity();
                        metadataEntity.setName(entry.getKey());
                        metadataEntity.setValue(String.valueOf(entry.getValue()));
                        metadataEntity.setUserId(user.getId());
                        metadataEntity.setFormat(MetadataFormat.STRING);
                        updatedMetadata.add(userMetadataService.create(executionContext, metadataEntity));
                    }
                }
            }

            return convert(updatedUser, true, updatedMetadata, true);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update {}", updateUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying update " + updateUserEntity, ex);
        }
    }

    @Override
    public Page<UserEntity> search(ExecutionContext executionContext, String query, Pageable pageable) {
        LOGGER.debug("search users");

        Query<UserEntity> userQuery;
        if (query == null || query.isEmpty()) {
            userQuery =
                QueryBuilder
                    .create(UserEntity.class)
                    .setQuery("*")
                    .setPage(pageable)
                    .setSort(new SortableImpl(UserDocumentTransformer.FIELD_LASTNAME_FIRSTNAME, true))
                    .build();
        } else {
            // UserDocumentTransformation remove domain from email address for security reasons
            // remove it during search phase to provide results
            String sanitizedQuery = query.indexOf('@') > 0 ? query.substring(0, query.indexOf('@')) : query;
            userQuery = QueryBuilder.create(UserEntity.class).setQuery(sanitizedQuery).setPage(pageable).build();
        }
        SearchResult results = searchEngineService.search(executionContext, userQuery);

        if (results.hasResults()) {
            List<String> orderedIds = new ArrayList<>(results.getDocuments());

            List<UserEntity> users = new ArrayList<>((findByIds(executionContext, orderedIds)));

            // Sort users based on their position in orderedIds
            users.sort(Comparator.comparingInt(user -> orderedIds.indexOf(user.getId())));

            populateUserFlags(executionContext.getOrganizationId(), users);

            return new Page<>(users, pageable.getPageNumber(), pageable.getPageSize(), results.getHits());
        }
        return new Page<>(emptyList(), 1, 0, 0);
    }

    private void populateUserFlags(String organizationId, final List<UserEntity> users) {
        RoleEntity apiPORole = roleService.findPrimaryOwnerRoleByOrganization(organizationId, RoleScope.API);
        RoleEntity applicationPORole = roleService.findPrimaryOwnerRoleByOrganization(organizationId, RoleScope.APPLICATION);

        users.forEach(user -> {
            final boolean apiPO = !membershipService
                .getMembershipsByMemberAndReferenceAndRole(
                    MembershipMemberType.USER,
                    user.getId(),
                    MembershipReferenceType.API,
                    apiPORole.getId()
                )
                .isEmpty();
            final boolean appPO = !membershipService
                .getMembershipsByMemberAndReferenceAndRole(
                    MembershipMemberType.USER,
                    user.getId(),
                    MembershipReferenceType.APPLICATION,
                    applicationPORole.getId()
                )
                .isEmpty();

            user.setPrimaryOwner(apiPO || appPO);
            user.setNbActiveTokens(tokenService.findByUser(user.getId()).size());
        });
    }

    @Override
    public Page<UserEntity> search(ExecutionContext executionContext, UserCriteria criteria, Pageable pageable) {
        try {
            LOGGER.debug("search users");
            UserCriteria.Builder builder = new UserCriteria.Builder()
                .organizationId(executionContext.getOrganizationId())
                .statuses(criteria.getStatuses());
            if (criteria.hasNoStatus()) {
                builder.noStatus();
            }
            UserCriteria newCriteria = builder.build();

            Page<User> users = userRepository.search(newCriteria, convert(pageable));

            List<UserEntity> entities = users
                .getContent()
                .stream()
                .map(u -> convert(u, false, userMetadataService.findAllByUserId(u.getId())))
                .collect(toList());

            populateUserFlags(executionContext.getOrganizationId(), entities);

            return new Page<>(entities, users.getPageNumber() + 1, (int) users.getPageElements(), users.getTotalElements());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to search users", ex);
            throw new TechnicalManagementException("An error occurs while trying to search users", ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String id) {
        try {
            User user = userRepository
                .findById(id)
                .filter(u -> u.getOrganizationId().equalsIgnoreCase(executionContext.getOrganizationId()))
                .orElseThrow(() -> new UserNotFoundException(id));

            // If the users is PO of apps or apis, throw an exception
            RoleEntity apiPoRole = roleService.findPrimaryOwnerRoleByOrganization(executionContext.getOrganizationId(), RoleScope.API);
            Set<MembershipEntity> apiPoMemberships = membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                id,
                MembershipReferenceType.API,
                apiPoRole.getId()
            );
            long apiCount = apiPoMemberships.size();

            RoleEntity appPoRole = roleService.findPrimaryOwnerRoleByOrganization(
                executionContext.getOrganizationId(),
                RoleScope.APPLICATION
            );
            Set<MembershipEntity> appPoMemberships = membershipService.getMembershipsByMemberAndReferenceAndRole(
                MembershipMemberType.USER,
                id,
                MembershipReferenceType.APPLICATION,
                appPoRole.getId()
            );
            long applicationCount = appPoMemberships.size();

            if (apiCount > 0 || applicationCount > 0) {
                throw new StillPrimaryOwnerException(apiCount, applicationCount);
            }

            membershipService.removeMemberMemberships(executionContext, MembershipMemberType.USER, id);

            //remove notifications
            portalNotificationService.deleteAll(user.getId());
            portalNotificationConfigService.deleteByUser(user.getId());
            genericNotificationConfigService.deleteByUser(user);

            //remove tokens
            tokenService.revokeByUser(executionContext, user.getId());

            // change user datas
            user.setSourceId("deleted-" + user.getSourceId());
            user.setStatus(UserStatus.ARCHIVED);
            user.setUpdatedAt(new Date());

            if (anonymizeOnDelete) {
                User anonym = new User();
                anonym.setId(user.getId());
                anonym.setOrganizationId(user.getOrganizationId());
                anonym.setCreatedAt(user.getCreatedAt());
                anonym.setUpdatedAt(user.getUpdatedAt());
                anonym.setStatus(user.getStatus());
                anonym.setSource(user.getSource());
                anonym.setLastConnectionAt(user.getLastConnectionAt());
                anonym.setSourceId("deleted-" + user.getId());
                anonym.setFirstname("Unknown");
                anonym.setLastname("");
                anonym.setLoginCount(user.getLoginCount());
                user = anonym;
            }

            userRepository.update(user);

            final UserEntity userEntity = convert(user, false);
            searchEngineService.delete(executionContext, userEntity);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete user", ex);
            throw new TechnicalManagementException("An error occurs while trying to delete user", ex);
        }
    }

    @Override
    public void resetPassword(ExecutionContext executionContext, final String id) {
        this.resetPassword(executionContext, id, null);
    }

    @Override
    public UserEntity resetPasswordFromSourceId(ExecutionContext executionContext, String sourceId, String resetPageUrl) {
        if (sourceId.startsWith("deleted")) {
            throw new UserNotActiveException(sourceId);
        }

        UrlSanitizerUtils.checkAllowed(resetPageUrl, portalWhitelist, true);

        UserEntity foundUser = this.findBySource(executionContext.getOrganizationId(), IDP_SOURCE_GRAVITEE, sourceId, false);
        if ("ACTIVE".equals(foundUser.getStatus())) {
            this.resetPassword(executionContext, foundUser.getId(), resetPageUrl);
            return foundUser;
        } else {
            throw new UserNotActiveException(foundUser.getSourceId());
        }
    }

    private boolean isInternalUser(User user) {
        return IDP_SOURCE_GRAVITEE.equals(user.getSource());
    }

    private void resetPassword(ExecutionContext executionContext, final String id, final String resetPageUrl) {
        try {
            LOGGER.debug("Resetting password of user id {}", id);

            Optional<User> optionalUser = userRepository
                .findById(id)
                .filter(user -> user.getOrganizationId().equalsIgnoreCase(executionContext.getOrganizationId()));

            if (!optionalUser.isPresent()) {
                throw new UserNotFoundException(id);
            }
            final User user = optionalUser.get();
            if (!isInternalUser(user)) {
                throw new UserNotInternallyManagedException(id);
            }

            if (isServiceAccount(user)) {
                throw new ServiceAccountNotManageableException(id);
            }

            // do not update password to null anymore to avoid DoS attack on a user account.
            // use the audit events to throttle the number of resetPassword for a given userid
            // see: https://github.com/gravitee-io/issues/issues/4410

            // do not perform this check if the request comes from an authenticated user (ie. admin or someone with right permission)
            if (!isAuthenticated() || !canResetPassword(executionContext)) {
                AuditQuery query = new AuditQuery();
                query.setEvents(Arrays.asList(User.AuditEvent.PASSWORD_RESET.name()));
                query.setFrom(Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli());
                query.setPage(1);
                query.setSize(100);
                MetadataPage<AuditEntity> events = auditService.search(executionContext, query);
                if (events != null) {
                    if (events.getContent().size() == 100) {
                        LOGGER.warn("More than 100 reset password received in less than 1 hour", user.getId());
                    }

                    Optional<AuditEntity> optReset = events
                        .getContent()
                        .stream()
                        .filter(evt -> user.getId().equals(evt.getProperties().get(USER.name())))
                        .findFirst();
                    if (optReset.isPresent()) {
                        LOGGER.warn("Multiple reset password received for user '{}' in less than 1 hour", user.getId());
                        throw new PasswordAlreadyResetException();
                    }
                }
            }

            final Map<String, Object> params = getTokenRegistrationParams(
                executionContext,
                convert(user, false),
                RESET_PASSWORD_PATH,
                RESET_PASSWORD,
                resetPageUrl
            );

            notifierService.trigger(executionContext, PortalHook.PASSWORD_RESET, params);

            auditService.createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                Collections.singletonMap(USER, user.getId()),
                User.AuditEvent.PASSWORD_RESET,
                new Date(),
                null,
                null
            );
            emailService.sendAsyncEmailNotification(
                executionContext,
                new EmailNotificationBuilder()
                    .to(user.getEmail())
                    .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_PASSWORD_RESET)
                    .params(params)
                    .build()
            );
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to reset password for user " + id;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    protected boolean canResetPassword(ExecutionContext executionContext) {
        if (isEnvironmentAdmin()) {
            return true;
        }
        return permissionService.hasPermission(
            executionContext,
            RolePermission.ORGANIZATION_USERS,
            executionContext.getOrganizationId(),
            new RolePermissionAction[] { UPDATE }
        );
    }

    private UserEntity convertWithFlags(final ExecutionContext executionContext, User user) {
        UserEntity userEntity = convert(user, true, userMetadataService.findAllByUserId(user.getId()), true);
        populateUserFlags(user.getOrganizationId(), List.of(userEntity));
        return userEntity;
    }

    private UserEntity convert(User user, boolean loadRoles) {
        return convert(user, loadRoles, emptyList(), true);
    }

    private UserEntity convert(User user, boolean loadRoles, List<UserMetadataEntity> customUserFields) {
        return convert(user, loadRoles, customUserFields, true);
    }

    private UserEntity convert(User user, boolean loadRoles, List<UserMetadataEntity> customUserFields, boolean nullifyPassword) {
        if (user == null) {
            return null;
        }

        UserEntity userEntity = userConverter.toUserEntity(user, customUserFields);

        if (loadRoles) {
            Set<UserRoleEntity> roles = new HashSet<>();
            Set<RoleEntity> roleEntities = membershipService.getRoles(
                MembershipReferenceType.ORGANIZATION,
                user.getOrganizationId(),
                MembershipMemberType.USER,
                user.getId()
            );
            if (!roleEntities.isEmpty()) {
                roleEntities.forEach(roleEntity -> roles.add(convert(roleEntity)));
            }

            List<EnvironmentEntity> environmentEntities = this.environmentService.findByOrganization(user.getOrganizationId());
            environmentEntities
                .stream()
                .flatMap(env ->
                    membershipService
                        .getRoles(MembershipReferenceType.ENVIRONMENT, env.getId(), MembershipMemberType.USER, user.getId())
                        .stream()
                )
                .filter(Objects::nonNull)
                .forEach(roleEntity -> roles.add(convert(roleEntity)));

            userEntity.setRoles(roles);

            Map<String, Set<UserRoleEntity>> envRolesMap = new HashMap<>();
            environmentEntities.forEach(env -> {
                Set<UserRoleEntity> envRoles = new HashSet<>();
                Set<RoleEntity> envRoleEntities = membershipService.getRoles(
                    MembershipReferenceType.ENVIRONMENT,
                    env.getId(),
                    MembershipMemberType.USER,
                    user.getId()
                );
                if (!envRoleEntities.isEmpty()) {
                    envRoleEntities.forEach(roleEntity -> envRoles.add(convert(roleEntity)));
                }
                envRolesMap.put(env.getId(), envRoles);
            });
            userEntity.setEnvRoles(envRolesMap);
        }

        if (nullifyPassword) {
            // Delete password for security reason
            userEntity.setPassword(null);
        }

        return userEntity;
    }

    private UserRoleEntity convert(RoleEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }

        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setId(roleEntity.getId());
        userRoleEntity.setScope(roleEntity.getScope());
        userRoleEntity.setName(roleEntity.getName());
        userRoleEntity.setPermissions(roleEntity.getPermissions());
        return userRoleEntity;
    }

    @Override
    public UserEntity createOrUpdateUserFromSocialIdentityProvider(
        ExecutionContext executionContext,
        SocialIdentityProviderEntity socialProvider,
        String userInfo
    ) {
        HashMap<String, String> attrs = getUserProfileAttrs(socialProvider.getUserProfileMapping(), userInfo);

        String email = attrs.get(SocialIdentityProviderEntity.UserProfile.EMAIL);
        if (email == null && socialProvider.isEmailRequired()) {
            throw new EmailRequiredException(attrs.get(SocialIdentityProviderEntity.UserProfile.ID));
        }

        // Compute group and role mappings
        // This is done BEFORE updating or creating the user account to ensure this one is properly created with correct
        // information (ie. mappings)
        Set<GroupEntity> userGroups = computeUserGroupsFromProfile(email, socialProvider.getGroupMappings(), userInfo, executionContext);

        List<RoleMappingEntity> rolesMapping = socialProvider.getRoleMappings() == null || socialProvider.getRoleMappings().isEmpty()
            ? emptyList()
            : socialProvider.getRoleMappings();

        Set<RoleEntity> userOrganizationRoles = this.computeOrganizationRoles(executionContext, rolesMapping, email, userInfo);
        Map<String, Set<RoleEntity>> userEnvironmentRoles = this.computeEnvironmentRoles(executionContext, rolesMapping, email, userInfo);

        UserEntity user = null;
        boolean created = false;
        try {
            user = refreshExistingUser(executionContext, socialProvider, attrs, email);
        } catch (UserNotFoundException unfe) {
            created = true;
            user = createNewExternalUser(executionContext, socialProvider, userInfo, attrs, email);
        }

        // Memberships must be refreshed only when it is a user creation context or mappings should be synced during
        // later authentication
        List<MembershipService.Membership> groupMemberships = refreshUserGroups(
            executionContext,
            user.getId(),
            socialProvider.getId(),
            userGroups
        );
        List<MembershipService.Membership> roleOrganizationMemberships = refreshUserOrganizationRoles(
            executionContext,
            user.getId(),
            socialProvider.getId(),
            userOrganizationRoles
        );
        List<MembershipService.Membership> roleEnvironmentMemberships = refreshUserEnvironmentRoles(
            user.getId(),
            socialProvider.getId(),
            userEnvironmentRoles
        );

        if (created || socialProvider.isSyncMappings()) {
            final boolean hasGroupMapping = socialProvider.getGroupMappings() != null && !socialProvider.getGroupMappings().isEmpty();
            refreshUserMemberships(
                executionContext,
                user.getId(),
                socialProvider.getId(),
                groupMemberships,
                hasGroupMapping,
                MembershipReferenceType.GROUP
            );

            final boolean hasRoleMapping = socialProvider.getRoleMappings() != null && !socialProvider.getRoleMappings().isEmpty();
            refreshUserMemberships(
                executionContext,
                user.getId(),
                socialProvider.getId(),
                roleOrganizationMemberships,
                hasRoleMapping,
                MembershipReferenceType.ORGANIZATION
            );
            refreshUserMemberships(
                executionContext,
                user.getId(),
                socialProvider.getId(),
                roleEnvironmentMemberships,
                hasRoleMapping,
                MembershipReferenceType.ENVIRONMENT
            );
        }

        return user;
    }

    private HashMap<String, String> getUserProfileAttrs(Map<String, String> userProfileMapping, String userInfo) {
        TemplateEngine templateEngine = TemplateEngine.templateEngine();
        templateEngine.getTemplateContext().setVariable(TEMPLATE_ENGINE_PROFILE_ATTRIBUTE, userInfo);

        ReadContext userInfoPath = JsonPath.parse(userInfo);
        HashMap<String, String> map = new HashMap<>(userProfileMapping.size());

        for (Map.Entry<String, String> entry : userProfileMapping.entrySet()) {
            String field = entry.getKey();
            String mapping = entry.getValue();

            if (!isEmpty(mapping)) {
                try {
                    if (mapping.contains("{#")) {
                        map.put(field, templateEngine.getValue(mapping, String.class));
                    } else {
                        map.put(field, userInfoPath.read(mapping, String.class));
                    }
                } catch (Exception e) {
                    LOGGER.warn("Using mapping: \"{}\" (on field:\"{}\"), no fields are located in {}", mapping, field, userInfo);
                }
            }
        }

        return map;
    }

    private UserEntity createNewExternalUser(
        ExecutionContext executionContext,
        final SocialIdentityProviderEntity socialProvider,
        final String userInfo,
        HashMap<String, String> attrs,
        String email
    ) {
        final NewExternalUserEntity newUser = new NewExternalUserEntity();
        newUser.setEmail(email);
        newUser.setSource(socialProvider.getId());

        if (attrs.get(SocialIdentityProviderEntity.UserProfile.ID) != null) {
            newUser.setSourceId(attrs.get(SocialIdentityProviderEntity.UserProfile.ID));
        }
        if (attrs.get(SocialIdentityProviderEntity.UserProfile.LASTNAME) != null) {
            newUser.setLastname(attrs.get(SocialIdentityProviderEntity.UserProfile.LASTNAME));
        }
        if (attrs.get(SocialIdentityProviderEntity.UserProfile.FIRSTNAME) != null) {
            newUser.setFirstname(attrs.get(SocialIdentityProviderEntity.UserProfile.FIRSTNAME));
        }
        if (attrs.get(SocialIdentityProviderEntity.UserProfile.PICTURE) != null) {
            newUser.setPicture(attrs.get(SocialIdentityProviderEntity.UserProfile.PICTURE));
        }

        return this.create(executionContext, newUser, false);
    }

    private UserEntity refreshExistingUser(
        ExecutionContext executionContext,
        final SocialIdentityProviderEntity socialProvider,
        HashMap<String, String> attrs,
        String email
    ) {
        String userId;
        UserEntity registeredUser =
            this.findBySource(
                    executionContext.getOrganizationId(),
                    socialProvider.getId(),
                    attrs.get(SocialIdentityProviderEntity.UserProfile.ID),
                    false
                );
        userId = registeredUser.getId();

        // User refresh
        UpdateUserEntity user = new UpdateUserEntity();

        if (attrs.get(SocialIdentityProviderEntity.UserProfile.LASTNAME) != null) {
            user.setLastname(attrs.get(SocialIdentityProviderEntity.UserProfile.LASTNAME));
        }
        if (attrs.get(SocialIdentityProviderEntity.UserProfile.FIRSTNAME) != null) {
            user.setFirstname(attrs.get(SocialIdentityProviderEntity.UserProfile.FIRSTNAME));
        }
        if (attrs.get(SocialIdentityProviderEntity.UserProfile.PICTURE) != null) {
            user.setPicture(attrs.get(SocialIdentityProviderEntity.UserProfile.PICTURE));
        }
        user.setEmail(email);

        return this.update(executionContext, userId, user);
    }

    protected Set<RoleEntity> computeOrganizationRoles(
        ExecutionContext executionContext,
        @NotNull List<RoleMappingEntity> rolesMapping,
        String username,
        String userInfo
    ) {
        // First get all org roles based on the mappings
        Set<RoleEntity> orgRoles = rolesMapping
            .stream()
            .filter(mapping -> mapping.getOrganizations() != null && !mapping.getOrganizations().isEmpty())
            // Filter mappings that match the user profile
            .filter(mapping -> {
                TemplateEngine templateEngine = TemplateEngine.templateEngine();
                templateEngine.getTemplateContext().setVariable(TEMPLATE_ENGINE_PROFILE_ATTRIBUTE, userInfo);

                boolean match = templateEngine.getValue(mapping.getCondition(), boolean.class);

                trace(username, match, mapping.getCondition());

                return match;
            })
            // Get all organization included in this mapping
            .flatMap(mapping -> mapping.getOrganizations().stream())
            // Get all roles for each organization
            .flatMap(org -> roleService.findByScopeAndName(RoleScope.ORGANIZATION, org, executionContext.getOrganizationId()).stream())
            .collect(toSet());

        // If no org roles are found, get the default ones
        if (orgRoles.isEmpty()) {
            orgRoles.addAll(roleService.findDefaultRoleByScopes(executionContext.getOrganizationId(), RoleScope.ORGANIZATION));
        }

        return orgRoles;
    }

    protected Map<String, Set<RoleEntity>> computeEnvironmentRoles(
        ExecutionContext executionContext,
        @NotNull List<RoleMappingEntity> rolesMapping,
        String username,
        String userInfo
    ) {
        Map<String, Set<RoleEntity>> environmentRoles = new HashMap<>();

        List<RoleMappingEntity> envRolesMapping = rolesMapping
            .stream()
            .filter(mapping -> mapping.getEnvironments() != null && !mapping.getEnvironments().isEmpty())
            .collect(toList());

        for (RoleMappingEntity mapping : envRolesMapping) {
            TemplateEngine templateEngine = TemplateEngine.templateEngine();
            templateEngine.getTemplateContext().setVariable(TEMPLATE_ENGINE_PROFILE_ATTRIBUTE, userInfo);

            boolean match = templateEngine.getValue(mapping.getCondition(), boolean.class);

            trace(username, match, mapping.getCondition());

            // Get roles
            if (match) {
                mapping
                    .getEnvironments()
                    .forEach((environmentName, roles) -> {
                        Set<RoleEntity> envRoles = environmentRoles.computeIfAbsent(environmentName, k -> new HashSet<>());
                        for (String environmentRoleName : roles) {
                            roleService
                                .findByScopeAndName(RoleScope.ENVIRONMENT, environmentRoleName, executionContext.getOrganizationId())
                                .ifPresent(envRoles::add);
                        }
                    });
            }
        }

        // If no env roles are found, get the default ones
        if (environmentRoles.isEmpty()) {
            String environmentId = executionContext.hasEnvironmentId()
                ? executionContext.getEnvironmentId()
                : GraviteeContext.getDefaultEnvironment();
            List<RoleEntity> defaultRoles = roleService.findDefaultRoleByScopes(
                executionContext.getOrganizationId(),
                RoleScope.ENVIRONMENT
            );
            environmentRoles.computeIfAbsent(environmentId, k -> new HashSet<>()).addAll(defaultRoles);
        }

        return environmentRoles;
    }

    private void addRolesToUser(
        ExecutionContext executionContext,
        String userId,
        Collection<RoleEntity> rolesToAdd,
        MembershipReferenceType referenceType,
        String referenceId
    ) {
        // add roles to user
        for (RoleEntity roleEntity : rolesToAdd) {
            MembershipService.MembershipReference ref = null;
            if (referenceType != null && referenceId != null) {
                ref = new MembershipService.MembershipReference(referenceType, referenceId);
            } else if (roleEntity.getScope() == RoleScope.ORGANIZATION) {
                ref = new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, executionContext.getOrganizationId());
            } else {
                ref = new MembershipService.MembershipReference(MembershipReferenceType.ENVIRONMENT, executionContext.getEnvironmentId());
            }
            membershipService.addRoleToMemberOnReference(
                executionContext,
                ref,
                new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.valueOf(roleEntity.getScope().name()), roleEntity.getName())
            );
        }
    }

    private void trace(String userId, boolean match, String condition) {
        if (LOGGER.isDebugEnabled()) {
            if (match) {
                LOGGER.debug("the expression {} match {} on user's info ", condition, userId);
            } else {
                LOGGER.debug("the expression {} didn't match {} on user's info ", condition, userId);
            }
        }
    }

    /**
     * Calculate the list of groups to associate to a user according to its OIDC profile (ie. UserInfo)
     *
     * @param userId
     * @param mappings
     * @param userInfo
     * @param executionContext
     * @return
     */
    private Set<GroupEntity> computeUserGroupsFromProfile(
        String userId,
        List<GroupMappingEntity> mappings,
        String userInfo,
        ExecutionContext executionContext
    ) {
        if (mappings == null || mappings.isEmpty()) {
            return Collections.emptySet();
        }

        Set<GroupEntity> groups = new HashSet<>();

        for (GroupMappingEntity mapping : mappings) {
            TemplateEngine templateEngine = TemplateEngine.templateEngine();
            templateEngine.getTemplateContext().setVariable(TEMPLATE_ENGINE_PROFILE_ATTRIBUTE, userInfo);

            boolean match = templateEngine.getValue(mapping.getCondition(), boolean.class);

            trace(userId, match, mapping.getCondition());

            // Get groups
            if (match) {
                for (String groupName : mapping.getGroups()) {
                    try {
                        groups.add(groupService.findById(executionContext, groupName));
                    } catch (GroupNotFoundException gnfe) {
                        LOGGER.warn("Unable to map user groups, missing group in repository: {}", groupName);
                    }
                }
            }
        }

        return groups;
    }

    private List<MembershipService.Membership> refreshUserGroups(
        ExecutionContext executionContext,
        String userId,
        String identityProviderId,
        Collection<GroupEntity> userGroups
    ) {
        List<MembershipService.Membership> memberships = new ArrayList<>();

        // Get the default group roles from system
        List<RoleEntity> roleEntities = roleService.findDefaultRoleByScopes(
            executionContext.getOrganizationId(),
            RoleScope.API,
            RoleScope.APPLICATION
        );

        // Add groups to user
        for (GroupEntity groupEntity : userGroups) {
            for (RoleEntity roleEntity : roleEntities) {
                String defaultRole = roleEntity.getName();

                // If defined, get the override default role at the group level
                if (groupEntity.getRoles() != null) {
                    String groupDefaultRole = groupEntity.getRoles().get(RoleScope.valueOf(roleEntity.getScope().name()));
                    if (groupDefaultRole != null) {
                        defaultRole = groupDefaultRole;
                    }
                }

                MembershipService.Membership membership = new MembershipService.Membership(
                    new MembershipService.MembershipReference(MembershipReferenceType.GROUP, groupEntity.getId()),
                    new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(roleEntity.getScope(), defaultRole)
                );

                membership.setSource(identityProviderId);

                memberships.add(membership);
            }
        }

        return memberships;
    }

    private List<MembershipService.Membership> refreshUserOrganizationRoles(
        ExecutionContext executionContext,
        String userId,
        String identityProviderId,
        Collection<RoleEntity> userOrganizationRoles
    ) {
        return userOrganizationRoles
            .stream()
            .map(roleEntity -> {
                MembershipService.Membership membership = new MembershipService.Membership(
                    new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, executionContext.getOrganizationId()),
                    new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                    new MembershipService.MembershipRole(RoleScope.valueOf(roleEntity.getScope().name()), roleEntity.getName())
                );
                membership.setSource(identityProviderId);
                return membership;
            })
            .collect(Collectors.toList());
    }

    private List<MembershipService.Membership> refreshUserEnvironmentRoles(
        String userId,
        String identityProviderId,
        Map<String, Set<RoleEntity>> userRolesByEnvironment
    ) {
        List<MembershipService.Membership> result = new ArrayList<>();
        userRolesByEnvironment.forEach((environmentId, userRoles) ->
            result.addAll(
                userRoles
                    .stream()
                    .map(roleEntity -> {
                        MembershipService.Membership membership = new MembershipService.Membership(
                            new MembershipService.MembershipReference(MembershipReferenceType.ENVIRONMENT, environmentId),
                            new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                            new MembershipService.MembershipRole(RoleScope.valueOf(roleEntity.getScope().name()), roleEntity.getName())
                        );

                        membership.setSource(identityProviderId);
                        return membership;
                    })
                    .collect(Collectors.toList())
            )
        );
        return result;
    }

    /**
     * Refresh user memberships.
     *
     * @param executionContext
     * @param userId             User identifier.
     * @param identityProviderId The identity provider used to authenticate the user.
     * @param memberships        List of memberships to associate to the user
     * @param hasMapping         If the social provider has a mapping for the given type
     * @param types              The types of user memberships to manage
     */
    private void refreshUserMemberships(
        ExecutionContext executionContext,
        String userId,
        String identityProviderId,
        List<MembershipService.Membership> memberships,
        boolean hasMapping,
        MembershipReferenceType... types
    ) {
        // Get existing memberships for a given type
        List<Membership> userMemberships = new ArrayList<>();

        for (MembershipReferenceType type : types) {
            try {
                userMemberships.addAll(
                    membershipRepository.findByMemberIdAndMemberTypeAndReferenceType(
                        userId,
                        io.gravitee.repository.management.model.MembershipMemberType.USER,
                        io.gravitee.repository.management.model.MembershipReferenceType.valueOf(type.name())
                    )
                );
            } catch (TechnicalException e) {
                final String msg = "An error occurs while finding memberships for user " + userId;
                LOGGER.error(msg, e);
                throw new TechnicalManagementException(msg, e);
            }
        }

        List<Membership> overrideUserMemberships = new ArrayList<>();
        // Delete existing memberships
        userMemberships.forEach(membership -> {
            // Consider only membership "created by" the identity provider
            if (identityProviderId.equals(membership.getSource())) {
                // if there is no mapping configured on the social idp, we do not remove / reset it
                if (hasMapping) {
                    membershipService.deleteReferenceMemberBySource(
                        executionContext,
                        MembershipReferenceType.valueOf(membership.getReferenceType().name()),
                        membership.getReferenceId(),
                        MembershipMemberType.USER,
                        userId,
                        membership.getSource()
                    );
                }
            } else {
                overrideUserMemberships.add(membership);
            }
        });

        Map<MembershipService.MembershipReference, Map<MembershipService.MembershipMember, Map<String, Collection<MembershipService.MembershipRole>>>> groupedRoles =
            new HashMap<>();
        memberships
            .stream()
            .filter(membership -> !containsMembership(overrideUserMemberships, membership))
            .forEach(membership ->
                groupedRoles
                    .computeIfAbsent(membership.getReference(), ignore -> new HashMap<>())
                    .computeIfAbsent(membership.getMember(), ignore -> new HashMap<>())
                    .computeIfAbsent(membership.getSource(), ignore -> new ArrayList<>())
                    .add(membership.getRole())
            );
        // Create updated memberships
        groupedRoles.forEach((reference, memberMapping) ->
            memberMapping.forEach((member, sourceMapping) ->
                sourceMapping.forEach((source, roles) ->
                    membershipService.updateRolesToMemberOnReferenceBySource(executionContext, reference, member, roles, source)
                )
            )
        );
    }

    private boolean containsMembership(List<Membership> overrideUserMemberships, MembershipService.Membership membership) {
        return overrideUserMemberships
            .stream()
            .anyMatch(membership1 -> {
                if (membership1.getReferenceId().equals(membership.getReference().getId())) {
                    RoleEntity byId = roleService.findById(membership1.getRoleId());
                    return membership.getRole().getScope().equals(byId.getScope());
                }
                return false;
            });
    }

    @Override
    public void updateUserRoles(
        ExecutionContext executionContext,
        String userId,
        MembershipReferenceType referenceType,
        String referenceId,
        List<String> roleIds
    ) {
        // check if user exist
        this.findById(executionContext, userId);
        MemberEntity userMember = membershipService.getUserMember(executionContext, referenceType, referenceId, userId);
        if (userMember != null) {
            userMember
                .getRoles()
                .forEach(role -> {
                    if (!roleIds.contains(role.getId())) {
                        membershipService.removeRole(referenceType, referenceId, MembershipMemberType.USER, userId, role.getId());
                    } else {
                        roleIds.remove(role.getId());
                    }
                });
        }
        if (!roleIds.isEmpty()) {
            this.addRolesToUser(
                    executionContext,
                    userId,
                    roleIds
                        .stream()
                        .map(roleService::findById)
                        .filter(role -> role.getScope().equals(RoleScope.valueOf(referenceType.name())))
                        .collect(toSet()),
                    referenceType,
                    referenceId
                );
        }
    }
}
