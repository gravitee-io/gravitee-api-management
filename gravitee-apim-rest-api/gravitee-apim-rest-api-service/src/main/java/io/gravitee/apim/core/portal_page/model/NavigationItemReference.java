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

public sealed interface NavigationItemReference {
    default boolean sharesRootNamespaceWith(NavigationItemReference other) {
        return switch (this) {
            case ApiReference api -> other instanceof ApiReference otherApi && api.apiId().equals(otherApi.apiId());
            case PortalReference portal -> other instanceof PortalReference otherPortal && portal.portalId().equals(otherPortal.portalId());
        };
    }

    record PortalReference(PortalId portalId) implements NavigationItemReference {}

    record ApiReference(String apiId) implements NavigationItemReference {}
}
