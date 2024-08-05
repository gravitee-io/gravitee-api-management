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
package io.gravitee.apim.infra.query_service.shared_policy_group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.infra.adapter.SharedPolicyGroupAdapter;
import io.gravitee.apim.infra.adapter.SharedPolicyGroupAdapterImpl;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.api.SharedPolicyGroupRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.SharedPolicyGroupLifecycleState;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SharedPolicyGroupQueryServiceImplTest {

    SharedPolicyGroupRepository repository;
    SharedPolicyGroupAdapter mapper;
    SharedPolicyGroupQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(SharedPolicyGroupRepository.class);
        mapper = new SharedPolicyGroupAdapterImpl();
        service = new SharedPolicyGroupQueryServiceImpl(repository, mapper);
    }

    @Nested
    class SearchByEnvironmentId {

        @Test
        @SneakyThrows
        void searchByEnvironmentId_should_return_empty_page() {
            // Given
            String q = "q";
            String environmentId = "environmentId";
            when(
                repository.search(
                    argThat(criteria -> criteria.getName().equals(q) && criteria.getEnvironmentId().equals(environmentId)),
                    eq(new PageableBuilder().pageNumber(0).pageSize(10).build()),
                    eq(new SortableBuilder().field(null).setAsc(false).build())
                )
            )
                .thenReturn(new Page<>(List.of(), 0, 0, 0));

            // When
            Pageable pageable = new PageableImpl(1, 10);
            Sortable sortable = new SortableImpl(null, false);

            Page<SharedPolicyGroup> result = service.searchByEnvironmentId(environmentId, q, pageable, sortable);
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @SneakyThrows
        void searchByEnvironmentId_should_return_page() {
            // Given
            String q = "q";
            String environmentId = "environmentId";
            when(
                repository.search(
                    argThat(criteria -> criteria.getName().equals(q) && criteria.getEnvironmentId().equals(environmentId)),
                    any(),
                    eq(new SortableBuilder().field("createdAt").setAsc(true).build())
                )
            )
                .thenReturn(
                    new Page<>(
                        List.of(
                            aRepositorySharedPolicyGroup("id"),
                            aRepositorySharedPolicyGroup("id2"),
                            aRepositorySharedPolicyGroup("id3")
                        ),
                        1,
                        3,
                        42
                    )
                );

            // When
            Pageable pageable = new PageableImpl(0, 3);
            Sortable sortable = new SortableImpl("createdAt", true);

            Page<SharedPolicyGroup> result = service.searchByEnvironmentId(environmentId, q, pageable, sortable);
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(42);
            assertThat(result.getContent().get(0).getId()).isEqualTo("id");
            assertThat(result.getContent().get(1).getId()).isEqualTo("id2");
            assertThat(result.getContent().get(2).getId()).isEqualTo("id3");
        }
    }

    @Nested
    class StreamByEnvironmentIdAndState {

        @Test
        @SneakyThrows
        void streamByEnvironmentIdAndState_should_return_empty_stream() {
            // Given
            String environmentId = "environmentId";
            when(
                repository.search(
                    argThat(criteria ->
                        criteria.getEnvironmentId().equals(environmentId) &&
                        criteria.getLifecycleState().equals(SharedPolicyGroupLifecycleState.UNDEPLOYED)
                    ),
                    any(),
                    eq(new SortableBuilder().field(null).setAsc(false).build())
                )
            )
                .thenReturn(new Page<>(List.of(), 0, 0, 0));

            // When
            Sortable sortable = new SortableImpl(null, false);

            List<SharedPolicyGroup> result = service
                .streamByEnvironmentIdAndState(environmentId, SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED, sortable)
                .toList();
            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @SneakyThrows
        void streamByEnvironmentIdAndState_should_return_stream() {
            // Given
            String environmentId = "environmentId";
            when(
                repository.search(
                    argThat(criteria ->
                        criteria.getEnvironmentId().equals(environmentId) &&
                        criteria.getLifecycleState().equals(SharedPolicyGroupLifecycleState.UNDEPLOYED)
                    ),
                    any(io.gravitee.repository.management.api.search.Pageable.class),
                    eq(new SortableBuilder().field("createdAt").setAsc(true).build())
                )
            )
                .thenAnswer(invocation -> {
                    io.gravitee.repository.management.api.search.Pageable pageable = invocation.getArgument(1);
                    if (pageable.pageNumber() == 0) {
                        return new Page<>(
                            List.of(
                                aRepositorySharedPolicyGroup("id"),
                                aRepositorySharedPolicyGroup("id2"),
                                aRepositorySharedPolicyGroup("id3")
                            ),
                            pageable.pageNumber(),
                            3,
                            6
                        );
                    }
                    if (pageable.pageNumber() == 1) {
                        return new Page<>(
                            List.of(
                                aRepositorySharedPolicyGroup("id4"),
                                aRepositorySharedPolicyGroup("id5"),
                                aRepositorySharedPolicyGroup("id6")
                            ),
                            pageable.pageNumber(),
                            3,
                            6
                        );
                    }
                    return null;
                });

            // When
            Sortable sortable = new SortableImpl("createdAt", true);
            var result = service
                .streamByEnvironmentIdAndState(environmentId, SharedPolicyGroup.SharedPolicyGroupLifecycleState.UNDEPLOYED, sortable)
                .toList();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getId()).isEqualTo("id");
            assertThat(result.get(1).getId()).isEqualTo("id2");
            assertThat(result.get(2).getId()).isEqualTo("id3");
            assertThat(result.get(3).getId()).isEqualTo("id4");
            assertThat(result.get(4).getId()).isEqualTo("id5");
            assertThat(result.get(5).getId()).isEqualTo("id6");
        }
    }

    private io.gravitee.repository.management.model.SharedPolicyGroup aRepositorySharedPolicyGroup(String id) {
        return io.gravitee.repository.management.model.SharedPolicyGroup
            .builder()
            .id(id)
            .name("name")
            .version(1)
            .description("description")
            .crossId("crossId")
            .apiType(ApiType.PROXY)
            .definition("{}")
            .lifecycleState(SharedPolicyGroupLifecycleState.UNDEPLOYED)
            .environmentId("environmentId")
            .organizationId("organizationId")
            .build();
    }
}
