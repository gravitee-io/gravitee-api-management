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
package io.gravitee.rest.api.portal.rest.resource.catalog;

import io.gravitee.rest.api.portal.rest.model.CatalogSearchResponse;
import io.gravitee.rest.api.portal.rest.model.CatalogSearchResult;
import io.gravitee.rest.api.portal.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.catalog.search.CatalogSemanticSearcher;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

public class CatalogSearchResource extends AbstractResource {

    @Inject
    private CatalogSemanticSearcher catalogSemanticSearcher;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCatalog(
        @QueryParam("intent") String intent,
        @QueryParam("mode") @DefaultValue("hybrid") String mode,
        @QueryParam("limit") @DefaultValue("5") int limit
    ) {
        if (intent == null || intent.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"'intent' query parameter is required\"}").build();
        }

        try {
            var results = "semantic".equalsIgnoreCase(mode)
                ? catalogSemanticSearcher.semanticSearch(intent, limit)
                : catalogSemanticSearcher.hybridSearch(intent, limit);

            var response = new CatalogSearchResponse()
                .mode("semantic".equalsIgnoreCase(mode) ? CatalogSearchResponse.ModeEnum.SEMANTIC : CatalogSearchResponse.ModeEnum.HYBRID)
                .totalResults(results.size())
                .results(
                    results
                        .stream()
                        .map(scored ->
                            new CatalogSearchResult()
                                .id(scored.item().getId())
                                .title(scored.item().getTitle())
                                .description(scored.item().getDescription())
                                .type(scored.item().getType())
                                .owner(scored.item().getOwner())
                                .tags(scored.item().getTags())
                                .score(scored.score())
                        )
                        .toList()
                );

            return Response.ok(response).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"Search failed: " + e.getMessage() + "\"}")
                .build();
        }
    }
}
