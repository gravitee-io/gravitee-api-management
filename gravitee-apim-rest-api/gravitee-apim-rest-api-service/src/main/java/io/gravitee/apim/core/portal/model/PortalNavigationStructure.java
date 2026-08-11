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
package io.gravitee.apim.core.portal.model;

import io.gravitee.apim.core.portal_page.model.PortalArea;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record PortalNavigationStructure(Map<PortalArea, List<NavigationPath>> areas) {
    public PortalNavigationStructure {
        areas = areas == null ? Map.of() : Map.copyOf(areas);
    }

    public static PortalNavigationStructure empty() {
        return new PortalNavigationStructure(Map.of());
    }

    public static PortalNavigationStructure ofTopNavbar(List<NavigationPath> topNavbar) {
        if (topNavbar == null || topNavbar.isEmpty()) {
            return empty();
        }
        return new PortalNavigationStructure(Map.of(PortalArea.TOP_NAVBAR, List.copyOf(topNavbar)));
    }

    public List<NavigationPath> forArea(PortalArea area) {
        return areas.getOrDefault(area, List.of());
    }

    public boolean isEmpty() {
        return areas.isEmpty();
    }
}
