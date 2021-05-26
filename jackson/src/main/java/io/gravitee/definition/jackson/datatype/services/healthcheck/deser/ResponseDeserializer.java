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
import io.gravitee.definition.model.services.healthcheck.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseDeserializer extends StdScalarDeserializer<Response> {

    public ResponseDeserializer(Class<Response> vc) {
        super(vc);
    }

    @Override
    public Response deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Response response = new Response();

        final JsonNode assertionsNode = node.get("assertions");
        if (assertionsNode != null) {
            List<String> assertions = new ArrayList<>();
            assertionsNode.elements().forEachRemaining(assertionNode -> assertions.add(assertionNode.asText()));
            response.setAssertions(assertions);
        }

        if (response.getAssertions().isEmpty()) {
            // Add default assertion
            response.setAssertions(Collections.singletonList(Response.DEFAULT_ASSERTION));
        }

        return response;
    }
}
