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
package io.gravitee.gateway.repository.plugins;

import io.gravitee.platform.repository.api.RepositoryScopeProvider;
import io.gravitee.platform.repository.api.Scope;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class GatewayRepositoryScopeProvider implements RepositoryScopeProvider {

    public Scope[] getHandledScopes() {
        return new Scope[] { Scope.MANAGEMENT, Scope.RATE_LIMIT };
    }

    @Override
    public Scope[] getOptionalHandledScopes() {
        return new Scope[] { Scope.DISTRIBUTED_SYNC };
    }
}
