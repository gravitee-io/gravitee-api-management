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
package io.gravitee.elasticsearch.index;

import io.gravitee.elasticsearch.utils.DateUtils;
import io.gravitee.elasticsearch.utils.Type;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractIndexNameGenerator implements IndexNameGenerator {

    private static final char INDEX_SEPARATOR = '-';
    private static final char INDEX_WILDCARD = '*';

    private final DateTimeFormatter ES_DAILY_INDICE = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault());

    @Override
    public String getIndexName(Type type, Instant instant) {
        return getIndexPrefix(type) + '-' + ES_DAILY_INDICE.format(instant);
    }

    @Override
    public String getIndexName(Type type, long from, long to) {
        return DateUtils.rangedIndices(from, to)
                .stream()
                .map(date -> getIndexPrefix(type) + INDEX_SEPARATOR + date)
                .collect(Collectors.joining(","));
    }

    @Override
    public String getTodayIndexName(Type type) {
        final String suffixDay = LocalDate.now().format(ES_DAILY_INDICE);
        return getIndexPrefix(type) + INDEX_SEPARATOR + suffixDay;
    }

    @Override
    public String getWildcardIndexName(Type type) {
        return getIndexPrefix(type) + INDEX_SEPARATOR + INDEX_WILDCARD;
    }

    protected abstract String getIndexPrefix(Type type);
}
