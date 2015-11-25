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

import io.gravitee.management.model.providers.User;
import io.gravitee.management.providers.core.identity.IdentityManager;
import io.gravitee.management.service.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class IdentityServiceImpl implements IdentityService {

    @Autowired
    private Collection<IdentityManager> identityManagers;

    @Override
    public Collection<User> search(String query) {
        Set<User> users = new HashSet<>();
        for (IdentityManager identityManager : identityManagers) {
            Collection<io.gravitee.management.providers.core.identity.User> lookupUsers = identityManager.search(query);
            if (lookupUsers != null) {
                users.addAll(lookupUsers.stream().map(this::convert).collect(Collectors.toSet()));
            }
        }

        return users;
    }

    private User convert(io.gravitee.management.providers.core.identity.User identity) {
        User user = new User();
        user.setEmail(identity.getEmail());
        user.setFirstname(identity.getFirstname());
        user.setLastname(identity.getLastname());
        user.setId(identity.getId());
        user.setProvider(identity.getProvider());
        return user;
    }
}
