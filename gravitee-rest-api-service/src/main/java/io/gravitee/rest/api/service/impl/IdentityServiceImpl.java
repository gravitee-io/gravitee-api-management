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
package io.gravitee.rest.api.service.impl;

import io.gravitee.rest.api.idp.api.identity.SearchableUser;
import io.gravitee.rest.api.idp.core.authentication.IdentityManager;
import io.gravitee.rest.api.model.providers.User;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class IdentityServiceImpl implements IdentityService {

    @Autowired
    private IdentityManager identityManager;

    @Autowired
    private UserService userService;

    @Override
    public Collection<SearchableUser> search(String query) {
        return identityManager.search(query);
    }

    @Override
    public Optional<User> findByReference(String reference) {
        Optional<io.gravitee.rest.api.idp.api.identity.User> optUser = identityManager.lookup(reference);
        return optUser.flatMap(user -> Optional.of(convert(user)));
    }

    private User convert(io.gravitee.rest.api.idp.api.identity.User identity) {
        User user = new User();
        user.setId(identity.getId());
        user.setSourceId(identity.getReference());
        user.setSource(identity.getSource());
        user.setEmail(identity.getEmail());
        user.setFirstname(identity.getFirstname());
        user.setLastname(identity.getLastname());
        user.setDisplayName(identity.getDisplayName());
        return user;
    }
}
