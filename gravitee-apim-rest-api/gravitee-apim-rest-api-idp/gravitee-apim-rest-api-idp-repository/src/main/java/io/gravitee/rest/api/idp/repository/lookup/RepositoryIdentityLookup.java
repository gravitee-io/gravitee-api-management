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
package io.gravitee.rest.api.idp.repository.lookup;

import io.gravitee.rest.api.idp.api.identity.IdentityLookup;
import io.gravitee.rest.api.idp.api.identity.IdentityReference;
import io.gravitee.rest.api.idp.api.identity.User;
import io.gravitee.rest.api.idp.repository.RepositoryIdentityProvider;
import io.gravitee.rest.api.idp.repository.lookup.spring.RepositoryIdentityLookupConfiguration;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Import(RepositoryIdentityLookupConfiguration.class)
public class RepositoryIdentityLookup implements IdentityLookup {

    @Autowired
    private UserService userService;

    @Autowired
    private Environment environment;

    @Override
    public io.gravitee.rest.api.idp.api.identity.User retrieve(IdentityReference identityReference) {
        try {
            return new RepositoryUser(
                userService.findBySource(
                    GraviteeContext.getCurrentOrganization(),
                    identityReference.getSource(),
                    identityReference.getReference(),
                    false
                )
            );
        } catch (UserNotFoundException te) {
            log.error("Unexpected error while looking for a user with id {}", identityReference.getReference(), te);
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
    public boolean allowEmailInSearchResults() {
        Boolean allow = environment.getProperty("allow-email-in-search-results", Boolean.class);
        return allow != null && allow;
    }

    @Override
    public Collection<User> search(String query) {
        return userService
            .search(GraviteeContext.getExecutionContext(), query, new PageableImpl(1, 20))
            .getContent()
            .stream()
            .map(RepositoryUser::new)
            .collect(Collectors.toList());
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
