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
package io.gravitee.apim.core.group.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.GroupFixtures;
import inmemory.GroupCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportApiGroupsDomainServiceTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String OTHER_ENVIRONMENT_ID = "other-environment-id";

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final GroupCrudServiceInMemory groupCrudService = new GroupCrudServiceInMemory(groupQueryService);

    private ImportApiGroupsDomainService service;

    @BeforeEach
    void setUp() {
        service = new ImportApiGroupsDomainService(groupQueryService, groupCrudService);
        UuidString.overrideGenerator(() -> "created-group-id");
    }

    @AfterEach
    void tearDown() {
        groupQueryService.reset();
        groupCrudService.reset();
        UuidString.reset();
    }

    @Test
    void should_resolve_existing_group_by_name() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("group-1").toBuilder().name("Helios").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), ENVIRONMENT_ID);

        assertThat(resolved).containsExactly("group-1");
        assertThat(groupCrudService.storage()).isEmpty();
    }

    @Test
    void should_not_bind_group_by_id_even_when_id_exists_in_same_environment() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("group-1").toBuilder().name("Helios").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("group-1"), ENVIRONMENT_ID);

        // Name-only: "group-1" is treated as a name, not as an ID → creates a new empty group.
        assertThat(resolved).containsExactly("created-group-id");
        assertThat(groupCrudService.storage().values())
            .singleElement()
            .satisfies(group -> {
                assertThat(group.getName()).isEqualTo("group-1");
                assertThat(group.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
            });
    }

    @Test
    void should_resolve_by_name_in_target_environment_only() {
        groupQueryService.initWith(
            List.of(
                GroupFixtures.aGroup("source-group-id").toBuilder().name("Helios").environmentId(OTHER_ENVIRONMENT_ID).build(),
                GroupFixtures.aGroup("target-group-id").toBuilder().name("Helios").environmentId(ENVIRONMENT_ID).build()
            )
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), ENVIRONMENT_ID);

        assertThat(resolved).containsExactly("target-group-id");
        assertThat(groupCrudService.storage()).isEmpty();
    }

    @Test
    void should_create_missing_group_by_name_and_return_its_id() {
        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), ENVIRONMENT_ID);

        assertThat(resolved).containsExactly("created-group-id");
        assertThat(groupCrudService.storage().values())
            .singleElement()
            .satisfies(group -> {
                assertThat(group.getName()).isEqualTo("Helios");
                assertThat(group.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
            });
    }

    @Test
    void should_reuse_group_when_name_appears_before_create() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("existing-group-id").toBuilder().name("Helios").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), ENVIRONMENT_ID);

        assertThat(resolved).containsExactly("existing-group-id");
        assertThat(groupCrudService.storage()).isEmpty();
    }

    @Test
    void should_keep_matching_group_and_create_missing_one() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("developers-id").toBuilder().name("Developers").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Developers", "Helios"), ENVIRONMENT_ID);

        assertThat(resolved).hasSize(2).contains("developers-id", "created-group-id");
    }

    @Test
    void should_return_null_when_group_refs_are_null() {
        assertThat(service.resolveOrCreateGroupIds(null, ENVIRONMENT_ID)).isNull();
    }

    @Test
    void should_return_empty_when_group_refs_are_empty() {
        assertThat(service.resolveOrCreateGroupIds(Set.of(), ENVIRONMENT_ID)).isEmpty();
    }
}
