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
package io.gravitee.apim.rest.api.mcp.core;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.MCP_CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.PROD;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.STAGING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnvironmentResolverTest {

    private static final ApiRef ORDERS = new ApiRef("orders-id", "Orders");
    private static final ApiRef STOCK_PROD = new ApiRef("stock-prod", "Stock");
    private static final ApiRef STOCK_STAGING = new ApiRef("stock-staging", "Stock");

    private final Fakes.Directory environments = Fakes.environments(PROD, STAGING);
    private final ApiDirectory apis = Fakes.apisByEnvironment(
        Map.of(PROD.id(), List.of(ORDERS, STOCK_PROD), STAGING.id(), List.of(STOCK_STAGING))
    );

    @ParameterizedTest
    @ValueSource(strings = { "e-staging", "staging", "Staging", "STAGING" })
    void should_find_an_environment_by_id_hrid_or_name(String wanted) {
        var scope = EnvironmentResolver.resolve(environments, apis, MCP_CALLER, wanted, null);

        assertThat(scope.environment()).isEqualTo(STAGING);
        assertThat(scope.auditInfo().environmentId()).isEqualTo("e-staging");
        assertThat(scope.auditInfo().organizationId()).isEqualTo("DEFAULT");
        assertThat(scope.api()).isNull();
    }

    @Test
    void should_name_the_readable_environments_when_the_one_asked_for_is_unknown() {
        assertThatThrownBy(() -> EnvironmentResolver.resolve(environments, apis, MCP_CALLER, "dev", null))
            .isInstanceOf(TriageException.class)
            .hasMessage("No environment 'dev' among those you can read: Production (prod), Staging (staging).");
    }

    @Test
    void should_find_the_environment_of_an_api_that_exists_in_only_one() {
        var scope = EnvironmentResolver.resolve(environments, apis, MCP_CALLER, null, "Orders");

        assertThat(scope.environment()).isEqualTo(PROD);
        assertThat(scope.api()).isEqualTo(ORDERS);
    }

    @Test
    void should_ask_for_the_environment_when_the_api_exists_in_several() {
        assertThatThrownBy(() -> EnvironmentResolver.resolve(environments, apis, MCP_CALLER, null, "Stock"))
            .isInstanceOf(TriageException.class)
            .hasMessage("Several environments have an API 'Stock': Production (prod), Staging (staging). Pass environment.");
    }

    @Test
    void should_resolve_the_api_within_the_environment_asked_for() {
        var scope = EnvironmentResolver.resolve(environments, apis, MCP_CALLER, "staging", "stock");

        assertThat(scope.api()).isEqualTo(STOCK_STAGING);
    }

    @Test
    void should_take_the_only_readable_environment_when_nothing_is_asked_for() {
        var scope = EnvironmentResolver.resolve(Fakes.environments(PROD), apis, MCP_CALLER, null, null);

        assertThat(scope.environment()).isEqualTo(PROD);
    }

    @Test
    void should_ask_for_the_environment_when_several_are_readable_and_nothing_names_one() {
        assertThatThrownBy(() -> EnvironmentResolver.resolve(environments, apis, MCP_CALLER, null, null))
            .isInstanceOf(TriageException.class)
            .hasMessage("Pass environment: the id, hrid or name of one of Production (prod), Staging (staging).");
    }

    @Test
    void should_refuse_a_caller_who_can_read_no_environment() {
        var none = Fakes.environments();
        none.unreadable.add(PROD);

        assertThatThrownBy(() -> EnvironmentResolver.resolve(none, apis, MCP_CALLER, null, null))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    void should_span_every_readable_environment_or_the_one_named() {
        assertThat(EnvironmentResolver.environments(environments, MCP_CALLER, null)).containsExactly(PROD, STAGING);
        assertThat(EnvironmentResolver.environments(environments, MCP_CALLER, "prod")).containsExactly(PROD);
    }
}
