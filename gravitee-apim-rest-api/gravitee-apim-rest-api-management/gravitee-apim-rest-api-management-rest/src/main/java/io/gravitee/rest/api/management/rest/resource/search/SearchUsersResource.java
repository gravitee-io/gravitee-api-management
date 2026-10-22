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
package io.gravitee.rest.api.management.rest.resource.search;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.idp.api.identity.SearchableUser;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.PermissionService.MemberSearchAccess;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Users")
public class SearchUsersResource extends AbstractResource {

    @Inject
    private IdentityService identityService;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Search for users",
        description = "User must manage members somewhere to use this service: ORGANIZATION_USERS[READ or UPDATE], " +
            "ORGANIZATION_ROLE[CREATE or UPDATE], ENVIRONMENT_GROUP[CREATE or UPDATE], AI_WORKSPACE_USER[CREATE or UPDATE], " +
            "or MEMBER[CREATE or UPDATE] on an API, an API product, an AI catalog, an AI workspace, an explorer, " +
            "an application, an integration, a cluster or a group. Users whose only member-management scope " +
            "is applications are restricted to exact-match queries (full display name, source ID, or email when " +
            "allow-email-in-search-results is enabled); partial queries return an empty list rather than 403."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of users",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = SearchableUser.class))
        )
    )
    @ApiResponse(responseCode = "400", description = "Bad query parameter")
    @ApiResponse(responseCode = "403", description = "User does not manage any member")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Collection<SearchableUser> searchUsers(@Parameter(name = "q", required = true) @NotNull @QueryParam("q") String query) {
        // This search feeds every member-management screen (API, application, group, integration, cluster, organization
        // role, ownership transfer), whose users are scoped to one resource and rarely hold ORGANIZATION_USERS[READ].
        // The @Permissions filter can only check a permission against the reference found in the path, and this route
        // carries none, so the "manages members on at least one reference" rule is evaluated in code instead.
        var access = permissionService.memberSearchAccess(GraviteeContext.getExecutionContext(), getAuthenticatedUser());
        if (access == MemberSearchAccess.NONE) {
            throw new ForbiddenAccessException();
        }
        var users = identityService
            .search(query)
            .stream()
            .sorted((o1, o2) -> CASE_INSENSITIVE_ORDER.compare(o1.getLastname(), o2.getLastname()));
        // On a default install every user owns a Default application. Application-only owners may search the
        // directory but only with an exact match on the full display name, source ID (login), or email — not a
        // single first or last name token, which would still enumerate the directory. The application member
        // picker must keep working, yet wildcard/prefix enumeration of the full directory must not, because
        // any user holds the application permission on the Default (or a self-created) application.
        if (access == MemberSearchAccess.APPLICATION_ONLY) {
            var lower = query.trim().toLowerCase();
            users = users.filter(u -> matchesExactly(u, lower));
        }
        return users.collect(Collectors.toList());
    }

    private static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

    private static boolean matchesExactly(SearchableUser user, String lowerQuery) {
        return Stream.of(user.getDisplayName(), user.getSourceId(), user.getEmail())
            .filter(Objects::nonNull)
            .anyMatch(v -> v.equalsIgnoreCase(lowerQuery));
    }

    private static class CaseInsensitiveComparator implements Comparator<String>, java.io.Serializable {

        // use serialVersionUID from JDK 1.2.2 for interoperability
        private static final long serialVersionUID = 8575799808933029326L;

        public int compare(String s1, String s2) {
            if (s1 == null) return 1;
            if (s2 == null) return -1;

            int n1 = s1.length();
            int n2 = s2.length();
            int min = Math.min(n1, n2);
            for (int i = 0; i < min; i++) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(i);
                if (c1 != c2) {
                    c1 = Character.toUpperCase(c1);
                    c2 = Character.toUpperCase(c2);
                    if (c1 != c2) {
                        c1 = Character.toLowerCase(c1);
                        c2 = Character.toLowerCase(c2);
                        if (c1 != c2) {
                            // No overflow because of numeric promotion
                            return c1 - c2;
                        }
                    }
                }
            }
            return n1 - n2;
        }

        /** Replaces the de-serialized object. */
        private Object readResolve() {
            return CASE_INSENSITIVE_ORDER;
        }
    }
}
