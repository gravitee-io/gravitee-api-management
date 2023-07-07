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

import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
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

    public static final EndpointCriteria NO_CRITERIA = new EndpointCriteria();

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

    public EndpointCriteria() {
        this(null, null);
    }

    public EndpointCriteria(ApiType apiType, Set<ConnectorMode> modes) {
        this(null, apiType, modes);
    }

    public EndpointCriteria(String name, ApiType apiType, Set<ConnectorMode> modes) {
        this.name = name;
        this.apiType = apiType;
        this.modes = modes;
    }

    public boolean matches(ManagedEndpointGroup managedEndpointGroup) {
        if (modes != null && !managedEndpointGroup.supportedModes().containsAll(modes)) {
            return false;
        }

        return apiType == null || apiType.equals(managedEndpointGroup.supportedApi());
    }

    public boolean matches(ManagedEndpoint managedEndpoint) {
        if (modes != null && !managedEndpoint.getConnector().supportedModes().containsAll(modes)) {
            return false;
        }

        return apiType == null || apiType.equals(managedEndpoint.getConnector().supportedApi());
    }
}
