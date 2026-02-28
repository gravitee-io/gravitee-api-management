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

import io.gravitee.apim.core.zee.domain_service.LlmEngineService;
import io.gravitee.apim.core.zee.usecase.GenerateResourceUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.model.Error;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final int MAX_PROMPT_LENGTH = 2000;
    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("json", "yaml", "yml", "txt", "md");

    @Inject
    private GenerateResourceUseCase generateResourceUseCase;

    /** Per-environment rate-limit state: [windowStart, count]. */
    private final ConcurrentHashMap<String, long[]> rateLimitState = new ConcurrentHashMap<>();

    @Path("/generate")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(@FormDataParam("request") ZeeRequestDto requestDto,
            @FormDataParam("files") List<FormDataBodyPart> files) {
        var envId = GraviteeContext.getCurrentEnvironment();
        var orgId = GraviteeContext.getCurrentOrganization();

        // Rate limit first — prevents validation probing without consuming quota
        if (isRateLimited(envId)) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                    .entity(new Error().httpStatus(429)
                            .message("Rate limit exceeded. Max " + MAX_REQUESTS_PER_MINUTE + " requests per minute."))
                    .build();
        }

        // Null / empty prompt guard
        if (requestDto == null || requestDto.getPrompt() == null || requestDto.getPrompt().trim().isEmpty()) {
            return badRequest("Prompt is required.");
        }

        // Prompt length guard
        if (requestDto.getPrompt().length() > MAX_PROMPT_LENGTH) {
            return badRequest("Prompt too long. Max " + MAX_PROMPT_LENGTH + " characters.");
        }

        // File count guard
        if (files != null && files.size() > MAX_FILES) {
            return badRequest("Too many files. Max " + MAX_FILES + ".");
        }

        // Per-file guards (size + type)
        if (files != null) {
            for (FormDataBodyPart part : files) {
                var body = part.getValueAs(String.class);
                if (body != null
                        && body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_FILE_SIZE_BYTES) {
                    return badRequest("File too large. Max 5MB per file.");
                }
                var filename = part.getContentDisposition() != null ? part.getContentDisposition().getFileName() : null;
                if (filename != null) {
                    var ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                            : "";
                    if (!ALLOWED_EXTENSIONS.contains(ext)) {
                        return badRequest(
                                "Unsupported file type \"" + filename + "\". Allowed: json, yaml, yml, txt, md.");
                    }
                }
            }
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

    /**
     * Builds a 400 Bad Request response using the project's standard {@link Error}
     * model.
     */
    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                .entity(new Error().httpStatus(Response.Status.BAD_REQUEST.getStatusCode()).message(message))
                .build();
    }
}
