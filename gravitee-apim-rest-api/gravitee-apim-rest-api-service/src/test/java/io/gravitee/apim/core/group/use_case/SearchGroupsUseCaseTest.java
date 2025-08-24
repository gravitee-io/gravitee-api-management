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
package io.gravitee.apim.core.group.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.GroupQueryServiceInMemory;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchGroupsUseCaseTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";

    private GroupQueryServiceInMemory groupQueryServiceInMemory;
    private SearchGroupsUseCase cut;

    @BeforeEach
    void setUp() {
        groupQueryServiceInMemory = new GroupQueryServiceInMemory();
        cut = new SearchGroupsUseCase(groupQueryServiceInMemory);
    }

    @Test
    void should_return_empty_page_when_group_ids_is_null() {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        Pageable pageable = new PageableImpl(1, 10);

        Page<Group> result = cut.execute(executionContext, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageElements()).isEqualTo(0);
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void should_return_empty_page_when_group_ids_is_empty() {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        Pageable pageable = new PageableImpl(1, 10);

        Page<Group> result = cut.execute(executionContext, Set.of(), pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageElements()).isEqualTo(0);
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void should_delegate_to_group_query_service_when_group_ids_is_not_empty() {
        ExecutionContext executionContext = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);
        Set<String> groupIds = Set.of("group-1", "group-2");
        Pageable pageable = new PageableImpl(0, 10);

        List<Group> groups = List.of(
            Group
                .builder()
                .id("group-1")
                .name("Group 1")
                .environmentId(ENVIRONMENT_ID)
                .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .build(),
            Group
                .builder()
                .id("group-2")
                .name("Group 2")
                .environmentId(ENVIRONMENT_ID)
                .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .build()
        );

        groupQueryServiceInMemory.initWith(groups);

        Page<Group> result = cut.execute(executionContext, groupIds, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo("group-1");
        assertThat(result.getContent().get(1).getId()).isEqualTo("group-2");
        assertThat(result.getPageNumber()).isEqualTo(pageable.getPageNumber());
        assertThat(result.getPageElements()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}
