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
package io.gravitee.apim.core.analytics_engine.query_service;

import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricDiscovery;

public interface AdditionalMetricDiscoveryQueryService {
    /**
     * The additional metrics the requests of the environment carried over the last 30 days, uncapped. A pass is
     * refreshed in the background a few minutes after it ran, the previous one served meanwhile, so a key a newly
     * deployed plugin starts writing shows up after the next refresh. A pass that fails keeps the previous successful
     * one, or answers {@link AdditionalMetricDiscovery#unavailable} when there is none; it is run again a minute later,
     * never on each request.
     *
     * <p>A key stored under another type than its prefix declares is left out, including a key stored under its declared
     * type in some indexes of the window and another type in others. Keys are not checked against the rest of the
     * additional metric grammar: callers drop what their parser refuses.
     *
     * @throws IllegalArgumentException when no organization or no environment is given: every environment of a cluster
     *     shares the indexes
     */
    AdditionalMetricDiscovery discover(String organizationId, String environmentId);
}
