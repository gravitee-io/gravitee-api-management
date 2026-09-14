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
package io.gravitee.apim.infra.performance_target.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PerformanceTargetFixtures;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.apim.core.notification.model.PerformanceTargetNotificationTemplateData;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition.Kind;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PerformanceTargetNotificationTemplateDataFactoryTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final Instant T0 = Instant.parse("2026-09-14T10:05:00Z");
    private static final PerformanceTargetNotificationTemplateDataFactory.Subject SUBJECT =
        new PerformanceTargetNotificationTemplateDataFactory.Subject("API", "my-api", "api-id", "https://gamma/targets");

    InstallationAccessQueryService installationAccess = mock(InstallationAccessQueryService.class);
    PerformanceTargetNotificationTemplateDataFactory factory = new PerformanceTargetNotificationTemplateDataFactory(
        new AnalyticsDefinitionYAMLQueryService(),
        installationAccess
    );

    @Test
    void should_word_each_rule_with_its_label_unit_and_schedule_and_date_the_report_by_the_latest_evaluation() {
        var latency = PerformanceTargetFixtures.aLatencyRule();
        var errors = PerformanceTarget.Rule.builder()
            .id("errors-rule")
            .metric(MetricSpec.Name.HTTP_ERROR_RATE)
            .measure(MetricSpec.Measure.PERCENTAGE)
            .operator(PerformanceTarget.Operator.LTE)
            .threshold(5)
            .build();
        var target = PerformanceTargetFixtures.aTarget("target-1")
            .toBuilder()
            .window(Duration.ofHours(1))
            .interval(Duration.ofMinutes(5))
            .rules(List.of(latency, errors))
            .build();
        var breached = PerformanceTargetFixtures.anEvaluation("e-1", "target-1", PerformanceTargetEvaluation.Status.BREACH, T0);

        var data = factory.build(
            SUBJECT,
            List.of(
                new PerformanceTargetRuleTransition(Kind.RULE_MISSED, target, latency, breached.rules().getFirst(), breached),
                new PerformanceTargetRuleTransition(Kind.RULE_MISSED, target, errors, errors.evaluate(7.25, 91, 20), breached)
            )
        );

        assertThat(data.getSubjectKind()).isEqualTo("API");
        assertThat(data.getSubjectName()).isEqualTo("my-api");
        assertThat(data.getSubjectUrl()).isEqualTo("https://gamma/targets");
        assertThat(data.getChangedAtText()).isEqualTo("2026-09-14 10:05 UTC");
        assertThat(data.getChangedAt()).isEqualTo(java.util.Date.from(T0));
        assertThat(data.getRules())
            .extracting(
                PerformanceTargetNotificationTemplateData.RuleChange::getDescription,
                PerformanceTargetNotificationTemplateData.RuleChange::getObservedText,
                PerformanceTargetNotificationTemplateData.RuleChange::getThresholdText,
                PerformanceTargetNotificationTemplateData.RuleChange::getWindowText,
                PerformanceTargetNotificationTemplateData.RuleChange::getIntervalText,
                PerformanceTargetNotificationTemplateData.RuleChange::getStatus
            )
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(
                    labelOf(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME) + " P95 ≤ 2000 ms",
                    "4810 ms",
                    "2000 ms",
                    "1 h",
                    "5 min",
                    "BREACH"
                ),
                org.assertj.core.groups.Tuple.tuple(
                    labelOf(MetricSpec.Name.HTTP_ERROR_RATE) + " ≤ 5 %",
                    "7.25 %",
                    "5 %",
                    "1 h",
                    "5 min",
                    "BREACH"
                )
            );
        assertThat(data.getRules().getFirst())
            .extracting(
                PerformanceTargetNotificationTemplateData.RuleChange::getRuleId,
                PerformanceTargetNotificationTemplateData.RuleChange::getMetric,
                PerformanceTargetNotificationTemplateData.RuleChange::getUnit,
                PerformanceTargetNotificationTemplateData.RuleChange::getSampleCount
            )
            .containsExactly(PerformanceTargetFixtures.LATENCY_RULE_ID, "HTTP_GATEWAY_RESPONSE_TIME", "MILLISECONDS", 63L);
    }

    @Test
    void should_say_no_data_for_a_rule_that_could_not_be_evaluated() {
        var target = PerformanceTargetFixtures.aTarget("target-1");
        var evaluation = PerformanceTargetFixtures.anEvaluation("e-1", "target-1", PerformanceTargetEvaluation.Status.NOT_EVALUABLE, T0);

        var data = factory.build(
            SUBJECT,
            List.of(
                new PerformanceTargetRuleTransition(
                    Kind.RULE_NOT_EVALUABLE,
                    target,
                    target.rules().getFirst(),
                    evaluation.rules().getFirst(),
                    evaluation
                )
            )
        );

        assertThat(data.getRules().getFirst().getObserved()).isNull();
        assertThat(data.getRules().getFirst().getObservedText()).isEqualTo("no data");
        assertThat(data.getRules().getFirst().getSampleCount()).isZero();
    }

    @Test
    void should_link_to_the_targets_page_under_the_environment_hrid_and_stay_silent_without_a_gamma_url() {
        var environment = Environment.builder().id("env-id").organizationId(ORGANIZATION_ID).hrids(List.of("dev", "development")).build();
        when(installationAccess.getGammaUrl(ORGANIZATION_ID)).thenReturn("https://gamma.example.com/");

        var withoutHrid = Environment.builder().id("env-id").organizationId(ORGANIZATION_ID).hrids(List.of()).build();

        var api = ApiFixtures.aProxyApiV4().toBuilder().id("api-1").name("orders").build();
        var subject = factory.apiSubject(environment, api);
        assertThat(subject.kind()).isEqualTo("API");
        assertThat(subject.name()).isEqualTo("orders");
        assertThat(subject.reference()).isEqualTo("api-1");
        assertThat(subject.url()).isEqualTo("https://gamma.example.com/environments/dev/apim/apis/api-1/targets");
        assertThat(factory.targetsUrl(withoutHrid, "aim/catalog/agents/a-1/targets")).isEqualTo(
            "https://gamma.example.com/environments/env-id/aim/catalog/agents/a-1/targets"
        );

        when(installationAccess.getGammaUrl(ORGANIZATION_ID)).thenReturn(null);
        assertThat(factory.apiSubject(environment, api).url()).isNull();
    }

    @Test
    void should_introduce_llm_mcp_and_a2a_proxies_by_their_kind_and_link_to_the_aim_pages() {
        var environment = Environment.builder().id("env-id").organizationId(ORGANIZATION_ID).hrids(List.of("dev")).build();
        when(installationAccess.getGammaUrl(ORGANIZATION_ID)).thenReturn("https://gamma.example.com");

        var llm = factory.apiSubject(environment, ApiFixtures.aLLMProxyApiV4().toBuilder().id("llm-1").build());
        var mcp = factory.apiSubject(environment, ApiFixtures.aMCPProxyApiV4().toBuilder().id("mcp-1").build());
        var a2a = factory.apiSubject(environment, ApiFixtures.anA2AProxyApiV4().toBuilder().id("a2a-1").build());
        var v2 = factory.apiSubject(environment, ApiFixtures.aProxyApiV2().toBuilder().id("v2-1").build());

        assertThat(llm.kind()).isEqualTo("LLM proxy");
        assertThat(llm.url()).isEqualTo("https://gamma.example.com/environments/dev/aim/llm-proxy/llm-1/targets");
        assertThat(mcp.kind()).isEqualTo("MCP proxy");
        assertThat(mcp.url()).isEqualTo("https://gamma.example.com/environments/dev/aim/mcp-proxy/mcp-1/targets");
        assertThat(a2a.kind()).isEqualTo("A2A proxy");
        assertThat(a2a.url()).isEqualTo("https://gamma.example.com/environments/dev/aim/agent-runtime/a2a-1/targets");
        assertThat(v2.kind()).isEqualTo("API");
        assertThat(v2.url()).isEqualTo("https://gamma.example.com/environments/dev/apim/apis/v2-1/targets");
    }

    private static String labelOf(MetricSpec.Name metric) {
        return new AnalyticsDefinitionYAMLQueryService().findMetric(metric).orElseThrow().label();
    }

    @ParameterizedTest
    @CsvSource(
        {
            "2000, MILLISECONDS, 2000 ms",
            "1.6, PERCENT, 1.6 %",
            "0.0123, NUMBER, 0.012",
            "1048576, BYTES, 1048576 B",
            "2.5, PER_SECOND, 2.5/s",
        }
    )
    void should_format_values_in_their_unit_without_trailing_zeros(double value, MetricSpec.Unit unit, String expected) {
        assertThat(PerformanceTargetNotificationTemplateDataFactory.format(value, unit)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "PT30S, 30 s", "PT15M, 15 min", "PT1H, 1 h", "PT24H, 24 h", "PT90M, 90 min" })
    void should_word_durations_in_whole_units(String iso, String expected) {
        assertThat(PerformanceTargetNotificationTemplateDataFactory.durationText(Duration.parse(iso))).isEqualTo(expected);
    }
}
