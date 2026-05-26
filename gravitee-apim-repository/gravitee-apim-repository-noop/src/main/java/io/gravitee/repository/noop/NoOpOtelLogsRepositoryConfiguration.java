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
package io.gravitee.repository.noop;

import io.gravitee.repository.noop.otel.log.NoOpOtelLogRepository;
import io.gravitee.repository.otel.log.api.OtelLogRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loaded for the {@code OTEL_LOGS} scope when {@code repositories.otel-logs.type=none} — exposes a
 * no-op {@link OtelLogRepository} bean so trace-detail consumers can call into the SPI uniformly and
 * render spans without their events / payload logs when the operator hasn't wired a backend.
 *
 * @author GraviteeSource Team
 */
@Configuration
public class NoOpOtelLogsRepositoryConfiguration {

    @Bean
    public OtelLogRepository otelLogRepository() {
        return new NoOpOtelLogRepository();
    }
}
