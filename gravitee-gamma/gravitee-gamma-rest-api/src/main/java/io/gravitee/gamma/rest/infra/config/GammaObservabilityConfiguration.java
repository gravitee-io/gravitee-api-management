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
package io.gravitee.gamma.rest.infra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Aggregates every per-sub-domain configuration under the Gamma Observability perimeter (traces,
 * filters, logs, analytics, dashboards — all mounted under {@code /observability/*} by
 * {@code GammaRootResource}) so {@code StandaloneConfiguration} only needs a single entry point.
 *
 * <p>Declares no beans of its own: each sub-domain still owns its {@code @ComponentScan} and adapter
 * {@code @Bean}s in its own {@code infra/config/<DomainName>Configuration.java} per AGENTS.md §6 —
 * this class is purely a re-export.
 *
 * @author GraviteeSource Team
 */
@Configuration
@Import(
    {
        GammaTracingConfiguration.class,
        GammaObservabilityFilterConfiguration.class,
        GammaLogsConfiguration.class,
        GammaAnalyticsConfiguration.class,
        GammaDashboardsConfiguration.class,
    }
)
public class GammaObservabilityConfiguration {}
