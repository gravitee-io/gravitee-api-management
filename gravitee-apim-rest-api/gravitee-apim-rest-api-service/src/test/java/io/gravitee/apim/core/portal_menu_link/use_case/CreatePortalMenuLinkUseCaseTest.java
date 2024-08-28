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
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLink;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkType;
import io.gravitee.apim.core.portal_menu_link.model.PortalMenuLinkVisibility;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class CreatePortalMenuLinkUseCaseTest {

    private final String ENV_ID = PortalMenuLinkFixtures.aPortalMenuLink().getEnvironmentId();

    private final PortalMenuLinkCrudServiceInMemory portalMenuLinkCrudService = new PortalMenuLinkCrudServiceInMemory();
    private final PortalMenuLinkQueryServiceInMemory portalMenuLinkQueryService = new PortalMenuLinkQueryServiceInMemory(
        portalMenuLinkCrudService
    );
    private CreatePortalMenuLinkUseCase createPortalMenuLinkUseCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @BeforeEach
    void setUp() {
        createPortalMenuLinkUseCase = new CreatePortalMenuLinkUseCase(portalMenuLinkCrudService, portalMenuLinkQueryService);
    }

    @AfterEach
    void afterEach() {
        portalMenuLinkCrudService.reset();
        portalMenuLinkQueryService.reset();
    }

    @Test
    void should_create() {
        // Given
        var toCreate = PortalMenuLinkFixtures.aCreatePortalMenuLink();

        // When
        var createdPortalMenuLink = createPortalMenuLinkUseCase.execute(new CreatePortalMenuLinkUseCase.Input(ENV_ID, toCreate));

        // Then
        var expected = PortalMenuLink
            .builder()
            .id("generated-id")
            .environmentId(ENV_ID)
            .name("portalMenuLinkNameToCreate")
            .target("portalMenuLinkTargetToCreate")
            .type(PortalMenuLinkType.EXTERNAL)
            .visibility(PortalMenuLinkVisibility.PUBLIC)
            .order(1)
            .build();
        assertThat(createdPortalMenuLink.portalMenuLink()).isNotNull();
        assertThat(createdPortalMenuLink.portalMenuLink()).isEqualTo(expected);
    }

    @Test
    void should_create_a_second_link() {
        // Given
        var toCreate = PortalMenuLinkFixtures.aCreatePortalMenuLink();
        var existingPortalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkQueryService.initWith(List.of(existingPortalMenuLink));

        // When
        var createdPortalMenuLink = createPortalMenuLinkUseCase.execute(new CreatePortalMenuLinkUseCase.Input(ENV_ID, toCreate));

        // Then
        var expected = PortalMenuLink
            .builder()
            .id("generated-id")
            .environmentId(ENV_ID)
            .name("portalMenuLinkNameToCreate")
            .target("portalMenuLinkTargetToCreate")
            .type(PortalMenuLinkType.EXTERNAL)
            .visibility(PortalMenuLinkVisibility.PUBLIC)
            .order(2)
            .build();
        assertThat(createdPortalMenuLink.portalMenuLink()).isNotNull();
        assertThat(createdPortalMenuLink.portalMenuLink()).isEqualTo(expected);
    }

    @Test
    void should_create_a_second_link_on_another_environment() {
        // Given
        var toCreate = PortalMenuLinkFixtures.aCreatePortalMenuLink();
        var existingPortalMenuLink = PortalMenuLinkFixtures.aPortalMenuLink();
        portalMenuLinkQueryService.initWith(List.of(existingPortalMenuLink));

        // When
        var createdPortalMenuLink = createPortalMenuLinkUseCase.execute(new CreatePortalMenuLinkUseCase.Input("anotherEnv", toCreate));

        // Then
        var expected = PortalMenuLink
            .builder()
            .id("generated-id")
            .environmentId("anotherEnv")
            .name("portalMenuLinkNameToCreate")
            .target("portalMenuLinkTargetToCreate")
            .type(PortalMenuLinkType.EXTERNAL)
            .visibility(PortalMenuLinkVisibility.PUBLIC)
            .order(1)
            .build();
        assertThat(createdPortalMenuLink.portalMenuLink()).isNotNull();
        assertThat(createdPortalMenuLink.portalMenuLink()).isEqualTo(expected);
    }

    @Test
    void should_throw_exception_when_name_is_null() {
        // Given
        var toCreate = PortalMenuLinkFixtures.aCreatePortalMenuLink().toBuilder().name(null).build();

        // When
        var throwable = Assertions.catchThrowable(() ->
            createPortalMenuLinkUseCase.execute(new CreatePortalMenuLinkUseCase.Input(ENV_ID, toCreate))
        );

        // Then
        Assertions.assertThat(throwable).isInstanceOf(InvalidDataException.class).hasMessage("Name is required.");
    }
}
