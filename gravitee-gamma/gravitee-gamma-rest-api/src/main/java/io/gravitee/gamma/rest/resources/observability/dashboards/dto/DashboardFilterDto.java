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
package io.gravitee.gamma.rest.resources.observability.dashboards.dto;

import io.gravitee.gamma.rest.core.observability.dashboard.model.DashboardFilter;
import java.util.List;

/**
 * Wire shape for one dashboard filter. {@code operator} is UPPERCASE; {@code value} is always an
 * array, even for single-value operators.
 */
public record DashboardFilterDto(String name, String label, String operator, List<String> value, boolean editable) {
    public static DashboardFilterDto from(DashboardFilter filter) {
        return new DashboardFilterDto(
            filter.condition().name(),
            filter.label(),
            filter.condition().operator().name(),
            filter.condition().values(),
            filter.editable()
        );
    }
}
