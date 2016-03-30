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
package io.gravitee.definition.jackson.datatype.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.HttpClientOptions;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author Gravitee.io Team
 */
public class HttpClientOptionsDeserializer extends AbstractStdScalarDeserializer<HttpClientOptions> {

    public HttpClientOptionsDeserializer(Class<HttpClientOptions> vc) {
        super(vc);
    }

    @Override
    public HttpClientOptions deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        HttpClientOptions httpClientOptions = new HttpClientOptions();

        JsonNode connectTimeoutNode = node.get("connectTimeout");
        if (connectTimeoutNode != null) {
            long connectTimeout = connectTimeoutNode.asLong(HttpClientOptions.DEFAULT_CONNECT_TIMEOUT);
            httpClientOptions.setConnectTimeout(connectTimeout);
        } else {
            httpClientOptions.setConnectTimeout(HttpClientOptions.DEFAULT_CONNECT_TIMEOUT);
        }

        JsonNode readTimeoutNode = node.get("readTimeout");
        if (readTimeoutNode != null) {
            long readTimeout = readTimeoutNode.asLong(HttpClientOptions.DEFAULT_READ_TIMEOUT);
            httpClientOptions.setReadTimeout(readTimeout);
        } else {
            httpClientOptions.setReadTimeout(HttpClientOptions.DEFAULT_READ_TIMEOUT);
        }

        JsonNode idleTimeoutNode = node.get("idleTimeout");
        if (idleTimeoutNode != null) {
            long idleTimeout = idleTimeoutNode.asLong(HttpClientOptions.DEFAULT_IDLE_TIMEOUT);
            httpClientOptions.setIdleTimeout(idleTimeout);
        } else {
            httpClientOptions.setIdleTimeout(HttpClientOptions.DEFAULT_IDLE_TIMEOUT);
        }

        JsonNode keepAliveNode = node.get("keepAlive");
        if (keepAliveNode != null) {
            boolean keepAlive = keepAliveNode.asBoolean(HttpClientOptions.DEFAULT_KEEP_ALIVE);
            httpClientOptions.setKeepAlive(keepAlive);
        } else {
            httpClientOptions.setKeepAlive(HttpClientOptions.DEFAULT_KEEP_ALIVE);
        }

        JsonNode dumpRequestNode = node.get("dumpRequest");
        if (dumpRequestNode != null) {
            boolean dumpRequest = dumpRequestNode.asBoolean(HttpClientOptions.DEFAULT_DUMP_REQUEST);
            httpClientOptions.setDumpRequest(dumpRequest);
        } else {
            httpClientOptions.setDumpRequest(HttpClientOptions.DEFAULT_DUMP_REQUEST);
        }

        JsonNode pipeliningNode = node.get("pipelining");
        if (pipeliningNode != null) {
            boolean pipelining = pipeliningNode.asBoolean(HttpClientOptions.DEFAULT_PIPELINING);
            httpClientOptions.setPipelining(pipelining);
        } else {
            httpClientOptions.setPipelining(HttpClientOptions.DEFAULT_PIPELINING);
        }

        JsonNode maxConcurrentConnectionsNode = node.get("maxConcurrentConnections");
        if (maxConcurrentConnectionsNode != null) {
            int maxConcurrentConnections = maxConcurrentConnectionsNode.asInt(HttpClientOptions.DEFAULT_MAX_CONCURRENT_CONNECTIONS);
            httpClientOptions.setMaxConcurrentConnections(maxConcurrentConnections);
        } else {
            httpClientOptions.setMaxConcurrentConnections(HttpClientOptions.DEFAULT_MAX_CONCURRENT_CONNECTIONS);
        }

        return httpClientOptions;
    }
}