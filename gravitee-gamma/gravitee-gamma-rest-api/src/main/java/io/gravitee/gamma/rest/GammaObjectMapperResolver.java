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
package io.gravitee.gamma.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.gravitee.gamma.rest.resources.RejectUnknownProperties;
import io.gravitee.rest.api.management.rest.mapper.ObjectMapperResolver;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * The management mapper, plus a refusal of unknown keys on {@link RejectUnknownProperties} types.
 *
 * <p>It replaces {@link ObjectMapperResolver} rather than sitting next to it: Jersey asks the resolvers
 * in registration order and keeps the first non-null mapper, so a second resolver would never run. The
 * check is per type rather than per mapper so that a body can be tolerant at its root and strict in a
 * nested object, as the dashboard save body is.
 *
 * @author GraviteeSource Team
 */
@Provider
public class GammaObjectMapperResolver extends ObjectMapperResolver {

    public GammaObjectMapperResolver() {
        getContext(Object.class).addHandler(new RejectUnknownPropertiesHandler());
    }

    private static class RejectUnknownPropertiesHandler extends DeserializationProblemHandler {

        @Override
        public boolean handleUnknownProperty(
            DeserializationContext context,
            JsonParser parser,
            JsonDeserializer<?> deserializer,
            Object beanOrClass,
            String propertyName
        ) throws IOException {
            Class<?> type = deserializer.handledType();
            if (type != null && type.isAnnotationPresent(RejectUnknownProperties.class)) {
                throw UnrecognizedPropertyException.from(parser, beanOrClass, propertyName, deserializer.getKnownPropertyNames());
            }
            return false;
        }
    }
}
