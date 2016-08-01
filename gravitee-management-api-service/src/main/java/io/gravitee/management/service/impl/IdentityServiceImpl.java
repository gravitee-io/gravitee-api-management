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

import io.gravitee.management.idp.core.authentication.IdentityManager;
import io.gravitee.management.model.providers.User;
import io.gravitee.management.service.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class IdentityServiceImpl implements IdentityService {

    @Autowired
    private IdentityManager identityManager;

    @Override
    public Collection<User> search(String query) {
        return identityManager.search(query).stream().map(this::convert).collect(Collectors.toSet());
    }

    @Override
    public User findOne(String id) {
        io.gravitee.management.idp.api.identity.User user = identityManager.retrieve(id);
        return (user != null) ? convert(user) : null;
    }

    private User convert(io.gravitee.management.idp.api.identity.User identity) {
        User user = new User();
        user.setEmail(identity.getEmail());
        user.setFirstname(identity.getFirstname());
        user.setLastname(identity.getLastname());
        user.setId((String) identity.getUsername());
        user.setSource(identity.getSource());
        user.setSourceId((String) identity.getInternalId());
        return user;
    }
}
