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
import static org.mockito.Mockito.when;

import io.gravitee.gateway.env.GatewayConfiguration;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
