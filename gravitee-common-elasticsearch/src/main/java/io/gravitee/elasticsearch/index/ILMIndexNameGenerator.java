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

import io.gravitee.elasticsearch.utils.Type;

import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * For the specific case of ILM, we don't care about the date in the index name, everything is managed internally
 * by ILM.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ILMIndexNameGenerator extends AbstractIndexNameGenerator {

    private static final char CLUSTER_SEPARATOR = ':';
    private static final String INDEX_SEPARATOR = ",";

    private final String indexPrefix;

    public ILMIndexNameGenerator(String indexPrefix) {
        this.indexPrefix = indexPrefix;
    }

    @Override
    public String getIndexName(Type type, Instant instant, String[] clusters) {
        return getILMIndexName(type, clusters);
    }

    @Override
    public String getIndexName(Type type, long from, long to, String[] clusters) {
        return getILMIndexName(type, clusters);
    }

    @Override
    public String getTodayIndexName(Type type, String[] clusters) {
        return getILMIndexName(type, clusters);
    }

    @Override
    public String getWildcardIndexName(Type type, String[] clusters) {
        return getILMIndexName(type, clusters);
    }

    private String getILMIndexName(Type type, String[] clusters) {
        if (clusters == null || clusters.length == 0) {
            return getIndexPrefix(type);
        } else {
            return Stream.of(clusters)
                    .map(cluster -> cluster + CLUSTER_SEPARATOR + getIndexPrefix(type))
                    .collect(Collectors.joining(INDEX_SEPARATOR));
        }
    }

    @Override
    protected String getIndexPrefix(Type type) {
        return indexPrefix + '-' + type.getType();
    }
}
