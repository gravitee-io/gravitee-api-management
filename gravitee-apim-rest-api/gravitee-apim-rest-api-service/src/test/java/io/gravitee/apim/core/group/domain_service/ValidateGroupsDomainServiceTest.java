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

import static io.gravitee.apim.core.group.model.Group.GroupEvent.API_CREATE;
import static io.gravitee.apim.core.group.model.Group.GroupEvent.APPLICATION_CREATE;
import static org.mockito.ArgumentMatchers.any;

import inmemory.GroupQueryServiceInMemory;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.model.Group.GroupEventRule;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.DefinitionVersion;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateGroupsDomainServiceTest {

    private static final String ENVIRONMENT = "TEST";

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();

    private ValidateGroupsDomainService validateGroupsDomainService;

    @BeforeEach
    void setUp() {
        groupQueryService.initWith(
            List.of(
                Group.builder().environmentId(ENVIRONMENT).id("group-with-po-id").name("group-with-po").apiPrimaryOwner("some-po").build(),
                Group.builder().environmentId(ENVIRONMENT).id("group-without-po-id").name("group-without-po").build(),
                Group.builder()
                    .environmentId(ENVIRONMENT)
                    .id("default-group")
                    .name("default-group")
                    .eventRules(List.of(new GroupEventRule(API_CREATE), new GroupEventRule(APPLICATION_CREATE)))
                    .build()
            )
        );
        validateGroupsDomainService = new ValidateGroupsDomainService(groupQueryService);
    }

    @Test
    void should_return_warnings_with_group_with_primary_owner_and_sanitize_v2() {
        var givenGroups = Set.of("group-with-po-id", "group-without-po");

        var input = new ValidateGroupsDomainService.Input(ENVIRONMENT, givenGroups, DefinitionVersion.V2.getLabel(), API_CREATE, true);

        var result = validateGroupsDomainService.validateAndSanitize(input);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.value()).isNotEmpty();
            soft.assertThat(result.value()).hasValue(input.sanitized(Set.of("group-without-po", "default-group")));
            soft.assertThat(result.errors()).isNotEmpty();
            soft
                .assertThat(result.errors())
                .hasValue(
                    List.of(
                        Validator.Error.warning(
                            "Group [group-with-po-id] will be discarded because it contains an API Primary Owner member, which is not supported with by the operator."
                        )
                    )
                );
        });
    }

    @Test
    void should_return_groups_without_default_groups_v2() {
        var givenGroups = Set.of("group-without-po");

        var input = new ValidateGroupsDomainService.Input(ENVIRONMENT, givenGroups, DefinitionVersion.V2.getLabel(), API_CREATE, false);

        var result = validateGroupsDomainService.validateAndSanitize(input);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.value()).isNotEmpty();
            soft.assertThat(result.value()).hasValue(input.sanitized(Set.of("group-without-po")));
            soft.assertThat(result.errors()).isEmpty();
        });
    }

    @Test
    void should_return_warnings_with_group_with_primary_owner_and_sanitize_v4() {
        var givenGroups = Set.of("group-with-po", "group-without-po-id");

        var input = new ValidateGroupsDomainService.Input(ENVIRONMENT, givenGroups, DefinitionVersion.V4.getLabel(), API_CREATE, true);

        var result = validateGroupsDomainService.validateAndSanitize(input);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.value()).isNotEmpty();
            soft.assertThat(result.value()).hasValue(input.sanitized(Set.of("group-without-po-id", "default-group")));
            soft.assertThat(result.errors()).isNotEmpty();
            soft
                .assertThat(result.errors())
                .hasValue(
                    List.of(
                        Validator.Error.warning(
                            "Group [group-with-po] will be discarded because it contains an API Primary Owner member, which is not supported with by the operator."
                        )
                    )
                );
        });
    }

    @Test
    void should_return_groups_without_default_groups_v4() {
        var givenGroups = Set.of("group-without-po-id");

        var input = new ValidateGroupsDomainService.Input(ENVIRONMENT, givenGroups, DefinitionVersion.V4.getLabel(), API_CREATE, false);

        var result = validateGroupsDomainService.validateAndSanitize(input);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.value()).isNotEmpty();
            soft.assertThat(result.value()).hasValue(input.sanitized(Set.of("group-without-po-id")));
            soft.assertThat(result.errors()).isEmpty();
        });
    }
}
