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
package io.gravitee.management.providers.memory.authentication;

import io.gravitee.management.providers.core.authentication.AuthenticationManager;
import io.gravitee.management.providers.core.authentication.GraviteeUserDetails;
import io.gravitee.management.providers.memory.InMemoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class InMemoryAuthentificationProvider extends AbstractUserDetailsAuthenticationProvider implements AuthenticationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryAuthentificationProvider.class);

    @Autowired
    private Environment environment;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InMemoryUserDetailsManager userDetailsService;

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder, int providerIdx) throws Exception {
        boolean found = true;
        int userIdx = 0;

        while (found) {
            String user = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].user");
            found = (user != null && user.isEmpty());

            if (found) {
                String username = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].username");
                String password = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].password");
                String roles = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].roles");
                List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(roles);
                LOGGER.debug("Adding an in-memory user for username {}", username);
                userIdx++;

                userDetailsService.createUser(new User(username, password, authorities));
            }
        }

        authenticationManagerBuilder.authenticationProvider(this);
    }

    @Override
    public boolean canHandle(String type) throws Exception {
        return (type != null && type.equalsIgnoreCase(InMemoryProvider.PROVIDER_TYPE));
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
    protected GraviteeUserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new GraviteeUserDetails(userDetails.getUsername(), userDetails.getPassword(), userDetails.getAuthorities());
    }
}
