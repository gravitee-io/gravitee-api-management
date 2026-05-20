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
package io.gravitee.repository.elasticsearch.tracing;

import io.gravitee.repository.tracing.TracingRepositoryTest;

/**
 * Runs the shared {@link TracingRepositoryTest} contract against the Elasticsearch implementation: the production
 * {@link ElasticsearchTracingRepository} queries an Elasticsearch testcontainer fed by an OTel Collector container
 * (configured for {@code mapping.mode: otel}), with fixtures seeded over OTLP HTTP by
 * {@link ElasticsearchTracingTestRepositoryInitializer}. {@code ElasticsearchTracingTest*} beans are picked up by
 * {@code AbstractRepositoryTest}'s {@code .*TestRepository.*} component scan.
 *
 * @author GraviteeSource Team
 */
public class ElasticsearchTracingRepositoryIT extends TracingRepositoryTest {}
