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
package io.gravitee.definition.jackson.datatype.services.healthcheck.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.services.healthcheck.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestDeserializer extends StdScalarDeserializer<Request> {

    public RequestDeserializer(Class<Request> vc) {
        super(vc);
    }

    @Override
    public Request deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Request request = new Request();

        JsonNode pathNode = node.get("path");
        if (pathNode != null) {
            request.setPath(pathNode.asText());
        } else {
            // Ensure backward compatibility
            JsonNode uriNode = node.get("uri");
            if (uriNode != null) {
                request.setPath(uriNode.asText());
            } else {
                throw ctxt.mappingException("[healthcheck] URI is required");
            }
        }

        final JsonNode methodNode = node.get("method");
        if (methodNode != null) {
            request.setMethod(HttpMethod.valueOf(methodNode.asText().toUpperCase()));
        } else {
            throw ctxt.mappingException("[healthcheck] Method is required");
        }

        JsonNode bodyNode = node.get("body");
        if (bodyNode != null) {
            request.setBody(bodyNode.asText());
        }

        JsonNode headersNode = node.get("headers");
        if (headersNode != null) {
            List<HttpHeader> headers = new ArrayList<>();
            headersNode.elements().forEachRemaining(headerNode -> {
                HttpHeader header = new HttpHeader();
                header.setName(headerNode.findValue("name").asText());
                header.setValue(headerNode.findValue("value").asText());
                headers.add(header);
            });

            request.setHeaders(headers);
        }

        request.setFromRoot(node.path("fromRoot").asBoolean(false));

        return request;
    }
}