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
package io.gravitee.rest.api.services.dynamicproperties.provider.http;

import static io.gravitee.gateway.api.http.HttpHeaderNames.USER_AGENT;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyProviderConfiguration;
import io.gravitee.definition.model.services.dynamicproperty.http.HttpDynamicPropertyProviderConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.utils.NodeUtils;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.services.dynamicproperties.model.DynamicProperty;
import io.gravitee.rest.api.services.dynamicproperties.provider.Provider;
import io.gravitee.rest.api.services.dynamicproperties.provider.http.mapper.JoltMapper;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class HttpProvider implements Provider {

    private static final String HTTPS_SCHEME = "https";

    private final HttpDynamicPropertyProviderConfiguration dpConfiguration;
    private final JoltMapper mapper;
    private final HttpClientService httpClientService;
    private final Node node;

    public HttpProvider(
        final DynamicPropertyProviderConfiguration dpConfiguration,
        final HttpClientService httpClientService,
        final Node node
    ) {
        this.dpConfiguration = (HttpDynamicPropertyProviderConfiguration) dpConfiguration;
        this.mapper = new JoltMapper(this.dpConfiguration.getSpecification());
        this.httpClientService = httpClientService;
        this.node = node;
    }

    @Override
    public Maybe<List<DynamicProperty>> get() {
        return Maybe.defer(() -> {
            URL requestUrl;
            try {
                requestUrl = new URL(dpConfiguration.getUrl());
            } catch (MalformedURLException ex) {
                log.error("Unable to parse URL: {}", dpConfiguration.getUrl(), ex);
                return Maybe.empty();
            }

            final io.vertx.rxjava3.core.http.HttpClient httpClient = new io.vertx.rxjava3.core.http.HttpClient(
                httpClientService.createHttpClient(requestUrl.getProtocol(), dpConfiguration.isUseSystemProxy())
            );

            final int port = requestUrl.getPort() != -1 ? requestUrl.getPort() : (HTTPS_SCHEME.equals(requestUrl.getProtocol()) ? 443 : 80);

            String relativeUri = (requestUrl.getQuery() == null)
                ? requestUrl.getPath()
                : requestUrl.getPath() + '?' + requestUrl.getQuery();

            RequestOptions options = new RequestOptions()
                .setMethod(HttpMethod.valueOf(dpConfiguration.getMethod().name()))
                .setHost(requestUrl.getHost())
                .setPort(port)
                .setURI(relativeUri);

            //headers
            options.putHeader(USER_AGENT, NodeUtils.userAgent(node));
            options.putHeader("X-Gravitee-Request-Id", UuidString.generateRandom());

            if (dpConfiguration.getHeaders() != null) {
                dpConfiguration.getHeaders().forEach(httpHeader -> options.putHeader(httpHeader.getName(), httpHeader.getValue()));
            }

            return httpClient
                .rxRequest(options)
                .observeOn(Schedulers.io())
                .flatMap(request -> {
                    log.debug(
                        "Dynamic properties will be fetched with the following request: {} {}",
                        request.getMethod(),
                        request.absoluteURI()
                    );
                    if (StringUtils.hasText(dpConfiguration.getBody())) {
                        return request.rxSend(dpConfiguration.getBody());
                    } else {
                        return request.rxSend();
                    }
                })
                .filter(response -> response.statusCode() == HttpStatusCode.OK_200)
                .flatMap(response -> response.rxBody().toMaybe())
                .observeOn(Schedulers.computation())
                .map(buffer -> mapper.map(buffer.toString()))
                .doFinally(httpClient::close);
        });
    }

    @Override
    public String name() {
        return "http-provider";
    }
}
