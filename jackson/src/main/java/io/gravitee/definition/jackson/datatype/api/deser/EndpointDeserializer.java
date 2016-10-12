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
package io.gravitee.definition.jackson.datatype.api.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointDeserializer extends StdScalarDeserializer<Endpoint> {

    public EndpointDeserializer(Class<Endpoint> vc) {
        super(vc);
    }

    @Override
    public Endpoint deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Endpoint endpoint = new Endpoint();
        endpoint.setTarget(node.get("target").asText());

        JsonNode nameNode = node.get("name");
        if (nameNode != null) {
            String name = nameNode.asText(UUID.random().toString());
            endpoint.setName(name);
        } else {
            endpoint.setName(UUID.random().toString());
        }

        JsonNode weightNode = node.get("weight");
        if (weightNode != null) {
            int weight = weightNode.asInt(Endpoint.DEFAULT_WEIGHT);
            endpoint.setWeight(weight);
        } else {
            endpoint.setWeight(Endpoint.DEFAULT_WEIGHT);
        }

        JsonNode backupNode = node.get("backup");
        if (backupNode != null) {
            boolean backup = backupNode.asBoolean(false);
            endpoint.setBackup(backup);
        } else {
            endpoint.setBackup(false);
        }

        JsonNode healthcheckNode = node.get("healthcheck");
        if (healthcheckNode != null) {
            boolean healthcheck = healthcheckNode.asBoolean(true);
            endpoint.setHealthcheck(healthcheck);
        } else {
            endpoint.setHealthcheck(true);
        }

        JsonNode httpProxyNode = node.get("proxy");
        if (httpProxyNode != null) {
            HttpProxy httpProxy = httpProxyNode.traverse(jp.getCodec()).readValueAs(HttpProxy.class);
            endpoint.setHttpProxy(httpProxy);
        }

        JsonNode httpClientOptionsNode = node.get("http");
        if (httpClientOptionsNode != null) {
            HttpClientOptions httpClientOptions = httpClientOptionsNode.traverse(jp.getCodec()).readValueAs(HttpClientOptions.class);
            endpoint.setHttpClientOptions(httpClientOptions);
        } else {
            endpoint.setHttpClientOptions(new HttpClientOptions());
        }

        JsonNode httpClientSslOptionsNode = node.get("ssl");
        if (httpClientSslOptionsNode != null) {
            HttpClientSslOptions httpClientSslOptions = httpClientSslOptionsNode.traverse(jp.getCodec()).readValueAs(HttpClientSslOptions.class);
            endpoint.setHttpClientSslOptions(httpClientSslOptions);
        }

        return endpoint;
    }
}