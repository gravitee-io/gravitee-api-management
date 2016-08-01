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
package io.gravitee.management.idp.core.authentication.impl;

import io.gravitee.management.idp.api.identity.IdentityLookup;
import io.gravitee.management.idp.api.identity.User;
import io.gravitee.management.idp.core.authentication.IdentityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class CompositeIdentityManager implements IdentityManager {

    private Collection<IdentityLookup> identityLookups = new ArrayList<>();

    @Override
    public User retrieve(Object id) {
        for (IdentityLookup identityLookup : identityLookups) {
            User user = identityLookup.retrieve(id);
            if (user != null) {
                return user;
            }
        }

        return null;
    }

    @Override
    public Collection<User> search(String query) {
        Set<User> users = new HashSet<>();
        for (IdentityLookup identityLookup : identityLookups) {
            Collection<User> lookupUsers = identityLookup.search(query);
            if (lookupUsers != null) {
                users.addAll(lookupUsers.stream().collect(Collectors.toSet()));
            }
        }

        return users;
    }

    public void addIdentityLookup(IdentityLookup identityLookup) {
        if (identityLookup != null) {
            identityLookups.add(identityLookup);
        }
    }
}
