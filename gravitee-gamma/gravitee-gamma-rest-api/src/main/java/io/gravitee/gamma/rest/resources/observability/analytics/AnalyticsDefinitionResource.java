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
package io.gravitee.gamma.rest.resources.observability.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.GetObservabilityAnalyticsDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.resources.observability.EnumQueryParam;
import io.gravitee.gamma.rest.resources.observability.analytics.dto.AnalyticsDefinitionResponseDto;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Providers;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * The analytics definition the widget editor is built from, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/analytics/definition}. {@code apiType},
 * required, names the kinds described; {@code origin}, optional, keeps the entries of those origins only.
 *
 * <h2>Authorization</h2>
 * A login only, like the filter catalog: the built-in metrics and the environment's records hold no traffic data.
 * The {@code DISCOVERED} metrics are traffic data, served by the use case to a caller who reads the environment's
 * traffic only (see {@code TrafficReadPermissionPort}).
 *
 * <h2>Caching</h2>
 * The body depends on the environment, on its records and on the caller's traffic right, and the discovery behind
 * its {@code DISCOVERED} entries refreshes every few minutes: the response is {@code private} and the client
 * revalidates it on every use. Its {@code ETag} hashes the response as the application's mapper writes it, so every
 * node of a cluster hands out the same one for the same body, and a client revalidating it gets a {@code 304}.
 *
 * @author GraviteeSource Team
 */
public class AnalyticsDefinitionResource {

    private static final CacheControl REVALIDATE = CacheControl.valueOf("private, no-cache");

    @Inject
    private GetObservabilityAnalyticsDefinitionUseCase getAnalyticsDefinitionUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAnalyticsDefinition(
        @QueryParam("apiType") List<String> apiTypes,
        @QueryParam("origin") List<String> origins,
        @Context Request request,
        @Context Providers providers
    ) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = getAnalyticsDefinitionUseCase.execute(
            new GetObservabilityAnalyticsDefinitionUseCase.Input(
                ctx.getOrganizationId(),
                ctx.getEnvironmentId(),
                EnumQueryParam.parse(apiTypes, ApiType.class, InvalidObservabilityQueryException::notAnApiKind),
                EnumQueryParam.parse(origins, CatalogOrigin.class, "origin")
            )
        );
        var body = AnalyticsDefinitionResponseDto.from(output.definition(), output.truncated());
        var tag = entityTagOf(body, providers);
        var notModified = request.evaluatePreconditions(tag);
        if (notModified != null) {
            return notModified.tag(tag).cacheControl(REVALIDATE).build();
        }
        return Response.ok(body).tag(tag).cacheControl(REVALIDATE).build();
    }

    private static EntityTag entityTagOf(AnalyticsDefinitionResponseDto body, Providers providers) {
        var resolver = providers.getContextResolver(ObjectMapper.class, MediaType.APPLICATION_JSON_TYPE);
        if (resolver == null) {
            throw new IllegalStateException("No JSON mapper is registered to compute the ETag of the analytics definition");
        }
        try {
            var bytes = resolver.getContext(AnalyticsDefinitionResponseDto.class).writeValueAsBytes(body);
            return new EntityTag(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot compute the ETag of the analytics definition", e);
        }
    }
}
