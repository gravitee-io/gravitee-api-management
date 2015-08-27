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
package io.gravitee.gateway.core.builder;

import java.net.URI;

import io.gravitee.gateway.core.model.Api;
import io.gravitee.gateway.core.model.ApiLifecycleState;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiBuilder {

    private final Api api = new Api();

    public ApiBuilder name(String name) {
        this.api.setName(name);
        return this;
    }

    public ApiBuilder target(String target) {
        this.api.setTargetURI(URI.create(target));
        return this;
    }

    public ApiBuilder origin(String origin) {
        this.api.setPublicURI(URI.create(origin));
        return this;
    }

    public ApiBuilder start() {
        this.api.setState(ApiLifecycleState.START);
        return this;
    }

    public Api build() {
        return this.api;
    }
}