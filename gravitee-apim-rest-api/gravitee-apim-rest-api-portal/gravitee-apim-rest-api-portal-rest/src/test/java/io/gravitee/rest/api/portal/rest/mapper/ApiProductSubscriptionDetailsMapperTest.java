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

import io.gravitee.apim.core.subscription.model.PortalApiProductSubscriptionDetails;
import io.gravitee.rest.api.portal.rest.model.ApiProductSubscriptionApiAvailability;
import io.gravitee.rest.api.portal.rest.model.ApiProductSubscriptionAvailability;
import io.gravitee.rest.api.portal.rest.model.ApiProductSubscriptionPlan;
import io.gravitee.rest.api.portal.rest.model.ApiType;
import io.gravitee.rest.api.portal.rest.model.PlanMode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductSubscriptionDetailsMapperTest {

    @Test
    void should_map_api_product_subscription_details() {
        var source = new PortalApiProductSubscriptionDetails(
            "00000000-0000-0000-0000-000000000101",
            "API Product",
            "1.0",
            PortalApiProductSubscriptionDetails.Availability.AVAILABLE,
            new PortalApiProductSubscriptionDetails.PlanSummary("plan-id", "Plan", "api-key", "STANDARD"),
            List.of(
                new PortalApiProductSubscriptionDetails.ApiSummary(
                    "api-id",
                    "API",
                    "2.0",
                    "PROXY",
                    PortalApiProductSubscriptionDetails.ApiAvailability.AVAILABLE,
                    List.of("https://gateway.example.com/api"),
                    new PortalApiProductSubscriptionDetails.DocumentationTarget(
                        "00000000-0000-0000-0000-000000000102",
                        "00000000-0000-0000-0000-000000000103"
                    )
                )
            )
        );

        var result = ApiProductSubscriptionDetailsMapper.INSTANCE.map(source);

        assertThat(result.getId()).isEqualTo(UUID.fromString(source.id()));
        assertThat(result.getAvailability()).isEqualTo(ApiProductSubscriptionAvailability.AVAILABLE);
        assertThat(result.getPlan().getSecurity()).isEqualTo(ApiProductSubscriptionPlan.SecurityEnum.API_KEY);
        assertThat(result.getPlan().getMode()).isEqualTo(PlanMode.STANDARD);
        assertThat(result.getApis())
            .singleElement()
            .satisfies(api -> {
                assertThat(api.getType()).isEqualTo(ApiType.PROXY);
                assertThat(api.getAvailability()).isEqualTo(ApiProductSubscriptionApiAvailability.AVAILABLE);
                assertThat(api.getEntrypoints()).containsExactly("https://gateway.example.com/api");
                assertThat(api.getDocumentation().getRootId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000102"));
                assertThat(api.getDocumentation().getNavigationItemId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000103"));
            });
    }

    @Test
    void should_keep_optional_values_absent_for_unavailable_data() {
        var source = new PortalApiProductSubscriptionDetails(
            "00000000-0000-0000-0000-000000000101",
            null,
            null,
            PortalApiProductSubscriptionDetails.Availability.UNAVAILABLE,
            new PortalApiProductSubscriptionDetails.PlanSummary("plan-id", null, null, null),
            List.of(
                new PortalApiProductSubscriptionDetails.ApiSummary(
                    "api-id",
                    null,
                    null,
                    null,
                    PortalApiProductSubscriptionDetails.ApiAvailability.UNAVAILABLE,
                    List.of(),
                    null
                )
            )
        );

        var result = ApiProductSubscriptionDetailsMapper.INSTANCE.map(source);

        assertThat(result.getName()).isNull();
        assertThat(result.getAvailability()).isEqualTo(ApiProductSubscriptionAvailability.UNAVAILABLE);
        assertThat(result.getPlan().getSecurity()).isNull();
        assertThat(result.getApis())
            .singleElement()
            .satisfies(api -> {
                assertThat(api.getAvailability()).isEqualTo(ApiProductSubscriptionApiAvailability.UNAVAILABLE);
                assertThat(api.getEntrypoints()).isEmpty();
                assertThat(api.getDocumentation()).isNull();
            });
    }
}
