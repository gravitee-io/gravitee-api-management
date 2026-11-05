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
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The organization, the environment and who is acting, as {@code AuditDomainService} needs them.
 *
 * <p>Resolved in the REST layer rather than inside a domain: both the execution context and the
 * acting user come from the request, which the core may not reach (AGENTS.md §5). The gamma
 * resources do not extend management-v2's {@code AbstractResource}, so there is no inherited
 * {@code getAuthenticatedUserDetails()} to call.
 *
 * @author GraviteeSource Team
 */
public final class CurrentAuditInfo {

    public static AuditInfo of() {
        var ctx = GraviteeContext.getExecutionContext();
        return AuditInfo.builder().organizationId(ctx.getOrganizationId()).environmentId(ctx.getEnvironmentId()).actor(actor()).build();
    }

    /**
     * An empty actor rather than a failure when nothing identifies the caller: the audit log records
     * what happened, and losing the whole entry because the principal is not a {@link UserDetails}
     * would hide the change itself.
     */
    private static AuditActor actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails user)) {
            return AuditActor.builder().build();
        }
        return AuditActor.builder().userId(user.getUsername()).userSource(user.getSource()).userSourceId(user.getSourceId()).build();
    }

    private CurrentAuditInfo() {}
}
