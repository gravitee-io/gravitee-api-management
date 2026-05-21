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
package io.gravitee.gateway.services.sync.process.common.synchronizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void every_order_index_is_unique() {
        Set<Integer> seen = new HashSet<>();
        for (Order order : Order.values()) {
            assertThat(seen.add(order.index())).as("duplicate Order index for %s (%d)", order.name(), order.index()).isTrue();
        }
    }

    @Test
    void authz_orders_run_before_api_and_dependents() {
        assertThat(Order.AUTHZ_ENTITY.index()).isLessThan(Order.API.index());
        assertThat(Order.AUTHZ_POLICY.index()).isLessThan(Order.API.index());
        assertThat(Order.AUTHZ_ENTITY.index()).isLessThan(Order.AUTHZ_POLICY.index());
        assertThat(Order.AUTHZ_POLICY.index()).isLessThan(Order.SUBSCRIPTION.index());
        assertThat(Order.AUTHZ_POLICY.index()).isLessThan(Order.API_KEY.index());
    }

    @Test
    void authz_indices_match_plan_specification() {
        assertThat(Order.AUTHZ_ENTITY.index()).isEqualTo(8);
        assertThat(Order.AUTHZ_POLICY.index()).isEqualTo(9);
    }

    @Test
    void enum_contains_all_expected_orders_with_no_silent_removal() {
        Set<String> names = Arrays.stream(Order.values()).map(Order::name).collect(Collectors.toUnmodifiableSet());
        assertThat(names).contains(
            "NODE_METADATA",
            "LICENSE",
            "ACCESS_POINT",
            "ORGANIZATION",
            "DICTIONARY",
            "CLUSTER",
            "SHARED_POLICY_GROUP",
            "API_PRODUCT",
            "API",
            "SUBSCRIPTION",
            "API_KEY",
            "DEBUG",
            "AUTHZ_ENTITY",
            "AUTHZ_POLICY"
        );
    }
}
