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
package io.gravitee.gateway.reactive.core.v4.overlay;

import io.gravitee.node.api.opentelemetry.sampling.SamplingConfig;
import java.util.Map;

/**
 * What one runtime overlay changes on the API it targets. Computed once, when the runtime overlay is activated,
 * so no contributor code runs on the request path. Extension points are added when a new kind needs them.
 */
public record RuntimeOverlayContribution(Map<String, Object> internalAttributes, SamplingConfig sampling) {
    public static RuntimeOverlayContribution ofInternalAttributes(final Map<String, Object> internalAttributes) {
        return new RuntimeOverlayContribution(internalAttributes, null);
    }

    public static RuntimeOverlayContribution ofSampling(final SamplingConfig sampling) {
        return new RuntimeOverlayContribution(Map.of(), sampling);
    }
}
