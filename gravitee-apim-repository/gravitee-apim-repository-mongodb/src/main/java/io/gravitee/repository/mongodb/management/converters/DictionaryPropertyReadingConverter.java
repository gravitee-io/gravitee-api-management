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
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * A property stored as a bare BSON string is unencrypted. This is both the pre-existing shape and
 * the shape {@link DictionaryPropertyWritingConverter} keeps producing for unencrypted values.
 *
 * @author GraviteeSource Team
 */
@ReadingConverter
public class DictionaryPropertyReadingConverter implements Converter<String, DictionaryPropertyMongo> {

    @Override
    public DictionaryPropertyMongo convert(String source) {
        return new DictionaryPropertyMongo(source, false);
    }
}
