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
package io.gravitee.apim.core.subscription.model;

import java.util.List;

public record PortalApiProductSubscriptionDetails(
    String id,
    String name,
    String version,
    Availability availability,
    PlanSummary plan,
    List<ApiSummary> apis
) {
    public enum Availability {
        AVAILABLE,
        UNAVAILABLE,
    }

    public enum ApiAvailability {
        AVAILABLE,
        UNPUBLISHED,
        UNAVAILABLE,
    }

    public record PlanSummary(String id, String name, String security, String mode) {}

    public record ApiSummary(
        String id,
        String name,
        String version,
        String type,
        ApiAvailability availability,
        List<String> entrypoints,
        DocumentationTarget documentation
    ) {}

    public record DocumentationTarget(String rootId, String navigationItemId) {}
}
