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
package io.gravitee.management.security.provider.memory;

import io.gravitee.management.security.provider.AuthenticationProvider;
import io.gravitee.management.security.provider.AuthenticationProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class InMemoryAuthentificationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryAuthentificationProvider.class);

    @Autowired
    private Environment environment;

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder, int providerIdx) throws Exception {
        boolean found = true;
        int userIdx = 0;

        while (found) {
            String user = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].user");
            found = (user != null);

            if (found) {
                String username = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].username");
                String password = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].password");
                String roles = environment.getProperty("security.providers[" + providerIdx + "].users[" + userIdx + "].roles");
                LOGGER.debug("Adding an in-memory user for username {}", username);
                userIdx++;
                authenticationManagerBuilder.inMemoryAuthentication().withUser(username).password(password).roles(roles);
            }
        }
    }

    @Override
    public AuthenticationProviderType type() {
        return AuthenticationProviderType.MEMORY;
    }
}
