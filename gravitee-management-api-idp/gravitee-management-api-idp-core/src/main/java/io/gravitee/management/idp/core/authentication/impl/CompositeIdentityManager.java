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
import io.gravitee.management.idp.api.identity.IdentityReference;
import io.gravitee.management.idp.api.identity.SearchableUser;
import io.gravitee.management.idp.api.identity.User;
import io.gravitee.management.idp.core.authentication.IdentityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CompositeIdentityManager implements IdentityManager {

    private final Logger LOGGER = LoggerFactory.getLogger(CompositeIdentityManager.class);

    @Autowired
    private ReferenceSerializer referenceSerializer;

    private Collection<IdentityLookup> identityLookups = new ArrayList<>();

    @Override
    public Optional<User> lookup(final String reference) {
        LOGGER.debug("Looking for a user: reference[{}]", reference);
        try {
            IdentityReference identityReference = referenceSerializer.deserialize(reference);
            LOGGER.debug("Lookup identity information from reference: source[{}] id[{}]",
                    identityReference.getSource(), identityReference.getReference());
            for (final IdentityLookup identityLookup : identityLookups) {
                if (identityLookup.canHandle(identityReference)) {
                    final User user = identityLookup.retrieve(identityReference);
                    if (user != null) {
                        return of(user);
                    }
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Unable to extract IDP: token[" + reference + "]", ex);
        }
        return empty();
    }

    @Override
    public Collection<SearchableUser> search(String query) {
        Set<SearchableUser> users = new HashSet<>();
        for (IdentityLookup identityLookup : identityLookups) {
            Collection<User> lookupUsers = identityLookup.search(query);
            if (lookupUsers != null) {
                users.addAll(lookupUsers
                        .stream()
                        .map((Function<User, SearchableUser>) DefaultSearchableUser::new)
                        .collect(Collectors.toSet()));
            }
        }

        return users;
    }

    public void addIdentityLookup(IdentityLookup identityLookup) {
        if (identityLookup != null) {
            identityLookups.add(identityLookup);
        }
    }

    private class DefaultSearchableUser implements SearchableUser {
        private final User user;

        DefaultSearchableUser(User user) {
            this.user = user;
        }

        @Override
        public String getReference() {
            try {
                return referenceSerializer.serialize(new IdentityReference(user.getSource(), user.getReference()));
            } catch (Exception ex) {
                LOGGER.error("An error occurs while serializing user reference", ex);
                return null;
            }
        }

        @Override
        public String getId() {
            return user.getId();
        }

        @Override
        public String getDisplayName() {
            return user.getDisplayName();
        }

        @Override
        public String getFirstname() {
            return user.getFirstname();
        }

        @Override
        public String getLastname() {
            return user.getLastname();
        }
    }
}
