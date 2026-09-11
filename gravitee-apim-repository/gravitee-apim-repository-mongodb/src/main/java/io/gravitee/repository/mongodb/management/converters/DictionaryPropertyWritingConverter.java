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

import io.gravitee.repository.mongodb.management.internal.model.DictionaryPropertyMongo;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Mirrors {@code io.gravitee.repository.management.model.DictionaryProperty.Serializer}: an
 * unencrypted property writes back as the bare string it always was, so a dictionary saved
 * through this code doesn't change its on-disk Mongo shape until a value is genuinely encrypted.
 *
 * @author GraviteeSource Team
 */
@WritingConverter
public class DictionaryPropertyWritingConverter implements Converter<DictionaryPropertyMongo, Object> {

    @Override
    public Object convert(DictionaryPropertyMongo source) {
        if (!source.encrypted()) {
            return source.value();
        }
        return new Document("value", source.value()).append("encrypted", true);
    }
}
