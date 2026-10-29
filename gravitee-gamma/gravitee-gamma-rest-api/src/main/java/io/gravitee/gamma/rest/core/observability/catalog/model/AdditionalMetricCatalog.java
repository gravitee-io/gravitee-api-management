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
import java.util.List;

/**
 * The additional metrics of an environment, built-in, declared and discovered, one entry per key.
 *
 * @param refreshedAt when the discovery pass behind the {@code DISCOVERED} entries and {@code lastSeen} ran.
 * @param truncated   {@code true} when the pass left keys out.
 *
 * @author GraviteeSource Team
 */
public record AdditionalMetricCatalog(List<AdditionalMetricCatalogEntry> entries, Instant refreshedAt, boolean truncated) {
    public AdditionalMetricCatalog {
        entries = List.copyOf(entries);
    }
}
