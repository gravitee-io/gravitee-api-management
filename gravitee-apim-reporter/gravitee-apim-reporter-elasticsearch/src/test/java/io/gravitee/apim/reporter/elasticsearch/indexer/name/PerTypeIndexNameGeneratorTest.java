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
package io.gravitee.apim.reporter.elasticsearch.indexer.name;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.indexer.PerTypeIndexNameGenerator;
import io.gravitee.reporter.api.v4.metric.event.ApiEventMetrics;
import io.gravitee.reporter.api.v4.metric.event.AuthzEventMetrics;
import io.gravitee.reporter.api.v4.metric.event.DecisionEventMetrics;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PerTypeIndexNameGeneratorTest {

    private PerTypeIndexNameGenerator cut;

    @BeforeEach
    public void beforeEach() {
        ReporterConfiguration configuration = new ReporterConfiguration();
        configuration.setIndexName("indexName");
        cut = new PerTypeIndexNameGenerator(configuration);
    }

    @Test
    public void generate_should_generate_index_name_with_type_and_no_date() {
        String indexName = cut.generate("indextype", Instant.parse("2018-04-28T18:35:24.00Z"));
        assertThat(indexName).isEqualTo("indexName-indextype");
    }

    @Test
    public void generate_should_route_authorization_decisions_to_their_own_index() {
        assertThat(
            cut.generate(
                AuthzEventMetrics.builder()
                    .gatewayId("gw")
                    .organizationId("org")
                    .environmentId("env")
                    .apiId("api")
                    .operation(AuthzEventMetrics.OPERATION_EVALUATE)
                    .eventId("evt-1")
                    .status(AuthzEventMetrics.STATUS_SUCCESS)
                    .build()
            )
        ).isEqualTo("indexName-authz-decisions");
    }

    @Test
    public void generate_should_route_decisions_to_their_own_index() {
        assertThat(
            cut.generate(
                DecisionEventMetrics.builder()
                    .gatewayId("gw")
                    .organizationId("org")
                    .environmentId("env")
                    .apiId("api")
                    .eventId("evt-1")
                    .phase(DecisionEventMetrics.Phase.RESOLVED)
                    .decisionPointType(DecisionEventMetrics.DECISION_POINT_GUARDIAN)
                    .decisionPointId("prompt-guardian")
                    .outcome(DecisionEventMetrics.Outcome.ALLOW)
                    .enforced(DecisionEventMetrics.Enforced.ALLOW)
                    .status(DecisionEventMetrics.Status.SUCCESS)
                    .build()
            )
        ).isEqualTo("indexName-decisions");
    }

    @Test
    public void generate_should_keep_the_other_event_metrics_on_the_shared_index() {
        assertThat(
            cut.generate(ApiEventMetrics.builder().gatewayId("gw").organizationId("org").environmentId("env").apiId("api").build())
        ).isEqualTo("indexName-event-metrics");
    }
}
