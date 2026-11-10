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
package io.gravitee.gateway.handlers.api.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.env.GatewayConfiguration;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductShardingFilterTest {

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Test
    void should_match_product_tags_via_gateway_configuration() {
        when(gatewayConfiguration.hasMatchingTags(Set.of("internal"))).thenReturn(true);

        assertThat(ApiProductShardingFilter.matchesProductTags(gatewayConfiguration, Set.of("internal"))).isTrue();
    }

    @Test
    void should_accept_plan_with_no_tags_on_any_matching_gateway() {
        assertThat(ApiProductShardingFilter.matchesPlanTags(gatewayConfiguration, null)).isTrue();
        assertThat(ApiProductShardingFilter.matchesPlanTags(gatewayConfiguration, Set.of())).isTrue();
    }

    @Test
    void should_delegate_plan_tag_matching_to_gateway_configuration() {
        when(gatewayConfiguration.hasMatchingTags(Set.of("external"))).thenReturn(false);

        assertThat(ApiProductShardingFilter.matchesPlanTags(gatewayConfiguration, Set.of("external"))).isFalse();
    }

    @Test
    void should_match_plan_when_gateway_has_matching_tags() {
        when(gatewayConfiguration.hasMatchingTags(Set.of("internal"))).thenReturn(true);

        assertThat(ApiProductShardingFilter.matchesPlanTags(gatewayConfiguration, Set.of("internal"))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PlanStatus.class, names = { "PUBLISHED", "DEPRECATED" })
    void should_serve_a_plan_open_to_calls_whose_tags_match_or_are_absent(PlanStatus status) {
        when(gatewayConfiguration.hasMatchingTags(Set.of("internal"))).thenReturn(true);

        assertThat(ApiProductShardingFilter.servesPlan(gatewayConfiguration, plan(status, Set.of("internal")))).isTrue();
        assertThat(ApiProductShardingFilter.servesPlan(gatewayConfiguration, plan(status, null))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PlanStatus.class, names = { "PUBLISHED", "DEPRECATED" })
    void should_not_serve_a_plan_open_to_calls_whose_tags_do_not_match(PlanStatus status) {
        when(gatewayConfiguration.hasMatchingTags(Set.of("external"))).thenReturn(false);

        assertThat(ApiProductShardingFilter.servesPlan(gatewayConfiguration, plan(status, Set.of("external")))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = PlanStatus.class, names = { "STAGING", "CLOSED" })
    void should_not_serve_a_plan_closed_to_calls_whatever_its_tags(PlanStatus status) {
        lenient().when(gatewayConfiguration.hasMatchingTags(Set.of("internal"))).thenReturn(true);

        assertThat(ApiProductShardingFilter.servesPlan(gatewayConfiguration, plan(status, Set.of("internal")))).isFalse();
        assertThat(ApiProductShardingFilter.servesPlan(gatewayConfiguration, plan(status, null))).isFalse();
    }

    private static Plan plan(PlanStatus status, Set<String> tags) {
        return Plan.builder().id("plan-1").status(status).tags(tags).build();
    }
}
