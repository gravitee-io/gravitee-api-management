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

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.PortalMenuLinkFixtures;
import inmemory.PortalMenuLinkCrudServiceInMemory;
import inmemory.PortalMenuLinkQueryServiceInMemory;
import io.gravitee.apim.core.portal_menu_link.exception.PortalMenuLinkNotFoundException;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class UpdatePortalMenuLinkUseCaseTest {

    private final PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudService = new PortalMenuLinkCrudServiceInMemory();
    private final PortalMenuLinkQueryServiceInMemory portalMenuQueryCrudService = new PortalMenuLinkQueryServiceInMemory(
        portalMenuLinkCrudService
    );
    private UpdatePortalMenuLinkUseCase updatePortalMenuLinkUseCase;

    @BeforeEach
    void setUp() {
        updatePortalMenuLinkUseCase = new UpdatePortalMenuLinkUseCase(portalMenuLinkCrudService, portalMenuQueryCrudService);
    }

    @AfterEach
    void afterEach() {
        portalMenuLinkCrudService.reset();
        portalMenuQueryCrudService.reset();
    }

    @Test
    void should_update_portal_menu_link() {
        // Given
        var toUpdate = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkCrudService.initWith(List.of(toUpdate));

        // When
        var portalMenuLinkToUpdate = PortalMenuLinkFixtures.anUpdatePortalMenuLink();
        updatePortalMenuLinkUseCase.execute(
            new UpdatePortalMenuLinkUseCase.Input(toUpdate.getId(), toUpdate.getEnvironmentId(), portalMenuLinkToUpdate)
        );

        // Then
        PortalMenuLink updatedPortalMenuLink = portalMenuLinkCrudService.getByIdAndEnvironmentId(
            toUpdate.getId(),
            toUpdate.getEnvironmentId()
        );
        assertThat(updatedPortalMenuLink.getName()).isEqualTo(portalMenuLinkToUpdate.getName());
        assertThat(updatedPortalMenuLink.getTarget()).isEqualTo(portalMenuLinkToUpdate.getTarget());
    }

    @Test
    void should_reorder_when_update() {
        // Given
        portalMenuLinkCrudService.initWith(
            List.of(
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id1").environmentId("env1").order(1).build(),
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id2").environmentId("env1").order(2).build(),
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id3").environmentId("env1").order(3).build()
            )
        );

        // When
        var portalMenuLinkToUpdate = PortalMenuLinkFixtures.anUpdatePortalMenuLink().toBuilder().order(3).build();
        updatePortalMenuLinkUseCase.execute(new UpdatePortalMenuLinkUseCase.Input("id2", "env1", portalMenuLinkToUpdate));

        // Then
        assertThat(portalMenuLinkCrudService.storage()).hasSize(3);
        assertThat(portalMenuLinkCrudService.getByIdAndEnvironmentId("id1", "env1").getOrder()).isEqualTo(1);
        assertThat(portalMenuLinkCrudService.getByIdAndEnvironmentId("id2", "env1").getOrder()).isEqualTo(3);
        assertThat(portalMenuLinkCrudService.getByIdAndEnvironmentId("id3", "env1").getOrder()).isEqualTo(2);
    }

    @Test
    void should_throw_not_found_exception_when_portal_menu_link_to_update_does_not_exist() {
        // Given
        var toUpdate = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkCrudService.initWith(List.of(toUpdate));

        // When
        var throwable = Assertions.catchThrowable(() ->
            updatePortalMenuLinkUseCase.execute(
                new UpdatePortalMenuLinkUseCase.Input(
                    "unknown",
                    toUpdate.getEnvironmentId(),
                    PortalMenuLinkFixtures.anUpdatePortalMenuLink()
                )
            )
        );

        // Then
        Assertions
            .assertThat(throwable)
            .isInstanceOf(PortalMenuLinkNotFoundException.class)
            .hasMessage("PortalMenuLink [ unknown ] not found");
    }

    @Test
    void should_throw_exception_when_name_is_null() {
        // Given
        var toUpdate = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkCrudService.initWith(List.of(toUpdate));

        // When
        var portalMenuLinkToUpdate = PortalMenuLinkFixtures.anUpdatePortalMenuLink().toBuilder().name(null).build();
        var throwable = Assertions.catchThrowable(() ->
            updatePortalMenuLinkUseCase.execute(
                new UpdatePortalMenuLinkUseCase.Input(toUpdate.getId(), toUpdate.getEnvironmentId(), portalMenuLinkToUpdate)
            )
        );

        // Then
        Assertions.assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessage("Name is required.");
    }
}
