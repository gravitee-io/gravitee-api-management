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
package io.gravitee.rest.api.management.v2.rest.model.analytics.engine;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/**
 * @author GraviteeSource Team
 */
public class CustomIntervalDeserializer extends StdDeserializer<CustomInterval> {

    protected CustomIntervalDeserializer() {
        super(CustomInterval.class);
    }

    @Override
    public CustomInterval deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonToken token = jp.currentToken();

        if (token == JsonToken.VALUE_NUMBER_INT) {
            return new CustomInterval(jp.getLongValue());
        }

        if (token == JsonToken.VALUE_STRING) {
            String str = jp.getText();
            if (str.isEmpty()) {
                throw new JsonMappingException(ctxt.getParser(), "Interval cannot be empty");
            }

            try {
                return new CustomInterval(Long.parseLong(str));
            } catch (NumberFormatException e) {
                // Not a number, must be a duration string.
                return new CustomInterval(str);
            }
        }

        throw new JsonMappingException(ctxt.getParser(), String.format("Failed deserialization for Interval: %s", token));
    }

    /**
     * Handle deserialization of the 'null' value.
     */
    @Override
    public CustomInterval getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        throw new JsonMappingException(ctxt.getParser(), "Interval cannot be null");
    }
}
