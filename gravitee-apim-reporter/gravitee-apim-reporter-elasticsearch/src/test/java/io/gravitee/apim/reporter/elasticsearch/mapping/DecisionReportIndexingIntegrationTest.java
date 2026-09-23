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
package io.gravitee.apim.reporter.elasticsearch.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.reporter.common.bulk.compressor.NoneBulkCompressor;
import io.gravitee.apim.reporter.common.formatter.FormatterFactory;
import io.gravitee.apim.reporter.common.formatter.Type;
import io.gravitee.apim.reporter.elasticsearch.IntegrationTestConfiguration;
import io.gravitee.apim.reporter.elasticsearch.bulk.ElasticBulkTransformer;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.factory.BeanFactoryBuilder;
import io.gravitee.apim.reporter.elasticsearch.indexer.IndexNameGenerator;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.bulk.BulkResponse;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.v4.metric.AdditionalMetric;
import io.gravitee.reporter.api.v4.report.DecisionReport;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(IntegrationTestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DecisionReportIndexingIntegrationTest {

    private static final String INDEX_NAME = "gravitee-decision-indexing";
    private static final String FORBIDDEN = "evt-forbidden";
    private static final String RULE_VERSION = "2026-09-23T10:00:00Z";

    @Autowired
    private Client client;

    @Autowired
    private Node node;

    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    @Autowired
    private PipelineConfiguration pipelineConfiguration;

    private IndexNameGenerator indexNameGenerator;
    private ElasticBulkTransformer transformer;
    private String dataStream;

    @BeforeAll
    void report_a_forbidden_decision() throws Exception {
        var configuration = new ReporterConfiguration();
        configuration.setIndexName(INDEX_NAME);
        var beanFactory = BeanFactoryBuilder.buildFactory(client);
        beanFactory
            .createIndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, client)
            .prepare()
            .test()
            .awaitDone(60, TimeUnit.SECONDS)
            .assertComplete();
        indexNameGenerator = beanFactory.createIndexNameGenerator(configuration);
        transformer = new ElasticBulkTransformer(
            new FormatterFactory(node, beanFactory.createFormatterFactoryConfiguration()).getFormatter(Type.ELASTICSEARCH),
            pipelineConfiguration,
            indexNameGenerator
        );

        var forbidden = decision(FORBIDDEN, "FORBID", List.of("Matched policy no-pii"));
        dataStream = indexNameGenerator.generate(forbidden);

        assertThat(report(forbidden).getErrors()).as("bulk indexing of %s into %s reported errors", FORBIDDEN, dataStream).isFalse();
    }

    @Test
    void should_write_the_decision_into_the_decisions_data_stream() {
        assertThat(dataStream).startsWith(INDEX_NAME + "-decisions");
        assertThat(hits(term("event-id", FORBIDDEN))).isOne();
    }

    @Test
    void should_find_the_decision_on_its_decision_point_its_phase_and_its_verdict() {
        assertThat(hitsOnTheForbiddenDecision(term("decision-point-type", "authz"))).isOne();
        assertThat(hitsOnTheForbiddenDecision(term("phase", "RESOLVED"))).isOne();
        assertThat(hitsOnTheForbiddenDecision(term("verdict", "FORBID"))).isOne();
    }

    @Test
    void should_find_the_decision_on_the_version_of_a_rule_it_matched() {
        var ruleVersion = JsonObject.of(
            "nested",
            JsonObject.of("path", "matched-rules", "query", term("matched-rules.version", RULE_VERSION))
        );

        assertThat(hitsOnTheForbiddenDecision(ruleVersion)).isOne();
    }

    @Test
    void should_find_the_decision_on_its_index_in_the_batch() {
        assertThat(hitsOnTheForbiddenDecision(term("additional-metrics.int_authz_batch-index", 1))).isOne();
    }

    @Test
    void should_find_the_decision_on_every_value_of_a_multi_valued_keyword_metric() {
        assertThat(hitsOnTheForbiddenDecision(term("additional-metrics.keyword_authz_tools", "search"))).isOne();
        assertThat(hitsOnTheForbiddenDecision(term("additional-metrics.keyword_authz_tools", "book"))).isOne();
    }

    private BulkResponse report(DecisionReport decision) {
        try {
            var bulk = new NoneBulkCompressor().compress(List.of(transformer.transform(decision))).compressed();
            return client.bulk(bulk, true).blockingGet();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private long hitsOnTheForbiddenDecision(JsonObject query) {
        return hits(JsonObject.of("bool", JsonObject.of("filter", JsonArray.of(term("event-id", FORBIDDEN), query))));
    }

    private long hits(JsonObject query) {
        return client.search(dataStream, null, JsonObject.of("query", query).encode()).blockingGet().getSearchHits().getTotal().getValue();
    }

    private static JsonObject term(String field, Object value) {
        return JsonObject.of("term", JsonObject.of(field, value));
    }

    private static DecisionReport decision(String eventId, String verdict, List<String> reasons) {
        return DecisionReport.builder()
            .timestamp(System.currentTimeMillis())
            .gatewayId("gw-1")
            .organizationId("org-1")
            .environmentId("env-1")
            .apiId("api-1")
            .eventId(eventId)
            .requestId("req-1")
            .batchId("batch-1")
            .phase(DecisionReport.Phase.RESOLVED)
            .decisionPointType(DecisionReport.DECISION_POINT_AUTHZ)
            .decisionPointId("pdp-a")
            .decisionPointVersion("7")
            .caller("pep")
            .subjectType("User")
            .subjectId("alice")
            .action("read")
            .resourceType("Account")
            .resourceId("acc-1")
            .outcome("FORBID".equals(verdict) ? DecisionReport.Outcome.DENY : DecisionReport.Outcome.ALLOW)
            .enforced("FORBID".equals(verdict) ? DecisionReport.Enforced.DENY : DecisionReport.Enforced.ALLOW)
            .verdict(verdict)
            .reasons(reasons)
            .matchedRules(List.of(new DecisionReport.MatchedRule("rule-1", "no-pii", RULE_VERSION, verdict, Map.of())))
            .status(DecisionReport.Status.SUCCESS)
            .durationNanos(120_000L)
            .additionalMetrics(
                Set.of(
                    new AdditionalMetric.IntegerMetric("int_authz_batch-index", 1),
                    new AdditionalMetric.IntegerMetric("int_authz_batch-size", 2),
                    new AdditionalMetric.KeywordListMetric("keyword_authz_tools", List.of("search", "book"))
                )
            )
            .build();
    }
}
