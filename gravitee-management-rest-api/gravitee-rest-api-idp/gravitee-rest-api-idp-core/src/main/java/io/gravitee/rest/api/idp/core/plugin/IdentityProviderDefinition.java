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
package io.gravitee.rest.api.idp.core.plugin;

import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.rest.api.idp.api.IdentityProvider;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class IdentityProviderDefinition {

    private final Plugin plugin;

    private final IdentityProvider identityProvider;

    public IdentityProviderDefinition(IdentityProvider identityProvider, Plugin plugin) {
        this.identityProvider = identityProvider;
        this.plugin = plugin;
    }

    public IdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
