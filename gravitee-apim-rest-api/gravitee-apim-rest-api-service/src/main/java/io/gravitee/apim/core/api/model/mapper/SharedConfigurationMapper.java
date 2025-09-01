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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.v4.ssl.KeyStore;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.definition.model.v4.ssl.TrustStore;
import java.util.Set;

public class SharedConfigurationMapper {

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SharedConfigurationMapper() {}

    public static String convert(io.gravitee.definition.model.EndpointGroup source) throws JsonProcessingException {
        ObjectNode httpClientOptions = mapHttpClientOptions(source.getHttpClientOptions());
        ObjectNode httpClientSslOptionsNode = mapHttpClientSslOptions(source.getHttpClientSslOptions());
        ObjectNode sharedConfiguration = OBJECT_MAPPER.createObjectNode();
        sharedConfiguration.set("http", httpClientOptions);
        sharedConfiguration.set("ssl", httpClientSslOptionsNode);
        sharedConfiguration.set("headers", source.getHeaders() == null ? null : OBJECT_MAPPER.valueToTree(source.getHeaders()));
        sharedConfiguration.set("proxy", source.getHttpProxy() == null ? null : OBJECT_MAPPER.valueToTree((source.getHttpProxy())));
        return OBJECT_MAPPER.writeValueAsString(sharedConfiguration);
    }

    private static ObjectNode mapHttpClientOptions(io.gravitee.definition.model.HttpClientOptions httpClientOptions) {
        if (httpClientOptions == null) {
            return null;
        }

        var v4 = new io.gravitee.definition.model.v4.http.HttpClientOptions();

        var versionV4 = httpClientOptions.getVersion().equals(io.gravitee.definition.model.ProtocolVersion.HTTP_1_1)
            ? io.gravitee.definition.model.v4.http.ProtocolVersion.HTTP_1_1
            : io.gravitee.definition.model.v4.http.ProtocolVersion.HTTP_2;

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

        if (versionV4 == io.gravitee.definition.model.v4.http.ProtocolVersion.HTTP_2) {
            v4.setClearTextUpgrade(httpClientOptions.isClearTextUpgrade());
        }

        ObjectNode node = OBJECT_MAPPER.valueToTree(v4);

        if (versionV4 == io.gravitee.definition.model.v4.http.ProtocolVersion.HTTP_1_1) {
            node.retain(HTTP11_ALLOWED);
        } else {
            node.retain(HTTP2_ALLOWED);
        }

        return node;
    }

    private static ObjectNode mapHttpClientSslOptions(HttpClientSslOptions httpClientSslOptions) {
        if (httpClientSslOptions == null) {
            return null;
        }
        SslOptions sslOptionsV4 = new SslOptions();
        sslOptionsV4.setHostnameVerifier(httpClientSslOptions.isHostnameVerifier());
        sslOptionsV4.setTrustAll(httpClientSslOptions.isTrustAll());
        TrustStore trustStoreV4 = httpClientSslOptions.getTrustStore() != null
            ? TrustStoreMapper.convert(httpClientSslOptions.getTrustStore())
            : null;
        sslOptionsV4.setTrustStore(trustStoreV4);
        KeyStore keyStoreV4 = httpClientSslOptions.getKeyStore() != null
            ? KeyStoreMapper.convert(httpClientSslOptions.getKeyStore())
            : null;
        sslOptionsV4.setKeyStore(keyStoreV4);

        return OBJECT_MAPPER.valueToTree(sslOptionsV4);
    }
}
