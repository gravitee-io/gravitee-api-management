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
package io.gravitee.rest.api.idp.core.authentication.impl;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import io.gravitee.rest.api.idp.api.identity.IdentityLookup;
import io.gravitee.rest.api.idp.api.identity.IdentityReference;
import io.gravitee.rest.api.idp.api.identity.SearchableUser;
import io.gravitee.rest.api.idp.api.identity.User;
import io.gravitee.rest.api.idp.core.authentication.IdentityManager;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class CompositeIdentityManager implements IdentityManager {

    @Autowired
    private ReferenceSerializer referenceSerializer;

    private List<IdentityLookup> identityLookups = new ArrayList<>();

    @Override
    public Optional<User> lookup(final String reference) {
        log.debug("Looking for a user: reference[{}]", reference);
        try {
            IdentityReference identityReference = referenceSerializer.deserialize(reference);
            log.debug(
                "Lookup identity information from reference: source[{}] id[{}]",
                identityReference.getSource(),
                identityReference.getReference()
            );
            for (final IdentityLookup identityLookup : identityLookups) {
                if (identityLookup.canHandle(identityReference)) {
                    final User user = identityLookup.retrieve(identityReference);
                    if (user != null) {
                        return of(user);
                    }
                }
            }
        } catch (final Exception ex) {
            log.error("Unable to extract IDP: token[" + reference + "]", ex);
        }
        return empty();
    }

    @Override
    public Collection<SearchableUser> search(String query) {
        Set<SearchableUser> users = new HashSet<>();
        for (IdentityLookup identityLookup : identityLookups) {
            if (identityLookup.searchable()) {
                Collection<User> lookupUsers = identityLookup.search(query);
                if (lookupUsers != null) {
                    boolean allowEmailInSearchResults = identityLookup.allowEmailInSearchResults();
                    users.addAll(
                        lookupUsers
                            .stream()
                            .map(user -> new DefaultSearchableUser(user, allowEmailInSearchResults))
                            .collect(Collectors.toSet())
                    );
                }
            }
        }

        return users;
    }

    public void addIdentityLookup(IdentityLookup identityLookup) {
        if (identityLookup != null) {
            identityLookups.add(identityLookup);
            Collections.sort(identityLookups);
        }
    }

    private class DefaultSearchableUser implements SearchableUser {

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + identityReference.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            DefaultSearchableUser other = (DefaultSearchableUser) obj;
            return identityReference.equals(other.identityReference);
        }

        private final User user;
        private final boolean allowEmail;
        private final IdentityReference identityReference;

        DefaultSearchableUser(User user, boolean allowEmail) {
            this.user = user;
            this.allowEmail = allowEmail;
            this.identityReference = new IdentityReference(user.getSource(), user.getReference());
        }

        @Override
        public String getReference() {
            try {
                return referenceSerializer.serialize(new IdentityReference(user.getSource(), user.getReference()));
            } catch (Exception ex) {
                log.error("An error occurs while serializing user reference", ex);
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

        @Override
        public String getEmail() {
            if (this.allowEmail) {
                return user.getEmail();
            } else {
                return null;
            }
        }

        @Override
        public String getPicture() {
            return user.getPicture();
        }
    }
}
