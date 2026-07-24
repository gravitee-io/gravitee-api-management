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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api_product.model.ApiProductKind;
import io.gravitee.apim.core.api_product.model.PortalApiProductDetails;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiProductMapperTest {

    private static final UUID API_PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID NAVIGATION_ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");

    @Test
    void should_map_consumer_safe_api_product_details() {
        var source = new PortalApiProductDetails(
            API_PRODUCT_ID.toString(),
            "AI Workspace",
            "Description",
            "1.0.0",
            ApiProductKind.AI_WORKSPACE,
            NAVIGATION_ITEM_ID.toString(),
            List.of("ai"),
            List.of(new PortalApiProductDetails.ApiSummary("api-id", "API", "2.0.0"))
        );

        var result = ApiProductMapper.INSTANCE.map(source);

        assertThat(result.getId()).isEqualTo(API_PRODUCT_ID);
        assertThat(result.getName()).isEqualTo("AI Workspace");
        assertThat(result.getDescription()).isEqualTo("Description");
        assertThat(result.getVersion()).isEqualTo("1.0.0");
        assertThat(result.getKind()).isEqualTo(io.gravitee.rest.api.portal.rest.model.PortalApiProductDetails.KindEnum.AI_WORKSPACE);
        assertThat(result.getNavigationItemId()).isEqualTo(NAVIGATION_ITEM_ID);
        assertThat(result.getTags()).containsExactly("ai");
        assertThat(result.getApis())
            .singleElement()
            .satisfies(api -> {
                assertThat(api.getId()).isEqualTo("api-id");
                assertThat(api.getName()).isEqualTo("API");
                assertThat(api.getVersion()).isEqualTo("2.0.0");
            });
    }
}
