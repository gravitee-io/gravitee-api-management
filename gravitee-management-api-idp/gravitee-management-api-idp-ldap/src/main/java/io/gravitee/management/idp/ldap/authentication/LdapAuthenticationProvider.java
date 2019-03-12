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
package io.gravitee.management.idp.ldap.authentication;

import io.gravitee.management.idp.api.authentication.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class LdapAuthenticationProvider implements AuthenticationProvider<SecurityConfigurer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapAuthenticationProvider.class);

    @Autowired
    private Environment environment;
    
    @Override
    public SecurityConfigurer configure() throws Exception {
        LOGGER.info("Configuring an LDAP Identity Provider");

        LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapAuthenticationProviderConfigurer =
                new LdapAuthenticationProviderConfigurer<>();

        // Create LDAP context
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(
                environment.getProperty("context.url"));
        contextSource.setBase(environment.getProperty("context.base"));
        contextSource.setUserDn(environment.getProperty("context.username"));
        contextSource.setPassword(environment.getProperty("context.password"));
        contextSource.afterPropertiesSet();

        ldapAuthenticationProviderConfigurer
                .userSearchBase(environment.getProperty("authentication.user.base", ""))
                .userSearchFilter(environment.getProperty("authentication.user.filter"))
                .groupSearchBase(environment.getProperty("authentication.group.base", ""))
                .groupSearchFilter(environment.getProperty("authentication.group.filter", "(uniqueMember={0})"))
                .groupRoleAttribute(environment.getProperty("authentication.group.role.attribute", "cn"))
                .rolePrefix("");

        DefaultLdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(contextSource,
                environment.getProperty("authentication.group.base", ""));
        populator.setRolePrefix("");
        populator.setGroupRoleAttribute(environment.getProperty("authentication.group.role.attribute", "cn"));
        populator.setGroupSearchFilter(environment.getProperty("authentication.group.filter", "(uniqueMember={0})"));

        ldapAuthenticationProviderConfigurer.ldapAuthoritiesPopulator(populator).contextSource(contextSource);

        // set up LDAP mapper
        UserDetailsContextPropertiesMapper userDetailsContextPropertiesMapper = new UserDetailsContextPropertiesMapper();
        userDetailsContextPropertiesMapper.setEnvironment(environment);
        userDetailsContextPropertiesMapper.afterPropertiesSet();
        ldapAuthenticationProviderConfigurer.userDetailsContextMapper(userDetailsContextPropertiesMapper);

        return ldapAuthenticationProviderConfigurer;
    }
}
