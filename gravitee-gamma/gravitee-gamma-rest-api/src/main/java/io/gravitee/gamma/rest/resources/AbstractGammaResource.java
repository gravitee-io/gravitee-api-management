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
package io.gravitee.gamma.rest.resources;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class AbstractGammaResource {

    @Context
    protected UriInfo uriInfo;

    protected UserDetails getAuthenticatedUserDetails() {
        return (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    protected AuditInfo getAuditInfo() {
        var ctx = GraviteeContext.getExecutionContext();
        var user = getAuthenticatedUserDetails();
        return AuditInfo.builder()
            .organizationId(ctx.getOrganizationId())
            .environmentId(ctx.getEnvironmentId())
            .actor(AuditActor.builder().userId(user.getUsername()).userSource(user.getSource()).userSourceId(user.getSourceId()).build())
            .build();
    }

    protected URI getLocationHeader(String... paths) {
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        for (String path : paths) {
            builder.path(path);
        }
        return builder.build();
    }
}
