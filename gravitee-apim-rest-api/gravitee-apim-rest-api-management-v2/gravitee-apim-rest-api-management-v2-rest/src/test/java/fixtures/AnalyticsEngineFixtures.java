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
package fixtures;

import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Filter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasureName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresRequestMetricsInner;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Operator;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.StringFilter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeRange;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsEngineFixtures {

    public static final String API_ID = "faf12066-2195-423c-ade4-ac5bd2bf8d4f";

    public static final Supplier<MeasuresRequest> v = () -> new MeasuresRequest().timeRange(timeRange());

    public static MeasuresRequest aRequestCountMeasureRequest(Filter... filters) {
        return new MeasuresRequest()
            .timeRange(timeRange())
            .filters(Arrays.asList(filters))
            .metrics(List.of(new MeasuresRequestMetricsInner().name(MetricName.HTTP_REQUESTS).measures(List.of(MeasureName.COUNT))));
    }

    public static TimeRange timeRange() {
        return new TimeRange().from(OffsetDateTime.parse("2025-01-01T00:00:00Z")).to(OffsetDateTime.parse("2025-01-02T00:00:00Z"));
    }
}
