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
import com.fasterxml.jackson.databind.node.TextNode;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggingDeserializer extends StdScalarDeserializer<Logging> {

    private static final JsonNode NULL = new TextNode("null");

    public LoggingDeserializer(Class<Logging> vc) {
        super(vc);
    }

    @Override
    public Logging deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        Logging logging = new Logging();

        JsonNode mode = node.get("mode");
        if (mode != null) {
            logging.setMode(LoggingMode.valueOf(mode.asText().toUpperCase()));
        }

        JsonNode condition = node.get("condition");

        // since 1.20
        // test "null" for legacy configuration
        if (condition != null && !NULL.equals(condition)) {
            logging.setCondition(condition.asText());
        }

        return logging;
    }
}
