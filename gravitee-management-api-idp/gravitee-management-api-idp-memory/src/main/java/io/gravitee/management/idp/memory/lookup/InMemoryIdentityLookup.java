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
package io.gravitee.management.idp.memory.lookup;

import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.idp.api.identity.IdentityLookup;
import io.gravitee.management.idp.api.identity.IdentityReference;
import io.gravitee.management.idp.api.identity.User;
import io.gravitee.management.idp.memory.InMemoryIdentityProvider;
import io.gravitee.management.idp.memory.authentication.spring.InMemoryGraviteeUserDetailsManager;
import io.gravitee.management.idp.memory.lookup.spring.InMemoryLookupConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David Brassely (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Import(InMemoryLookupConfiguration.class)
public class InMemoryIdentityLookup implements IdentityLookup, InitializingBean {

    @Autowired
    private InMemoryGraviteeUserDetailsManager userDetailsService;

    @Autowired
    private Environment environment;

    private Set<String> users;

    @Override
    public User retrieve(IdentityReference identityReference) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(identityReference.getReference());
            return convert(userDetails);
        } catch (UsernameNotFoundException unnfe) {
            return null;
        }
    }

    @Override
    public boolean canHandle(IdentityReference identityReference) {
        return InMemoryIdentityProvider.PROVIDER_TYPE.equalsIgnoreCase(identityReference.getSource());
    }

    @Override
    public Collection<User> search(String query) {
        return users.stream()
                .filter(username -> username.contains(query))
                .map(username -> convert(userDetailsService.loadUserByUsername(username)))
                .collect(Collectors.toList());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        boolean found = true;
        int userIdx = 0;

        while (found) {
            String user = environment.getProperty("users[" + userIdx + "].user");
            found = (user != null && user.isEmpty());

            if (found) {
                String username = environment.getProperty("users[" + userIdx + "].username");
                String password = environment.getProperty("users[" + userIdx + "].password");
                String email = environment.getProperty("users[" + userIdx + "].email");
                String firstname = environment.getProperty("users[" + userIdx + "].firstname");
                String lastname = environment.getProperty("users[" + userIdx + "].lastname");
                String roles = environment.getProperty("users[" + userIdx + "].roles");
                List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(roles);
                userIdx++;

                io.gravitee.management.idp.api.authentication.UserDetails newUser = new io.gravitee.management.idp.api.authentication.UserDetails(username, password, email, authorities);

                newUser.setSource(InMemoryIdentityProvider.PROVIDER_TYPE);
                newUser.setSourceId(username);
                newUser.setFirstname(firstname);
                newUser.setLastname(lastname);
                userDetailsService.createUser(newUser);
            }
        }

        // Get a reference to stored users
        Field fieldUser = userDetailsService.getClass().getDeclaredField("users");
        boolean accessible = fieldUser.isAccessible();
        fieldUser.setAccessible(true);
        users = (Set<String>) ((Map) fieldUser.get(userDetailsService)).keySet();
        fieldUser.setAccessible(accessible);
    }

    @Override
    public boolean allowEmailInSearchResults() {
        return environment.getProperty("allow-email-in-search-results",Boolean.class, false);
    }

    private User convert(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        InMemoryUser user = new InMemoryUser(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFirstname(userDetails.getFirstname());
        user.setLastname(userDetails.getLastname());
        return user;
    }
}
