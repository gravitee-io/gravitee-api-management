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
package io.gravitee.gamma.module.platform.rest.resource.am;

import io.gravitee.apim.plugin.gamma.api.identity.AmNotConfiguredException;
import io.gravitee.gamma.module.platform.core.am.exception.AmUpstreamException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

// Translates AM-specific exceptions whose HTTP shape APIM's registered mappers don't preserve:
// AmNotConfiguredException → 503 with code "am_not_configured" (the UI banner keys on this);
// AmUpstreamException → upstream status (or 502) so AM 4xx/5xx aren't flattened to 500.
final class AmCalls {

    private AmCalls() {}

    static <T> T run(Supplier<T> action) {
        try {
            return action.get();
        } catch (AmNotConfiguredException e) {
            throw notConfigured(e);
        } catch (AmUpstreamException e) {
            throw upstream(e);
        }
    }

    private static WebApplicationException notConfigured(AmNotConfiguredException e) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", e.code());
        body.put("message", e.getMessage() == null ? "" : e.getMessage());
        return new WebApplicationException(Response.status(503).type(MediaType.APPLICATION_JSON).entity(body).build());
    }

    private static WebApplicationException upstream(AmUpstreamException e) {
        Integer upstream = e.upstreamStatus();
        int status = upstream != null ? upstream : 502;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "am_upstream_error");
        body.put("message", e.getMessage() == null ? "" : e.getMessage());
        body.put("upstreamStatus", upstream);

        return new WebApplicationException(Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build());
    }
}
