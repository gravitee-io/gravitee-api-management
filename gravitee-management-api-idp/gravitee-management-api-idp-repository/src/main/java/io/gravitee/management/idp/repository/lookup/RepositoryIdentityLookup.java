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
import io.gravitee.management.idp.api.identity.IdentityReference;
import io.gravitee.management.idp.api.identity.User;
import io.gravitee.management.idp.repository.RepositoryIdentityProvider;
import io.gravitee.management.idp.repository.lookup.spring.RepositoryIdentityLookupConfiguration;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.common.PageableImpl;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Import(RepositoryIdentityLookupConfiguration.class)
public class RepositoryIdentityLookup implements IdentityLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryIdentityLookup.class);

    @Autowired
    private UserService userService;

    @Override
    public io.gravitee.management.idp.api.identity.User retrieve(IdentityReference identityReference) {
        try {
            return new RepositoryUser(userService.findBySource(
                    identityReference.getSource(),
                    identityReference.getReference(),
                    false));
        } catch (UserNotFoundException te) {
            LOGGER.error("Unexpected error while looking for a user with id " + identityReference.getReference(), te);
        }
        return null;
    }

    @Override
    public boolean canHandle(IdentityReference identityReference) {
        return RepositoryIdentityProvider.PROVIDER_TYPE.equalsIgnoreCase(identityReference.getSource());
    }

    @Override
    public boolean searchable() {
        return true;
    }

    @Override
    public Collection<User> search(String query) {
        return userService
                .search(query, new PageableImpl(1, 20))
                .getContent()
                .stream()
                .filter(userEntity -> !userEntity.getSource().equalsIgnoreCase("ldap")
                        && !userEntity.getSource().equalsIgnoreCase("memory"))
                .map((Function<UserEntity, User>) RepositoryUser::new)
                .collect(Collectors.toList());
    }
}
