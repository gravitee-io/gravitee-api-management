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
import inmemory.PortalMenuLinkQueryServiceInMemory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListPortalMenuLinksForEnvironmentUseCaseTest {

    private final PortalMenuLinkQueryServiceInMemory portalMenuLinkQueryService = new PortalMenuLinkQueryServiceInMemory();
    private ListPortalMenuLinksForEnvironmentUseCase listPortalMenuLinksForEnvironmentUseCase;

    @BeforeEach
    void setUp() {
        listPortalMenuLinksForEnvironmentUseCase = new ListPortalMenuLinksForEnvironmentUseCase(portalMenuLinkQueryService);
    }

    @Test
    void should_return_portal_menu_link() {
        // Given
        portalMenuLinkQueryService.initWith(
            List.of(
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id1").environmentId("env1").order(2).build(),
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id2").environmentId("env2").order(1).build(),
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id3").environmentId("env1").order(1).build(),
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id4").environmentId("env3").order(1).build()
            )
        );
        // When
        var result = listPortalMenuLinksForEnvironmentUseCase.execute(new ListPortalMenuLinksForEnvironmentUseCase.Input("env1"));

        // Then
        assertNotNull(result.portalMenuLinkList());
        assertEquals(2, result.portalMenuLinkList().size());
        assertEquals("id3", result.portalMenuLinkList().get(0).getId());
        assertEquals("id1", result.portalMenuLinkList().get(1).getId());
    }

    @Test
    void should_return_empty_list_when_no_portal_menu_link() {
        // Given
        portalMenuLinkQueryService.initWith(List.of());
        // When
        var result = listPortalMenuLinksForEnvironmentUseCase.execute(new ListPortalMenuLinksForEnvironmentUseCase.Input("env1"));

        // Then
        assertNotNull(result.portalMenuLinkList());
        assertEquals(0, result.portalMenuLinkList().size());
    }
}
