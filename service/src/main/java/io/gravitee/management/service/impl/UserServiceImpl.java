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

import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.exceptions.UsernameAlreadyExistsException;
import io.gravitee.management.model.NewUserEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.service.UserService;
import io.gravitee.repository.api.UserRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.User;
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
public class UserServiceImpl implements UserService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public Optional<UserEntity> findByName(String username) {
        try {
            LOGGER.debug("Find user by name: {}", username);
            return userRepository.findByUsername(username).map(UserServiceImpl::convert);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a user using its name {}", username, ex);
            throw new TechnicalManagementException("An error occurs while trying to find a user using its name " + username, ex);
        }
    }

    @Override
    public UserEntity create(NewUserEntity newUserEntity) throws UsernameAlreadyExistsException {
        try {
            LOGGER.debug("Create {}", newUserEntity);
            Optional<UserEntity> checkUser = findByName(newUserEntity.getUsername());
            if (checkUser.isPresent()) {
                throw new UsernameAlreadyExistsException(newUserEntity.getUsername());
            }

            checkUser = userRepository.findByEmail(newUserEntity.getEmail()).map(UserServiceImpl::convert);
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
        User user = new User();

        user.setUsername(newUserEntity.getUsername());
        user.setEmail(newUserEntity.getEmail());

        return user;
    }

    private static UserEntity convert(User user) {
        UserEntity userEntity = new UserEntity();

        userEntity.setUsername(user.getUsername());
        userEntity.setMail(user.getEmail());
        userEntity.setCreatedAt(user.getCreatedAt());
        userEntity.setUpdatedAt(user.getUpdatedAt());

        return userEntity;
    }
}
