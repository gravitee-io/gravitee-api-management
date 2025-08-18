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
package io.gravitee.rest.api.repository.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.repository.analytics.api.AnalyticsRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.vertx.core.Vertx;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsRepositoryProbeTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    private AnalyticsRepositoryProbe cut;

    @BeforeEach
    public void beforeEach() {
        cut = new AnalyticsRepositoryProbe();
        cut.setAnalyticsRepository(analyticsRepository);
        cut.setVertx(Vertx.vertx());
    }

    @Test
    void should_check_completed_with_healthy_state() throws ExecutionException, InterruptedException {
        Result result = cut.check().toCompletableFuture().get();
        assertThat(result.isHealthy()).isTrue();
    }

    @Test
    void should_check_completed_with_unhealthy_state_when_repository_failed()
        throws TechnicalException, ExecutionException, InterruptedException {
        RuntimeException runtimeException = new RuntimeException();
        when(analyticsRepository.query(any(), any())).thenThrow(runtimeException);
        Result result = cut.check().toCompletableFuture().get();
        assertThat(result.isHealthy()).isFalse();
    }

    @Test
    void should_not_be_cacheable() {
        assertThat(cut.isCacheable()).isFalse();
    }
}
