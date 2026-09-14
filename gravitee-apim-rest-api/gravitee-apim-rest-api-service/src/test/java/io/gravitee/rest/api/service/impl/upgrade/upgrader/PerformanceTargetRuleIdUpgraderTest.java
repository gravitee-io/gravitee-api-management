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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PerformanceTargetRepository;
import io.gravitee.repository.management.model.PerformanceTarget;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PerformanceTargetRuleIdUpgraderTest {

    @Mock
    private PerformanceTargetRepository performanceTargetRepository;

    @InjectMocks
    private PerformanceTargetRuleIdUpgrader upgrader;

    @Test
    void should_give_an_id_to_every_rule_that_has_none() throws Exception {
        var target = PerformanceTarget.builder()
            .id("target-id")
            .rules(
                List.of(
                    rule(null, "HTTP_ERROR_RATE", 1.0),
                    rule("kept-id", "HTTP_GATEWAY_LATENCY", 25.0),
                    rule(null, "HTTP_ERROR_RATE", 5.0)
                )
            )
            .build();
        when(performanceTargetRepository.findAll()).thenReturn(Set.of(target));

        assertThat(upgrader.upgrade()).isTrue();

        var captor = ArgumentCaptor.forClass(PerformanceTarget.class);
        verify(performanceTargetRepository).update(captor.capture());
        var rules = captor.getValue().getRules();
        assertThat(rules).extracting(PerformanceTarget.Rule::id).doesNotContainNull().doesNotHaveDuplicates().contains("kept-id");
        assertThat(rules)
            .extracting(PerformanceTarget.Rule::metric)
            .containsExactly("HTTP_ERROR_RATE", "HTTP_GATEWAY_LATENCY", "HTTP_ERROR_RATE");
        assertThat(rules).extracting(PerformanceTarget.Rule::threshold).containsExactly(1.0, 25.0, 5.0);
    }

    @Test
    void should_leave_a_target_whose_rules_all_have_ids_alone() throws Exception {
        var target = PerformanceTarget.builder().id("target-id").rules(List.of(rule("a", "HTTP_ERROR_RATE", 1.0))).build();
        when(performanceTargetRepository.findAll()).thenReturn(Set.of(target));

        assertThat(upgrader.upgrade()).isTrue();

        verify(performanceTargetRepository, never()).update(target);
    }

    @Test
    void should_report_failure_when_the_store_cannot_be_read() throws Exception {
        when(performanceTargetRepository.findAll()).thenThrow(new TechnicalException("down"));

        assertThat(upgrader.upgrade()).isFalse();
    }

    @Test
    void should_run_after_the_performance_target_store_exists() {
        assertThat(upgrader.getOrder()).isEqualTo(UpgraderOrder.PERFORMANCE_TARGET_RULE_ID_UPGRADER);
    }

    private static PerformanceTarget.Rule rule(String id, String metric, double threshold) {
        return new PerformanceTarget.Rule(id, metric, "P95", "LTE", threshold, List.of(), List.of());
    }
}
