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
package io.gravitee.apim.core.portal_menu_link.use_case;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fixtures.core.model.PortalMenuLinkFixtures;
import inmemory.PortalMenuLinkCrudServiceInMemory;
import inmemory.PortalMenuLinkQueryServiceInMemory;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkVisibility;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListAllPortalMenuLinksForEnvironmentUseCaseTest {

    private final PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudService = new PortalMenuLinkCrudServiceInMemory();
    private final PortalMenuLinkQueryServiceInMemory portalMenuLinkQueryService = new PortalMenuLinkQueryServiceInMemory(
        portalMenuLinkCrudService
    );
    private ListAllPortalMenuLinksForEnvironmentUseCase listAllPortalMenuLinksForEnvironmentUseCase;

    @BeforeEach
    void setUp() {
        listAllPortalMenuLinksForEnvironmentUseCase = new ListAllPortalMenuLinksForEnvironmentUseCase(portalMenuLinkQueryService);
    }

    @Test
    void should_return_portal_menu_link() {
        // Given
        portalMenuLinkQueryService.initWith(
            List.of(
                PortalMenuLinkFixtures
                    .aPortalMenuLink()
                    .toBuilder()
                    .id("id1")
                    .environmentId("env1")
                    .visibility(PortalMenuLinkVisibility.PUBLIC)
                    .order(2)
                    .build(),
                PortalMenuLinkFixtures
                    .aPortalMenuLink()
                    .toBuilder()
                    .id("id2")
                    .environmentId("env2")
                    .visibility(PortalMenuLinkVisibility.PUBLIC)
                    .order(1)
                    .build(),
                PortalMenuLinkFixtures
                    .aPortalMenuLink()
                    .toBuilder()
                    .id("id3")
                    .environmentId("env1")
                    .visibility(PortalMenuLinkVisibility.PUBLIC)
                    .order(1)
                    .build(),
                PortalMenuLinkFixtures
                    .aPortalMenuLink()
                    .toBuilder()
                    .id("id4")
                    .environmentId("env3")
                    .visibility(PortalMenuLinkVisibility.PUBLIC)
                    .order(1)
                    .build(),
                PortalMenuLinkFixtures
                    .aPortalMenuLink()
                    .toBuilder()
                    .id("id5")
                    .environmentId("env1")
                    .visibility(PortalMenuLinkVisibility.PRIVATE)
                    .order(3)
                    .build()
            )
        );
        // When
        var result = listAllPortalMenuLinksForEnvironmentUseCase.execute(new ListAllPortalMenuLinksForEnvironmentUseCase.Input("env1"));

        // Then
        assertNotNull(result.portalMenuLinkList());
        assertEquals(3, result.portalMenuLinkList().size());
        assertEquals("id3", result.portalMenuLinkList().get(0).getId());
        assertEquals("id1", result.portalMenuLinkList().get(1).getId());
        assertEquals("id5", result.portalMenuLinkList().get(2).getId());
    }

    @Test
    void should_return_empty_list_when_no_portal_menu_link() {
        // Given
        portalMenuLinkQueryService.initWith(List.of());
        // When
        var result = listAllPortalMenuLinksForEnvironmentUseCase.execute(new ListAllPortalMenuLinksForEnvironmentUseCase.Input("env1"));

        // Then
        assertNotNull(result.portalMenuLinkList());
        assertEquals(0, result.portalMenuLinkList().size());
    }
}
