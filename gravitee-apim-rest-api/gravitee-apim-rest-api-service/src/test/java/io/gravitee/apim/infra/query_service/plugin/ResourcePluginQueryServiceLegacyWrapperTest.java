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
package io.gravitee.apim.infra.query_service.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.plugin.model.ResourcePlugin;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.ResourceService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourcePluginQueryServiceLegacyWrapperTest {

    ResourceService resourceService;
    ResourcePluginQueryServiceLegacyWrapper service;

    @BeforeEach
    void setup() {
        resourceService = mock(ResourceService.class);
        service = new ResourcePluginQueryServiceLegacyWrapper(resourceService);
    }

    @Test
    void findAll() {
        when(resourceService.findAll())
            .thenReturn(
                Set.of(
                    PlatformPluginEntity.builder().id("resource-1").name("Resource 1").build(),
                    PlatformPluginEntity.builder().id("resource-2").name("Resource 2").build()
                )
            );

        assertThat(service.findAll())
            .containsExactlyInAnyOrder(
                ResourcePlugin.builder().id("resource-1").name("Resource 1").build(),
                ResourcePlugin.builder().id("resource-2").name("Resource 2").build()
            );
    }
}
