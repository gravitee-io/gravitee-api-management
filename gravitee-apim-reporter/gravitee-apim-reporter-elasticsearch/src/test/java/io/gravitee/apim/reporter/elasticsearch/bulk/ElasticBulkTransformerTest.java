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
package io.gravitee.apim.reporter.elasticsearch.bulk;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.reporter.common.formatter.Formatter;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.buffer.Buffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElasticBulkTransformerTest {

    private final AtomicReference<Map<String, Object>> formatterOptions = new AtomicReference<>();
    private final Formatter<Reportable> formatter = new Formatter<>() {
        @Override
        public Buffer format(Reportable reportable) {
            return Buffer.buffer();
        }

        @Override
        public Buffer format(Reportable reportable, Map<String, Object> options) {
            formatterOptions.set(options);
            return Buffer.buffer();
        }
    };
    private PipelineConfiguration pipelineConfiguration;
    private ElasticBulkTransformer transformer;

    @BeforeEach
    void setUp() {
        pipelineConfiguration = new PipelineConfiguration(null);
        transformer = new ElasticBulkTransformer(formatter, pipelineConfiguration, reportable -> "gravitee-request-2026.11.04");
    }

    @Test
    void should_omit_the_pipeline_when_it_is_not_valid() {
        transformer.transform(Metrics.on(System.currentTimeMillis()).build());

        assertThat(formatterOptions.get()).containsExactly(Map.entry("index", "gravitee-request-2026.11.04"));
    }

    @Test
    void should_pass_the_pipeline_when_it_is_valid() {
        pipelineConfiguration.valid();

        transformer.transform(Metrics.on(System.currentTimeMillis()).build());

        assertThat(formatterOptions.get()).containsOnly(
            Map.entry("index", "gravitee-request-2026.11.04"),
            Map.entry("pipeline", "gravitee_pipeline")
        );
    }
}
