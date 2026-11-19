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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.apim.core.portal.model.PortalId;
import jakarta.ws.rs.core.UriInfo;
import java.util.Optional;

/** The portal a Portal API request is scoped to, named by the {@code portalId} path parameter of {@link PortalResource}. */
final class PortalScope {

    static final String PORTAL_ID_PARAM = "portalId";

    private PortalScope() {}

    /**
     * @return empty on the unscoped paths, which read the environment's default portal
     */
    static Optional<PortalId> fromPath(UriInfo uriInfo) {
        return Optional.ofNullable(uriInfo.getPathParameters().getFirst(PORTAL_ID_PARAM)).map(id -> IdParams.portalId(PORTAL_ID_PARAM, id));
    }
}
