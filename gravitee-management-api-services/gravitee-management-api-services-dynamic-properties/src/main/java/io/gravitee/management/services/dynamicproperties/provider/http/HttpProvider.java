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
package io.gravitee.management.services.dynamicproperties.provider.http;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.management.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.management.services.dynamicproperties.provider.Provider;
import io.gravitee.management.services.dynamicproperties.provider.http.mapper.JoltMapper;
import org.asynchttpclient.*;
import org.asynchttpclient.util.HttpConstants;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProvider implements Provider {

    private final DynamicPropertyService dpService;

    private final HttpDynamicPropertyProviderConfiguration dpConfiguration;

    private final AsyncHttpClient httpClient;

    private JoltMapper mapper;

    public HttpProvider(final DynamicPropertyService dpService) {
        Objects.requireNonNull(dpService, "Service must not be null");

        this.dpService = dpService;
        this.dpConfiguration = (HttpDynamicPropertyProviderConfiguration) dpService.getConfiguration();
        this.httpClient = create();
        this.mapper = new JoltMapper(dpConfiguration.getSpecification());
    }

    @Override
    public CompletableFuture<Collection<DynamicProperty>> get() {
        Request request = new RequestBuilder()
                .setUrl(dpConfiguration.getUrl())
                .setMethod(HttpConstants.Methods.GET)
                .build();

        return httpClient
                .prepareRequest(request)
                .execute()
                .toCompletableFuture()
                .thenApply(response -> (HttpStatusCode.OK_200 == response.getStatusCode()) ?
                        mapper.map(response.getResponseBody()) : null);
    }

    @Override
    public String name() {
        return "custom";
    }

    public void setMapper(JoltMapper mapper) {
        this.mapper = mapper;
    }

    private AsyncHttpClient create() {
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
        builder.setAcceptAnyCertificate(true);
        AsyncHttpClientConfig cf = builder.build();
        return new DefaultAsyncHttpClient(cf);
    }
}
