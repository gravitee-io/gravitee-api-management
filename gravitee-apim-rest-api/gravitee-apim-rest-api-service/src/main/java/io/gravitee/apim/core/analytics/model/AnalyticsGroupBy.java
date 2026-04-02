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
package io.gravitee.apim.core.analytics.model;

import java.util.Map;

/**
 * Core domain model for a GROUP_BY analytics result.
 *
 * @param values   document count per distinct field value (e.g. {"200": 1000, "404": 50})
 * @param metadata human-readable labels per field value (e.g. {"200": {"name": "200"}})
 */
public record AnalyticsGroupBy(Map<String, Long> values, Map<String, Map<String, String>> metadata) {}
