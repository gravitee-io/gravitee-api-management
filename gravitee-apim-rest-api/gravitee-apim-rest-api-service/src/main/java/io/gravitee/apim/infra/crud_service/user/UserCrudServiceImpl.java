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
package io.gravitee.apim.infra.crud_service.user;

import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.crud_service.user.adapter.UserAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserCrudServiceImpl implements UserCrudService {

    private final UserRepository userRepository;

    public UserCrudServiceImpl(@Lazy UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<BaseUserEntity> findBaseUserById(String id) {
        try {
            log.debug("Find user [userId={}]", id);
            Optional<User> optionalUser = userRepository.findById(id);
            return optionalUser.map(UserAdapter::toBaseUserEntity);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find user using [userId={}]", id, ex);
            throw new TechnicalManagementException(ex);
        }
    }

    @Override
    public Set<BaseUserEntity> findBaseUsersByIds(List<String> userIds) {
        try {
            log.debug("Find users [userIds={}]", userIds);
            return userRepository.findByIds(userIds).stream().map(UserAdapter::toBaseUserEntity).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find user using [userIds={}]", userIds, ex);
            throw new TechnicalManagementException(ex);
        }
    }

    @Override
    public BaseUserEntity getBaseUser(String id) {
        try {
            log.debug("Find user [userId={}]", id);
            Optional<User> optionalUser = userRepository.findById(id);
            return optionalUser.map(UserAdapter::toBaseUserEntity).orElseThrow(() -> new UserNotFoundException(id));
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to get user using [userId={}]", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to get user " + id, ex);
        }
    }
}
