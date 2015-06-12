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
package io.gravitee.gateway.core.impl;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Registry;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.components.client.HttpClient;
import io.gravitee.gateway.core.components.client.HttpClientFactory;
import io.gravitee.gateway.core.components.client.jetty.JettyHttpClientFactory;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.http.ServerResponse;
import io.gravitee.model.Api;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DefaultReactor implements Reactor {

    @Autowired
    private Registry registry;

    private final HttpClientFactory httpClientFactory = new JettyHttpClientFactory();

	@Override
	public Observable<Response> process(Request request) {
        // TODO: get the associated API / service from the request using the registry
        Api api = registry.findMatchingApi(request.uri());

        if (api == null) {
            // Not found -> 404
            ServerResponse response = new ServerResponse();
            response.setStatus(HttpStatusCode.NOT_FOUND_404);

            return Observable.just((Response)response);
        } else {

            // 1_ Apply request policies

            // 2_ Call remote service
            HttpClient client = httpClientFactory.create(api);
            return client.invoke(request);

            // 3_ Apply response policies
        }
	}

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
}
