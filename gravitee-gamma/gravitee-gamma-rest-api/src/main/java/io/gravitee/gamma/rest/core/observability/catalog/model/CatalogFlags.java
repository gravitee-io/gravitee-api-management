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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import java.time.Instant;

/**
 * The catalog flags of an entry backed by an additional metric (the {@code CatalogEntryFlags} of the observability
 * OpenAPI).
 *
 * @param origin   {@code null} on the spec the filter registry gives a name read in a query, where only the key's
 *                 grammar matters, not where the field comes from.
 * @param lastSeen {@code null} when unknown or not served to the caller.
 *
 * @author GraviteeSource Team
 */
public record CatalogFlags(CatalogOrigin origin, String rawKey, AdditionalMetricValueType valueType, Instant lastSeen, boolean exposed) {}
