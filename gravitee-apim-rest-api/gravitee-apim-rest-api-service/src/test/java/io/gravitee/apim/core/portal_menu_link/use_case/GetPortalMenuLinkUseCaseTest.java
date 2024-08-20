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
import static org.junit.jupiter.api.Assertions.assertThrows;

import fixtures.core.model.PortalMenuLinkFixtures;
import inmemory.PortalMenuLinkCrudServiceInMemory;
import io.gravitee.apim.core.portal_menu_link.exception.PortalMenuLinkNotFoundException;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetPortalMenuLinkUseCaseTest {

    private final PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudService = new PortalMenuLinkCrudServiceInMemory();
    private GetPortalMenuLinkUseCase getPortalMenuLinkUseCase;

    @BeforeEach
    void setUp() {
        getPortalMenuLinkUseCase = new GetPortalMenuLinkUseCase(portalMenuLinkCrudService);
    }

    @Test
    void should_return_portal_menu_link() {
        // Given
        PortalMenuLink expectedPortalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkCrudService.initWith(List.of(expectedPortalMenuLink));
        // When
        var result = getPortalMenuLinkUseCase.execute(
            new GetPortalMenuLinkUseCase.Input(expectedPortalMenuLink.getId(), expectedPortalMenuLink.getEnvironmentId())
        );
        // Then
        assertEquals(expectedPortalMenuLink, result.portalMenuLinkEntity());
    }

    @Test
    void should_throw_exception_when_portal_menu_link_not_found_in_environment() {
        // Given
        PortalMenuLink expectedPortalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkCrudService.initWith(List.of(expectedPortalMenuLink));
        // When
        var throwable = assertThrows(
            PortalMenuLinkNotFoundException.class,
            () -> getPortalMenuLinkUseCase.execute(new GetPortalMenuLinkUseCase.Input("unknown", expectedPortalMenuLink.getEnvironmentId()))
        );
        // Then
        assertEquals("PortalMenuLink [ unknown ] not found", throwable.getMessage());
    }

    @Test
    void should_throw_exception_when_portal_menu_link_not_found() {
        // Given
        PortalMenuLink expectedPortalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkCrudService.initWith(List.of(expectedPortalMenuLink));
        // When
        var throwable = assertThrows(
            PortalMenuLinkNotFoundException.class,
            () -> getPortalMenuLinkUseCase.execute(new GetPortalMenuLinkUseCase.Input(expectedPortalMenuLink.getId(), "unknown"))
        );
        // Then
        assertEquals("PortalMenuLink [ portalMenuLinkId ] not found", throwable.getMessage());
    }
}
