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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.apim.core.zee.usecase.GenerateResourceUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.model.ZeeRequestDto;
import io.gravitee.rest.api.management.v2.rest.model.ZeeResultDto;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * JAX-RS resource for Zee AI-powered resource generation.
 *
 * <p>
 * Mounted at {@code /environments/{envId}/ai/generate} via
 * {@link io.gravitee.rest.api.management.v2.rest.resource.installation.EnvironmentResource}.
 * Auth is inherited from {@link AbstractResource} — Console JWT filters apply
 * automatically.
 *
 * @author Derek Burger
 */
public class ZeeResource extends AbstractResource {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000L;

    @Inject
    private GenerateResourceUseCase generateResourceUseCase;

    /** Per-environment rate-limit state: [windowStart, count]. */
    private final ConcurrentHashMap<String, long[]> rateLimitState = new ConcurrentHashMap<>();

    @Path("/generate")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(@FormDataParam("request") ZeeRequestDto requestDto, @FormDataParam("files") List<FormDataBodyPart> files) {
        var envId = GraviteeContext.getCurrentEnvironment();
        var orgId = GraviteeContext.getCurrentOrganization();

        // Basic in-memory rate limiting per environment
        if (isRateLimited(envId)) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity("{\"message\":\"Rate limit exceeded. Max " + MAX_REQUESTS_PER_MINUTE + " requests per minute.\"}")
                .build();
        }

        var request = requestDto.toDomain(files);
        var result = generateResourceUseCase.execute(request, envId, orgId);

        return Response.ok(ZeeResultDto.from(result)).build();
    }

    /**
     * Simple sliding-window rate limiter. Returns {@code true} if the request
     * should be rejected.
     */
    private boolean isRateLimited(String envId) {
        var now = System.currentTimeMillis();
        var state = rateLimitState.compute(envId, (key, existing) -> {
            if (existing == null || now - existing[0] >= WINDOW_MS) {
                // New window
                return new long[] { now, 1 };
            }
            existing[1]++;
            return existing;
        });
        return state[1] > MAX_REQUESTS_PER_MINUTE;
    }
}
