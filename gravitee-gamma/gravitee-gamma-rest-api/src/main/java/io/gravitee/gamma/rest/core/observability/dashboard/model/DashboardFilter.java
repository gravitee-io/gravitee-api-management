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

import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;

/**
 * A dashboard-definition filter, applied to every widget of the dashboard. Composes the shared
 * {@link FilterCondition} (name/operator/values) rather than redeclaring it, and adds the two fields
 * a dashboard filter needs on top: a persisted display {@link #label} (not derivable — see
 * {@code GammaDashboard.Filter}) and whether the viewer may change the value.
 *
 * @author GraviteeSource Team
 */
public record DashboardFilter(FilterCondition condition, String label, boolean editable) {}
