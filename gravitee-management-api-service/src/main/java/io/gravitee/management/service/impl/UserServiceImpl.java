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

import com.auth0.jwt.JWTSigner;
import com.auth0.jwt.JWTVerifier;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.*;
import io.gravitee.management.model.common.Pageable;
import io.gravitee.management.model.parameters.Key;
import io.gravitee.management.service.*;
import io.gravitee.management.service.builder.EmailNotificationBuilder;
import io.gravitee.management.service.common.JWTHelper.Claims;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.notification.NotificationParamsBuilder;
import io.gravitee.management.service.notification.PortalHook;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.management.model.User;
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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER;
import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static io.gravitee.repository.management.model.Audit.AuditProperties.USER;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserServiceImpl extends AbstractService implements UserService {

    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

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

    @Value("${user.avatar:${gravitee.home}/assets/default_user_avatar.png}")
    private String defaultAvatar;

    @Value("${user.login.defaultApplication:true}")
    private boolean defaultApplicationForFirstConnection;

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
            // First connection: create default application for user
            if (defaultApplicationForFirstConnection && user.getLastConnectionAt() == null) {
                LOGGER.debug("Create a default application for {}", userId);
                NewApplicationEntity defaultApp = new NewApplicationEntity();
                defaultApp.setName("Default application");
                defaultApp.setDescription("My default application");
                applicationService.create(defaultApp, userId);
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
            return convert(updatedUser, true);
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
    public UserEntity findByUsername(String username, boolean loadRoles) {
        try {
            LOGGER.debug("Find user by name: {}", username);

            Optional<User> optionalUser = userRepository.findByUsername(username);

            if (optionalUser.isPresent()) {
                return convert(optionalUser.get(), loadRoles);
            }
            //should never happen
            throw new UserNotFoundException(username);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find user using its username {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to find user using its username " + username, ex);
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
    public UserEntity create(final RegisterUserEntity registerUserEntity) {
        checkUserRegistrationEnabled();
        try {
            final String jwtSecret = environment.getProperty("jwt.secret");
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                throw new IllegalStateException("JWT secret is mandatory");
            }

            final Map<String, Object> claims = new JWTVerifier(jwtSecret).verify(registerUserEntity.getToken());
            String username = claims.get(Claims.SUBJECT).toString();

            LOGGER.debug("Create an internal user {}", username);
            Optional<User> checkUser = userRepository.findByUsername(username);
            User user = checkUser.orElseThrow(() -> new UserNotFoundException(username));

            if (StringUtils.isNotBlank(user.getPassword())) {
                throw new UsernameAlreadyExistsException(username);
            }

            // Set date fields
            user.setUpdatedAt(new Date());
            // Encrypt password if internal user
            if (registerUserEntity.getPassword() != null) {
                user.setPassword(passwordEncoder.encode(registerUserEntity.getPassword()));
            }

            user = userRepository.update(user);
            auditService.createPortalAuditLog(
                    Collections.singletonMap(USER, user.getUsername()),
                    User.AuditEvent.USER_CREATED,
                    user.getUpdatedAt(),
                    null,
                    user);
            return convert(user, true);
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
     * Allows to pre-create a user.
     * @param newExternalUserEntity
     * @return
     */
    @Override
    public UserEntity create(NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole) {
        try {
            LOGGER.debug("Create an external user {}", newExternalUserEntity);
            Optional<User> checkUser = userRepository.findById(newExternalUserEntity.getUsername());
            if (checkUser.isPresent()) {
                throw new UsernameAlreadyExistsException(newExternalUserEntity.getUsername());
            }

            User user = convert(newExternalUserEntity);
            user.setId(UUID.toString(UUID.random()));
            
            // Set date fields
            user.setCreatedAt(new Date());
            user.setUpdatedAt(user.getCreatedAt());

            User createdUser = userRepository.create(user);
            auditService.createPortalAuditLog(
                    Collections.singletonMap(USER, user.getUsername()),
                    User.AuditEvent.USER_CREATED,
                    user.getCreatedAt(),
                    null,
                    user);
            if (addDefaultRole) {
                addDefaultMembership(createdUser);
            }

            return convert(createdUser, true);
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

    /**
     * Allows to pre-create a user and send an email notification to finalize its creation.
     */
    @Override
    public UserEntity register(final NewExternalUserEntity newExternalUserEntity) {
        checkUserRegistrationEnabled();

        try {
            new InternetAddress(newExternalUserEntity.getEmail()).validate();
        } catch (final AddressException ex) {
            throw new EmailFormatInvalidException(newExternalUserEntity.getEmail());
        }

        final Optional<User> optionalUser;
        try {
            optionalUser = userRepository.findByUsername(newExternalUserEntity.getEmail());
            if (optionalUser.isPresent()) {
                throw new UsernameAlreadyExistsException(newExternalUserEntity.getEmail());
            }
        } catch (final TechnicalException e) {
            LOGGER.error("An error occurs while trying to register user {}", newExternalUserEntity.getEmail(), e);
            throw new TechnicalManagementException(e.getMessage(), e);
        }

        newExternalUserEntity.setUsername(newExternalUserEntity.getEmail());
        newExternalUserEntity.setSource("gravitee");
        newExternalUserEntity.setSourceId(newExternalUserEntity.getUsername());

        final UserEntity userEntity = create(newExternalUserEntity, true);
        final Map<String, Object> params = getTokenRegistrationParams(userEntity, "/#!/registration/confirm/");

        notifierService.trigger(PortalHook.USER_REGISTERED, params);

        emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                .to(userEntity.getEmail())
                .subject("User registration - " + userEntity.getUsername())
                .template(EmailNotificationBuilder.EmailTemplate.USER_REGISTRATION)
                .params(params)
                .build()
        );

        return userEntity;
    }

    private Map<String, Object> getTokenRegistrationParams(final UserEntity userEntity, final String portalUri) {
        // generate a JWT to store user's information and for security purpose
        final Map<String, Object> claims = new HashMap<>();
        claims.put(Claims.ISSUER, environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER));

        claims.put(Claims.SUBJECT, userEntity.getUsername());
        claims.put(Claims.EMAIL, userEntity.getEmail());
        claims.put(Claims.FIRSTNAME, userEntity.getFirstname());
        claims.put(Claims.LASTNAME, userEntity.getLastname());

        final JWTSigner.Options options = new JWTSigner.Options();
        options.setExpirySeconds(environment.getProperty("user.creation.token.expire-after",
                Integer.class, DEFAULT_JWT_EMAIL_REGISTRATION_EXPIRE_AFTER));
        options.setIssuedAt(true);
        options.setJwtId(true);

        // send a confirm email with the token
        final String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT secret is mandatory");
        }

        final String token = new JWTSigner(jwtSecret).sign(claims, options);
        String portalUrl = environment.getProperty("portalURL");

        if (portalUrl!= null && portalUrl.endsWith("/")) {
            portalUrl = portalUrl.substring(0, portalUrl.length() - 1);
        }

        String registrationUrl = portalUrl + portalUri + token;

        return new NotificationParamsBuilder()
                .user(userEntity)
                .token(token)
                .registrationUrl(registrationUrl)
                .build();
    }

    @Override
    public UserEntity update(UpdateUserEntity updateUserEntity) {
        try {
            LOGGER.debug("Updating {}", updateUserEntity);
            Optional<User> checkUser = userRepository.findByUsername(updateUserEntity.getUsername());
            if (!checkUser.isPresent()) {
                throw new UserNotFoundException(updateUserEntity.getUsername());
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

            User updatedUser = userRepository.update(user);
            auditService.createPortalAuditLog(
                    Collections.singletonMap(USER, user.getUsername()),
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
    public Page<UserEntity> search(Pageable pageable) {
        try {
            LOGGER.debug("search users");

            Page<User> users = userRepository.search(new PageableBuilder()
                    .pageNumber(pageable.getPageNumber() - 1)
                    .pageSize(pageable.getPageSize())
                    .build());

            List<UserEntity> entities = users.getContent()
                    .stream()
                    .map(u -> convert(u, false))
                    .collect(Collectors.toList());

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
            long apiCount = apiService.findByUser(id)
                    .stream()
                    .filter(entity -> entity.getPrimaryOwner().getId().equals(id))
                    .count();
            long applicationCount = applicationService.findByUser(id)
                    .stream()
                    .filter(entity -> entity.getPrimaryOwner().getId().equals(id))
                    .count();
            if (apiCount > 0 || applicationCount > 0) {
                throw new StillPrimaryOwnerException(apiCount, applicationCount);
            }

            Optional<User> optionalUser = userRepository.findById(id);
            if (!optionalUser.isPresent()) {
                throw new UserNotFoundException(id);
            }

            membershipService.removeUser(id);
            userRepository.delete(id);

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
            if (!"gravitee".equals(user.getSource())) {
                throw new UserNotInternallyManagedException(id);
            }
            user.setPassword(null);
            user.setUpdatedAt(new Date());
            userRepository.update(user);

            final Map<String, Object> params = getTokenRegistrationParams(convert(user, false),
                    "/#!/resetPassword/");

            notifierService.trigger(PortalHook.PASSWORD_RESET, params);

            auditService.createPortalAuditLog(
                    Collections.singletonMap(USER, user.getUsername()),
                    User.AuditEvent.PASSWORD_RESET,
                    user.getUpdatedAt(),
                    null,
                    null);
            emailService.sendAsyncEmailNotification(new EmailNotificationBuilder()
                    .to(user.getEmail())
                    .subject("Password reset - " + user.getUsername())
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

    public void setDefaultApplicationForFirstConnection(boolean defaultApplicationForFirstConnection) {
        this.defaultApplicationForFirstConnection = defaultApplicationForFirstConnection;
    }

    private static User convert(NewExternalUserEntity newExternalUserEntity) {
        if (newExternalUserEntity == null) {
            return null;
        }
        User user = new User();

        user.setUsername(newExternalUserEntity.getUsername());
        user.setEmail(newExternalUserEntity.getEmail());
        user.setFirstname(newExternalUserEntity.getFirstname());
        user.setLastname(newExternalUserEntity.getLastname());
        user.setSource(newExternalUserEntity.getSource());
        user.setSourceId(newExternalUserEntity.getSourceId());

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
        userEntity.setUsername(user.getUsername());
        userEntity.setEmail(user.getEmail());
        userEntity.setFirstname(user.getFirstname());
        userEntity.setLastname(user.getLastname());
        userEntity.setPassword(user.getPassword());
        userEntity.setCreatedAt(user.getCreatedAt());
        userEntity.setUpdatedAt(user.getUpdatedAt());
        userEntity.setLastConnectionAt(user.getLastConnectionAt());
        userEntity.setPicture(user.getPicture());

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
