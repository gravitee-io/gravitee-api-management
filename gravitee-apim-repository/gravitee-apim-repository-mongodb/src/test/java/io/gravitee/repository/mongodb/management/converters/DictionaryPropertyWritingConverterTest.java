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
package io.gravitee.repository.mongodb.management.converters;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.mongodb.management.internal.model.DictionaryPropertyMongo;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class DictionaryPropertyWritingConverterTest {

    private final DictionaryPropertyWritingConverter cut = new DictionaryPropertyWritingConverter();

    @Test
    void should_write_an_unencrypted_property_as_a_bare_string() {
        Object result = cut.convert(new DictionaryPropertyMongo("v", false));

        assertThat(result).isInstanceOf(String.class).isEqualTo("v");
    }

    @Test
    void should_write_an_encrypted_property_as_a_value_and_flag_document() {
        Object result = cut.convert(new DictionaryPropertyMongo("v", true));

        assertThat(result).isInstanceOf(Document.class).isEqualTo(new Document("value", "v").append("encrypted", true));
    }
}
