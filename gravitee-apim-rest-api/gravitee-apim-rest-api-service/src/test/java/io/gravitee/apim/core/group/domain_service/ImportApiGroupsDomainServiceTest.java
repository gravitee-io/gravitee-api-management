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

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.GroupFixtures;
import inmemory.CreateGroupDomainServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ImportApiGroupsDomainServiceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String OTHER_ENVIRONMENT_ID = "other-environment-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, "user-id");

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final CreateGroupDomainServiceInMemory createGroupDomainService = new CreateGroupDomainServiceInMemory(groupQueryService);

    private ImportApiGroupsDomainService service;

    @BeforeEach
    void setUp() {
        service = new ImportApiGroupsDomainService(groupQueryService, createGroupDomainService);
        UuidString.overrideGenerator(() -> "created-group-id");
    }

    @AfterEach
    void tearDown() {
        groupQueryService.reset();
        createGroupDomainService.reset();
        UuidString.reset();
    }

    @Test
    void should_resolve_existing_group_by_name() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("group-1").toBuilder().name("Helios").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), AUDIT_INFO);

        assertThat(resolved).containsExactly("group-1");
        assertThat(createGroupDomainService.storage()).isEmpty();
    }

    @Test
    void should_not_bind_group_by_id_even_when_id_exists_in_same_environment() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("group-1").toBuilder().name("Helios").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("group-1"), AUDIT_INFO);

        // Name-only: "group-1" is treated as a name, not as an ID → creates a new empty group.
        assertThat(resolved).containsExactly("created-group-id");
        assertThat(createGroupDomainService.storage().values())
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

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), AUDIT_INFO);

        assertThat(resolved).containsExactly("target-group-id");
        assertThat(createGroupDomainService.storage()).isEmpty();
    }

    @Test
    void should_create_missing_group_by_name_and_return_its_id() {
        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), AUDIT_INFO);

        assertThat(resolved).containsExactly("created-group-id");
        assertThat(createGroupDomainService.storage().values())
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

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), AUDIT_INFO);

        assertThat(resolved).containsExactly("existing-group-id");
        assertThat(createGroupDomainService.storage()).isEmpty();
    }

    @Test
    void should_reuse_group_when_name_appears_between_lookup_and_create() {
        // Simulates the race: batch findByNames saw nothing, but create sees the name already present.
        var createThatSeesConcurrentInsert = new CreateGroupDomainService() {
            @Override
            public CreateResult createEmpty(String name, AuditInfo auditInfo) {
                groupQueryService.initWith(
                    List.of(GroupFixtures.aGroup("raced-group-id").toBuilder().name(name).environmentId(ENVIRONMENT_ID).build())
                );
                return createGroupDomainService.createEmpty(name, auditInfo);
            }
        };
        service = new ImportApiGroupsDomainService(groupQueryService, createThatSeesConcurrentInsert);

        var resolved = service.resolveOrCreateGroupIds(Set.of("Helios"), AUDIT_INFO);

        assertThat(resolved).containsExactly("raced-group-id");
        assertThat(createGroupDomainService.storage()).isEmpty();
    }

    @Test
    void should_keep_matching_group_and_create_missing_one() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("developers-id").toBuilder().name("Developers").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Developers", "Helios"), AUDIT_INFO);

        assertThat(resolved).hasSize(2).contains("developers-id", "created-group-id");
    }

    @Test
    void should_return_null_when_group_refs_are_null() {
        assertThat(service.resolveOrCreateGroupIds(null, AUDIT_INFO)).isNull();
    }

    @Test
    void should_return_empty_when_group_refs_are_empty() {
        assertThat(service.resolveOrCreateGroupIds(Set.of(), AUDIT_INFO)).isEmpty();
    }

    @Test
    void should_filter_out_blank_group_names() {
        groupQueryService.initWith(
            List.of(GroupFixtures.aGroup("developers-id").toBuilder().name("Developers").environmentId(ENVIRONMENT_ID).build())
        );

        var resolved = service.resolveOrCreateGroupIds(Set.of("Developers", "", "  ", "Helios"), AUDIT_INFO);

        // Blank names filtered out, Developers resolved, Helios created
        assertThat(resolved).hasSize(2).contains("developers-id", "created-group-id");
        assertThat(createGroupDomainService.storage().values())
            .singleElement()
            .satisfies(group -> assertThat(group.getName()).isEqualTo("Helios"));
    }

    @Test
    void should_return_empty_when_all_group_names_are_blank() {
        var refs = new HashSet<>(Arrays.asList("", "  ", null));
        var resolved = service.resolveOrCreateGroupIds(refs, AUDIT_INFO);

        assertThat(resolved).isEmpty();
        assertThat(createGroupDomainService.storage()).isEmpty();
    }
}
