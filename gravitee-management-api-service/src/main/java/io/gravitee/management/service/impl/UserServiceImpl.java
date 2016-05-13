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

import io.gravitee.management.model.NewUserEntity;
import io.gravitee.management.model.UserEntity;
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
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class UserServiceImpl extends TransactionalService implements UserService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserEntity findByName(String username) {
        try {
            LOGGER.debug("Find user by name: {}", username);

            Optional<User> user = userRepository.findByUsername(username);

            if (user.isPresent()) {
                return convert(user.get());
            }

            throw new UserNotFoundException(username);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a user using its name {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to find a user using its name " + username, ex);
        }
    }

    @Override
    public UserEntity create(NewUserEntity newUserEntity) {
        try {
            LOGGER.debug("Create {}", newUserEntity);
            Optional<User> checkUser = userRepository.findByUsername(newUserEntity.getUsername());
            if (checkUser.isPresent()) {
                throw new UsernameAlreadyExistsException(newUserEntity.getUsername());
            }

            User user = convert(newUserEntity);

            // Set date fields
            user.setCreatedAt(new Date());
            user.setUpdatedAt(user.getCreatedAt());

            User createdUser = userRepository.create(user);
            return convert(createdUser);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create {}", newUserEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying create " + newUserEntity, ex);
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
        user.setRoles(newUserEntity.getRoles());
        return user;
    }

    private static UserEntity convert(User user) {
        if (user == null) {
            return null;
        }
        UserEntity userEntity = new UserEntity();

        userEntity.setUsername(user.getUsername());
        userEntity.setEmail(user.getEmail());
        userEntity.setFirstname(user.getFirstname());
        userEntity.setLastname(user.getLastname());
        userEntity.setPassword(user.getPassword());
        userEntity.setRoles(user.getRoles());
        userEntity.setCreatedAt(user.getCreatedAt());
        userEntity.setUpdatedAt(user.getUpdatedAt());

        return userEntity;
    }
}
