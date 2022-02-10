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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.HttpRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpRequestDeserializer extends StdScalarDeserializer<HttpRequest> {

    public HttpRequestDeserializer(Class<HttpRequest> vc) {
        super(vc);
    }

    @Override
    public HttpRequest deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setPath(readStringValue(node, "path"));
        httpRequest.setMethod(readStringValue(node, "method"));
        httpRequest.setBody(readStringValue(node, "body"));

        JsonNode headersNode = node.get("headers");
        if (headersNode != null && !headersNode.isEmpty(null)) {
            httpRequest.setHeaders(headersNode.traverse(jp.getCodec()).readValueAs(new TypeReference<HashMap<String, List<String>>>() {}));
        }

        return httpRequest;
    }

    private String readStringValue(JsonNode rootNode, String fieldName) {
        JsonNode fieldNode = rootNode.get(fieldName);
        if (fieldNode != null) {
            return fieldNode.asText();
        }
        return null;
    }
}
