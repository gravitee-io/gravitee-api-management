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
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.model.WorkflowState;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GraviteeDefinitionAdapterTest {

    @Test
    void mapV4_should_propagate_allowedInApiProducts_from_definition() {
        // Given a core Api with a V4 HTTP PROXY definition
        var v4Definition = new io.gravitee.definition.model.v4.Api();
        v4Definition.setDefinitionVersion(DefinitionVersion.V4);
        v4Definition.setType(ApiType.PROXY);
        v4Definition.setApiVersion("1.0.0");
        v4Definition.setName("my-api");
        v4Definition.setAllowedInApiProducts(true);

        Api coreApi = Api.builder()
            .id("api-id")
            .environmentId("env-id")
            .version("1.0.0")
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            .apiDefinitionHttpV4(v4Definition)
            .build();

        var primaryOwner = PrimaryOwnerEntity.builder()
            .id("po-id")
            .email("po@acme.test")
            .displayName("PO")
            .type(PrimaryOwnerEntity.Type.USER)
            .build();
        var metadata = java.util.Collections.<NewApiMetadata>emptyList();

        // When
        ApiDescriptor.ApiDescriptorV4 descriptor = GraviteeDefinitionAdapter.INSTANCE.mapV4(
            coreApi,
            primaryOwner,
            WorkflowState.REVIEW_OK,
            Set.of("group-1"),
            (java.util.Collection<NewApiMetadata>) metadata,
            List.of(new Flow()),
            false
        );

        // Then
        assertThat(descriptor.allowedInApiProducts()).isTrue();
    }

    @Test
    void mapV4_should_leave_allowedInApiProducts_null_when_missing_in_definition() {
        var v4Definition = new io.gravitee.definition.model.v4.Api();
        v4Definition.setDefinitionVersion(DefinitionVersion.V4);
        v4Definition.setType(ApiType.PROXY);
        v4Definition.setApiVersion("1.0.0");
        v4Definition.setName("my-api");
        // no allowedInApiProducts set

        Api coreApi = Api.builder()
            .id("api-id")
            .environmentId("env-id")
            .version("1.0.0")
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.PROXY)
            .apiDefinitionHttpV4(v4Definition)
            .build();

        var primaryOwner = PrimaryOwnerEntity.builder()
            .id("po-id")
            .email("po@acme.test")
            .displayName("PO")
            .type(PrimaryOwnerEntity.Type.USER)
            .build();
        var metadata = java.util.Collections.<NewApiMetadata>emptyList();

        ApiDescriptor.ApiDescriptorV4 descriptor = GraviteeDefinitionAdapter.INSTANCE.mapV4(
            coreApi,
            primaryOwner,
            WorkflowState.REVIEW_OK,
            Set.of("group-1"),
            (java.util.Collection<NewApiMetadata>) metadata,
            List.of(new Flow()),
            false
        );

        assertThat(descriptor.allowedInApiProducts()).isNull();
    }
}
