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

import io.gravitee.gateway.api.Registry;
import io.gravitee.gateway.core.components.client.HttpClient;
import io.gravitee.gateway.core.components.client.HttpClientFactory;
import io.gravitee.gateway.core.components.client.jetty.JettyHttpClientFactory;
import io.gravitee.gateway.core.http.Request;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.http.Response;
import io.gravitee.gateway.core.registry.FileRegistry;
import io.gravitee.model.Api;
import rx.Observable;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DefaultReactor implements Reactor {

    // TODO: externalize it
    private Registry registry = new FileRegistry();

    private final HttpClientFactory httpClientFactory = new JettyHttpClientFactory();

	@Override
	public Observable<Response> process(Request request) {
        // TODO: get the associated API / service from the request using the registry
        Api api = registry.findMatchingApi(request.getDestination());

        if (api == null) {
            // Not found -> 404
            Response response = new Response();
            response.setStatus(404);

            return Observable.just(response);
        } else {
            HttpClient client = httpClientFactory.create(api);
            return client.invoke(request);
        }
	}

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
}
