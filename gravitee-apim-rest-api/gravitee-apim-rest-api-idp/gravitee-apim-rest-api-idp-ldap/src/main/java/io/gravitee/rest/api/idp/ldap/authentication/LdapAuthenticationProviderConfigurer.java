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
package io.gravitee.rest.api.idp.ldap.authentication;

import java.io.IOException;
import java.net.ServerSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.ProviderManagerBuilder;
import org.springframework.security.config.annotation.web.configurers.ChannelSecurityConfigurer;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.*;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.server.ApacheDSContainer;
import org.springframework.security.ldap.userdetails.*;
import org.springframework.util.Assert;

/**
 * NOTE: This is a copy of org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer
 * to avoid classloader collision.
 *
 * Configures LDAP {@link AuthenticationProvider} in the {@link ProviderManagerBuilder}.
 *
 * @param <B> the {@link ProviderManagerBuilder} type that this is configuring.
 *
 * @author Rob Winch
 * @since 3.2
 */
@Slf4j
public class LdapAuthenticationProviderConfigurer<B extends ProviderManagerBuilder<B>>
    extends SecurityConfigurerAdapter<AuthenticationManager, B> {

    private String groupRoleAttribute = "cn";
    private String groupSearchBase = "";
    private String groupSearchFilter = "(uniqueMember={0})";
    private String rolePrefix = "ROLE_";
    private String userSearchBase = ""; // only for search
    private String userSearchFilter = null; // "uid={0}"; // only for search
    private String[] userDnPatterns;
    private BaseLdapPathContextSource contextSource;
    private ContextSourceBuilder contextSourceBuilder = new ContextSourceBuilder();
    private UserDetailsContextMapper userDetailsContextMapper;
    private PasswordEncoder passwordEncoder;
    private String passwordAttribute;
    private LdapAuthoritiesPopulator ldapAuthoritiesPopulator;

    private LdapAuthenticationProvider build() throws Exception {
        BaseLdapPathContextSource contextSource = getContextSource();
        LdapAuthenticator ldapAuthenticator = createLdapAuthenticator(contextSource);

        LdapAuthoritiesPopulator authoritiesPopulator = getLdapAuthoritiesPopulator();

        LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProviderProxy(
            ldapAuthenticator,
            authoritiesPopulator
        );
        SimpleAuthorityMapper simpleAuthorityMapper = new SimpleAuthorityMapper();
        simpleAuthorityMapper.setPrefix(rolePrefix);
        simpleAuthorityMapper.afterPropertiesSet();
        ldapAuthenticationProvider.setAuthoritiesMapper(simpleAuthorityMapper);
        if (userDetailsContextMapper != null) {
            ldapAuthenticationProvider.setUserDetailsContextMapper(userDetailsContextMapper);
        }
        return ldapAuthenticationProvider;
    }

    /**
     * Specifies the {@link LdapAuthoritiesPopulator}.
     *
     * @param ldapAuthoritiesPopulator the {@link LdapAuthoritiesPopulator} the default is
     * {@link DefaultLdapAuthoritiesPopulator}
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customizations
     */
    public LdapAuthenticationProviderConfigurer<B> ldapAuthoritiesPopulator(LdapAuthoritiesPopulator ldapAuthoritiesPopulator) {
        this.ldapAuthoritiesPopulator = ldapAuthoritiesPopulator;
        return this;
    }

    /**
     * Adds an {@link ObjectPostProcessor} for this class.
     *
     * @param objectPostProcessor
     * @return the {@link ChannelSecurityConfigurer} for further customizations
     */
    public LdapAuthenticationProviderConfigurer<B> withObjectPostProcessor(ObjectPostProcessor<?> objectPostProcessor) {
        addObjectPostProcessor(objectPostProcessor);
        return this;
    }

    /**
     * Gets the {@link LdapAuthoritiesPopulator} and defaults to
     * {@link DefaultLdapAuthoritiesPopulator}
     *
     * @return the {@link LdapAuthoritiesPopulator}
     */
    private LdapAuthoritiesPopulator getLdapAuthoritiesPopulator() {
        if (ldapAuthoritiesPopulator != null) {
            return ldapAuthoritiesPopulator;
        }

        DefaultLdapAuthoritiesPopulator defaultAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, groupSearchBase);
        defaultAuthoritiesPopulator.setGroupRoleAttribute(groupRoleAttribute);
        defaultAuthoritiesPopulator.setGroupSearchFilter(groupSearchFilter);

        this.ldapAuthoritiesPopulator = defaultAuthoritiesPopulator;
        return defaultAuthoritiesPopulator;
    }

    /**
     * Creates the {@link LdapAuthenticator} to use
     *
     * @param contextSource the {@link BaseLdapPathContextSource} to use
     * @return the {@link LdapAuthenticator} to use
     */
    private LdapAuthenticator createLdapAuthenticator(BaseLdapPathContextSource contextSource) {
        AbstractLdapAuthenticator ldapAuthenticator = passwordEncoder == null
            ? createBindAuthenticator(contextSource)
            : createPasswordCompareAuthenticator(contextSource);
        LdapUserSearch userSearch = createUserSearch();
        if (userSearch != null) {
            ldapAuthenticator.setUserSearch(userSearch);
        }
        if (userDnPatterns != null && userDnPatterns.length > 0) {
            ldapAuthenticator.setUserDnPatterns(userDnPatterns);
        }
        return postProcess(ldapAuthenticator);
    }

    /**
     * Creates {@link PasswordComparisonAuthenticator}
     *
     * @param contextSource the {@link BaseLdapPathContextSource} to use
     * @return
     */
    private PasswordComparisonAuthenticator createPasswordCompareAuthenticator(BaseLdapPathContextSource contextSource) {
        PasswordComparisonAuthenticator ldapAuthenticator = new PasswordComparisonAuthenticator(contextSource);
        if (passwordAttribute != null) {
            ldapAuthenticator.setPasswordAttributeName(passwordAttribute);
        }
        ldapAuthenticator.setPasswordEncoder(passwordEncoder);
        return ldapAuthenticator;
    }

    /**
     * Creates a {@link BindAuthenticator}
     *
     * @param contextSource the {@link BaseLdapPathContextSource} to use
     * @return the {@link BindAuthenticator} to use
     */
    private BindAuthenticator createBindAuthenticator(BaseLdapPathContextSource contextSource) {
        return new BindAuthenticator(contextSource);
    }

    private LdapUserSearch createUserSearch() {
        if (userSearchFilter == null) {
            return null;
        }
        return new FilterBasedLdapUserSearch(userSearchBase, userSearchFilter, contextSource);
    }

    /**
     * Specifies the {@link BaseLdapPathContextSource} to be used. If not specified, an
     * embedded LDAP server will be created using {@link #contextSource()}.
     *
     * @param contextSource the {@link BaseLdapPathContextSource} to use
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customizations
     * @see #contextSource()
     */
    public LdapAuthenticationProviderConfigurer<B> contextSource(BaseLdapPathContextSource contextSource) {
        this.contextSource = contextSource;
        return this;
    }

    /**
     * Allows easily configuring of a {@link BaseLdapPathContextSource} with defaults
     * pointing to an embedded LDAP server that is created.
     *
     * @return the {@link ContextSourceBuilder} for further customizations
     */
    public ContextSourceBuilder contextSource() {
        return contextSourceBuilder;
    }

    /**
     * Specifies the {@link org.springframework.security.crypto.password.PasswordEncoder}
     * to be used when authenticating with password comparison.
     *
     * @param passwordEncoder the
     * {@link org.springframework.security.crypto.password.PasswordEncoder} to use
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customization
     */
    public LdapAuthenticationProviderConfigurer<B> passwordEncoder(final PasswordEncoder passwordEncoder) {
        Assert.notNull(passwordEncoder, "passwordEncoder must not be null.");
        this.passwordEncoder = passwordEncoder;
        return this;
    }

    /**
     * If your users are at a fixed location in the directory (i.e. you can work out the
     * DN directly from the username without doing a directory search), you can use this
     * attribute to map directly to the DN. It maps directly to the userDnPatterns
     * property of AbstractLdapAuthenticator. The value is a specific pattern used to
     * build the user's DN, for example "uid={0},ou=people". The key "{0}" must be present
     * and will be substituted with the username.
     *
     * @param userDnPatterns the LDAP patterns for finding the usernames
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customizations
     */
    public LdapAuthenticationProviderConfigurer<B> userDnPatterns(String... userDnPatterns) {
        this.userDnPatterns = userDnPatterns;
        return this;
    }

    /**
     * Allows explicit customization of the loaded user object by specifying a
     * UserDetailsContextMapper bean which will be called with the context information
     * from the user's directory entry.
     *
     * @param userDetailsContextMapper the {@link UserDetailsContextMapper} to use
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customizations
     *
     * @see PersonContextMapper
     * @see InetOrgPersonContextMapper
     * @see LdapUserDetailsMapper
     */
    public LdapAuthenticationProviderConfigurer<B> userDetailsContextMapper(UserDetailsContextMapper userDetailsContextMapper) {
        this.userDetailsContextMapper = userDetailsContextMapper;
        return this;
    }

    /**
     * Specifies the attribute name which contains the role name. Default is "cn".
     * @param groupRoleAttribute the attribute name that maps a group to a role.
     * @return
     */
    public LdapAuthenticationProviderConfigurer<B> groupRoleAttribute(String groupRoleAttribute) {
        this.groupRoleAttribute = groupRoleAttribute;
        return this;
    }

    /**
     * The search base for group membership searches. Defaults to "".
     * @param groupSearchBase
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customizations
     */
    public LdapAuthenticationProviderConfigurer<B> groupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
        return this;
    }

    /**
     * The LDAP filter to search for groups. Defaults to "(uniqueMember={0})". The
     * substituted parameter is the DN of the user.
     *
     * @param groupSearchFilter the LDAP filter to search for groups
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customizations
     */
    public LdapAuthenticationProviderConfigurer<B> groupSearchFilter(String groupSearchFilter) {
        this.groupSearchFilter = groupSearchFilter;
        return this;
    }

    /**
     * A non-empty string prefix that will be added as a prefix to the existing roles. The
     * default is "ROLE_".
     *
     * @param rolePrefix the prefix to be added to the roles that are loaded.
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customizations
     * @see SimpleAuthorityMapper#setPrefix(String)
     */
    public LdapAuthenticationProviderConfigurer<B> rolePrefix(String rolePrefix) {
        this.rolePrefix = rolePrefix;
        return this;
    }

    /**
     * Search base for user searches. Defaults to "". Only used with
     * {@link #userSearchFilter(String)}.
     *
     * @param userSearchBase search base for user searches
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customizations
     */
    public LdapAuthenticationProviderConfigurer<B> userSearchBase(String userSearchBase) {
        this.userSearchBase = userSearchBase;
        return this;
    }

    /**
     * The LDAP filter used to search for users (optional). For example "(uid={0})". The
     * substituted parameter is the user's login name.
     *
     * @param userSearchFilter the LDAP filter used to search for users
     * @return the {@link LdapAuthenticationProviderConfigurer} for further customizations
     */
    public LdapAuthenticationProviderConfigurer<B> userSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
        return this;
    }

    @Override
    public void configure(B builder) throws Exception {
        LdapAuthenticationProvider provider = postProcess(build());
        builder.authenticationProvider(provider);
    }

    /**
     * Sets up Password based comparison
     *
     * @author Rob Winch
     */
    public final class PasswordCompareConfigurer {

        /**
         * Allows specifying the {@link PasswordEncoder} to use. The default is
         * @param passwordEncoder the {@link PasswordEncoder} to use
         * @return the {@link PasswordEncoder} to use
         */
        public PasswordCompareConfigurer passwordEncoder(PasswordEncoder passwordEncoder) {
            LdapAuthenticationProviderConfigurer.this.passwordEncoder = passwordEncoder;
            return this;
        }

        /**
         * The attribute in the directory which contains the user password. Defaults to
         * "userPassword".
         *
         * @param passwordAttribute the attribute in the directory which contains the user
         * password
         * @return the {@link PasswordCompareConfigurer} for further customizations
         */
        public PasswordCompareConfigurer passwordAttribute(String passwordAttribute) {
            LdapAuthenticationProviderConfigurer.this.passwordAttribute = passwordAttribute;
            return this;
        }

        /**
         * Allows obtaining a reference to the
         * {@link LdapAuthenticationProviderConfigurer} for further customizations
         *
         * @return attribute in the directory which contains the user password
         */
        public LdapAuthenticationProviderConfigurer<B> and() {
            return LdapAuthenticationProviderConfigurer.this;
        }

        private PasswordCompareConfigurer() {}
    }

    /**
     * Allows building a {@link BaseLdapPathContextSource} and optionally creating an
     * embedded LDAP instance.
     *
     * @author Rob Winch
     * @since 3.2
     */
    public final class ContextSourceBuilder {

        private String ldif = "classpath*:*.ldif";
        private String managerPassword;
        private String managerDn;
        private Integer port;
        private static final int DEFAULT_PORT = 33389;
        private String root = "dc=springframework,dc=org";
        private String url;

        /**
         * Specifies an ldif to load at startup for an embedded LDAP server. This only
         * loads if using an embedded instance. The default is "classpath*:*.ldif".
         *
         * @param ldif the ldif to load at startup for an embedded LDAP server.
         * @return the {@link ContextSourceBuilder} for further customization
         */
        public ContextSourceBuilder ldif(String ldif) {
            this.ldif = ldif;
            return this;
        }

        /**
         * Username (DN) of the "manager" user identity (i.e. "uid=admin,ou=system") which
         * will be used to authenticate to a (non-embedded) LDAP server. If omitted,
         * anonymous access will be used.
         *
         * @param managerDn the username (DN) of the "manager" user identity used to
         * authenticate to a LDAP server.
         * @return the {@link ContextSourceBuilder} for further customization
         */
        public ContextSourceBuilder managerDn(String managerDn) {
            this.managerDn = managerDn;
            return this;
        }

        /**
         * The password for the manager DN. This is required if the manager-dn is
         * specified.
         * @param managerPassword password for the manager DN
         * @return the {@link ContextSourceBuilder} for further customization
         */
        public ContextSourceBuilder managerPassword(String managerPassword) {
            this.managerPassword = managerPassword;
            return this;
        }

        /**
         * The port to connect to LDAP to (the default is 33389 or random available port
         * if unavailable).
         * @param port the port to connect to
         * @return the {@link ContextSourceBuilder} for further customization
         */
        public ContextSourceBuilder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Optional root suffix for the embedded LDAP server. Default is
         * "dc=springframework,dc=org"
         *
         * @param root root suffix for the embedded LDAP server
         * @return the {@link ContextSourceBuilder} for further customization
         */
        public ContextSourceBuilder root(String root) {
            this.root = root;
            return this;
        }

        /**
         * Specifies the ldap server URL when not using the embedded LDAP server. For
         * example, "ldaps://ldap.example.com:33389/dc=myco,dc=org".
         *
         * @param url the ldap server URL
         * @return the {@link ContextSourceBuilder} for further customization
         */
        public ContextSourceBuilder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Gets the {@link LdapAuthenticationProviderConfigurer} for further
         * customizations
         *
         * @return the {@link LdapAuthenticationProviderConfigurer} for further
         * customizations
         */
        public LdapAuthenticationProviderConfigurer<B> and() {
            return LdapAuthenticationProviderConfigurer.this;
        }

        private DefaultSpringSecurityContextSource build() throws Exception {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(getProviderUrl());
            if (managerDn != null) {
                contextSource.setUserDn(managerDn);
                if (managerPassword == null) {
                    throw new IllegalStateException("managerPassword is required if managerDn is supplied");
                }
                contextSource.setPassword(managerPassword);
            }
            contextSource = postProcess(contextSource);
            if (url != null) {
                return contextSource;
            }
            ApacheDSContainer apacheDsContainer = new ApacheDSContainer(root, ldif);
            apacheDsContainer.setPort(getPort());
            postProcess(apacheDsContainer);
            return contextSource;
        }

        private int getPort() {
            if (port == null) {
                port = getDefaultPort();
            }
            return port;
        }

        private int getDefaultPort() {
            ServerSocket serverSocket = null;
            try {
                try {
                    serverSocket = new ServerSocket(DEFAULT_PORT);
                } catch (IOException e) {
                    try {
                        serverSocket = new ServerSocket(0);
                    } catch (IOException e2) {
                        return DEFAULT_PORT;
                    }
                }
                return serverSocket.getLocalPort();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        log.debug("IOException ignored in LdapAuthenticationProviderConfigurer: {}", e.getMessage(), e);
                    }
                }
            }
        }

        private String getProviderUrl() {
            if (url == null) {
                return "ldap://127.0.0.1:" + getPort() + "/" + root;
            }
            return url;
        }

        private ContextSourceBuilder() {}
    }

    private BaseLdapPathContextSource getContextSource() throws Exception {
        if (contextSource == null) {
            contextSource = contextSourceBuilder.build();
        }
        return contextSource;
    }
}
