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
import static org.junit.Assert.assertNull;

import io.gravitee.rest.api.portal.rest.model.ApplicationMembersSearchFilters;
import io.gravitee.rest.api.portal.rest.model.ApplicationMembersSearchInput;
import org.junit.Test;

public class ApplicationMembersSearchCriteriaMapperTest {

    private final ApplicationMembersSearchCriteriaMapper mapper = ApplicationMembersSearchCriteriaMapper.INSTANCE;

    @Test
    public void should_map_display_name_filter() {
        var input = new ApplicationMembersSearchInput().filters(new ApplicationMembersSearchFilters().displayName("john"));

        var criteria = mapper.toSearchCriteria(input);

        assertEquals("john", criteria.displayName());
    }

    @Test
    public void should_map_null_filters() {
        var input = new ApplicationMembersSearchInput();

        var criteria = mapper.toSearchCriteria(input);

        assertNull(criteria.displayName());
    }
}
