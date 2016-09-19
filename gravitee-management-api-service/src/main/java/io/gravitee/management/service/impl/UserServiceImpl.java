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

import io.gravitee.management.model.NewExternalUserEntity;
import io.gravitee.management.model.NewUserEntity;
import io.gravitee.management.model.UpdateUserEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.permissions.Role;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.management.service.exceptions.UsernameAlreadyExistsException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserServiceImpl extends TransactionalService implements UserService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserEntity connect(String username) {
        try {
            LOGGER.debug("Connection of {}", username);
            Optional<User> checkUser = userRepository.findByUsername(username);
            if (!checkUser.isPresent()) {
                throw new UserNotFoundException(username);
            }

            User user = checkUser.get();

            // Set date fields
            user.setLastConnectionAt(new Date());
            user.setUpdatedAt(user.getLastConnectionAt());

            User updatedUser = userRepository.update(user);
            return convert(updatedUser);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to connect {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to connect " + username, ex);
        }
    }

    @Override
    public UserEntity findByName(String username) {
        LOGGER.debug("Find user by name: {}", username);

        Optional<UserEntity> optionalUser = this.findByNames(Collections.singletonList(username)).stream().findFirst();
        if (optionalUser.isPresent()) {
            return optionalUser.get();
        }
        //should never happen
        throw new UserNotFoundException(username);
    }

    @Override
    public Set<UserEntity> findByNames(List<String> usernames) {
        try {
            LOGGER.debug("Find user by names: {}", usernames);

            Set<User> users = userRepository.findByUsernames(usernames);

            if (!users.isEmpty()) {
                return users.stream().map(UserServiceImpl::convert).collect(Collectors.toSet());
            }

            Optional<String> usernamesAsString = usernames.stream().reduce((a, b) -> a + "/" + b);
            if(usernamesAsString.isPresent()) {
                throw new UserNotFoundException(usernamesAsString.get());
            } else {
                throw new UserNotFoundException("?");
            }
        } catch (TechnicalException ex) {
            Optional<String> usernamesAsString = usernames.stream().reduce((a, b) -> a + "/" + b);
            LOGGER.error("An error occurs while trying to find users using their names {}", usernamesAsString, ex);
            throw new TechnicalManagementException("An error occurs while trying to find users using their names " + usernamesAsString, ex);
        }
    }

    @Override
    public UserEntity create(NewUserEntity newUserEntity) {
        try {
            LOGGER.debug("Create an internal user {}", newUserEntity);
            Optional<User> checkUser = userRepository.findByUsername(newUserEntity.getUsername());
            if (checkUser.isPresent()) {
                throw new UsernameAlreadyExistsException(newUserEntity.getUsername());
            }

            User user = convert(newUserEntity);

            // Encrypt password if internal user
            if (user.getPassword() != null) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            // Set date fields
            user.setCreatedAt(new Date());
            user.setUpdatedAt(user.getCreatedAt());
            user.setSource("gravitee");
            user.setSourceId(user.getUsername());

            // Set default role
            user.setRoles(Collections.singleton(Role.API_CONSUMER.name()));

            User createdUser = userRepository.create(user);
            return convert(createdUser);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create an internal user {}", newUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create an internal user" + newUserEntity, ex);
        }
    }

    @Override
    public UserEntity create(NewExternalUserEntity newExternalUserEntity) {
        try {
            LOGGER.debug("Create an external user {}", newExternalUserEntity);
            Optional<User> checkUser = userRepository.findByUsername(newExternalUserEntity.getUsername());
            if (checkUser.isPresent()) {
                throw new UsernameAlreadyExistsException(newExternalUserEntity.getUsername());
            }

            User user = convert(newExternalUserEntity);

            // Set date fields
            user.setCreatedAt(new Date());
            user.setUpdatedAt(user.getCreatedAt());

            // Set default role
            user.setRoles(Collections.singleton(Role.API_CONSUMER.name()));

            User createdUser = userRepository.create(user);
            return convert(createdUser);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create an external user {}", newExternalUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create an external user" + newExternalUserEntity, ex);
        }
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

            // Set date fields
            user.setUpdatedAt(new Date());

            // Set variant fields
            user.setPicture(updateUserEntity.getPicture());

            User updatedUser = userRepository.update(user);
            return convert(updatedUser);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update {}", updateUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying update " + updateUserEntity, ex);
        }
    }

    @Override
    public Set<UserEntity> findAll() {
        try {
            LOGGER.debug("Find all users");

            Set<User> users = userRepository.findAll();

            return users.stream().map(UserServiceImpl::convert).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find all users", ex);
            throw new TechnicalManagementException("An error occurs while trying to find all users", ex);
        }
    }

    private static User convert(NewUserEntity newUserEntity) {
        if (newUserEntity == null) {
            return null;
        }
        User user = new User();

        user.setUsername(newUserEntity.getUsername());
        user.setEmail(newUserEntity.getEmail());
        user.setFirstname(newUserEntity.getFirstname());
        user.setLastname(newUserEntity.getLastname());
        user.setPassword(newUserEntity.getPassword());

        return user;
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

    private static UserEntity convert(User user) {
        if (user == null) {
            return null;
        }
        UserEntity userEntity = new UserEntity();

        userEntity.setSource(user.getSource());
        userEntity.setSourceId(user.getSourceId());
        userEntity.setUsername(user.getUsername());
        userEntity.setEmail(user.getEmail());
        userEntity.setFirstname(user.getFirstname());
        userEntity.setLastname(user.getLastname());
        userEntity.setPassword(user.getPassword());
        userEntity.setRoles(user.getRoles());
        userEntity.setCreatedAt(user.getCreatedAt());
        userEntity.setUpdatedAt(user.getUpdatedAt());
        userEntity.setLastConnectionAt(user.getLastConnectionAt());
        userEntity.setPicture(user.getPicture());

        return userEntity;
    }
}
