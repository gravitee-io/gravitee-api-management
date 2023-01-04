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
package io.gravitee.gateway.jupiter.core.v4.endpoint;

import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class EndpointCriteria {

    public static final EndpointCriteria ENDPOINT_UP = new EndpointCriteria();

    /*
     * Name could be either:
     * <ul>
     *     <li>the name of an endpoint</li>
     *     <li>the name of an endpoint group</li>
     * </ul>
     */
    private String name;
    private ApiType apiType;
    private Set<ConnectorMode> modes;
    private ManagedEndpoint.Status endpointStatus;

    public EndpointCriteria() {
        this(null, null);
    }

    public EndpointCriteria(ApiType apiType, Set<ConnectorMode> modes) {
        this(null, apiType, modes, ManagedEndpoint.Status.UP);
    }

    public EndpointCriteria(String name, ApiType apiType, Set<ConnectorMode> modes) {
        this(name, apiType, modes, ManagedEndpoint.Status.UP);
    }

    public EndpointCriteria(String name, ApiType apiType, Set<ConnectorMode> modes, ManagedEndpoint.Status endpointStatus) {
        this.name = name;
        this.apiType = apiType;
        this.modes = modes;
        this.endpointStatus = endpointStatus;
    }

    public boolean matches(ManagedEndpointGroup managedEndpointGroup) {
        if (modes != null && !managedEndpointGroup.supportedModes().containsAll(modes)) {
            return false;
        }

        return apiType == null || apiType.equals(managedEndpointGroup.supportedApi());
    }

    public boolean matches(ManagedEndpoint managedEndpoint) {
        if (endpointStatus != null && endpointStatus != managedEndpoint.getStatus()) {
            return false;
        }

        if (modes != null && !managedEndpoint.getConnector().supportedModes().containsAll(modes)) {
            return false;
        }

        return apiType == null || apiType.equals(managedEndpoint.getConnector().supportedApi());
    }
}
