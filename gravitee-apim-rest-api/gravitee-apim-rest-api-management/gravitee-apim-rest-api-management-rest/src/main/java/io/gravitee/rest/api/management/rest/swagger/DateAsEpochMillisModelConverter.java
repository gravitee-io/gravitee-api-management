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
package io.gravitee.rest.api.management.rest.swagger;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Date;
import java.util.Iterator;

/**
 * Management API v1 serializes {@link Date} as Unix epoch milliseconds. swagger-core v3 would otherwise
 * publish those fields as string/date-time.
 */
public class DateAsEpochMillisModelConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (isJavaUtilDate(type)) {
            Schema<?> fromChain = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
            IntegerSchema epochMillis = new IntegerSchema().format("int64");
            if (fromChain != null) {
                epochMillis.setDescription(fromChain.getDescription());
                epochMillis.setTitle(fromChain.getTitle());
                epochMillis.setNullable(fromChain.getNullable());
            }
            return epochMillis;
        }
        return chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    }

    private static boolean isJavaUtilDate(AnnotatedType type) {
        if (type == null || type.getType() == null) {
            return false;
        }
        JavaType javaType = Json.mapper().constructType(type.getType());
        return javaType != null && Date.class.isAssignableFrom(javaType.getRawClass());
    }
}
