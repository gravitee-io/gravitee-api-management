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
package io.gravitee.repository.management.model;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * What an environment says about one additional metric key: its label, description and unit, and whether the
 * observability pickers offer it. One record per key and environment.
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(of = "id")
public class GammaMetricDefinition {

    private String id;
    private String environmentId;

    /**
     * Key of the additional metric, type prefix included, as the gateway reports it, e.g.
     * {@code double_checkout_basket-value}. Unique per environment. May contain {@code /} and {@code .}.
     */
    private String rawKey;

    private String label;
    private String description;

    /** A unit name ({@code MILLISECONDS}, …) or a free suffix ({@code €}); this layer stores it verbatim. */
    private String unit;

    private boolean exposed;
    private Date createdAt;
    private Date updatedAt;
}
