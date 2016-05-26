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
import io.gravitee.definition.model.services.healthcheck.Expectation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ExpectationDeserializer extends StdScalarDeserializer<Expectation> {

    private final Logger logger = LoggerFactory.getLogger(ExpectationDeserializer.class);

    public ExpectationDeserializer(Class<Expectation> vc) {
        super(vc);
    }

    @Override
    public Expectation deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Expectation expectation = new Expectation();

        final JsonNode assertionsNode = node.get("assertions");
        if (assertionsNode != null) {
            List<String> assertions = new ArrayList<>();
            assertionsNode.elements().forEachRemaining(assertionNode -> assertions.add(assertionNode.asText()));
            expectation.setAssertions(assertions);
        }

        if (expectation.getAssertions() == null || expectation.getAssertions().isEmpty()) {
            logger.error("[healthcheck] Expectation must contains at least a status or assertion(s)");
            throw ctxt.mappingException("[healthcheck] Expectation must contains at least a status or assertion(s)");
        }

        return expectation;
    }
}