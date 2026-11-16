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
package io.gravitee.rest.api.management.v2.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.definition.model.v4.resource.Resource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ApiProductMapperTest {

    private final ApiProductMapper apiProductMapper = ApiProductMapper.INSTANCE;

    @Test
    void map_should_include_sharding_tags_in_rest_response() {
        var coreProduct = ApiProduct.builder().id("product-1").name("aa").tags(Set.of("external", "internal")).build();

        var restProduct = apiProductMapper.map(coreProduct);

        assertThat(restProduct.getTags()).containsExactlyInAnyOrder("external", "internal");
    }

    @Test
    void map_should_leave_resources_off_list_and_search_results() {
        var cache = Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").enabled(true).build();
        var coreProduct = ApiProduct.builder().id("product-1").name("aa").resources(List.of(cache)).build();

        var restProduct = apiProductMapper.map(coreProduct);

        assertThat(restProduct.getResources()).isNull();
    }

    @Test
    void mapWithResources_should_return_resources_for_definition_reads() {
        var cache = Resource.builder().name("cache").type("cache").configuration("{\"ttl\":10}").enabled(true).build();
        var coreProduct = ApiProduct.builder().id("product-1").name("aa").resources(List.of(cache)).build();

        var restProduct = apiProductMapper.mapWithResources(coreProduct);

        assertThat(restProduct.getResources())
            .singleElement()
            .satisfies(resource -> {
                assertThat(resource.getName()).isEqualTo("cache");
                assertThat(resource.getType()).isEqualTo("cache");
                assertThat(resource.getEnabled()).isTrue();
                assertThat(resource.getConfiguration()).isNotNull();
            });
    }

    @Test
    void map_should_map_update_payload_resources_to_definition_model() {
        var updatePayload = new io.gravitee.rest.api.management.v2.rest.model.UpdateApiProduct();
        updatePayload.setName("Updated");
        updatePayload.setVersion("1.0.0");
        updatePayload.setResources(
            List.of(
                new io.gravitee.rest.api.management.v2.rest.model.Resource()
                    .name("cache")
                    .type("cache")
                    .enabled(true)
                    .configuration(new java.util.LinkedHashMap<>(java.util.Map.of("ttl", 10)))
            )
        );

        var coreUpdate = apiProductMapper.map(updatePayload);

        assertThat(coreUpdate.getName()).isEqualTo("Updated");
        assertThat(coreUpdate.getResources())
            .singleElement()
            .satisfies(resource -> {
                assertThat(resource.getName()).isEqualTo("cache");
                assertThat(resource.getType()).isEqualTo("cache");
                assertThat(resource.isEnabled()).isTrue();
                assertThat(resource.getConfiguration()).contains("ttl");
            });
    }

    @Test
    void map_should_leave_omitted_update_lists_null_so_partial_puts_do_not_wipe_them() {
        var updatePayload = new io.gravitee.rest.api.management.v2.rest.model.UpdateApiProduct();
        updatePayload.setName("Updated");
        updatePayload.setVersion("1.0.0");

        var coreUpdate = apiProductMapper.map(updatePayload);

        assertThat(coreUpdate.getTags()).isNull();
        assertThat(coreUpdate.getGroups()).isNull();
        assertThat(coreUpdate.getApiIds()).isNull();
        assertThat(coreUpdate.getResources()).isNull();
        assertThat(coreUpdate.getDisableMembershipNotifications()).isNull();
    }

    @Test
    void map_should_map_explicit_empty_tags_so_callers_can_clear_them() {
        var updatePayload = new io.gravitee.rest.api.management.v2.rest.model.UpdateApiProduct();
        updatePayload.setName("Updated");
        updatePayload.setVersion("1.0.0");
        updatePayload.setTags(List.of());

        var coreUpdate = apiProductMapper.map(updatePayload);

        assertThat(coreUpdate.getTags()).isEmpty();
    }

    @Test
    void mapWithResources_should_return_groups_deployment_state_and_membership_flag() {
        var coreProduct = ApiProduct.builder()
            .id("product-1")
            .name("aa")
            .groups(Set.of("group-1"))
            .deploymentState(ApiProduct.DeploymentState.NEED_REDEPLOY)
            .disableMembershipNotifications(true)
            .build();

        var restProduct = apiProductMapper.mapWithResources(coreProduct);

        assertThat(restProduct.getGroups()).containsExactly("group-1");
        assertThat(restProduct.getDeploymentState()).isEqualTo(
            io.gravitee.rest.api.management.v2.rest.model.ApiProductDeploymentState.NEED_REDEPLOY
        );
        assertThat(restProduct.getDisableMembershipNotifications()).isTrue();
    }
}
