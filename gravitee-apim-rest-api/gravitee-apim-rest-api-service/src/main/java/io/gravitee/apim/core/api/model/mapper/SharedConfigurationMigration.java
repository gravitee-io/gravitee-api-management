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
package io.gravitee.apim.core.api.model.mapper;

import static io.gravitee.definition.model.v4.http.ProtocolVersion.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SharedConfigurationMigration {

    private static final Set<String> HTTP11_ALLOWED = Set.of(
        "version",
        "keepAlive",
        "keepAliveTimeout",
        "connectTimeout",
        "pipelining",
        "readTimeout",
        "useCompression",
        "propagateClientAcceptEncoding",
        "propagateClientHost",
        "idleTimeout",
        "followRedirects",
        "maxConcurrentConnections"
    );

    private static final Set<String> HTTP2_ALLOWED = Set.of(
        "version",
        "clearTextUpgrade",
        "keepAlive",
        "keepAliveTimeout",
        "connectTimeout",
        "pipelining",
        "readTimeout",
        "useCompression",
        "propagateClientAcceptEncoding",
        "propagateClientHost",
        "idleTimeout",
        "followRedirects",
        "maxConcurrentConnections",
        "http2MultiplexingLimit"
    );

    private final ObjectMapper objectMapper;

    public String convert(io.gravitee.definition.model.EndpointGroup source) throws JsonProcessingException {
        ObjectNode httpClientOptions = mapHttpClientOptions(source.getHttpClientOptions());
        ObjectNode httpClientSslOptionsNode = mapHttpClientSslOptions(source.getHttpClientSslOptions());
        ObjectNode sharedConfiguration = objectMapper.createObjectNode();
        if (httpClientOptions != null) {
            sharedConfiguration.set("http", httpClientOptions);
        }
        if (httpClientSslOptionsNode != null) {
            sharedConfiguration.set("ssl", httpClientSslOptionsNode);
        }
        if (source.getHeaders() != null) {
            sharedConfiguration.set("headers", objectMapper.valueToTree(source.getHeaders()));
        }
        if (source.getHttpProxy() != null) {
            ObjectNode proxyNode = (ObjectNode) objectMapper.valueToTree(source.getHttpProxy());
            if (source.getHttpProxy().isEnabled() && source.getHttpProxy().isUseSystemProxy()) {
                proxyNode.remove("host");
                proxyNode.remove("port");
                proxyNode.remove("type");
                proxyNode.put("useSystemProxy", true);
                proxyNode.remove("enabled");
            } else {
                String host = source.getHttpProxy().getHost();
                if (host == null || host.isEmpty()) {
                    proxyNode.put("host", "/");
                }
            }
            sharedConfiguration.set("proxy", proxyNode);
        }
        return objectMapper.writeValueAsString(sharedConfiguration);
    }

    private ObjectNode mapHttpClientOptions(io.gravitee.definition.model.HttpClientOptions httpClientOptions) {
        if (httpClientOptions == null) {
            return null;
        }

        var v4 = new io.gravitee.definition.model.v4.http.HttpClientOptions();

        var versionV4 = httpClientOptions.getVersion() == io.gravitee.definition.model.ProtocolVersion.HTTP_1_1 ? HTTP_1_1 : HTTP_2;

        v4.setVersion(versionV4);
        v4.setKeepAlive(httpClientOptions.isKeepAlive());
        v4.setPipelining(httpClientOptions.isPipelining());
        v4.setUseCompression(httpClientOptions.isUseCompression());
        v4.setPropagateClientAcceptEncoding(httpClientOptions.isPropagateClientAcceptEncoding());
        v4.setFollowRedirects(httpClientOptions.isFollowRedirects());
        v4.setMaxConcurrentConnections(httpClientOptions.getMaxConcurrentConnections());
        v4.setIdleTimeout(httpClientOptions.getIdleTimeout());
        v4.setKeepAliveTimeout(httpClientOptions.getKeepAliveTimeout());
        v4.setConnectTimeout(httpClientOptions.getConnectTimeout());
        v4.setReadTimeout(httpClientOptions.getReadTimeout());

        if (versionV4 == HTTP_2) {
            v4.setClearTextUpgrade(httpClientOptions.isClearTextUpgrade());
        }

        ObjectNode node = objectMapper.valueToTree(v4);

        return versionV4 == HTTP_1_1 ? node.retain(HTTP11_ALLOWED) : node.retain(HTTP2_ALLOWED);
    }

    private ObjectNode mapHttpClientSslOptions(HttpClientSslOptions httpClientSslOptions) {
        if (httpClientSslOptions == null) {
            return null;
        }
        SslOptions sslOptionsV4 = new SslOptions();
        sslOptionsV4.setHostnameVerifier(httpClientSslOptions.isHostnameVerifier());
        sslOptionsV4.setTrustAll(httpClientSslOptions.isTrustAll());
        sslOptionsV4.setTrustStore(TrustStoreMigration.convert(httpClientSslOptions.getTrustStore()));
        sslOptionsV4.setKeyStore(KeyStoreMigration.convert(httpClientSslOptions.getKeyStore()));

        return objectMapper.valueToTree(sslOptionsV4);
    }
}
