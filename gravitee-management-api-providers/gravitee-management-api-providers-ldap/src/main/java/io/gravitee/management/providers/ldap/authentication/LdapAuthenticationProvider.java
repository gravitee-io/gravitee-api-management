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
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class LdapAuthenticationProvider implements AuthenticationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapAuthenticationProvider.class);

    @Autowired
    private Environment environment;

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder, int providerIdx) throws Exception {
        LOGGER.info("Configuring LDAP provider []", providerIdx);
        LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapAuthenticationProviderConfigurer =
                authenticationManagerBuilder.ldapAuthentication();

        // Create LDAP context
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(
                environment.getProperty("security.providers[" + providerIdx + "].context-source-url"));
        contextSource.setBase(environment.getProperty("security.providers[" + providerIdx + "].context-source-base"));
        contextSource.setUserDn(environment.getProperty("security.providers[" + providerIdx + "].context-source-username"));
        contextSource.setPassword(environment.getProperty("security.providers[" + providerIdx + "].context-source-password"));
        contextSource.afterPropertiesSet();

        String userDNPattern = environment.getProperty("security.providers[" + providerIdx + "].user-dn-pattern");
        if (userDNPattern == null || userDNPattern.isEmpty()) {
            ldapAuthenticationProviderConfigurer
                    .userSearchBase(environment.getProperty("security.providers[" + providerIdx + "].user-search-base"))
                    .userSearchFilter(environment.getProperty("security.providers[" + providerIdx + "].user-search-filter"));
        } else {
            ldapAuthenticationProviderConfigurer.userDnPatterns(userDNPattern);
        }

        ldapAuthenticationProviderConfigurer
                .groupSearchBase(environment.getProperty("security.providers[" + providerIdx + "].group-search-base", ""))
                .groupSearchFilter(environment.getProperty("security.providers[" + providerIdx + "].group-search-filter", "(uniqueMember={0})"))
                .groupRoleAttribute(environment.getProperty("security.providers[" + providerIdx + "].group-role-attribute", "cn"))
                .rolePrefix("");

        DefaultLdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(contextSource,
                environment.getProperty("security.providers[" + providerIdx + "].group-search-base", ""));
        populator.setRolePrefix("");

        ldapAuthenticationProviderConfigurer.ldapAuthoritiesPopulator(populator).contextSource(contextSource);

        // set up roles mapper
        if (environment.getProperty("security.providers[" + providerIdx + "].role-mapping", Boolean.class, false)) {
            UserDetailsContextPropertiesMapper userDetailsContextPropertiesMapper = new UserDetailsContextPropertiesMapper();
            userDetailsContextPropertiesMapper.setAuthenticationProviderId(providerIdx);
            userDetailsContextPropertiesMapper.setEnvironment(environment);
            ldapAuthenticationProviderConfigurer.userDetailsContextMapper(userDetailsContextPropertiesMapper);
        }
    }

    @Override
    public boolean canHandle(String type) throws Exception {
        return LdapProvider.PROVIDER_TYPE.equalsIgnoreCase(type);
    }
}
