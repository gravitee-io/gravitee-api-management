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
package io.gravitee.apim.infra.crud_service.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.ResourceService;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.exceptions.ResourceNotFoundException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ResourceCrudServiceImplTest {

    ResourceService resourceService;

    ResourceCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        resourceService = mock(ResourceService.class);

        service = new ResourceCrudServiceImpl(resourceService);
    }

    @Nested
    class FindById {

        @Test
        void should_return_resource_and_adapt_it() throws TechnicalException {
            // Given
            var resourceId = "resource-id";
            when(resourceService.findById(resourceId))
                .thenAnswer(invocation ->
                    PlatformPluginEntity
                        .builder()
                        .id(invocation.getArgument(0))
                        .name("name")
                        .description("description")
                        .icon("icon")
                        .feature("feature")
                        .deployed(true)
                        .category("category")
                        .version("1")
                        .build()
                );

            // When
            var resource = service.get(resourceId);

            // Then
            assertThat(resource.isPresent()).isTrue();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(resource.get().getId()).isEqualTo(resourceId);
                soft.assertThat(resource.get().getName()).isEqualTo("name");
                soft.assertThat(resource.get().getDescription()).isEqualTo("description");
                soft.assertThat(resource.get().getIcon()).isEqualTo("icon");
                soft.assertThat(resource.get().getFeature()).isEqualTo("feature");
                soft.assertThat(resource.get().isDeployed()).isEqualTo(true);
                soft.assertThat(resource.get().getCategory()).isEqualTo("category");
                soft.assertThat(resource.get().getVersion()).isEqualTo("1");
            });
        }

        @Test
        void should_throw_when_no_resource_found() throws TechnicalException {
            // Given
            String resourceId = "unknown";
            when(resourceService.findById(resourceId)).thenThrow(PluginNotFoundException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.get(resourceId));

            // Then
            assertThat(throwable)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource [" + resourceId + "] cannot be found.");
        }
    }
}
