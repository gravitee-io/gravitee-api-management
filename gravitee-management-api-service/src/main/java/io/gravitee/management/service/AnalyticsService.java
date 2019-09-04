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
package io.gravitee.management.service;

import io.gravitee.management.model.analytics.HistogramAnalytics;
import io.gravitee.management.model.analytics.HitsAnalytics;
import io.gravitee.management.model.analytics.StatsAnalytics;
import io.gravitee.management.model.analytics.TopHitsAnalytics;
import io.gravitee.management.model.analytics.query.CountQuery;
import io.gravitee.management.model.analytics.query.DateHistogramQuery;
import io.gravitee.management.model.analytics.query.GroupByQuery;
import io.gravitee.management.model.analytics.query.StatsQuery;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AnalyticsService {
    StatsAnalytics execute(StatsQuery query);
    HitsAnalytics execute(CountQuery query);
    HistogramAnalytics execute(DateHistogramQuery query);
    TopHitsAnalytics execute(GroupByQuery query);
}
