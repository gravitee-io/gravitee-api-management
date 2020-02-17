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
package io.gravitee.management.idp.memory.authentication;

import io.gravitee.management.idp.api.authentication.AuthenticationProvider;
import io.gravitee.management.idp.memory.InMemoryIdentityProvider;
import io.gravitee.management.idp.memory.authentication.spring.InMemoryAuthenticationProviderConfiguration;
import io.gravitee.management.idp.memory.authentication.spring.InMemoryGraviteeUserDetailsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * @author David Brassely (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Import(InMemoryAuthenticationProviderConfiguration.class)
public class InMemoryAuthentificationProvider extends AbstractUserDetailsAuthenticationProvider
        implements AuthenticationProvider<org.springframework.security.authentication.AuthenticationProvider> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryAuthentificationProvider.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InMemoryGraviteeUserDetailsManager userDetailsService;

    @Autowired
    private Environment environment;

    @Override
    public org.springframework.security.authentication.AuthenticationProvider
        configure() throws Exception {

        boolean found = true;
        int userIdx = 0;

        while (found) {
            String user = environment.getProperty("users[" + userIdx + "].user");
            found = (user != null && user.isEmpty());

            if (found) {
                String username = environment.getProperty("users[" + userIdx + "].username");
                String firstname = environment.getProperty("users[" + userIdx + "].firstname");
                String lastname = environment.getProperty("users[" + userIdx + "].lastname");
                String password = environment.getProperty("users[" + userIdx + "].password");
                String email = environment.getProperty("users[" + userIdx + "].email");
                String roles = environment.getProperty("users[" + userIdx + "].roles");
                List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(roles);
                userIdx++;

                io.gravitee.management.idp.api.authentication.UserDetails newUser = new io.gravitee.management.idp.api.authentication.UserDetails(username, password, email, authorities);

                newUser.setSource(InMemoryIdentityProvider.PROVIDER_TYPE);
                newUser.setSourceId(username);
                newUser.setFirstname(firstname);
                newUser.setLastname(lastname);
                LOGGER.debug("Add an in-memory user: {}", newUser);
                userDetailsService.createUser(newUser);
            }
        }

        return this;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            LOGGER.debug("Authentication failed: no credentials provided");
            throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }

        String presentedPassword = authentication.getCredentials().toString();

        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            LOGGER.debug("Authentication failed: password does not match stored value");
            throw new BadCredentialsException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        return userDetailsService.loadUserByUsername(username);
    }
}
