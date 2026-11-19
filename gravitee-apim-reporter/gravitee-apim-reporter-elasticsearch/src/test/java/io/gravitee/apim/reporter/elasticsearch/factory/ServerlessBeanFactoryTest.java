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
package io.gravitee.apim.reporter.elasticsearch.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.reporter.common.formatter.FormatterFactory;
import io.gravitee.apim.reporter.common.formatter.Type;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ServerlessRetention;
import io.gravitee.apim.reporter.elasticsearch.factory.es8.Elastic8xBeanFactory;
import io.gravitee.apim.reporter.elasticsearch.factory.es9.Elastic9xBeanFactory;
import io.gravitee.apim.reporter.elasticsearch.mapping.serverless.ServerlessIndexPreparer;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.http.Metrics;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ServerlessBeanFactoryTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-11-17T10:00:00Z");
    private static final ReporterConfiguration CONFIGURATION = new ReporterConfiguration();

    static Stream<BeanFactory> backends() {
        return Stream.of(new Elastic8xBeanFactory(), new Elastic9xBeanFactory());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("backends")
    void should_write_each_type_to_the_data_stream_named_after_the_prefix_and_the_type(BeanFactory backend) {
        var indexNameGenerator = serverless(backend).createIndexNameGenerator(CONFIGURATION);

        assertThat(indexNameGenerator.generate(Metrics.on(TIMESTAMP.toEpochMilli()).build())).isEqualTo("gravitee-request");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("backends")
    void should_create_documents_with_their_id(BeanFactory backend) {
        var formatterConfiguration = serverless(backend).createFormatterFactoryConfiguration();
        Node node = when(mock(Node.class).id()).thenReturn("gateway-id").getMock();
        EndpointStatus status = EndpointStatus.forEndpoint("api-id", "endpoint").apiName("api").on(TIMESTAMP.toEpochMilli()).build();

        String rendered = new FormatterFactory(node, formatterConfiguration)
            .getFormatter(Type.ELASTICSEARCH)
            .format(status, Map.of("index", "gravitee-health"))
            .toString();

        assertThat(rendered.strip()).startsWith("{ \"create\" : { \"_index\" : \"gravitee-health\", \"_id\" : \"" + status.getId() + "\"");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("backends")
    void should_prepare_the_serverless_templates(BeanFactory backend) {
        assertThat(serverless(backend).createIndexPreparer(CONFIGURATION, null, null, null)).isInstanceOf(ServerlessIndexPreparer.class);
    }

    private static ServerlessBeanFactory serverless(BeanFactory backend) {
        return new ServerlessBeanFactory(backend, ServerlessRetention.of(CONFIGURATION));
    }
}
