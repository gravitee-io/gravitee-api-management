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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class DateAsEpochMillisModelConverterTest {

    private final DateAsEpochMillisModelConverter converter = new DateAsEpochMillisModelConverter();
    private final ModelConverterContext context = mock(ModelConverterContext.class);

    @Test
    void should_map_java_util_date_to_int64() {
        Schema<?> schema = converter.resolve(new AnnotatedType(Date.class), context, Collections.emptyIterator());

        assertThat(schema).isInstanceOf(IntegerSchema.class);
        assertThat(schema.getType()).isEqualTo("integer");
        assertThat(schema.getFormat()).isEqualTo("int64");
    }

    @Test
    void should_keep_description_from_the_default_date_schema() {
        ModelConverter next = mock(ModelConverter.class);
        when(next.resolve(any(), any(), any())).thenReturn(
            new StringSchema().format("date-time").description("When this object was created")
        );

        Schema<?> schema = converter.resolve(new AnnotatedType(Date.class), context, iterator(next));

        assertThat(schema.getType()).isEqualTo("integer");
        assertThat(schema.getFormat()).isEqualTo("int64");
        assertThat(schema.getDescription()).isEqualTo("When this object was created");
    }

    @Test
    void should_delegate_non_date_types() {
        ModelConverter next = mock(ModelConverter.class);
        StringSchema stringSchema = new StringSchema();
        when(next.resolve(any(), any(), any())).thenReturn(stringSchema);

        Schema<?> schema = converter.resolve(new AnnotatedType(String.class), context, iterator(next));

        assertThat(schema).isSameAs(stringSchema);
        verify(next).resolve(any(), any(), any());
    }

    private static Iterator<ModelConverter> iterator(ModelConverter next) {
        return List.of(next).iterator();
    }
}
