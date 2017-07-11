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
package io.gravitee.management.idp.repository.lookup;

import io.gravitee.management.idp.api.identity.IdentityLookup;
import io.gravitee.management.idp.api.identity.User;
import io.gravitee.management.idp.repository.RepositoryIdentityProvider;
import io.gravitee.management.idp.repository.lookup.spring.RepositoryIdentityLookupConfiguration;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@Import(RepositoryIdentityLookupConfiguration.class)
public class RepositoryIdentityLookup implements IdentityLookup<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryIdentityLookup.class);

    public final static Set<String> MANAGED_USER_TYPES = new HashSet<>();

    static {
        MANAGED_USER_TYPES.add(RepositoryIdentityProvider.PROVIDER_TYPE);
        MANAGED_USER_TYPES.add("oauth2");
        MANAGED_USER_TYPES.add("google");
        MANAGED_USER_TYPES.add("github");
    }

    @Autowired
    private UserRepository userRepository;

    @Override
    public io.gravitee.management.idp.api.identity.User retrieve(String id) {
        try {
            Optional<io.gravitee.repository.management.model.User> optUser = userRepository.findByUsername(id);

            if (optUser.isPresent()) {
                return convert(optUser.get());
            }
        } catch (TechnicalException te) {
            LOGGER.error("Unexpected error while looking for a user with id " + id, te);
        }
        return null;
    }

    @Override
    public Collection<User> search(String query) {
        try {
            return userRepository.findAll().stream().filter(user -> MANAGED_USER_TYPES.contains(user.getSource())).filter(
                    user -> (user.getUsername() != null && StringUtils.containsIgnoreCase(user.getUsername(), query)) ||
                            (user.getFirstname() != null && StringUtils.containsIgnoreCase(user.getFirstname(), query)) ||
                            (user.getLastname() != null && StringUtils.containsIgnoreCase(user.getLastname(), query)) ||
                            (user.getEmail() != null && StringUtils.containsIgnoreCase(user.getEmail(), query))
            ).map(this::convert).collect(Collectors.toSet());
        } catch (TechnicalException te) {
            LOGGER.error("Unexpected error while searching for users in repository", te);
            return null;
        }
    }

    private User convert(io.gravitee.repository.management.model.User identity) {
        RepositoryUser user = new RepositoryUser(identity.getUsername());
        user.setEmail(identity.getEmail());
        user.setFirstname(identity.getFirstname());
        user.setLastname(identity.getLastname());
        return user;
    }
}
