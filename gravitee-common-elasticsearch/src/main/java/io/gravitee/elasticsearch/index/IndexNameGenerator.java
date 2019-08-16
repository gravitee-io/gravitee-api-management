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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface IndexNameGenerator {

    /**
     * Create the ES index name given the timestamp inside the metric.
     *
     * @param type Type of metrics
     * @return the ES index name
     */
    String getIndexName(Type type, Instant timestamp, String[] clusters);

    String getIndexName(Type type, long from, long to, String[] clusters);

    String getTodayIndexName(Type type, String[] clusters);

    String getWildcardIndexName(Type type, String[] clusters);
}
