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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context built once per create validation (single or bulk) to hold shared data and avoid repeated fetches.
 */
public record CreateValidationContext(
    List<PortalNavigationItem> navigationItems,
    Map<PortalNavigationItemId, PortalNavigationItem> itemsById,
    Set<String> seenApiIdsInPayload
) {
    public static CreateValidationContext empty() {
        return new CreateValidationContext(List.of(), Map.of(), new HashSet<>());
    }
}
