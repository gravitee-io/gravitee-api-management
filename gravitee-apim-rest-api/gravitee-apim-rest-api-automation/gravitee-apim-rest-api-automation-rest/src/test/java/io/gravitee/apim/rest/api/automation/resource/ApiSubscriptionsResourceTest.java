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
package io.gravitee.apim.rest.api.automation.resource;

import static io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.ACCEPTED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDStatus;
import io.gravitee.apim.core.subscription.use_case.ImportSubscriptionSpecUseCase;
import io.gravitee.apim.rest.api.automation.model.SubscriptionState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.ZoneOffset;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiSubscriptionsResourceTest extends AbstractResourceTest {

    @Autowired
    private ImportSubscriptionSpecUseCase importSubscriptionSpecUseCase;

    @AfterEach
    void tearDown() {
        reset(importSubscriptionSpecUseCase);
    }

    static final String API_HRID = "api-hrid";

    @Nested
    class Run {

        @Test
        void should_return_state_from_hrid() {
            when(importSubscriptionSpecUseCase.execute(any(ImportSubscriptionSpecUseCase.Input.class)))
                .thenReturn(
                    new ImportSubscriptionSpecUseCase.Output(
                        SubscriptionCRDStatus
                            .builder()
                            .startingAt(Instant.now().atZone(ZoneOffset.UTC))
                            .status(ACCEPTED.name())
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .build()
                    )
                );

            var state = expectEntity("subscription-with-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getHrid()).isEqualTo("subscription_hrid");
                soft.assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                soft.assertThat(state.getApplicationHrid()).isEqualTo("application_hrid");
                soft.assertThat(state.getPlanHrid()).isEqualTo("plan_hrid");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_return_state_from_legacy_id() {
            when(importSubscriptionSpecUseCase.execute(any(ImportSubscriptionSpecUseCase.Input.class)))
                .thenReturn(
                    new ImportSubscriptionSpecUseCase.Output(
                        SubscriptionCRDStatus
                            .builder()
                            .id("subscription_id")
                            .startingAt(Instant.now().atZone(ZoneOffset.UTC))
                            .status(ACCEPTED.name())
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .build()
                    )
                );

            var state = expectEntity("subscription-with-id.json", true);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isEqualTo("subscription_id");
                soft.assertThat(state.getApiHrid()).isEqualTo(API_HRID);
                soft.assertThat(state.getApplicationHrid()).isEqualTo("application_id");
                soft.assertThat(state.getPlanHrid()).isEqualTo("plan_id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/apis/" + API_HRID + "/subscriptions";
    }

    private SubscriptionState expectEntity(String spec) {
        return expectEntity(spec, false);
    }

    private SubscriptionState expectEntity(String spec, boolean legacy) {
        try (
            var response = rootTarget()
                .queryParam("legacy", legacy)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(SubscriptionState.class);
        }
    }
}
