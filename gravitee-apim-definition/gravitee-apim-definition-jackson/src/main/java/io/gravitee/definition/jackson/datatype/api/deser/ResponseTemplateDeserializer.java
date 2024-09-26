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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import io.gravitee.definition.model.ResponseTemplate;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTemplateDeserializer extends StdScalarDeserializer<ResponseTemplate> {

    public ResponseTemplateDeserializer(Class<ResponseTemplate> vc) {
        super(vc);
    }

    @Override
    public ResponseTemplate deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        ResponseTemplate template = new ResponseTemplate();

        template.setStatusCode(node.path("status").asInt());
        template.setBody(node.path("body").asText());
        template.setPropagateErrorKeyToLogs(node.path("propagateErrorKeyToLogs").asBoolean());

        JsonNode headersNode = node.get("headers");
        if (headersNode != null && !headersNode.isEmpty(null)) {
            Map<String, String> headers = headersNode.traverse(jp.getCodec()).readValueAs(new TypeReference<HashMap<String, String>>() {});
            if (headers != null && !headers.isEmpty()) {
                if (template.getHeaders() == null) {
                    template.setHeaders(headers);
                } else {
                    headers.forEach(template.getHeaders()::putIfAbsent);
                }
            }
        }

        return template;
    }
}
