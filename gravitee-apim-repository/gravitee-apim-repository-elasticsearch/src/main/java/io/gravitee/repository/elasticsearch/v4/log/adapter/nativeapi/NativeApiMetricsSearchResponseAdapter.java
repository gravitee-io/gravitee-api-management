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
package io.gravitee.repository.elasticsearch.v4.log.adapter.nativeapi;

import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetrics;
import java.util.List;

public final class NativeApiMetricsSearchResponseAdapter {

    private NativeApiMetricsSearchResponseAdapter() {}

    public static LogResponse<NativeApiMetrics> adapt(SearchResponse response) {
        var hits = response.getSearchHits();
        if (hits == null || hits.getHits() == null) {
            return new LogResponse<>(0, List.of());
        }
        var total = hits.getTotal() == null ? 0 : (int) hits.getTotal().getValue();
        return new LogResponse<>(
            total,
            hits
                .getHits()
                .stream()
                .map(h -> NativeApiMetricsSourceMapper.from(h.getSource()))
                .toList()
        );
    }
}
