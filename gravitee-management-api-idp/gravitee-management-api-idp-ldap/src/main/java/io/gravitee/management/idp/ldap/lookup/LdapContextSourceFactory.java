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
package io.gravitee.management.idp.ldap.lookup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.server.ApacheDSContainer;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LdapContextSourceFactory extends AbstractFactoryBean<LdapContextSource> {

    @Autowired
    private Environment environment;

    private LdapContextSource ldapContextSource;
    private ApacheDSContainer apacheDsContainer;

    @Override
    public Class<?> getObjectType() {
        return LdapContextSource.class;
    }

    @Override
    protected LdapContextSource createInstance() throws Exception {
        ContextSourceBuilder contextSourceBuilder = new ContextSourceBuilder();

        contextSourceBuilder
                .root(environment.getProperty("context.base"));

        // set up embedded mode
        if (environment.getProperty("embedded", boolean.class, false)) {
            contextSourceBuilder.ldif("classpath:/ldap/gravitee-io-management-rest-api-ldap-test.ldif");
        } else {
            contextSourceBuilder
                    .managerDn(environment.getProperty("context.username"))
                    .managerPassword(environment.getProperty("context.password"))
                    .url(environment.getProperty("context.url"));
        }

        ldapContextSource = contextSourceBuilder.build();
        return ldapContextSource;
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

        private DefaultSpringSecurityContextSource build() throws Exception {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(
                    getProviderUrl());
            if (managerDn != null) {
                contextSource.setUserDn(managerDn);
                if (managerPassword == null) {
                    throw new IllegalStateException(
                            "managerPassword is required if managerDn is supplied");
                }
                contextSource.setPassword(managerPassword);
            }
//            contextSource = postProcess(contextSource);
            if (url != null) {
                return contextSource;
            }
            ApacheDSContainer embeddedApacheContainer = new ApacheDSContainer(root, ldif);
            embeddedApacheContainer.setPort(getPort());
            apacheDsContainer = embeddedApacheContainer;
//            postProcess(apacheDsContainer);
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
                }
                catch (IOException e) {
                    try {
                        serverSocket = new ServerSocket(0);
                    }
                    catch (IOException e2) {
                        return DEFAULT_PORT;
                    }
                }
                return serverSocket.getLocalPort();
            }
            finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    }
                    catch (IOException e) {
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

        private ContextSourceBuilder() {
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        ldapContextSource.afterPropertiesSet();

        if (apacheDsContainer != null) {
            apacheDsContainer.afterPropertiesSet();
        }
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();

        if (apacheDsContainer != null) {
            apacheDsContainer.destroy();
        }
    }
}
