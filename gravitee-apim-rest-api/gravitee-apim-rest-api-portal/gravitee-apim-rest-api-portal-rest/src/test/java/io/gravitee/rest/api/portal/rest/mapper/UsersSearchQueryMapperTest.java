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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;

import io.gravitee.apim.core.user.model.UserSearchQuery;
import io.gravitee.rest.api.portal.rest.model.UsersSearchFilters;
import io.gravitee.rest.api.portal.rest.model.UsersSearchIncludes;
import io.gravitee.rest.api.portal.rest.model.UsersSearchInput;
import org.junit.Test;

public class UsersSearchQueryMapperTest {

    private final UsersSearchQueryMapper mapper = UsersSearchQueryMapper.INSTANCE;

    @Test
    public void should_map_filters_query() {
        var input = new UsersSearchInput()
            .filters(new UsersSearchFilters().query("john"))
            .includes(new UsersSearchIncludes().applicationMembership("app-123"));

        var searchQuery = mapper.toSearchQuery(input);

        assertEquals("john", searchQuery.query());
    }

    @Test
    public void should_default_query_when_nested_objects_are_null() {
        var searchQuery = mapper.toSearchQuery(new UsersSearchInput());

        assertEquals(UserSearchQuery.DEFAULT_QUERY, searchQuery.query());
    }
}
