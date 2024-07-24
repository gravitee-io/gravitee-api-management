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

import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.integration.api.model.Api;
import io.gravitee.integration.api.model.Metadata;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrationAdapterTest {

    @Test
    void mapMetadata() {
        // Given
        var source = Api
            .builder()
            .metadata(List.of(Metadata.builder().name("name1").format(Metadata.Format.STRING).value("value1").build()))
            .build();
        String integrationId = "My ID";

        // When
        IntegrationApi integrationApi = IntegrationAdapter.INSTANCE.map(source, integrationId);

        // Then
        assertThat(integrationApi.metadata())
            .containsExactly(
                new IntegrationApi.Metadata("name1", "value1", io.gravitee.apim.core.metadata.model.Metadata.MetadataFormat.STRING)
            );
    }
}
