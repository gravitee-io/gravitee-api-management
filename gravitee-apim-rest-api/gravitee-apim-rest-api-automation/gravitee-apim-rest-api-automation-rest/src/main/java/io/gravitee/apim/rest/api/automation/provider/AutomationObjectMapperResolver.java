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
package io.gravitee.apim.rest.api.automation.provider;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.apim.rest.api.automation.model.FlowMode;
import io.gravitee.rest.api.management.v2.rest.provider.ObjectMapperResolver;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * The Management API v2 mapper, plus the spellings GKO 4.11 accepted for Automation API enums.
 *
 * @author GraviteeSource Team
 */
@Provider
public class AutomationObjectMapperResolver extends ObjectMapperResolver {

    public AutomationObjectMapperResolver() {
        getContext(Object.class).registerModule(new SimpleModule().addDeserializer(FlowMode.class, new FlowModeDeserializer()));
    }

    /**
     * Reads {@code best-match} as {@code BEST_MATCH}: GKO 4.11 accepted the hyphenated form.
     */
    static class FlowModeDeserializer extends StdScalarDeserializer<FlowMode> {

        FlowModeDeserializer() {
            super(FlowMode.class);
        }

        @Override
        public FlowMode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            try {
                return FlowMode.fromValue(value.replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return (FlowMode) ctxt.handleWeirdStringValue(FlowMode.class, value, "expected DEFAULT or BEST_MATCH");
            }
        }
    }
}
