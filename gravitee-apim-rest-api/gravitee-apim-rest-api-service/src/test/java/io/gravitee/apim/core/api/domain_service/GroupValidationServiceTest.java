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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.GroupFixtures;
import inmemory.GroupQueryServiceInMemory;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GroupValidationServiceTest {

    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String GROUP_1 = "group-1";
    private static final PrimaryOwnerEntity PRIMARY_OWNER = PrimaryOwnerEntity.builder().id("user-id").build();

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();

    private GroupValidationService groupValidationService;

    @BeforeEach
    public void setUp() throws Exception {
        groupValidationService = new GroupValidationService(groupQueryService);
    }

    @AfterEach
    public void tearDown() throws Exception {
        groupQueryService.reset();
    }

    @Test
    public void should_throw_when_groups_provided_do_not_exist() {
        var throwable = catchThrowable(() ->
            groupValidationService.validateAndSanitize(Set.of(GROUP_1), ENVIRONMENT_ID, PRIMARY_OWNER, false)
        );

        assertThat(throwable).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void should_add_default_groups() {
        // Given
        givenExistingGroups(
            GroupFixtures.aGroup(GROUP_1),
            GroupFixtures.aGroup("default-group-1")
                .toBuilder()
                .environmentId(ENVIRONMENT_ID)
                .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                .build(),
            GroupFixtures.aGroup("default-group-2")
                .toBuilder()
                .environmentId(ENVIRONMENT_ID)
                .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                .build()
        );

        // When
        var sanitized = groupValidationService.validateAndSanitize(Set.of(GROUP_1), ENVIRONMENT_ID, PRIMARY_OWNER, true);

        // Then
        assertThat(sanitized).containsExactlyInAnyOrder(GROUP_1, "default-group-1", "default-group-2");
    }

    @Test
    void should_filter_any_groups_having_PrimaryOwner_users() {
        // Given
        givenExistingGroups(
            GroupFixtures.aGroup("group-1").toBuilder().build(),
            GroupFixtures.aGroup("group-2").toBuilder().apiPrimaryOwner("user-2").build(),
            GroupFixtures.aGroup("default-group-1")
                .toBuilder()
                .environmentId(ENVIRONMENT_ID)
                .eventRules(List.of(new Group.GroupEventRule(Group.GroupEvent.API_CREATE)))
                .apiPrimaryOwner("user-3")
                .build()
        );

        // When
        var sanitized = groupValidationService.validateAndSanitize(Set.of(GROUP_1), ENVIRONMENT_ID, PRIMARY_OWNER, false);

        // Then
        assertThat(sanitized).containsExactlyInAnyOrder(GROUP_1);
    }

    @Test
    public void should_do_nothing_when_no_groups() {
        Set<String> validatedGroups = groupValidationService.validateAndSanitize(Set.of(), ENVIRONMENT_ID, null, false);
        assertThat(validatedGroups).isEmpty();
    }

    @Test
    void should_add_current_primary_owner_when_it_is_a_group() {
        // Given
        givenExistingGroups(GroupFixtures.aGroup(GROUP_1).toBuilder().build());
        var primaryOwner = PRIMARY_OWNER.toBuilder().id("group-id").type(PrimaryOwnerEntity.Type.GROUP).build();

        // When
        var sanitized = groupValidationService.validateAndSanitize(Set.of(GROUP_1), ENVIRONMENT_ID, primaryOwner, false);

        // Then
        assertThat(sanitized).containsExactlyInAnyOrder(GROUP_1, "group-id");
    }

    void givenExistingGroups(Group... groups) {
        groupQueryService.initWith(Arrays.asList(groups));
    }
}
