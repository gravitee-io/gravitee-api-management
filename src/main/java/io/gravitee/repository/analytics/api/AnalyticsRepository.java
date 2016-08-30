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
package io.gravitee.repository.analytics.api;

import io.gravitee.common.data.domain.Order;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.Query;
import io.gravitee.repository.analytics.query.response.HealthResponse;
import io.gravitee.repository.analytics.query.response.HitsResponse;
import io.gravitee.repository.analytics.query.response.Response;
import io.gravitee.repository.analytics.query.response.TopHitsResponse;
import io.gravitee.repository.analytics.query.response.histogram.HistogramResponse;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AnalyticsRepository {

   <T extends Response> T query(Query<T> query) throws AnalyticsException;

   HealthResponse query(String api, long interval, long from, long to) throws AnalyticsException;

   HitsResponse query(String query, String key, long from, long to) throws AnalyticsException;

   TopHitsResponse query(String query, String key, String field, Order order, long from, long to, int size) throws AnalyticsException;

   HistogramResponse query(String query, String key, String field, List<String> aggTypes, long from, long to, long interval) throws AnalyticsException;

}
