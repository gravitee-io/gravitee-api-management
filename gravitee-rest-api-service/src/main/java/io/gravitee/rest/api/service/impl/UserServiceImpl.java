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

import static io.gravitee.repository.management.model.Audit.AuditProperties.USER;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.*;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static io.gravitee.rest.api.service.notification.NotificationParamsBuilder.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.util.Maps;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.spel.function.JsonPathFunction;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.User;
import io.gravitee.repository.management.model.UserStatus;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.configuration.identity.GroupMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.RoleMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper.ACTION;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import io.gravitee.rest.api.service.common.RandomString;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.sanitizer.UrlSanitizerUtils;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    /** A default source used for user registration.*/
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

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotifierService notifierService;

    @Autowired
    private ApiService apiService;

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
    public UserEntity connect(String userId) {
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
                notifierService.trigger(PortalHook.USER_FIRST_LOGIN, new NotificationParamsBuilder().user(convert(user, false)).build());
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
                        applicationService.create(defaultApp, userId, true);
                    } catch (IllegalStateException ex) {
                        //do not fail to create a user even if we are not able to create its default app
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
                Collections.singletonMap(USER, userId),
                User.AuditEvent.USER_CONNECTED,
                user.getUpdatedAt(),
                previousUser,
                user
            );

            final UserEntity userEntity = convert(updatedUser, true);
            searchEngineService.index(userEntity, false);
            return userEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to connect {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to connect " + userId, ex);
        }
    }

    @Override
    public UserEntity findById(String id, boolean defaultValue) {
        return GraviteeContext
            .getCurrentUsers()
            .computeIfAbsent(
                id,
                k -> {
                    try {
                        LOGGER.debug("Find user by ID: {}", k);

                        Optional<User> optionalUser = userRepository.findById(k);

                        if (optionalUser.isPresent()) {
                            return convert(optionalUser.get(), false, userMetadataService.findAllByUserId(k));
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
    public Optional<UserEntity> findByEmail(String email) {
        try {
            LOGGER.debug("Find user by Email: {}", email);
            Optional<User> optionalUser = userRepository.findByEmail(email, GraviteeContext.getCurrentOrganization());
            return optionalUser.map(user -> convert(optionalUser.get(), false));
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find user using its email", ex);
            throw new TechnicalManagementException("An error occurs while trying to find user using its email", ex);
        }
    }

    @Override
    public UserEntity findByIdWithRoles(String id) {
        try {
            LOGGER.debug("Find user by ID: {}", id);

            Optional<User> optionalUser = userRepository.findById(id);

            if (optionalUser.isPresent()) {
                UserEntity userEntity = convert(optionalUser.get(), true, userMetadataService.findAllByUserId(id));

                populateUserFlags(Collections.singletonList(userEntity));

                return userEntity;
            }
            //should never happen
            throw new UserNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find user using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to find user using its ID " + id, ex);
        }
    }

    @Override
    public UserEntity findBySource(String source, String sourceId, boolean loadRoles) {
        try {
            LOGGER.debug("Find user by source[{}] user[{}]", source, sourceId);

            Optional<User> optionalUser = userRepository.findBySource(source, sourceId, GraviteeContext.getCurrentOrganization());

            if (optionalUser.isPresent()) {
                return convert(optionalUser.get(), loadRoles);
            }

            throw new UserNotFoundException(sourceId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find user using source[{}], user[{}]", source, sourceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find user using source " + source + ':' + sourceId, ex);
        }
    }

    @Override
    public Set<UserEntity> findByIds(List<String> ids) {
        return this.findByIds(ids, true);
    }

    @Override
    public Set<UserEntity> findByIds(List<String> ids, boolean withUserMetadata) {
        try {
            LOGGER.debug("Find users by ID: {}", ids);

            Set<User> users = userRepository.findByIds(ids);

            if (!users.isEmpty()) {
                return users
                    .stream()
                    .map(
                        u ->
                            this.convert(
                                    u,
                                    false,
                                    withUserMetadata ? userMetadataService.findAllByUserId(u.getId()) : Collections.emptyList()
                                )
                    )
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

    private void checkUserRegistrationEnabled(GraviteeContext.ReferenceContext currentContext) {
        boolean userCreationEnabled;
        if (currentContext.getReferenceType().equals(GraviteeContext.ReferenceContextType.ORGANIZATION)) {
            userCreationEnabled =
                parameterService.findAsBoolean(
                    Key.CONSOLE_USERCREATION_ENABLED,
                    currentContext.getReferenceId(),
                    ParameterReferenceType.ORGANIZATION
                );
        } else {
            userCreationEnabled =
                parameterService.findAsBoolean(
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
     * @param registerUserEntity a valid token and a password
     * @return the user
     */
    @Override
    public UserEntity finalizeRegistration(final RegisterUserEntity registerUserEntity) {
        try {
            DecodedJWT jwt = getDecodedJWT(registerUserEntity.getToken());

            final String action = jwt.getClaim(Claims.ACTION).asString();
            if (RESET_PASSWORD.name().equals(action)) {
                throw new UserStateConflictException("Reset password forbidden on this resource");
            }

            if (USER_REGISTRATION.name().equals(action)) {
                checkUserRegistrationEnabled(GraviteeContext.getCurrentContext());
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
                user = convert(create(externalUser, true));
                user.setOrganizationId(GraviteeContext.getCurrentOrganization());
            } else {
                final String username = subject.toString();
                LOGGER.debug("Create an internal user {}", username);
                Optional<User> checkUser = userRepository.findById(username);
                user = checkUser.orElseThrow(() -> new UserNotFoundException(username));
                if (StringUtils.isNotBlank(user.getPassword())) {
                    throw new UserAlreadyFinalizedException(GraviteeContext.getCurrentOrganization());
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
                    .forEach(
                        invitation -> {
                            invitationService.addMember(
                                invitation.getReferenceType().name(),
                                invitation.getReferenceId(),
                                userId,
                                invitation.getApiRole(),
                                invitation.getApplicationRole()
                            );
                            invitationService.delete(invitation.getId(), invitation.getReferenceId());
                        }
                    );
            }

            // Set date fields
            user.setUpdatedAt(new Date());

            // Encrypt password if internal user
            encryptPassword(user, registerUserEntity.getPassword());

            user = userRepository.update(user);
            auditService.createOrganizationAuditLog(
                Collections.singletonMap(USER, user.getId()),
                User.AuditEvent.USER_CREATED,
                user.getUpdatedAt(),
                null,
                user
            );

            // Do not send back the password
            user.setPassword(null);

            final UserEntity userEntity = convert(user, true);
            searchEngineService.index(userEntity, false);
            return userEntity;
        } catch (AbstractManagementException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to create an internal user with the token {}", registerUserEntity.getToken(), ex);
            throw new TechnicalManagementException(ex.getMessage(), ex);
        }
    }

    @Override
    public UserEntity finalizeResetPassword(ResetPasswordUserEntity registerUserEntity) {
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
                Optional<User> checkUser = userRepository.findById(username);
                user = checkUser.orElseThrow(() -> new UserNotFoundException(username));
            }

            // Set date fields
            user.setUpdatedAt(new Date());

            // Encrypt password if internal user
            encryptPassword(user, registerUserEntity.getPassword());

            user = userRepository.update(user);

            auditService.createOrganizationAuditLog(
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
    public PictureEntity getPicture(String id) {
        UserEntity user = findById(id);

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
     * @param newExternalUserEntity
     * @return
     */
    @Override
    public UserEntity create(NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole) {
        return create(newExternalUserEntity, addDefaultRole, true);
    }

    private UserEntity create(NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole, boolean autoRegistrationEnabled) {
        try {
            /*
             TODO: getCurrentEnvironnemenet and call database to fetch the corresponding organization OR add parameters in methods
              Because, this method is called by portal and console. And in portal, we don't have a "current organization" in path.
             */
            String organizationId = GraviteeContext.getCurrentOrganization();

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

            User user = convert(newExternalUserEntity);
            user.setId(RandomString.generate());
            user.setOrganizationId(organizationId);
            user.setStatus(autoRegistrationEnabled ? UserStatus.ACTIVE : UserStatus.PENDING);

            // Set date fields
            user.setCreatedAt(new Date());
            user.setUpdatedAt(user.getCreatedAt());

            User createdUser = userRepository.create(user);
            auditService.createOrganizationAuditLog(
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
                    metadata.add(userMetadataService.create(metadataEntity));
                }
            }

            if (addDefaultRole) {
                addDefaultMembership(createdUser);
            }

            final UserEntity userEntity = convert(createdUser, true, metadata);
            searchEngineService.index(userEntity, false);
            return userEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create an external user {}", newExternalUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create an external user" + newExternalUserEntity, ex);
        }
    }

    private void addDefaultMembership(User user) {
        RoleScope[] scopes = { RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT };
        List<RoleEntity> defaultRoleByScopes = roleService.findDefaultRoleByScopes(scopes);
        if (defaultRoleByScopes == null || defaultRoleByScopes.isEmpty()) {
            throw new DefaultRoleNotFoundException(scopes);
        }
        for (RoleEntity defaultRoleByScope : defaultRoleByScopes) {
            switch (defaultRoleByScope.getScope()) {
                case ORGANIZATION:
                    membershipService.addRoleToMemberOnReference(
                        new MembershipService.MembershipReference(
                            MembershipReferenceType.ORGANIZATION,
                            GraviteeContext.getCurrentOrganization()
                        ),
                        new MembershipService.MembershipMember(user.getId(), null, MembershipMemberType.USER),
                        new MembershipService.MembershipRole(RoleScope.ORGANIZATION, defaultRoleByScope.getName())
                    );
                    break;
                case ENVIRONMENT:
                    membershipService.addRoleToMemberOnReference(
                        new MembershipService.MembershipReference(
                            MembershipReferenceType.ENVIRONMENT,
                            GraviteeContext.getCurrentEnvironmentOrDefault()
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
    public UserEntity register(final NewExternalUserEntity newExternalUserEntity) {
        return register(newExternalUserEntity, null);
    }

    @Override
    public UserEntity register(final NewExternalUserEntity newExternalUserEntity, final String confirmationPageUrl) {
        final GraviteeContext.ReferenceContext currentContext = GraviteeContext.getCurrentContext();

        if (confirmationPageUrl != null) {
            UrlSanitizerUtils.checkAllowed(confirmationPageUrl, portalWhitelist, true);
        }

        checkUserRegistrationEnabled(currentContext);
        boolean autoRegistrationEnabled = isAutoRegistrationEnabled(currentContext);

        return createAndSendEmail(newExternalUserEntity, USER_REGISTRATION, confirmationPageUrl, autoRegistrationEnabled);
    }

    private boolean isAutoRegistrationEnabled(GraviteeContext.ReferenceContext currentContext) {
        if (currentContext.getReferenceType().equals(GraviteeContext.ReferenceContextType.ORGANIZATION)) {
            return parameterService.findAsBoolean(
                Key.CONSOLE_USERCREATION_AUTOMATICVALIDATION_ENABLED,
                currentContext.getReferenceId(),
                ParameterReferenceType.ORGANIZATION
            );
        }
        return parameterService.findAsBoolean(
            Key.PORTAL_USERCREATION_AUTOMATICVALIDATION_ENABLED,
            currentContext.getReferenceId(),
            ParameterReferenceType.ENVIRONMENT
        );
    }

    @Override
    public UserEntity create(final NewExternalUserEntity newExternalUserEntity) {
        return createAndSendEmail(newExternalUserEntity, USER_CREATION, null, true);
    }

    /**
     * Allows to create an user and send an email notification to finalize its creation.
     */
    private UserEntity createAndSendEmail(
        final NewExternalUserEntity newExternalUserEntity,
        final ACTION action,
        final String confirmationPageUrl,
        final boolean autoRegistrationEnabled
    ) {
        if (!EmailValidator.isValid(newExternalUserEntity.getEmail())) {
            throw new EmailFormatInvalidException(newExternalUserEntity.getEmail());
        }

        String organizationId = GraviteeContext.getCurrentOrganization();

        if (isBlank(newExternalUserEntity.getSource())) {
            newExternalUserEntity.setSource(IDP_SOURCE_GRAVITEE);
        } else {
            if (!IDP_SOURCE_GRAVITEE.equals(newExternalUserEntity.getSource())) {
                // check if IDP exists
                identityProviderService.findById(newExternalUserEntity.getSource());
            }
        }

        if (isBlank(newExternalUserEntity.getSourceId())) {
            newExternalUserEntity.setSourceId(newExternalUserEntity.getEmail());
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

        final UserEntity userEntity = create(newExternalUserEntity, true, autoRegistrationEnabled);

        if (IDP_SOURCE_GRAVITEE.equals(newExternalUserEntity.getSource())) {
            final Map<String, Object> params = getTokenRegistrationParams(userEntity, REGISTRATION_PATH, action, confirmationPageUrl);
            emailService.sendAsyncEmailNotification(
                new EmailNotificationBuilder()
                    .to(userEntity.getEmail())
                    .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_REGISTRATION)
                    .params(params)
                    .param("registrationAction", USER_REGISTRATION.equals(action) ? "registration" : "creation")
                    .build(),
                GraviteeContext.getCurrentContext()
            );

            if (autoRegistrationEnabled) {
                notifierService.trigger(
                    ACTION.USER_REGISTRATION.equals(action) ? PortalHook.USER_REGISTERED : PortalHook.USER_CREATED,
                    params
                );
            } else {
                notifierService.trigger(PortalHook.USER_REGISTRATION_REQUEST, params);
            }
        }

        if (newExternalUserEntity.getNewsletter() != null && newExternalUserEntity.getNewsletter()) {
            newsletterService.subscribe(newExternalUserEntity.getEmail());
        }

        return userEntity;
    }

    @Override
    public UserEntity processRegistration(String userId, boolean accepted) {
        UserEntity userToProcess = findById(userId);
        UserEntity processedUser = this.changeUserStatus(userId, accepted ? UserStatus.ACTIVE : UserStatus.REJECTED);
        final Map<String, Object> params = new NotificationParamsBuilder().user(processedUser).build();
        emailService.sendAsyncEmailNotification(
            new EmailNotificationBuilder()
                .to(userToProcess.getEmail())
                .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_REGISTRATION_REQUEST_PROCESSED)
                .params(params)
                .param("registrationStatus", accepted ? "accepted" : "rejected")
                .build(),
            GraviteeContext.getCurrentContext()
        );
        auditService.createEnvironmentAuditLog(
            Collections.singletonMap(USER, processedUser.getId()),
            accepted ? User.AuditEvent.USER_CONFIRMED : User.AuditEvent.USER_REJECTED,
            processedUser.getUpdatedAt(),
            userToProcess,
            processedUser
        );

        return processedUser;
    }

    private UserEntity changeUserStatus(String userId, UserStatus newStatus) {
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
                return convert(this.userRepository.update(user), true);
            }
            throw new UserNotFoundException(userId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to validate user registration {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to create an external user" + userId, ex);
        }
    }

    @Override
    public Map<String, Object> getTokenRegistrationParams(final UserEntity userEntity, final String managementUri, final ACTION action) {
        return getTokenRegistrationParams(userEntity, managementUri, action, null);
    }

    @Override
    public Map<String, Object> getTokenRegistrationParams(
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

        String managementURL = parameterService.find(Key.MANAGEMENT_URL, ParameterReferenceType.ORGANIZATION);
        String userURL = "";
        if (!StringUtils.isEmpty(managementURL)) {
            if (managementURL.endsWith("/")) {
                managementURL = managementURL.substring(0, managementURL.length() - 1);
            }
            userURL = managementURL + "/#!/settings/users/" + userEntity.getId();
        }

        String registrationUrl = "";
        if (targetPageUrl != null && !targetPageUrl.isEmpty()) {
            registrationUrl += targetPageUrl;
            if (!targetPageUrl.endsWith("/")) {
                registrationUrl += "/";
            }
            registrationUrl += token;
        } else if (!StringUtils.isEmpty(managementURL)) {
            registrationUrl = managementURL + managementUri + token;
        } else {
            // This value is used as a fallback when no Management URL has been configured by the platform admin.
            registrationUrl = DEFAULT_MANAGEMENT_URL + managementUri + token;
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
    public UserEntity update(String id, UpdateUserEntity updateUserEntity) {
        return this.update(id, updateUserEntity, updateUserEntity.getEmail());
    }

    @Override
    public UserEntity update(String id, UpdateUserEntity updateUserEntity, String newsletterEmail) {
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
                        updatedMetadata.add(userMetadataService.update(metadataEntity));
                    } else {
                        // some additional fields may have been added after the user registration
                        NewUserMetadataEntity metadataEntity = new NewUserMetadataEntity();
                        metadataEntity.setName(entry.getKey());
                        metadataEntity.setValue(String.valueOf(entry.getValue()));
                        metadataEntity.setUserId(user.getId());
                        metadataEntity.setFormat(MetadataFormat.STRING);
                        updatedMetadata.add(userMetadataService.create(metadataEntity));
                    }
                }
            }

            return convert(updatedUser, true, updatedMetadata);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update {}", updateUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying update " + updateUserEntity, ex);
        }
    }

    @Override
    public Page<UserEntity> search(String query, Pageable pageable) {
        LOGGER.debug("search users");

        if (query == null || query.isEmpty()) {
            return search(
                new UserCriteria.Builder().statuses(UserStatus.ACTIVE, UserStatus.PENDING, UserStatus.REJECTED).build(),
                pageable
            );
        }
        // UserDocumentTransformation remove domain from email address for security reasons
        // remove it during search phase to provide results
        String sanitizedQuery = query.indexOf('@') > 0 ? query.substring(0, query.indexOf('@')) : query;
        Query<UserEntity> userQuery = QueryBuilder.create(UserEntity.class).setQuery(sanitizedQuery).setPage(pageable).build();

        SearchResult results = searchEngineService.search(userQuery);

        if (results.hasResults()) {
            List<UserEntity> users = new ArrayList<>((findByIds(results.getDocuments())));

            populateUserFlags(users);

            return new Page<>(users, pageable.getPageNumber(), pageable.getPageSize(), results.getHits());
        }
        return new Page<>(Collections.emptyList(), 1, 0, 0);
    }

    private void populateUserFlags(final List<UserEntity> users) {
        RoleEntity apiPORole = roleService.findPrimaryOwnerRoleByOrganization(GraviteeContext.getCurrentOrganization(), RoleScope.API);
        RoleEntity applicationPORole = roleService.findPrimaryOwnerRoleByOrganization(
            GraviteeContext.getCurrentOrganization(),
            RoleScope.APPLICATION
        );

        users.forEach(
            user -> {
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
            }
        );
    }

    @Override
    public Page<UserEntity> search(UserCriteria criteria, Pageable pageable) {
        try {
            LOGGER.debug("search users");
            UserCriteria.Builder builder = new UserCriteria.Builder()
                .organizationId(GraviteeContext.getCurrentOrganization())
                .statuses(criteria.getStatuses());
            if (criteria.hasNoStatus()) {
                builder.noStatus();
            }
            UserCriteria newCriteria = builder.build();

            Page<User> users = userRepository.search(
                newCriteria,
                new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build()
            );

            List<UserEntity> entities = users.getContent().stream().map(u -> convert(u, false)).collect(toList());

            populateUserFlags(entities);

            return new Page<>(entities, users.getPageNumber() + 1, (int) users.getPageElements(), users.getTotalElements());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to search users", ex);
            throw new TechnicalManagementException("An error occurs while trying to search users", ex);
        }
    }

    @Override
    public void delete(String id) {
        try {
            // If the users is PO of apps or apis, throw an exception
            long apiCount = apiService
                .findByUser(id, null, false)
                .stream()
                .filter(entity -> entity.getPrimaryOwner().getId().equals(id))
                .count();
            long applicationCount = applicationService
                .findByUser(id)
                .stream()
                .filter(app -> app.getPrimaryOwner() != null)
                .filter(app -> app.getPrimaryOwner().getId().equals(id))
                .count();
            if (apiCount > 0 || applicationCount > 0) {
                throw new StillPrimaryOwnerException(apiCount, applicationCount);
            }

            Optional<User> optionalUser = userRepository.findById(id);
            if (!optionalUser.isPresent()) {
                throw new UserNotFoundException(id);
            }

            membershipService.removeMemberMemberships(MembershipMemberType.USER, id);
            User user = optionalUser.get();

            //remove notifications
            portalNotificationService.deleteAll(user.getId());
            portalNotificationConfigService.deleteByUser(user.getId());
            genericNotificationConfigService.deleteByUser(user);

            //remove tokens
            tokenService.revokeByUser(user.getId());

            // change user datas
            user.setSourceId("deleted-" + user.getSourceId());
            user.setStatus(UserStatus.ARCHIVED);
            user.setUpdatedAt(new Date());

            if (anonymizeOnDelete) {
                User anonym = new User();
                anonym.setId(user.getId());
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

            final UserEntity userEntity = convert(optionalUser.get(), false);
            searchEngineService.delete(userEntity, false);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to delete user", ex);
            throw new TechnicalManagementException("An error occurs while trying to delete user", ex);
        }
    }

    @Override
    public void resetPassword(final String id) {
        this.resetPassword(id, null);
    }

    @Override
    public UserEntity resetPasswordFromSourceId(String sourceId, String resetPageUrl) {
        if (sourceId.startsWith("deleted")) {
            throw new UserNotActiveException(sourceId);
        }

        UrlSanitizerUtils.checkAllowed(resetPageUrl, portalWhitelist, true);

        UserEntity foundUser = this.findBySource(IDP_SOURCE_GRAVITEE, sourceId, false);
        if ("ACTIVE".equals(foundUser.getStatus())) {
            this.resetPassword(foundUser.getId(), resetPageUrl);
            return foundUser;
        } else {
            throw new UserNotActiveException(foundUser.getSourceId());
        }
    }

    private boolean isInternalUser(User user) {
        return IDP_SOURCE_GRAVITEE.equals(user.getSource());
    }

    private void resetPassword(final String id, final String resetPageUrl) {
        try {
            LOGGER.debug("Resetting password of user id {}", id);

            Optional<User> optionalUser = userRepository.findById(id);

            if (!optionalUser.isPresent()) {
                throw new UserNotFoundException(id);
            }
            final User user = optionalUser.get();
            if (!isInternalUser(user)) {
                throw new UserNotInternallyManagedException(id);
            }

            // do not update password to null anymore to avoid DoS attack on a user account.
            // use the audit events to throttle the number of resetPassword for a given userid
            // see: https://github.com/gravitee-io/issues/issues/4410

            // do not perform this check if the request comes from an authenticated user (ie. admin or someone with right permission)
            if (!isAuthenticated() || !canResetPassword()) {
                AuditQuery query = new AuditQuery();
                query.setEvents(Arrays.asList(User.AuditEvent.PASSWORD_RESET.name()));
                query.setFrom(Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli());
                query.setPage(1);
                query.setSize(100);
                MetadataPage<AuditEntity> events = auditService.search(query);
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
                convert(user, false),
                RESET_PASSWORD_PATH,
                RESET_PASSWORD,
                resetPageUrl
            );

            notifierService.trigger(PortalHook.PASSWORD_RESET, params);

            auditService.createOrganizationAuditLog(
                Collections.singletonMap(USER, user.getId()),
                User.AuditEvent.PASSWORD_RESET,
                new Date(),
                null,
                null
            );
            emailService.sendAsyncEmailNotification(
                new EmailNotificationBuilder()
                    .to(user.getEmail())
                    .template(EmailNotificationBuilder.EmailTemplate.TEMPLATES_FOR_ACTION_USER_PASSWORD_RESET)
                    .params(params)
                    .build(),
                GraviteeContext.getCurrentContext()
            );
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to reset password for user " + id;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    protected boolean canResetPassword() {
        if (isAdmin()) {
            return true;
        }
        return permissionService.hasPermission(
            RolePermission.ORGANIZATION_USERS,
            GraviteeContext.getCurrentOrganization(),
            new RolePermissionAction[] { UPDATE }
        );
    }

    private User convert(NewExternalUserEntity newExternalUserEntity) {
        if (newExternalUserEntity == null) {
            return null;
        }
        User user = new User();
        user.setEmail(newExternalUserEntity.getEmail());
        user.setFirstname(newExternalUserEntity.getFirstname());
        user.setLastname(newExternalUserEntity.getLastname());
        user.setSource(newExternalUserEntity.getSource());
        user.setSourceId(newExternalUserEntity.getSourceId());
        user.setStatus(UserStatus.ACTIVE);
        user.setPicture(newExternalUserEntity.getPicture());
        user.setNewsletterSubscribed(newExternalUserEntity.getNewsletter());
        return user;
    }

    private User convert(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }
        User user = new User();
        user.setId(userEntity.getId());
        user.setEmail(userEntity.getEmail());
        user.setFirstname(userEntity.getFirstname());
        user.setLastname(userEntity.getLastname());
        user.setSource(userEntity.getSource());
        user.setSourceId(userEntity.getSourceId());
        if (userEntity.getStatus() != null) {
            user.setStatus(UserStatus.valueOf(userEntity.getStatus()));
        }
        return user;
    }

    private UserEntity convert(User user, boolean loadRoles) {
        return convert(user, loadRoles, Collections.emptyList());
    }

    private UserEntity convert(User user, boolean loadRoles, List<UserMetadataEntity> customUserFields) {
        if (user == null) {
            return null;
        }
        UserEntity userEntity = new UserEntity();

        final String userId = user.getId();
        userEntity.setId(userId);
        userEntity.setSource(user.getSource());
        userEntity.setSourceId(user.getSourceId());
        userEntity.setEmail(user.getEmail());
        userEntity.setFirstname(user.getFirstname());
        userEntity.setLastname(user.getLastname());
        userEntity.setPassword(user.getPassword());
        userEntity.setCreatedAt(user.getCreatedAt());
        userEntity.setUpdatedAt(user.getUpdatedAt());
        userEntity.setLastConnectionAt(user.getLastConnectionAt());
        userEntity.setFirstConnectionAt(user.getFirstConnectionAt());
        userEntity.setPicture(user.getPicture());
        if (user.getStatus() != null) {
            userEntity.setStatus(user.getStatus().name());
        }

        if (loadRoles) {
            Set<UserRoleEntity> roles = new HashSet<>();
            Set<RoleEntity> roleEntities = membershipService.getRoles(
                MembershipReferenceType.ORGANIZATION,
                GraviteeContext.getCurrentOrganization(),
                MembershipMemberType.USER,
                userId
            );
            if (!roleEntities.isEmpty()) {
                roleEntities.forEach(roleEntity -> roles.add(convert(roleEntity)));
            }

            this.environmentService.findByOrganization(GraviteeContext.getCurrentOrganization())
                .stream()
                .flatMap(
                    env ->
                        membershipService
                            .getRoles(MembershipReferenceType.ENVIRONMENT, env.getId(), MembershipMemberType.USER, userId)
                            .stream()
                )
                .filter(Objects::nonNull)
                .forEach(roleEntity -> roles.add(convert(roleEntity)));

            userEntity.setRoles(roles);

            Map<String, Set<UserRoleEntity>> envRolesMap = new HashMap<>();
            this.environmentService.findByOrganization(GraviteeContext.getCurrentOrganization())
                .forEach(
                    env -> {
                        Set<UserRoleEntity> envRoles = new HashSet<>();
                        Set<RoleEntity> envRoleEntities = membershipService.getRoles(
                            MembershipReferenceType.ENVIRONMENT,
                            env.getId(),
                            MembershipMemberType.USER,
                            userId
                        );
                        if (!envRoleEntities.isEmpty()) {
                            envRoleEntities.forEach(roleEntity -> envRoles.add(convert(roleEntity)));
                        }
                        envRolesMap.put(env.getId(), envRoles);
                    }
                );
            userEntity.setEnvRoles(envRolesMap);
        }

        userEntity.setLoginCount(user.getLoginCount());
        userEntity.setNewsletterSubscribed(user.getNewsletterSubscribed());

        if (customUserFields != null && !customUserFields.isEmpty()) {
            Maps.MapBuilder builder = Maps.builder();
            for (UserMetadataEntity meta : customUserFields) {
                builder.put(meta.getKey(), meta.getValue());
            }
            userEntity.setCustomFields(builder.build());
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
    public UserEntity createOrUpdateUserFromSocialIdentityProvider(SocialIdentityProviderEntity socialProvider, String userInfo) {
        HashMap<String, String> attrs = getUserProfileAttrs(socialProvider.getUserProfileMapping(), userInfo);

        String email = attrs.get(SocialIdentityProviderEntity.UserProfile.EMAIL);
        if (email == null && socialProvider.isEmailRequired()) {
            throw new EmailRequiredException(attrs.get(SocialIdentityProviderEntity.UserProfile.ID));
        }

        // Compute group and role mappings
        // This is done BEFORE updating or creating the user account to ensure this one is properly created with correct
        // information (ie. mappings)
        Set<GroupEntity> userGroups = computeUserGroupsFromProfile(email, socialProvider.getGroupMappings(), userInfo);
        Set<RoleEntity> userRoles = computeUserRolesFromProfile(email, socialProvider.getRoleMappings(), userInfo);

        UserEntity user = null;
        boolean created = false;
        try {
            user = refreshExistingUser(socialProvider, attrs, email);
        } catch (UserNotFoundException unfe) {
            created = true;
            user = createNewExternalUser(socialProvider, userInfo, attrs, email);
        }

        // Memberships must be refresh only when it is a user creation context or mappings should be synced during
        // later authentication
        List<MembershipService.Membership> groupMemberships = refreshUserGroups(user.getId(), socialProvider.getId(), userGroups);
        List<MembershipService.Membership> envRoleMemberships = refreshUserRoles(
            user.getId(),
            socialProvider.getId(),
            userRoles,
            RoleScope.ENVIRONMENT
        );
        List<MembershipService.Membership> orgRoleMemberships = refreshUserRoles(
            user.getId(),
            socialProvider.getId(),
            userRoles,
            RoleScope.ORGANIZATION
        );

        if (created || socialProvider.isSyncMappings()) {
            refreshUserMemberships(user.getId(), socialProvider.getId(), groupMemberships, MembershipReferenceType.GROUP);
            refreshUserMemberships(user.getId(), socialProvider.getId(), envRoleMemberships, MembershipReferenceType.ENVIRONMENT);
            refreshUserMemberships(user.getId(), socialProvider.getId(), orgRoleMemberships, MembershipReferenceType.ORGANIZATION);
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

            if (mapping != null) {
                try {
                    if (mapping.contains("{#")) {
                        map.put(field, templateEngine.getValue(mapping, String.class));
                    } else {
                        map.put(field, userInfoPath.read(mapping, String.class));
                    }
                } catch (Exception e) {
                    LOGGER.error("Using mapping: \"{}\", no fields are located in {}", mapping, userInfo);
                }
            }
        }

        return map;
    }

    private UserEntity createNewExternalUser(
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

        return this.create(newUser, false);
    }

    private UserEntity refreshExistingUser(final SocialIdentityProviderEntity socialProvider, HashMap<String, String> attrs, String email) {
        String userId;
        UserEntity registeredUser =
            this.findBySource(socialProvider.getId(), attrs.get(SocialIdentityProviderEntity.UserProfile.ID), false);
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

        return this.update(userId, user);
    }

    private void addRolesToUser(
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
                ref =
                    new MembershipService.MembershipReference(
                        MembershipReferenceType.ORGANIZATION,
                        GraviteeContext.getCurrentOrganization()
                    );
            } else {
                ref =
                    new MembershipService.MembershipReference(MembershipReferenceType.ENVIRONMENT, GraviteeContext.getCurrentEnvironment());
            }
            membershipService.addRoleToMemberOnReference(
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
     * @return
     */
    private Set<GroupEntity> computeUserGroupsFromProfile(String userId, List<GroupMappingEntity> mappings, String userInfo) {
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
                        groups.add(groupService.findById(groupName));
                    } catch (GroupNotFoundException gnfe) {
                        LOGGER.error("Unable to create user, missing group in repository : {}", groupName);
                    }
                }
            }
        }

        return groups;
    }

    /**
     * Calculate the list of roles to associate to a user according to its OIDC profile (ie. UserInfo)
     *
     * @param userId
     * @param mappings
     * @param userInfo
     * @return
     */
    private Set<RoleEntity> computeUserRolesFromProfile(String userId, List<RoleMappingEntity> mappings, String userInfo) {
        if (mappings == null || mappings.isEmpty()) {
            // provide default roles in this case otherwise user will not have roles if the RoleMapping isn't provided and if the
            // option to refresh user profile on each connection is enabled
            return roleService.findDefaultRoleByScopes(RoleScope.ORGANIZATION, RoleScope.ENVIRONMENT).stream().collect(toSet());
        }

        Set<RoleEntity> roles = new HashSet<>();

        for (RoleMappingEntity mapping : mappings) {
            TemplateEngine templateEngine = TemplateEngine.templateEngine();
            templateEngine.getTemplateContext().setVariable(TEMPLATE_ENGINE_PROFILE_ATTRIBUTE, userInfo);

            boolean match = templateEngine.getValue(mapping.getCondition(), boolean.class);

            trace(userId, match, mapping.getCondition());

            // Get roles
            if (match) {
                if (mapping.getEnvironments() != null) {
                    try {
                        mapping
                            .getEnvironments()
                            .forEach(env -> roleService.findByScopeAndName(RoleScope.ENVIRONMENT, env).ifPresent(roles::add));
                    } catch (RoleNotFoundException rnfe) {
                        LOGGER.error("Unable to create user, missing role in repository : {}", mapping.getEnvironments());
                    }
                }

                if (mapping.getOrganizations() != null) {
                    try {
                        mapping
                            .getOrganizations()
                            .forEach(org -> roleService.findByScopeAndName(RoleScope.ORGANIZATION, org).ifPresent(roles::add));
                    } catch (RoleNotFoundException rnfe) {
                        LOGGER.error("Unable to create user, missing role in repository : {}", mapping.getOrganizations());
                    }
                }
            }
        }

        return roles;
    }

    private List<MembershipService.Membership> refreshUserGroups(
        String userId,
        String identityProviderId,
        Collection<GroupEntity> userGroups
    ) {
        List<MembershipService.Membership> memberships = new ArrayList<>();

        // Get the default group roles from system
        List<RoleEntity> roleEntities = roleService.findDefaultRoleByScopes(RoleScope.API, RoleScope.APPLICATION);

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

    private List<MembershipService.Membership> refreshUserRoles(
        String userId,
        String identityProviderId,
        Collection<RoleEntity> userRoles,
        RoleScope scope
    ) {
        return userRoles
            .stream()
            .filter(role -> role.getScope().equals(scope))
            .map(
                roleEntity -> {
                    MembershipService.Membership membership = new MembershipService.Membership(
                        new MembershipService.MembershipReference(
                            RoleScope.ENVIRONMENT == roleEntity.getScope()
                                ? MembershipReferenceType.ENVIRONMENT
                                : MembershipReferenceType.ORGANIZATION,
                            RoleScope.ENVIRONMENT == roleEntity.getScope()
                                ? GraviteeContext.getCurrentEnvironmentOrDefault()
                                : GraviteeContext.getCurrentOrganization()
                        ),
                        new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
                        new MembershipService.MembershipRole(RoleScope.valueOf(roleEntity.getScope().name()), roleEntity.getName())
                    );

                    membership.setSource(identityProviderId);

                    return membership;
                }
            )
            .collect(toList());
    }

    /**
     * Refresh user memberships.
     *
     * @param userId User identifier.
     * @param identityProviderId The identity provider used to authenticate the user.
     * @param memberships List of memberships to associate to the user
     * @param types The types of user memberships to manage
     */
    private void refreshUserMemberships(
        String userId,
        String identityProviderId,
        List<MembershipService.Membership> memberships,
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

        // Delete existing memberships
        userMemberships.forEach(
            membership -> {
                membershipService.deleteReferenceMember(
                    MembershipReferenceType.valueOf(membership.getReferenceType().name()),
                    membership.getReferenceId(),
                    MembershipMemberType.USER,
                    userId
                );
            }
        );

        Map<MembershipService.MembershipReference, Map<MembershipService.MembershipMember, Map<String, Collection<MembershipService.MembershipRole>>>> groupedRoles = new HashMap<>();
        memberships.forEach(
            membership ->
                groupedRoles
                    .computeIfAbsent(membership.getReference(), ignore -> new HashMap<>())
                    .computeIfAbsent(membership.getMember(), ignore -> new HashMap<>())
                    .computeIfAbsent(membership.getSource(), ignore -> new ArrayList<>())
                    .add(membership.getRole())
        );
        // Create updated memberships
        groupedRoles.forEach(
            (reference, memberMapping) ->
                memberMapping.forEach(
                    (member, sourceMapping) ->
                        sourceMapping.forEach(
                            (source, roles) -> membershipService.updateRolesToMemberOnReferenceBySource(reference, member, roles, source)
                        )
                )
        );
    }

    @Override
    public void updateUserRoles(String userId, MembershipReferenceType referenceType, String referenceId, List<String> roleIds) {
        // check if user exist
        this.findById(userId);
        MemberEntity userMember = membershipService.getUserMember(referenceType, referenceId, userId);
        if (userMember != null) {
            userMember
                .getRoles()
                .forEach(
                    role -> {
                        if (!roleIds.contains(role.getId())) {
                            membershipService.removeRole(referenceType, referenceId, MembershipMemberType.USER, userId, role.getId());
                        } else {
                            roleIds.remove(role.getId());
                        }
                    }
                );
        }
        if (!roleIds.isEmpty()) {
            this.addRolesToUser(
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
