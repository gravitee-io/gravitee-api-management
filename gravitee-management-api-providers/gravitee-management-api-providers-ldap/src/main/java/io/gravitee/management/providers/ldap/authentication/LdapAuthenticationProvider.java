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
package io.gravitee.management.providers.ldap.authentication;

import io.gravitee.management.providers.core.authentication.AuthenticationManager;
import io.gravitee.management.providers.ldap.LdapProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class LdapAuthenticationProvider implements AuthenticationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapAuthenticationProvider.class);

    @Autowired
    private Environment environment;

    @Autowired
    private LdapContextSource ldapContextSource;

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder, int providerIdx) throws Exception {
        LOGGER.info("Configuring LDAP provider []", providerIdx);
        LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapAuthenticationProviderConfigurer =
                authenticationManagerBuilder.ldapAuthentication();

        ldapAuthenticationProviderConfigurer.userDnPatterns(environment.getProperty("security.providers[" + providerIdx + "].user-dn-patterns","uid={0},ou=people"));
        ldapAuthenticationProviderConfigurer.groupSearchBase(environment.getProperty("security.providers[" + providerIdx + "].group-search-base","ou=groups"));
        ldapAuthenticationProviderConfigurer.contextSource(ldapContextSource);

        // set up roles mapper
        if (environment.getProperty("security.providers[" + providerIdx + "].role-mapping", boolean.class, false)) {
            UserDetailsContextPropertiesMapper userDetailsContextPropertiesMapper = new UserDetailsContextPropertiesMapper();
            userDetailsContextPropertiesMapper.setAuthenticationProviderId(providerIdx);
            userDetailsContextPropertiesMapper.setEnvironment(environment);
            ldapAuthenticationProviderConfigurer.userDetailsContextMapper(userDetailsContextPropertiesMapper);
        }
    }

    @Override
    public boolean canHandle(String type) throws Exception {
        return (type != null && type.equalsIgnoreCase(LdapProvider.PROVIDER_TYPE));
    }
}
