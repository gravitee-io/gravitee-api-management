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
package io.gravitee.gateway.handlers.api.builders;

import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;

import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProxyDefinitionBuilder {

    private final Proxy proxyDefinition = new Proxy();

    public ProxyDefinitionBuilder contextPath(String contextPath) {
        this.proxyDefinition.setContextPath(contextPath);
        return this;
    }

    public ProxyDefinitionBuilder target(String name, String target) {
        if (this.proxyDefinition.getGroups() == null) {
            this.proxyDefinition.setGroups(Collections.singleton(new EndpointGroup()));
        }
        this.proxyDefinition.getGroups().iterator().next().setEndpoints(Collections.singleton(new HttpEndpoint(name, target)));
        return this;
    }

    public ProxyDefinitionBuilder stripContextPath(boolean stripContextPath) {
        this.proxyDefinition.setStripContextPath(stripContextPath);
        return this;
    }

    public Proxy build() {
        return this.proxyDefinition;
    }

}
