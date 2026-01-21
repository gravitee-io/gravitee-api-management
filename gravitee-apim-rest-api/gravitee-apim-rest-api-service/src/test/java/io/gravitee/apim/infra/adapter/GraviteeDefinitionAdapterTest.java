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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.WorkflowState;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GraviteeDefinitionAdapterTest {

    private final GraviteeDefinitionAdapter cut = GraviteeDefinitionAdapter.INSTANCE;

    @Test
    void should_map_allowInApiProduct_true_for_v4_proxy_api() {
        // Given
        io.gravitee.definition.model.v4.Api v4Def = io.gravitee.definition.model.v4.Api.builder()
            .type(ApiType.PROXY)
            .allowInApiProduct(true)
            .build();

        Api apiEntity = Api.builder()
            .id("api-id")
            .name("My API")
            .version("1.0.0")
            .type(ApiType.PROXY)
            .definitionVersion(DefinitionVersion.V4)
            .apiDefinitionHttpV4(v4Def)
            .build();

        PrimaryOwnerEntity primaryOwner = PrimaryOwnerEntity.builder()
            .id("user-id")
            .displayName("User")
            .email("user@gravitee.io")
            .type(PrimaryOwnerEntity.Type.USER)
            .build();

        // When
        ApiDescriptor.ApiDescriptorV4 result = cut.mapV4(
            apiEntity,
            primaryOwner,
            WorkflowState.REVIEW_OK,
            Set.of("group1"),
            Collections.emptyList(),
            null,
            false
        );

        // Then
        assertThat(result.allowInApiProduct()).isTrue();
    }

    @Test
    void should_not_set_allowInApiProduct_when_not_proxy_or_definition_missing() {
        // Given: MESSAGE API without http v4 definition
        Api apiEntity = Api.builder()
            .id("api-id")
            .name("My API")
            .version("1.0.0")
            .type(ApiType.MESSAGE)
            .definitionVersion(DefinitionVersion.V4)
            .build();

        PrimaryOwnerEntity primaryOwner = PrimaryOwnerEntity.builder()
            .id("user-id")
            .displayName("User")
            .email("user@gravitee.io")
            .type(PrimaryOwnerEntity.Type.USER)
            .build();

        // When
        ApiDescriptor.ApiDescriptorV4 result = cut.mapV4(
            apiEntity,
            primaryOwner,
            WorkflowState.REVIEW_OK,
            Set.of("group1"),
            Collections.emptyList(),
            null,
            false
        );

        // Then
        assertThat(result.allowInApiProduct()).isNull();
    }
}
