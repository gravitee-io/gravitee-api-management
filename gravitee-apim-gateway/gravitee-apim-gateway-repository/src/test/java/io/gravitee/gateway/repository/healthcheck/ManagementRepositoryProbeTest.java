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
package io.gravitee.gateway.repository.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.repository.management.api.EventRepository;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ManagementRepositoryProbeTest {

    @Mock
    private EventRepository eventRepository;

    private ManagementRepositoryProbe cut;

    @BeforeEach
    public void beforeEach() {
        cut = new ManagementRepositoryProbe();
        cut.setEventRepository(eventRepository);
    }

    @Test
    void should_check_completed_with_healthy_state() throws ExecutionException, InterruptedException {
        Result result = cut.check().get();
        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    void should_check_completed_with_unhealthy_state_when_repository_failed() throws ExecutionException, InterruptedException {
        RuntimeException runtimeException = new RuntimeException();
        when(eventRepository.search(any())).thenThrow(runtimeException);
        Result result = cut.check().get();
        assertThat(result.isHealthy()).isFalse();
    }
}
