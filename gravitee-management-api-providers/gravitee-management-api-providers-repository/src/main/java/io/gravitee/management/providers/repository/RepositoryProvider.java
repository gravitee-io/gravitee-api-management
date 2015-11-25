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
package io.gravitee.management.providers.repository;

import io.gravitee.management.providers.core.authentication.AuthenticationManager;
import io.gravitee.management.providers.core.identity.IdentityManager;
import io.gravitee.management.providers.core.Provider;
import io.gravitee.management.providers.repository.authentication.RepositoryAuthenticationProvider;
import io.gravitee.management.providers.repository.identity.RepositoryIdentityManager;
import io.gravitee.management.providers.repository.spring.RepositoryConfiguration;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RepositoryProvider implements Provider {

    public final static String PROVIDER_TYPE = "gravitee";

    @Override
    public String type() {
        return PROVIDER_TYPE;
    }

    @Override
    public Class<? extends AuthenticationManager> authenticationManager() {
        return RepositoryAuthenticationProvider.class;
    }

    @Override
    public Class<? extends IdentityManager> identityManager() {
        return RepositoryIdentityManager.class;
    }

    @Override
    public Class<?> configuration() {
        return RepositoryConfiguration.class;
    }
}
