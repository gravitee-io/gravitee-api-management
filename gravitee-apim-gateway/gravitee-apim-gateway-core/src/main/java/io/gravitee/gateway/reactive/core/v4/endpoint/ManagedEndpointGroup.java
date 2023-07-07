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
package io.gravitee.gateway.reactive.core.v4.endpoint;

import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import java.util.Set;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ManagedEndpointGroup {
    ManagedEndpoint next();

    ManagedEndpoint addManagedEndpoint(ManagedEndpoint managedEndpoint);

    ManagedEndpoint removeManagedEndpoint(ManagedEndpoint managedEndpoint);

    ManagedEndpoint removeManagedEndpoint(String name);

    EndpointGroup getDefinition();

    Set<ConnectorMode> supportedModes();

    ApiType supportedApi();
}
