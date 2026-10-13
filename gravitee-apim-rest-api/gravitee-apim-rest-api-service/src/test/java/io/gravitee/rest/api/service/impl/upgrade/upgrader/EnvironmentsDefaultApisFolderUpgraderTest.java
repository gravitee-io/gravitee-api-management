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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.use_case.CreateDefaultPortalNavigationItemsUseCase;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EnvironmentsDefaultApisFolderUpgraderTest {

    private static final Environment ANOTHER_ENVIRONMENT = Environment.builder()
        .id("ANOTHER_ENVIRONMENT")
        .hrids(List.of("another environment"))
        .name("another environment")
        .organizationId("ANOTHER_ORG")
        .build();

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    CreateDefaultPortalNavigationItemsUseCase createDefaultPortalNavigationItemsUseCase;

    private EnvironmentsDefaultApisFolderUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new EnvironmentsDefaultApisFolderUpgrader(environmentRepository, createDefaultPortalNavigationItemsUseCase);
    }

    @Test
    @SneakyThrows
    void should_do_nothing_when_there_is_no_environment() {
        when(environmentRepository.findAll()).thenReturn(Collections.emptySet());

        assertThat(upgrader.upgrade()).isTrue();

        verifyNoInteractions(createDefaultPortalNavigationItemsUseCase);
    }

    @Test
    @SneakyThrows
    void should_add_the_section_to_every_environment() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT, ANOTHER_ENVIRONMENT));

        assertThat(upgrader.upgrade()).isTrue();

        verify(createDefaultPortalNavigationItemsUseCase).ensureApisSection("DEFAULT", "DEFAULT");
        verify(createDefaultPortalNavigationItemsUseCase).ensureApisSection("ANOTHER_ORG", "ANOTHER_ENVIRONMENT");
    }

    @Test
    @SneakyThrows
    void should_not_recreate_the_other_default_items() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT));

        upgrader.upgrade();

        // Seeding everything would bring back a "Guides" folder or a Home Page that a user removed
        verify(createDefaultPortalNavigationItemsUseCase, never()).execute(any(), any());
    }

    @Test
    @SneakyThrows
    void should_fail_when_the_environments_cannot_be_read() {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("this is a test exception"));

        assertThatThrownBy(() -> upgrader.upgrade())
            .isInstanceOf(UpgraderException.class)
            .hasMessageContaining("this is a test exception");
    }

    @Test
    void should_run_after_the_other_portal_navigation_upgraders() {
        assertThat(upgrader.getOrder())
            .isEqualTo(727)
            .isGreaterThan(UpgraderOrder.PORTAL_NAVIGATION_ITEM_API_OWNED_REKEY_UPGRADER)
            .isGreaterThan(UpgraderOrder.ENVIRONMENTS_DEFAULT_PORTAL_NAVIGATION_ITEMS_UPGRADER);
    }
}
