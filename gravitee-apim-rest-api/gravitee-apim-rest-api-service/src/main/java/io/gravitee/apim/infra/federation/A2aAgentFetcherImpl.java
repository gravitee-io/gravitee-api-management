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
package io.gravitee.apim.infra.federation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.gravitee.apim.core.integration.service_provider.A2aAgentFetcher;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.ext.web.client.HttpResponse;
import io.vertx.rxjava3.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class A2aAgentFetcherImpl implements A2aAgentFetcher {

    private final Vertx vertx;
    private ObjectMapper objectMapper;
    private WebClient webClient;

    @PostConstruct
    public void init() {
        webClient = WebClient.create(vertx);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public Single<FederatedAgent> fetchAgentCard(String url) {
        return executeQuery(webClient, url).map(r -> objectMapper.readValue(r.bodyAsString(), FederatedAgent.class));
    }

    private static Single<HttpResponse<Buffer>> executeQuery(WebClient webClient, String url) {
        try {
            var uri = new URI(url).toURL();
            int port = uri.getPort();
            if (port < 0) {
                port =
                    switch (uri.getProtocol()) {
                        case "http" -> 80;
                        case "https" -> 443;
                        default -> throw new IllegalArgumentException("Invalid port " + uri.getPort());
                    };
            }
            boolean ssl = "https".equalsIgnoreCase(uri.getProtocol());
            return webClient.get(port, uri.getHost(), uri.getFile()).ssl(ssl).rxSend();
        } catch (Exception e) {
            return Single.error(e);
        }
    }
}
