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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import inmemory.GroupQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ValidateFederatedApiDomainServiceTest {

    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final CategoryDomainService categoryDomainService = mock(CategoryDomainService.class);

    ValidateFederatedApiDomainService service;

    @BeforeEach
    void setUp() {
        var groupValidationService = new GroupValidationService(groupQueryService);
        service = new ValidateFederatedApiDomainService(groupValidationService, categoryDomainService);
        groupQueryService.initWith(List.of(Group.builder().id("group-1").name("group-1").build()));
    }

    @AfterEach
    void tearDown() {
        groupQueryService.reset();
    }

    @Test
    void should_return_the_api_when_valid() {
        var api = ApiFixtures.aFederatedApi();

        var result = service.validateAndSanitizeForCreation(api, null);

        assertThat(result).isSameAs(api);
    }

    @Test
    void should_reset_lifecycle_state_when_defined() {
        var api = ApiFixtures.aFederatedApi().toBuilder().lifecycleState(Api.LifecycleState.STARTED).build();

        var result = service.validateAndSanitizeForCreation(api, null);

        assertThat(result).extracting(Api::getLifecycleState).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = DefinitionVersion.class, mode = EnumSource.Mode.EXCLUDE, names = { "FEDERATED" })
    void should_throw_when_definition_version_is_incorrect(DefinitionVersion definitionVersion) {
        var api = ApiFixtures.aFederatedApi().toBuilder().definitionVersion(definitionVersion).build();

        var throwable = catchThrowable(() -> service.validateAndSanitizeForCreation(api, null));

        assertThat(throwable).isInstanceOf(ValidationDomainException.class);
    }

    @Test
    void should_validate_and_sanitize_api_for_update() {
        var existingApi = ApiFixtures.aFederatedApi().toBuilder().categories(Set.of("oldcat")).build();
        var updateApi = ApiFixtures
            .aFederatedApi()
            .toBuilder()
            .apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED)
            .categories(Set.of("cat"))
            .build();
        var primaryOwner = PrimaryOwnerEntity
            .builder()
            .id("primary-owner-id")
            .displayName("primary-owner-displayName")
            .email("primary-owner-email")
            .type(PrimaryOwnerEntity.Type.USER)
            .build();
        when(categoryDomainService.toCategoryId(any(), any())).thenReturn(Set.of("catId"));

        var result = service.validateAndSanitizeForUpdate(updateApi, existingApi, primaryOwner);

        assertThat(result)
            .isEqualTo(
                ApiFixtures
                    .aFederatedApi()
                    .toBuilder()
                    .apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED)
                    .categories(Set.of("catId"))
                    .build()
            );
    }
}
