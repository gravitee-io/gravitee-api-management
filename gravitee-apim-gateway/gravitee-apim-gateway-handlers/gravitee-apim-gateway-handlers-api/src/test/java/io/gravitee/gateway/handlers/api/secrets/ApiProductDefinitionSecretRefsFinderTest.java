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
package io.gravitee.gateway.handlers.api.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductDefinitionSecretRefsFinderTest {

    private final DefinitionSecretRefsFinder<ReactableApiProduct> underTest = new ApiProductDefinitionSecretRefsFinder();

    @Test
    void should_only_handle_api_products() {
        assertThat(underTest.canHandle(null)).isFalse();
        assertThat(underTest.canHandle(new Api())).isFalse();
        assertThat(underTest.canHandle(new ReactableApiProduct())).isTrue();
    }

    @Test
    void should_describe_the_product_with_the_api_product_kind() {
        ReactableApiProduct product = ReactableApiProduct.builder().id("product-1").build();

        assertThat(underTest.toDefinitionDescriptor(product, new DefinitionMetadata("3"))).isEqualTo(
            new DefinitionDescriptor(new Definition("api-product", "product-1"), Optional.of("3"))
        );
    }

    @Test
    void should_find_secrets_in_plan_security_plan_flows_product_flows_and_resources() {
        ReactableApiProduct product = ReactableApiProduct.builder()
            .id("product-1")
            .plans(
                List.of(
                    Plan.builder()
                        .id("plan-1")
                        .security(PlanSecurity.builder().type("jwt").configuration("security-config").build())
                        .flows(
                            List.of(
                                flow(
                                    step("plan-request-config", "plan-request-policy"),
                                    step("plan-response-config", "plan-response-policy")
                                )
                            )
                        )
                        .build()
                )
            )
            .flows(
                List.of(
                    flow(
                        step("product-request-config", "product-request-policy"),
                        step("product-response-config", "product-response-policy")
                    )
                )
            )
            .resources(List.of(Resource.builder().name("cache").type("cache-redis").configuration("resource-config").build()))
            .build();

        Map<String, String> locationByConfig = new HashMap<>();
        underTest.findSecretRefs(product, (config, location, setter) -> {
            locationByConfig.put(config, location.id());
            setter.accept(config + " - updated");
        });

        assertThat(locationByConfig).containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "security-config",
                "jwt",
                "plan-request-config",
                "plan-request-policy",
                "plan-response-config",
                "plan-response-policy",
                "product-request-config",
                "product-request-policy",
                "product-response-config",
                "product-response-policy",
                "resource-config",
                "cache-redis"
            )
        );
        assertThat(product.getPlans().getFirst().getSecurity().getConfiguration()).isEqualTo("security-config - updated");
        assertThat(product.getPlans().getFirst().getFlows().getFirst().getRequest().getFirst().getConfiguration()).isEqualTo(
            "plan-request-config - updated"
        );
        assertThat(product.getFlows().getFirst().getResponse().getFirst().getConfiguration()).isEqualTo(
            "product-response-config - updated"
        );
        assertThat(product.getResources().getFirst().getConfiguration()).isEqualTo("resource-config - updated");
    }

    @Test
    void should_not_fail_on_a_product_with_nothing_configured() {
        assertThatCode(() ->
            underTest.findSecretRefs(
                ReactableApiProduct.builder().id("product-1").plans(List.of(Plan.builder().id("plan-1").build())).build(),
                (config, location, setter) -> setter.accept(config)
            )
        ).doesNotThrowAnyException();
    }

    private static Flow flow(Step request, Step response) {
        Flow flow = new Flow();
        flow.setRequest(List.of(request));
        flow.setResponse(List.of(response));
        return flow;
    }

    private static Step step(String configuration, String policy) {
        return Step.builder().policy(policy).configuration(configuration).build();
    }
}
