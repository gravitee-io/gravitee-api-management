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
package io.gravitee.gamma.rest.core.observability.dashboard.model;

/**
 * Discriminates a dashboard's time window. Lowercase on the wire ({@code relative}/{@code absolute})
 * to match the frontend's discriminated union and the persisted {@code GammaDashboard.TimeRange}
 * shape — see {@code DashboardTimeRangeDto} for the enum-to-lowercase-string mapping.
 *
 * @author GraviteeSource Team
 */
public enum TimeRangeType {
    RELATIVE,
    ABSOLUTE,
}
