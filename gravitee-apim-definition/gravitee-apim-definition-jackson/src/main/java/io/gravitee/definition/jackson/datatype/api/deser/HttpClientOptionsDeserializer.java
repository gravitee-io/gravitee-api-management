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
package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.ProtocolVersion;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClientOptionsDeserializer extends AbstractStdScalarDeserializer<HttpClientOptions> {

    public HttpClientOptionsDeserializer(Class<HttpClientOptions> vc) {
        super(vc);
    }

    @Override
    public HttpClientOptions deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        HttpClientOptions httpClientOptions = new HttpClientOptions();

        httpClientOptions.setConnectTimeout(node.path("connectTimeout").asLong(HttpClientOptions.DEFAULT_CONNECT_TIMEOUT));

        httpClientOptions.setReadTimeout(node.path("readTimeout").asLong(HttpClientOptions.DEFAULT_READ_TIMEOUT));

        httpClientOptions.setIdleTimeout(node.path("idleTimeout").asLong(HttpClientOptions.DEFAULT_IDLE_TIMEOUT));

        httpClientOptions.setKeepAliveTimeout(node.path("keepAliveTimeout").asLong(HttpClientOptions.DEFAULT_KEEP_ALIVE_TIMEOUT));

        httpClientOptions.setKeepAlive(node.path("keepAlive").asBoolean(HttpClientOptions.DEFAULT_KEEP_ALIVE));

        httpClientOptions.setPipelining(node.path("pipelining").asBoolean(HttpClientOptions.DEFAULT_PIPELINING));

        httpClientOptions.setMaxConcurrentConnections(
            node.path("maxConcurrentConnections").asInt(HttpClientOptions.DEFAULT_MAX_CONCURRENT_CONNECTIONS)
        );

        httpClientOptions.setUseCompression(node.path("useCompression").asBoolean(HttpClientOptions.DEFAULT_USE_COMPRESSION));

        if (node.get("propagateClientAcceptEncoding") != null) {
            httpClientOptions.setPropagateClientAcceptEncoding(
                node.path("propagateClientAcceptEncoding").asBoolean(HttpClientOptions.DEFAULT_PROPAGATE_CLIENT_ACCEPT_ENCODING)
            );
        }

        httpClientOptions.setFollowRedirects(node.path("followRedirects").asBoolean(HttpClientOptions.DEFAULT_FOLLOW_REDIRECTS));

        if (node.get("clearTextUpgrade") != null) {
            httpClientOptions.setClearTextUpgrade(node.get("clearTextUpgrade").asBoolean(HttpClientOptions.DEFAULT_CLEAR_TEXT_UPGRADE));
        }

        if (node.get("version") != null) {
            httpClientOptions.setVersion(
                ProtocolVersion.valueOf(node.path("version").asText(HttpClientOptions.DEFAULT_PROTOCOL_VERSION.name()))
            );
        }

        return httpClientOptions;
    }
}
