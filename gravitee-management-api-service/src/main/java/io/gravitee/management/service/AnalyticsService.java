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

import io.gravitee.common.data.domain.Order;
import io.gravitee.management.model.analytics.HealthAnalytics;
import io.gravitee.management.model.analytics.HistogramAnalytics;
import io.gravitee.management.model.analytics.HitsAnalytics;
import io.gravitee.management.model.analytics.TopHitsAnalytics;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AnalyticsService {

    HistogramAnalytics hitsBy(String query, String key, String field, List<String> aggTypes, long from, long to, long interval);

    HitsAnalytics globalHits(String query, String key, long from, long to);

    TopHitsAnalytics topHits(String query, String key, String field, long from, long to, int size);

    TopHitsAnalytics topHits(String query, String key, String field, Order order, long from, long to, int size);

    HistogramAnalytics apiKeyHits(String apiKey, long from, long to, long interval);

    HistogramAnalytics apiKeyHitsByStatus(String apiKey, long from, long to, long interval);

    HistogramAnalytics apiKeyHitsByLatency(String apiKey, long from, long to, long interval);

    HealthAnalytics health(String api, long from, long to, long interval);
}
