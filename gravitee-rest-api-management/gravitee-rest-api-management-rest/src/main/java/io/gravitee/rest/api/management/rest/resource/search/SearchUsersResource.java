/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.rest.api.service.IdentityService;
import io.swagger.annotations.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Users" })
public class SearchUsersResource {

    @Inject
    private IdentityService identityService;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Search for users")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List of users", response = SearchableUser.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Bad query parameter"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Collection<SearchableUser> searchUsers(@ApiParam(name = "q", required = true) @NotNull @QueryParam("q") String query) {
        return identityService
            .search(query)
            .stream()
            .sorted((o1, o2) -> CASE_INSENSITIVE_ORDER.compare(o1.getLastname(), o2.getLastname()))
            .collect(Collectors.toList());
    }

    private static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

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
