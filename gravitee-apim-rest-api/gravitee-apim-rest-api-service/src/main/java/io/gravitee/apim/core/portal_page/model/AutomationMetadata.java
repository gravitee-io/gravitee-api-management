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
package io.gravitee.apim.core.portal_page.model;

import io.gravitee.apim.core.portal.model.PortalId;
import java.util.Optional;

public record AutomationMetadata(
    ReferenceType referenceType,
    String referenceId,
    String name,
    Optional<String> location,
    Optional<Integer> order,
    Optional<PortalArea> area
) {
    public AutomationMetadata(
        ReferenceType referenceType,
        String referenceId,
        String name,
        Optional<String> location,
        Optional<Integer> order
    ) {
        this(referenceType, referenceId, name, location, order, Optional.empty());
    }

    public NavigationItemReference reference() {
        return switch (referenceType) {
            case PORTAL -> PortalId.of(referenceId);
            case API -> ApiId.of(referenceId);
        };
    }

    public enum ReferenceType {
        PORTAL,
        API,
    }
}
