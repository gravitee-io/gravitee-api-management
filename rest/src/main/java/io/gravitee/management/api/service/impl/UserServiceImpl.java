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
package io.gravitee.management.api.service.impl;

import io.gravitee.management.api.exceptions.UsernameAlreadyExistsException;
import io.gravitee.management.api.model.NewUserEntity;
import io.gravitee.management.api.model.UserEntity;
import io.gravitee.management.api.service.UserService;
import io.gravitee.repository.api.UserRepository;
import io.gravitee.repository.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Optional<UserEntity> findByName(String username) {
        return userRepository.findByUsername(username).map(user -> convert(user));
    }

    @Override
    public UserEntity create(NewUserEntity newUserEntity) throws UsernameAlreadyExistsException {
        Optional<UserEntity> checkUser = findByName(newUserEntity.getUsername());
        if (checkUser.isPresent()) {
            throw new UsernameAlreadyExistsException(newUserEntity.getUsername());
        }

        checkUser = userRepository.findByEmail(newUserEntity.getEmail()).map(user -> convert(user));
        if (checkUser.isPresent()) {
            throw new UsernameAlreadyExistsException(newUserEntity.getUsername());
        }

        User createdUser = userRepository.create(convert(newUserEntity));
        return convert(createdUser);
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
