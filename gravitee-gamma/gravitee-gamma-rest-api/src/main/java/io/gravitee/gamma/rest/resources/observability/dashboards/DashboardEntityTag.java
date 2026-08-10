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
package io.gravitee.gamma.rest.resources.observability.dashboards;

import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.DashboardDto;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Response;

/**
 * The one place a dashboard's {@code ETag} is spelled.
 *
 * <p>Every response that names a revision goes through here — create, read, update, and the {@code 412} that refuses a
 * save. {@code If-Match} only works if the value handed out on one response is the value accepted on the next request,
 * and independent formatting sites are how that quietly stops being true: the refusal path formatted its own tag for
 * exactly one commit, and got the no-version case wrong that the read path had already fixed.
 *
 * @author GraviteeSource Team
 */
public final class DashboardEntityTag {

    private DashboardEntityTag() {}

    /** Attaches the dashboard as the response entity, plus its {@code ETag}. */
    public static Response.ResponseBuilder withETag(Response.ResponseBuilder builder, Dashboard dashboard) {
        return withETagOnly(builder.entity(DashboardDto.from(dashboard)), dashboard);
    }

    /**
     * Attaches the {@code ETag} alone, for responses carrying something other than the dashboard itself.
     *
     * <p>No version, no tag — rather than the string {@code "null"}, which would be a validator this API refuses on
     * the way back in, leaving the dashboard unsaveable by a client that did exactly what it was told. Omitting the
     * header states the truth instead: there is nothing to match on, so the only way to save it is {@code If-Match: *}.
     */
    public static Response.ResponseBuilder withETagOnly(Response.ResponseBuilder builder, Dashboard dashboard) {
        return dashboard.version() == null ? builder : builder.tag(new EntityTag(String.valueOf(dashboard.version())));
    }
}
