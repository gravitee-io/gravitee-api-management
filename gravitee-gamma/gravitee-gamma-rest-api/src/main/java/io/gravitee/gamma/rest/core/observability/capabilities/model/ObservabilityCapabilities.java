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
package io.gravitee.gamma.rest.core.observability.capabilities.model;

/**
 * What the caller may open in the observability UIs: the sections and actions a UI offers. The data each section
 * shows still comes from the rows the caller reads, and the saved dashboards still need
 * {@code ENVIRONMENT_DASHBOARD[READ]}: {@code dashboardsRead} opens the built-in dashboards.
 *
 * @author GraviteeSource Team
 */
public record ObservabilityCapabilities(boolean logsRead, boolean tracesRead, boolean dashboardsRead, boolean dashboardsWrite) {}
