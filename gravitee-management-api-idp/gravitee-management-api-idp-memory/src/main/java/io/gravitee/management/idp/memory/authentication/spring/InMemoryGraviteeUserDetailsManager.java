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
package io.gravitee.management.idp.memory.authentication.spring;

import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.idp.memory.InMemoryIdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.core.userdetails.memory.UserAttributeEditor;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InMemoryGraviteeUserDetailsManager implements UserDetailsManager,
        UserDetailsPasswordService {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryGraviteeUserDetailsManager.class);

    private final Map<String, UserDetails> users = new HashMap<>();

    private AuthenticationManager authenticationManager;

    public InMemoryGraviteeUserDetailsManager() {
    }

    public InMemoryGraviteeUserDetailsManager(Collection<UserDetails> users) {
        for (UserDetails user : users) {
            createUser(user);
        }
    }

    public InMemoryGraviteeUserDetailsManager(UserDetails... users) {
        for (UserDetails user : users) {
            createUser(user);
        }
    }

    public InMemoryGraviteeUserDetailsManager(Properties users) {
        Enumeration<?> names = users.propertyNames();
        UserAttributeEditor editor = new UserAttributeEditor();

        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            editor.setAsText(users.getProperty(name));
            UserAttribute attr = (UserAttribute) editor.getValue();
            UserDetails user = new UserDetails(name, attr.getPassword(), attr.getAuthorities());
            createUser(user);
        }
    }

    @Override
    public void createUser(org.springframework.security.core.userdetails.UserDetails user) {
        Assert.isTrue(!userExists(user.getUsername()), "user should not exist");
        users.put(user.getUsername().toLowerCase(), convert(user));
    }

    @Override
    public void deleteUser(String username) {
        users.remove(username.toLowerCase());
    }

    @Override
    public void updateUser(org.springframework.security.core.userdetails.UserDetails user) {
        Assert.isTrue(userExists(user.getUsername()), "user should exist");

        users.put(user.getUsername().toLowerCase(), convert(user));
    }

    @Override
    public boolean userExists(String username) {
        return users.containsKey(username.toLowerCase());
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserDetails updatePassword(org.springframework.security.core.userdetails.UserDetails user, String newPassword) {
        throw new UnsupportedOperationException();
    }

    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        UserDetails user = users.get(username.toLowerCase());

        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        //return a copy of the user 'cause spring will erase credentials in the security process
        UserDetails userDetails = new UserDetails(user.getUsername(), user.getPassword(), user.getEmail(), user.getAuthorities());
        userDetails.setSource(InMemoryIdentityProvider.PROVIDER_TYPE);
        userDetails.setSourceId(user.getUsername());
        userDetails.setFirstname(user.getFirstname());
        userDetails.setLastname(user.getLastname());

        return userDetails;
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    private UserDetails convert(org.springframework.security.core.userdetails.UserDetails user) {
        if (user instanceof UserDetails) {
            return (UserDetails) user;
        }

        UserDetails userDetails = new UserDetails(user.getUsername(), user.getPassword(), user.getAuthorities());
        userDetails.setSource(InMemoryIdentityProvider.PROVIDER_TYPE);
        userDetails.setSourceId(user.getUsername());

        if (user instanceof io.gravitee.management.idp.api.authentication.UserDetails) {
            userDetails.setFirstname(((io.gravitee.management.idp.api.authentication.UserDetails) user).getFirstname());
            userDetails.setLastname(((io.gravitee.management.idp.api.authentication.UserDetails) user).getLastname());
        }

        return userDetails;
    }
}
