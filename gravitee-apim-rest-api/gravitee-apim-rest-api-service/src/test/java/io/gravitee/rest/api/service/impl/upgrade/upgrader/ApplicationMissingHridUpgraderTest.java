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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Environment;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class ApplicationMissingHridUpgraderTest {

    private static final String ENV_ID = "DEFAULT";

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @InjectMocks
    private ApplicationMissingHridUpgrader upgrader;

    @Test
    void should_set_hrid_to_id_when_missing() throws TechnicalException, UpgraderException {
        var application = Application.builder().id("app-id").environmentId(ENV_ID).build();
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.builder().id(ENV_ID).build()));
        when(applicationRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(application));

        assertThat(upgrader.upgrade()).isTrue();

        var captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).update(captor.capture());
        assertThat(captor.getValue().getHrid()).isEqualTo("app-id");
    }

    @Test
    void should_skip_application_with_existing_hrid() throws TechnicalException, UpgraderException {
        var application = Application.builder().id("app-id").hrid("gitops-hrid").environmentId(ENV_ID).build();
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.builder().id(ENV_ID).build()));
        when(applicationRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(application));

        assertThat(upgrader.upgrade()).isTrue();

        verify(applicationRepository, never()).update(any());
    }

    @Test
    void should_update_only_applications_with_missing_hrid_in_mixed_set() throws TechnicalException, UpgraderException {
        var withoutHrid = Application.builder().id("app-without-hrid").environmentId(ENV_ID).build();
        var withHrid = Application.builder().id("app-with-hrid").hrid("existing-hrid").environmentId(ENV_ID).build();
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.builder().id(ENV_ID).build()));
        when(applicationRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(withoutHrid, withHrid));

        assertThat(upgrader.upgrade()).isTrue();

        verify(applicationRepository).update(
            argThat(app -> "app-without-hrid".equals(app.getId()) && "app-without-hrid".equals(app.getHrid()))
        );
        verify(applicationRepository, times(1)).update(any());
    }

    @Test
    void should_set_hrid_to_id_when_hrid_is_empty() throws TechnicalException, UpgraderException {
        var application = Application.builder().id("app-id").hrid("").environmentId(ENV_ID).build();
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.builder().id(ENV_ID).build()));
        when(applicationRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(application));

        assertThat(upgrader.upgrade()).isTrue();

        verify(applicationRepository).update(argThat(app -> "app-id".equals(app.getId()) && "app-id".equals(app.getHrid())));
    }

    @Test
    void should_set_hrid_to_id_when_hrid_is_whitespace() throws TechnicalException, UpgraderException {
        var application = Application.builder().id("app-id").hrid(" ").environmentId(ENV_ID).build();
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.builder().id(ENV_ID).build()));
        when(applicationRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(application));

        assertThat(upgrader.upgrade()).isTrue();

        verify(applicationRepository).update(argThat(app -> "app-id".equals(app.getId()) && "app-id".equals(app.getHrid())));
    }

    @Test
    void should_throw_upgrader_exception_when_find_all_environments_fails() throws TechnicalException {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("failure"));

        assertThatThrownBy(() -> upgrader.upgrade())
            .isInstanceOf(UpgraderException.class)
            .hasMessageContaining("failure");
    }

    @Test
    void should_throw_upgrader_exception_when_update_fails() throws TechnicalException {
        var application = Application.builder().id("app-id").environmentId(ENV_ID).build();
        when(environmentRepository.findAll()).thenReturn(Set.of(Environment.builder().id(ENV_ID).build()));
        when(applicationRepository.findAllByEnvironment(ENV_ID)).thenReturn(Set.of(application));
        when(applicationRepository.update(any())).thenThrow(new TechnicalException("update failed"));

        assertThatThrownBy(() -> upgrader.upgrade())
            .isInstanceOf(UpgraderException.class)
            .hasMessageContaining("update failed");
    }

    @Test
    void should_have_correct_order() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.APPLICATION_MISSING_HRID_UPGRADER);
    }
}
