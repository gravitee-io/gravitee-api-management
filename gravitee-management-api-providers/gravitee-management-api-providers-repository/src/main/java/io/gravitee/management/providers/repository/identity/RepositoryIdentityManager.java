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
package io.gravitee.management.providers.repository.identity;

import io.gravitee.management.providers.core.identity.IdentityManager;
import io.gravitee.management.providers.core.identity.User;
import io.gravitee.management.providers.repository.RepositoryProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RepositoryIdentityManager implements IdentityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryIdentityManager.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public Collection<User> search(String query) {
        try {
            return userRepository.findAll().stream().map(this::convert).collect(Collectors.toSet());
        } catch (TechnicalException te) {
            LOGGER.error("Unexpected error while searching for users in repository", te);
            return null;
        }
    }

    private User convert(io.gravitee.repository.management.model.User identity) {
        User user = new User();
        user.setEmail(identity.getEmail());
        user.setFirstname(identity.getFirstname());
        user.setLastname(identity.getLastname());
        user.setId(identity.getUsername());
        user.setProvider(RepositoryProvider.PROVIDER_TYPE);
        return user;
    }
}
