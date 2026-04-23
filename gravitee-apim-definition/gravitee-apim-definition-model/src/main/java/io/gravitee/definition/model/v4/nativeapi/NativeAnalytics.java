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
package io.gravitee.definition.model.v4.nativeapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NativeAnalytics {

    /** Gates event-metrics reporting. Independent of reporterMetricsEnabled. */
    // TODO: rename to `eventMetricsEnabled` for symmetry with `reporterMetricsEnabled`. Breaking change — persisted in v4 API definitions.
    @Builder.Default
    protected boolean enabled = true;

    /** Gates the connection-metrics reporter on the gateway. Independent of analytics.enabled. */
    @Builder.Default
    protected boolean reporterMetricsEnabled = true;
}
