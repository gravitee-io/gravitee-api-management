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
package io.gravitee.repository.analytics.engine.api.result;

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import java.util.List;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public record FacetBucketResult(String key, List<FacetBucketResult> buckets, Map<Measure, Number> measures) {
    public static FacetBucketResult ofBuckets(String key, List<FacetBucketResult> buckets) {
        return new FacetBucketResult(key, buckets, null);
    }

    public static FacetBucketResult ofMeasures(String key, Map<Measure, Number> measures) {
        return new FacetBucketResult(key, null, measures);
    }
}
