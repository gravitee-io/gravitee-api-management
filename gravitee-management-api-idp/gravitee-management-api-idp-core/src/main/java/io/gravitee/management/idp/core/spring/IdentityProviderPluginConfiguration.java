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
package io.gravitee.management.idp.core.spring;

import io.gravitee.management.idp.core.authentication.impl.CompositeIdentityManager;
import io.gravitee.management.idp.core.authentication.impl.ReferenceSerializer;
import io.gravitee.management.idp.core.plugin.IdentityProviderManager;
import io.gravitee.management.idp.core.plugin.impl.IdentityProviderManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class IdentityProviderPluginConfiguration {

    @Bean
    public IdentityProviderManager identityProviderManager() {
        return new IdentityProviderManagerImpl();
    }

    @Bean
    public CompositeIdentityManager identityManager() { return new CompositeIdentityManager(); }

    @Bean
    public ReferenceSerializer referenceSerializer() {
        return new ReferenceSerializer();
    }
}
