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
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.HttpProxyType;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpProxyDeserializer extends StdScalarDeserializer<HttpProxy> {

    public HttpProxyDeserializer(Class<HttpProxy> vc) {
        super(vc);
    }

    @Override
    public HttpProxy deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        HttpProxy httpProxy = new HttpProxy();

        JsonNode enabledNode = node.get("enabled");
        if (enabledNode != null) {
            boolean enabled = enabledNode.asBoolean(false);
            httpProxy.setEnabled(enabled);
        }

        JsonNode useSystemProxyNode = node.get("useSystemProxy");
        if (useSystemProxyNode != null) {
            boolean useSystemProxy = useSystemProxyNode.asBoolean(false);
            httpProxy.setUseSystemProxy(useSystemProxy);
        }

        httpProxy.setHost(readStringValue(node, "host"));

        String sPort = readStringValue(node, "port");
        if (sPort != null) {
            httpProxy.setPort(Integer.parseInt(sPort));
        }

        httpProxy.setPassword(readStringValue(node, "password"));
        httpProxy.setUsername(readStringValue(node, "username"));

        final JsonNode typeNode = node.get("type");
        if (typeNode != null) {
            httpProxy.setType(HttpProxyType.valueOf(typeNode.asText().toUpperCase()));
        }

        return httpProxy;
    }

    private String readStringValue(JsonNode rootNode, String fieldName) {
        JsonNode fieldNode = rootNode.get(fieldName);
        if (fieldNode != null) {
            return fieldNode.asText();
        }

        return null;
    }
}
