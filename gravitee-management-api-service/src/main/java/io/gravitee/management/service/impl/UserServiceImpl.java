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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.*;
import io.gravitee.management.model.application.ApplicationSettings;
import io.gravitee.management.model.application.SimpleApplicationSettings;
import io.gravitee.management.model.common.Pageable;
import io.gravitee.management.model.parameters.Key;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.common.JWTHelper.ACTION;
import io.gravitee.management.service.common.JWTHelper.Claims;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.impl.search.SearchResult;
import io.gravitee.management.service.notification.NotificationParamsBuilder;
import io.gravitee.management.service.notification.PortalHook;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.management.service.search.query.Query;
import io.gravitee.management.service.search.query.QueryBuilder;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.management.service.common.JWTHelper.ACTION.*;
import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER;
import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static io.gravitee.management.service.notification.NotificationParamsBuilder.REGISTRATION_PATH;
import static io.gravitee.management.service.notification.NotificationParamsBuilder.RESET_PASSWORD_PATH;
import static io.gravitee.repository.management.model.Audit.AuditProperties.USER;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserServiceImpl extends AbstractService implements UserService {

    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    /** A default source used for user registration.*/
    private final static String IDP_SOURCE_GRAVITEE = "gravitee";
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
    private PasswordValidator passwordValidator;
    @Autowired
    private TokenService tokenService;

    @Value("${user.avatar:${gravitee.home}/assets/default_user_avatar.png}")
    private String defaultAvatar;
    @Value("${user.login.defaultApplication:true}")
    private boolean defaultApplicationForFirstConnection;

    @Value("${user.anonymize-on-delete.enabled:false}")
    private boolean anonymizeOnDelete;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
            if (user.getLastConnectionAt() == null) {
                notifierService.trigger(PortalHook.USER_FIRST_LOGIN, new NotificationParamsBuilder()
                        .user(convert(user, false))
                        .build());
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
                        applicationService.create(defaultApp, userId);
                    } catch (IllegalStateException ex) {
                        //do not fail to create a user even if we are not able to create its default app
                    }
                }
            }

            // Set date fields
            user.setLastConnectionAt(new Date());
            user.setUpdatedAt(user.getLastConnectionAt());

            User updatedUser = userRepository.update(user);
            auditService.createPortalAuditLog(
                    Collections.singletonMap(USER, userId),
                    User.AuditEvent.USER_CONNECTED,
                    user.getUpdatedAt(),
                    previousUser,
                    user);

            final UserEntity userEntity = convert(updatedUser, true);
            searchEngineService.index(userEntity, false);
            return userEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to connect {}", userId, ex);
            throw new TechnicalManagementException("An error occurs while trying to connect " + userId, ex);
        }
    }

    @Override
    public UserEntity findById(String id) {
        try {
            LOGGER.debug("Find user by ID: {}", id);

            Optional<User> optionalUser = userRepository.findById(id);

            if (optionalUser.isPresent()) {
                return convert(optionalUser.get(), false);
            }
            //should never happen
            throw new UserNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find user using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to find user using its ID " + id, ex);
        }
    }

    @Override
    public UserEntity findByIdWithRoles(String id) {
        try {
            LOGGER.debug("Find user by ID: {}", id);

            Optional<User> optionalUser = userRepository.findById(id);

            if (optionalUser.isPresent()) {
                return convert(optionalUser.get(), true);
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

            Optional<User> optionalUser = userRepository.findBySource(source, sourceId);

            if (optionalUser.isPresent()) {
                return convert(optionalUser.get(), loadRoles);
            }

            // Should never happen
            throw new UserNotFoundException(sourceId);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find user using source[{}], user[{}]", source, sourceId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find user using source " + source + ':' + sourceId, ex);
        }
    }

    @Override
    public Set<UserEntity> findByIds(List<String> ids) {
        try {
            LOGGER.debug("Find users by ID: {}", ids);

            Set<User> users = userRepository.findByIds(ids);

            if (!users.isEmpty()) {
                return users.stream().map(u -> this.convert(u, false)).collect(Collectors.toSet());
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

    private void checkUserRegistrationEnabled() {
        if (!parameterService.findAsBoolean(Key.PORTAL_USERCREATION_ENABLED)) {
            throw new IllegalStateException("The user registration is disabled");
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
            final String jwtSecret = environment.getProperty("jwt.secret");
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                throw new IllegalStateException("JWT secret is mandatory");
            }

            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
                    .build();

            DecodedJWT jwt = verifier.verify(registerUserEntity.getToken());

            final String action = jwt.getClaim(Claims.ACTION).asString();
            if (USER_REGISTRATION.name().equals(action)) {
                checkUserRegistrationEnabled();
            } else if (GROUP_INVITATION.name().equals(action)) {
                // check invitations
                final String email = jwt.getClaim(Claims.EMAIL).asString();
                final List<InvitationEntity> invitations = invitationService.findAll();
                final List<InvitationEntity> userInvitations = invitations.stream()
                        .filter(invitation -> invitation.getEmail().equals(email))
                        .collect(toList());
                if (userInvitations.isEmpty()) {
                    throw new IllegalStateException("Invitation has been canceled");
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
            } else {
                final String username = subject.toString();
                LOGGER.debug("Create an internal user {}", username);
                Optional<User> checkUser = userRepository.findById(username);
                user = checkUser.orElseThrow(() -> new UserNotFoundException(username));
                if (StringUtils.isNotBlank(user.getPassword())) {
                    throw new UserAlreadyExistsException(IDP_SOURCE_GRAVITEE, username);
                }
            }

            if (GROUP_INVITATION.name().equals(action)) {
                // check invitations
                final String email = user.getEmail();
                final String userId = user.getId();
                final List<InvitationEntity> invitations = invitationService.findAll();
                invitations.stream()
                        .filter(invitation -> invitation.getEmail().equals(email))
                        .forEach(invitation -> {
                            invitationService.addMember(invitation.getReferenceType().name(), invitation.getReferenceId(),
                                    userId, invitation.getApiRole(), invitation.getApplicationRole());
                            invitationService.delete(invitation.getId(), invitation.getReferenceId());
                        });
            }

            // Set date fields
            user.setUpdatedAt(new Date());

            // Encrypt password if internal user
            if (registerUserEntity.getPassword() != null) {
                if (passwordValidator.validate(registerUserEntity.getPassword())) {
                    user.setPassword(passwordEncoder.encode(registerUserEntity.getPassword()));
                } else {
                    throw new PasswordFormatInvalidException();
                }
            }

            user = userRepository.update(user);
            auditService.createPortalAuditLog(
                    Collections.singletonMap(USER, user.getId()),
                    User.AuditEvent.USER_CREATED,
                    user.getUpdatedAt(),
                    null,
                    user);

            // Do not send back the password
            user.setPassword(null);

            final UserEntity userEntity = convert(user, true);
            searchEngineService.index(userEntity, false);
            return userEntity;
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to create an internal user with the token {}", registerUserEntity.getToken(), ex);
            throw new TechnicalManagementException(ex.getMessage(), ex);
        }
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

        // Return default inline user avatar
        InlinePictureEntity imageEntity = new InlinePictureEntity();
        imageEntity.setType("image/png");
        try {
            imageEntity.setContent(IOUtils.toByteArray(new FileInputStream(defaultAvatar)));
        } catch (IOException ioe) {
            LOGGER.error("Default icon for API does not exist", ioe);
        }
        return imageEntity;
    }

    /**
     * Allows to create a user.
     * @param newExternalUserEntity
     * @return
     */
    @Override
    public UserEntity create(NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole) {
        try {
            LOGGER.debug("Create an external user {}", newExternalUserEntity);
            Optional<User> checkUser = userRepository.findBySource(
                    newExternalUserEntity.getSource(), newExternalUserEntity.getSourceId());

            if (checkUser.isPresent()) {
                throw new UserAlreadyExistsException(newExternalUserEntity.getSource(), newExternalUserEntity.getSourceId());
            }

            User user = convert(newExternalUserEntity);
            user.setId(UUID.toString(UUID.random()));
            user.setStatus(UserStatus.ACTIVE);

            // Set date fields
            user.setCreatedAt(new Date());
            user.setUpdatedAt(user.getCreatedAt());

            User createdUser = userRepository.create(user);
            auditService.createPortalAuditLog(
                    Collections.singletonMap(USER, user.getId()),
                    User.AuditEvent.USER_CREATED,
                    user.getCreatedAt(),
                    null,
                    user);

            if (addDefaultRole) {
                addDefaultMembership(createdUser);
            }

            final UserEntity userEntity = convert(createdUser, true);
            searchEngineService.index(userEntity, false);
            return userEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create an external user {}", newExternalUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create an external user" + newExternalUserEntity, ex);
        }
    }

    private void addDefaultMembership(User user) {
        RoleScope[] scopes = {RoleScope.MANAGEMENT, RoleScope.PORTAL};
        List<RoleEntity> defaultRoleByScopes = roleService.findDefaultRoleByScopes(scopes);
        if (defaultRoleByScopes == null || defaultRoleByScopes.isEmpty()) {
            throw new DefaultRoleNotFoundException(scopes);
        }

        for (RoleEntity defaultRoleByScope : defaultRoleByScopes) {
            switch (defaultRoleByScope.getScope()) {
                case MANAGEMENT:
                    membershipService.addOrUpdateMember(
                            new MembershipService.MembershipReference(MembershipReferenceType.MANAGEMENT, MembershipDefaultReferenceId.DEFAULT.name()),
                            new MembershipService.MembershipUser(user.getId(), null),
                            new MembershipService.MembershipRole(RoleScope.MANAGEMENT, defaultRoleByScope.getName()));
                    break;
                case PORTAL:
                    membershipService.addOrUpdateMember(
                            new MembershipService.MembershipReference(MembershipReferenceType.PORTAL, MembershipDefaultReferenceId.DEFAULT.name()),
                            new MembershipService.MembershipUser(user.getId(), null),
                            new MembershipService.MembershipRole(RoleScope.PORTAL, defaultRoleByScope.getName()));
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public UserEntity register(final NewExternalUserEntity newExternalUserEntity) {
        checkUserRegistrationEnabled();
        return createAndSendEmail(newExternalUserEntity, USER_REGISTRATION);
    }

    @Override
    public UserEntity create(final NewExternalUserEntity newExternalUserEntity) {
        return createAndSendEmail(newExternalUserEntity, USER_CREATION);
    }

    /**
     * Allows to create an user and send an email notification to finalize its creation.
     */
    private UserEntity createAndSendEmail(final NewExternalUserEntity newExternalUserEntity, final ACTION action) {
        if (!EmailValidator.isValid(newExternalUserEntity.getEmail())){
            throw new EmailFormatInvalidException(newExternalUserEntity.getEmail());
        }

        final Optional<User> optionalUser;
        try {
            optionalUser = userRepository.findBySource(IDP_SOURCE_GRAVITEE, newExternalUserEntity.getEmail());
            if (optionalUser.isPresent()) {
                throw new UserAlreadyExistsException(IDP_SOURCE_GRAVITEE, newExternalUserEntity.getEmail());
            }
        } catch (final TechnicalException e) {
            LOGGER.error("An error occurs while trying to create user {}", newExternalUserEntity.getEmail(), e);
            throw new TechnicalManagementException(e.getMessage(), e);
        }

        newExternalUserEntity.setSource(IDP_SOURCE_GRAVITEE);
        newExternalUserEntity.setSourceId(newExternalUserEntity.getEmail());

        final UserEntity userEntity = create(newExternalUserEntity, true);

        final Map<String, Object> params = getTokenRegistrationParams(userEntity, REGISTRATION_PATH, action);
        notifierService.trigger(ACTION.USER_REGISTRATION.equals(action) ? PortalHook.USER_REGISTERED : PortalHook.USER_CREATED, params);
        emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                .to(userEntity.getEmail())
                .subject(format("User %s - %s", USER_REGISTRATION.equals(action)?"registration":"creation", userEntity.getDisplayName()))
                .template(EmailNotificationBuilder.EmailTemplate.USER_REGISTRATION)
                .params(params)
                .build()
        );

        return userEntity;
    }

    @Override
    public Map<String, Object> getTokenRegistrationParams(final UserEntity userEntity, final String portalUri,
                                                          final ACTION action) {
        // generate a JWT to store user's information and for security purpose
        final String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));

        Date issueAt = new Date();
        Instant expireAt = issueAt.toInstant().plus(Duration.ofSeconds(environment.getProperty("user.creation.token.expire-after",
                Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER)));

        final String token = JWT.create()
                .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
                .withIssuedAt(issueAt)
                .withExpiresAt(Date.from(expireAt))
                .withSubject(userEntity.getId())
                .withClaim(Claims.EMAIL, userEntity.getEmail())
                .withClaim(Claims.FIRSTNAME, userEntity.getFirstname())
                .withClaim(Claims.LASTNAME, userEntity.getLastname())
                .withClaim(Claims.ACTION, action.name())
                .sign(algorithm);

        String portalUrl = environment.getProperty("portalURL");

        if (portalUrl!= null && portalUrl.endsWith("/")) {
            portalUrl = portalUrl.substring(0, portalUrl.length() - 1);
        }

        String registrationUrl = portalUrl + portalUri + token;

        // send a confirm email with the token
        return new NotificationParamsBuilder()
                .user(userEntity)
                .token(token)
                .registrationUrl(registrationUrl)
                .build();
    }

    @Override
    public UserEntity update(String id, UpdateUserEntity updateUserEntity) {
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
            if (updateUserEntity.getEmail() != null) {
                user.setEmail(updateUserEntity.getEmail());
            }
            if (updateUserEntity.getStatus() != null) {
                user.setStatus(UserStatus.valueOf(updateUserEntity.getStatus()));
            }

            User updatedUser = userRepository.update(user);
            auditService.createPortalAuditLog(
                    Collections.singletonMap(USER, user.getId()),
                    User.AuditEvent.USER_UPDATED,
                    user.getUpdatedAt(),
                    previousUser,
                    user);
            return convert(updatedUser, true);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update {}", updateUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying update " + updateUserEntity, ex);
        }
    }

    @Override
    public Page<UserEntity> search(String query, Pageable pageable) {
        LOGGER.debug("search users");

        if (query == null || query.isEmpty()) {
            return search(new UserCriteria.Builder().statuses(UserStatus.ACTIVE).build(), pageable);
        }

        Query<UserEntity> userQuery = QueryBuilder.create(UserEntity.class)
                .setQuery(query)
                .setPage(pageable)
                .build();

        SearchResult results = searchEngineService.search(userQuery);

        if (results.hasResults()) {
            List<UserEntity> users = new ArrayList<>((findByIds(results.getDocuments())));

            populateUserFlags(users);

            return new Page<>(users,
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    results.getHits());
        }
        return new Page<>(Collections.emptyList(), 1, 0, 0);
    }

    private void populateUserFlags(final List<UserEntity> users) {
        users.forEach(user -> {
            final boolean apiPO = membershipService.findUserMembership(user.getId(), MembershipReferenceType.API)
                    .stream().anyMatch(membership -> membership.getRoles() != null &&
                            SystemRole.PRIMARY_OWNER.name().equals(membership.getRoles().get(RoleScope.API.getId())));

            final boolean appPO = membershipService.findUserMembership(user.getId(), MembershipReferenceType.APPLICATION)
                    .stream().anyMatch(membership -> membership.getRoles() != null &&
                            SystemRole.PRIMARY_OWNER.name().equals(membership.getRoles().get(RoleScope.APPLICATION.getId())));
            user.setPrimaryOwner(apiPO || appPO);
            user.setNbActiveTokens(tokenService.findByUser(user.getId()).size());
        });
    }

    @Override
    public Page<UserEntity> search(UserCriteria criteria, Pageable pageable) {
        try {
            LOGGER.debug("search users");

            Page<User> users = userRepository.search(criteria, new PageableBuilder()
                    .pageNumber(pageable.getPageNumber() - 1)
                    .pageSize(pageable.getPageSize())
                    .build());

            List<UserEntity> entities = users.getContent()
                    .stream()
                    .map(u -> convert(u, false))
                    .collect(toList());

            populateUserFlags(entities);

            return new Page<>(entities,
                    users.getPageNumber() + 1,
                    (int) users.getPageElements(),
                    users.getTotalElements());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to search users", ex);
            throw new TechnicalManagementException("An error occurs while trying to search users", ex);
        }
    }

    @Override
    public void delete(String id) {
        try {
            // If the users is PO of apps or apis, throw an exception
            long apiCount = apiService.findByUser(id, null)
                    .stream()
                    .filter(entity -> entity.getPrimaryOwner().getId().equals(id))
                    .count();
            long applicationCount = applicationService.findByUser(id)
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

            membershipService.removeUser(id);
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
        try {
            LOGGER.debug("Resetting password of user id {}", id);

            Optional<User> optionalUser = userRepository.findById(id);

            if (!optionalUser.isPresent()) {
                throw new UserNotFoundException(id);
            }
            final User user = optionalUser.get();
            if (!IDP_SOURCE_GRAVITEE.equals(user.getSource())) {
                throw new UserNotInternallyManagedException(id);
            }
            user.setPassword(null);
            user.setUpdatedAt(new Date());
            userRepository.update(user);

            final Map<String, Object> params = getTokenRegistrationParams(convert(user, false),
                    RESET_PASSWORD_PATH, RESET_PASSWORD);

            notifierService.trigger(PortalHook.PASSWORD_RESET, params);

            auditService.createPortalAuditLog(
                    Collections.singletonMap(USER, user.getId()),
                    User.AuditEvent.PASSWORD_RESET,
                    user.getUpdatedAt(),
                    null,
                    null);
            emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                    .to(user.getEmail())
                    .subject("Password reset - " + convert(user, false).getDisplayName())
                    .template(EmailNotificationBuilder.EmailTemplate.PASSWORD_RESET)
                    .params(params)
                    .build()
            );
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to reset password for user " + id;
            LOGGER.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
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
        if (user == null) {
            return null;
        }
        UserEntity userEntity = new UserEntity();

        userEntity.setId(user.getId());
        userEntity.setSource(user.getSource());
        userEntity.setSourceId(user.getSourceId());
        userEntity.setEmail(user.getEmail());
        userEntity.setFirstname(user.getFirstname());
        userEntity.setLastname(user.getLastname());
        userEntity.setPassword(user.getPassword());
        userEntity.setCreatedAt(user.getCreatedAt());
        userEntity.setUpdatedAt(user.getUpdatedAt());
        userEntity.setLastConnectionAt(user.getLastConnectionAt());
        userEntity.setPicture(user.getPicture());
        if (user.getStatus() != null) {
            userEntity.setStatus(user.getStatus().name());
        }

        if (loadRoles) {
            Set<UserRoleEntity> roles = new HashSet<>();
            RoleEntity roleEntity = membershipService.getRole(
                    MembershipReferenceType.PORTAL,
                    MembershipDefaultReferenceId.DEFAULT.name(),
                    user.getId(),
                    RoleScope.PORTAL);
            if (roleEntity != null) {
                roles.add(convert(roleEntity));
            }

            roleEntity = membershipService.getRole(
                    MembershipReferenceType.MANAGEMENT,
                    MembershipDefaultReferenceId.DEFAULT.name(),
                    user.getId(),
                    RoleScope.MANAGEMENT);
            if (roleEntity != null) {
                roles.add(convert(roleEntity));
            }

            userEntity.setRoles(roles);
        }

        return userEntity;
    }

    private UserRoleEntity convert(RoleEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }

        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setScope(roleEntity.getScope());
        userRoleEntity.setName(roleEntity.getName());
        userRoleEntity.setPermissions(roleEntity.getPermissions());
        return userRoleEntity;
    }
}
