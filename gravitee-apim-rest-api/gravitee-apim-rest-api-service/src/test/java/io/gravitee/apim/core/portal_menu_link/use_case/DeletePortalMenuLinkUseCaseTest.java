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
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class DeletePortalMenuLinkUseCaseTest {

    private final PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudService = new PortalMenuLinkCrudServiceInMemory();
    private final PortalMenuLinkQueryServiceInMemory portalMenuLinkQueryService = new PortalMenuLinkQueryServiceInMemory(
        portalMenuLinkCrudService
    );
    private DeletePortalMenuLinkUseCase deletePortalMenuLinkUseCase;

    @BeforeEach
    void setUp() {
        deletePortalMenuLinkUseCase = new DeletePortalMenuLinkUseCase(portalMenuLinkCrudService, portalMenuLinkQueryService);
    }

    @AfterEach
    void afterEach() {
        portalMenuLinkCrudService.reset();
    }

    @Test
    void should_delete() {
        // Given
        var toDelete = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkCrudService.initWith(List.of(toDelete));

        // When
        deletePortalMenuLinkUseCase.execute(new DeletePortalMenuLinkUseCase.Input(toDelete.getId(), toDelete.getEnvironmentId()));

        // Then
        assertThat(portalMenuLinkCrudService.storage()).isEmpty();
    }

    @Test
    void should_reorder_when_delete() {
        // Given
        portalMenuLinkCrudService.initWith(
            List.of(
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id1").environmentId("env1").order(1).build(),
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id2").environmentId("env2").order(1).build(),
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id3").environmentId("env1").order(3).build(),
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id4").environmentId("env1").order(2).build(),
                PortalMenuLinkFixtures.aPortalMenuLink().toBuilder().id("id5").environmentId("env1").order(4).build()
            )
        );

        // When
        deletePortalMenuLinkUseCase.execute(new DeletePortalMenuLinkUseCase.Input("id4", "env1"));

        // Then
        assertThat(portalMenuLinkCrudService.storage()).hasSize(4);
        assertThat(portalMenuLinkCrudService.getByIdAndEnvironmentId("id1", "env1").getOrder()).isEqualTo(1);
        assertThat(portalMenuLinkCrudService.getByIdAndEnvironmentId("id2", "env2").getOrder()).isEqualTo(1);
        assertThat(portalMenuLinkCrudService.getByIdAndEnvironmentId("id3", "env1").getOrder()).isEqualTo(2);
        assertThat(portalMenuLinkCrudService.getByIdAndEnvironmentId("id5", "env1").getOrder()).isEqualTo(3);
    }

    @Test
    void should_throw_not_found_exception_when_portal_menu_link_to_delete_does_not_exist() {
        // Given
        var toDelete = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkCrudService.initWith(List.of(toDelete));

        // When
        var throwable = Assertions.catchThrowable(() ->
            deletePortalMenuLinkUseCase.execute(new DeletePortalMenuLinkUseCase.Input("unknown", toDelete.getEnvironmentId()))
        );

        // Then
        Assertions
            .assertThat(throwable)
            .isInstanceOf(PortalMenuLinkNotFoundException.class)
            .hasMessage("PortalMenuLink [ unknown ] not found");
    }
}
