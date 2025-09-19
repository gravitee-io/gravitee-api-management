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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.service.PortalPageService;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class EnvironmentsDefaultPortalPageUpgraderTest {

    private static final Environment ANOTHER_ENVIRONMENT = Environment.builder()
        .id("ANOTHER_ENVIRONMENT")
        .hrids(List.of("another environment"))
        .name("another environment")
        .organizationId("DEFAULT")
        .build();

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    PortalPageService portalPageService;

    private EnvironmentsDefaultPortalPageUpgrader upgrader;

    @BeforeEach
    public void setUp() {
        upgrader = new EnvironmentsDefaultPortalPageUpgrader(environmentRepository, portalPageService);
    }

    @Test
    @SneakyThrows
    void should_do_nothing_when_there_is_no_environment() {
        when(environmentRepository.findAll()).thenReturn(Collections.emptySet());
        assertThat(upgrader.upgrade()).isTrue();
        verifyNoInteractions(portalPageService);
    }

    @Test
    @SneakyThrows
    void should_return_false_when_something_wrong_happens() {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("this is a test exception"));
        assertThat(upgrader.upgrade()).isFalse();
    }

    @Test
    @SneakyThrows
    void should_create_default_portal_page_for_both_environments() {
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.DEFAULT, ANOTHER_ENVIRONMENT));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<String> portalPageCaptor = ArgumentCaptor.forClass(String.class);

        verify(portalPageService, times(2)).createDefaultPortalHomePage(portalPageCaptor.capture());
        List<String> capturedValues = portalPageCaptor.getAllValues();
        assertThat(capturedValues).containsExactlyInAnyOrder("DEFAULT", "ANOTHER_ENVIRONMENT");
    }
}
