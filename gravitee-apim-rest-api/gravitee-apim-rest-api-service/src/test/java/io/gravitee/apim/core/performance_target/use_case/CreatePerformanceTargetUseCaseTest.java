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
package io.gravitee.apim.core.performance_target.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PerformanceTargetFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PerformanceTargetCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.performance_target.domain_service.ValidatePerformanceTargetDomainService;
import io.gravitee.apim.core.performance_target.exception.InvalidPerformanceTargetException;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatePerformanceTargetUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId(PerformanceTargetFixtures.ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    PerformanceTargetCrudServiceInMemory targetCrudService = new PerformanceTargetCrudServiceInMemory();

    CreatePerformanceTargetUseCase useCase = new CreatePerformanceTargetUseCase(
        targetCrudService,
        new ValidatePerformanceTargetDomainService(apiCrudService, new AnalyticsDefinitionYAMLQueryService())
    );

    @BeforeEach
    void setUp() {
        apiCrudService.initWith(List.of(ApiFixtures.anA2AProxyApiV4().toBuilder().id(PerformanceTargetFixtures.A2A_API_ID).build()));
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiCrudService, targetCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_give_the_target_and_each_of_its_rules_an_id() {
        var declared = PerformanceTargetFixtures.aTarget()
            .toBuilder()
            .id(null)
            .rules(List.of(anonymous(PerformanceTargetFixtures.aLatencyRule()), anonymous(anErrorRateRule())))
            .build();

        var output = useCase.execute(new CreatePerformanceTargetUseCase.Input(declared, AUDIT_INFO));

        assertThat(output.target().id()).isNotBlank();
        assertThat(output.target().rules()).extracting(PerformanceTarget.Rule::id).doesNotContainNull().doesNotHaveDuplicates();
        assertThat(targetCrudService.storage()).singleElement().isEqualTo(output.target());
    }

    @Test
    void should_reject_a_rule_declared_with_an_id_since_none_can_be_known_yet() {
        var declared = PerformanceTargetFixtures.aTarget().toBuilder().id(null).build();

        assertThatThrownBy(() -> useCase.execute(new CreatePerformanceTargetUseCase.Input(declared, AUDIT_INFO)))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessage("Unknown rule id: " + PerformanceTargetFixtures.LATENCY_RULE_ID);
        assertThat(targetCrudService.storage()).isEmpty();
    }

    private static PerformanceTarget.Rule anonymous(PerformanceTarget.Rule rule) {
        return rule.toBuilder().id(null).build();
    }

    private static PerformanceTarget.Rule anErrorRateRule() {
        return PerformanceTarget.Rule.builder()
            .metric(io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.HTTP_ERROR_RATE)
            .measure(io.gravitee.apim.core.analytics_engine.model.MetricSpec.Measure.PERCENTAGE)
            .operator(PerformanceTarget.Operator.LTE)
            .threshold(5)
            .build();
    }
}
