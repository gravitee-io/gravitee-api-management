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
package io.gravitee.gateway.handlers.api.manager;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;

import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.util.Date;

class ApiBuilder {

    private final Api api = new Api();

    public ApiBuilder id(String id) {
        this.api.setId(id);
        return this;
    }

    public ApiBuilder name(String name) {
        this.api.setName(name);
        return this;
    }

    public ApiBuilder proxy(Proxy proxy) {
        this.api.setProxy(proxy);
        return this;
    }

    public ApiBuilder deployedAt(Date updatedAt) {
        this.api.setDeployedAt(updatedAt);
        return this;
    }

    public ApiBuilder mockProxy() {
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(singletonList(mock(VirtualHost.class)));
        return proxy(proxy);
    }

    public Api build() {
        api.setEnabled(true);

        return this.api;
    }
}
