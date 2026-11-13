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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductDefinitionSecretRefsFinderTest {

    private final DefinitionSecretRefsFinder<ReactableApiProduct> underTest = new ApiProductDefinitionSecretRefsFinder();

    @Test
    void should_only_handle_api_product_definitions() {
        assertThat(underTest.canHandle(null)).isFalse();
        assertThat(underTest.canHandle(new Api())).isFalse();
        assertThat(underTest.canHandle(ReactableApiProduct.builder().id("product-1").build())).isTrue();
    }

    @Test
    void should_describe_definition_with_api_product_kind() {
        ReactableApiProduct product = ReactableApiProduct.builder().id("product-123").build();
        assertThat(underTest.toDefinitionDescriptor(product, new DefinitionMetadata(null))).isEqualTo(
            new DefinitionDescriptor(new Definition("api-product", "product-123"), Optional.empty())
        );
        assertThat(underTest.toDefinitionDescriptor(product, new DefinitionMetadata("v2"))).isEqualTo(
            new DefinitionDescriptor(new Definition("api-product", "product-123"), Optional.of("v2"))
        );
    }

    @Test
    void should_find_secrets_in_product_resources() {
        ReactableApiProduct product = ReactableApiProduct.builder()
            .id("product-1")
            .resources(List.of(Resource.builder().name("cache").type("cache-redis").configuration("resource-config").build()))
            .build();

        Set<String> configs = new HashSet<>();
        underTest.findSecretRefs(product, (config, location, setter) -> {
            configs.add(config);
            setter.accept(config + " - updated!");
        });

        assertThat(configs).containsExactly("resource-config");
        assertThat(product.getResources().get(0).getConfiguration()).isEqualTo("resource-config - updated!");
    }

    @Test
    void should_find_secrets_in_plan_flow_steps_and_plan_security() {
        Step requestStep = Step.builder().policy("policy").configuration("request-step").build();
        Step responseStep = Step.builder().policy("policy").configuration("response-step").build();
        Flow planFlow = Flow.builder().request(List.of(requestStep)).response(List.of(responseStep)).build();
        Plan plan = Plan.builder()
            .id("plan-1")
            .flows(List.of(planFlow))
            .security(PlanSecurity.builder().type("JWT").configuration("plan-security").build())
            .build();
        Flow productFlow = Flow.builder().publish(List.of(Step.builder().policy("policy").configuration("publish-step").build())).build();

        ReactableApiProduct product = ReactableApiProduct.builder()
            .id("product-1")
            .plans(List.of(plan))
            .flows(List.of(productFlow))
            .build();

        Set<String> configs = new HashSet<>();
        underTest.findSecretRefs(product, (config, location, setter) -> configs.add(config));

        assertThat(configs).containsExactlyInAnyOrder("request-step", "response-step", "publish-step", "plan-security");
    }

    @Test
    void should_not_fail_with_null_resources() {
        assertThatCode(() ->
            underTest.findSecretRefs(ReactableApiProduct.builder().resources(null).build(), (config, location, setter) ->
                setter.accept(config)
            )
        ).doesNotThrowAnyException();
    }
}
