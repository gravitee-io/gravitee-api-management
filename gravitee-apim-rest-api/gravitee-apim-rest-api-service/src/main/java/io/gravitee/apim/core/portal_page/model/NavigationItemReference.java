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
    /**
     * A method rather than a constant, because {@code sharesRootNamespaceWith} makes this a
     * superinterface declaring a default method: initializing {@link PortalReference} therefore also
     * initializes this interface (JLS 12.4.1). A constant initializer here would read
     * {@code PortalReference.DEFAULT} while that record is still mid-initialization on the same
     * thread, see {@code null}, and keep it forever. Deferring the read to call time breaks the cycle.
     */
    static NavigationItemReference defaultReference() {
        return PortalReference.DEFAULT;
    }

    default boolean sharesRootNamespaceWith(NavigationItemReference other) {
        return switch (this) {
            case ApiReference api -> other instanceof ApiReference otherApi && api.apiId().equals(otherApi.apiId());
            // All portals share one root namespace: multiple portals per environment is opt-in
            // configuration, and their roots are not told apart here.
            case PortalReference ignored -> !(other instanceof ApiReference);
        };
    }

    record PortalReference(PortalId portalId) implements NavigationItemReference {
        public static final PortalReference DEFAULT = new PortalReference(PortalId.ZERO);
    }

    record ApiReference(String apiId) implements NavigationItemReference {}
}
