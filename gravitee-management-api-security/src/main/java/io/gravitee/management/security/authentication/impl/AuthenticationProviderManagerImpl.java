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
package io.gravitee.management.security.authentication.impl;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.management.security.authentication.AuthenticationProvider;
import io.gravitee.management.security.authentication.AuthenticationProviderManager;
import io.gravitee.management.security.config.BasicSecurityConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthenticationProviderManagerImpl implements AuthenticationProviderManager, InitializingBean {

    @Autowired
    private ConfigurableEnvironment environment;

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicSecurityConfigurerAdapter.class);

    private static final Set<String> OAUTH2_AUTHENTICATION_PROVIDERS = new HashSet(Arrays.asList("google", "github", "oauth2"));

    private List<AuthenticationProvider> identityProviders;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("Loading authentication providers");
        loadAuthenticationProviders();
    }

    public List<AuthenticationProvider> getIdentityProviders() {
        return this.identityProviders;
    }

    @Override
    public Optional<AuthenticationProvider> findIdentityProviderByType(String type) {
        return identityProviders.stream()
                .filter(provider -> provider.type().equalsIgnoreCase(type))
                .findFirst();
    }

    private Map<String, Object> getConfiguration(AuthenticationProvider provider) {
        String prefix = "security.providers[" + provider.index() + "].";
        Map<String, Object> properties = EnvironmentUtils.getPropertiesStartingWith(environment, prefix);
        Map<String, Object> unprefixedProperties = new HashMap<>(properties.size());
        properties.entrySet().stream().forEach(propEntry -> unprefixedProperties.put(
                EnvironmentUtils.encodedKey(propEntry.getKey()).substring(EnvironmentUtils.encodedKey(prefix).length()), propEntry.getValue()));
        return unprefixedProperties;
    }

    private void loadAuthenticationProviders() {
        LOGGER.debug("Looking for authentication providers...");
        identityProviders = new ArrayList<>();

        boolean found = true;
        int idx = 0;

        while (found) {
            String type = environment.getProperty("security.providers[" + idx + "].type");
            found = (type != null);
            if (found) {
                DefaultAuthenticationProvider provider;
                if (OAUTH2_AUTHENTICATION_PROVIDERS.contains(type)) {
                    provider = new OAuth2AuthenticationProvider(type, idx);
                } else {
                    provider = new DefaultAuthenticationProvider(type, idx);
                }

                provider.setConfiguration(getConfiguration(provider));
                identityProviders.add(provider);
                LOGGER.debug("\tAuthentication provider [{}] has been defined", type);
            }
            idx++;
        }
    }
}
