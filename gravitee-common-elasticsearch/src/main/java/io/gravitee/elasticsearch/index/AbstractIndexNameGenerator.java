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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractIndexNameGenerator implements IndexNameGenerator {

    private static final char CLUSTER_SEPARATOR = ':';
    private static final char INDEX_DATE_SEPARATOR = '-';
    private static final String INDEX_SEPARATOR = ",";
    private static final char INDEX_WILDCARD = '*';

    private final DateTimeFormatter ES_DAILY_INDICE = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.systemDefault());

    @Override
    public String getIndexName(Type type, Instant instant, String[] clusters) {
        if (clusters == null || clusters.length == 0) {
            return getIndexPrefix(type) + INDEX_DATE_SEPARATOR + ES_DAILY_INDICE.format(instant);
        } else {
            return Stream.of(clusters)
                    .map(cluster -> cluster + CLUSTER_SEPARATOR + getIndexPrefix(type) + INDEX_DATE_SEPARATOR + ES_DAILY_INDICE.format(instant))
                    .collect(Collectors.joining(INDEX_SEPARATOR));
        }
    }

    @Override
    public String getIndexName(Type type, long from, long to, String[] clusters) {
        if (clusters == null || clusters.length == 0) {
            return DateUtils.rangedIndices(from, to)
                    .stream()
                    .map(date -> getIndexPrefix(type) + INDEX_DATE_SEPARATOR + date)
                    .collect(Collectors.joining(INDEX_SEPARATOR));
        } else {
            return DateUtils.rangedIndices(from, to)
                    .stream()
                    .flatMap((Function<String, Stream<String>>) date -> Stream.of(clusters)
                            .map(cluster -> cluster + CLUSTER_SEPARATOR + getIndexPrefix(type) + INDEX_DATE_SEPARATOR + date))
                    .collect(Collectors.joining(INDEX_SEPARATOR));
        }
    }

    @Override
    public String getTodayIndexName(Type type, String[] clusters) {
        final String suffixDay = LocalDate.now().format(ES_DAILY_INDICE);
        if (clusters == null || clusters.length == 0) {
            return getIndexPrefix(type) + INDEX_DATE_SEPARATOR + suffixDay;
        } else {
            return Stream.of(clusters)
                    .map(cluster -> cluster + CLUSTER_SEPARATOR + getIndexPrefix(type) + INDEX_DATE_SEPARATOR + suffixDay)
                    .collect(Collectors.joining(INDEX_SEPARATOR));
        }
    }

    @Override
    public String getWildcardIndexName(Type type, String[] clusters) {
        if (clusters == null || clusters.length == 0) {
            return getIndexPrefix(type) + INDEX_DATE_SEPARATOR + INDEX_WILDCARD;
        } else {
            return Stream.of(clusters)
                    .map(cluster -> cluster + CLUSTER_SEPARATOR + getIndexPrefix(type) + INDEX_DATE_SEPARATOR + INDEX_WILDCARD)
                    .collect(Collectors.joining(INDEX_SEPARATOR));
        }
    }

    protected abstract String getIndexPrefix(Type type);
}
