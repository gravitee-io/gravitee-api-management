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
package io.gravitee.apim.core.membership.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiPortalMembershipDomainServiceProductSubscriptionTest {

    private static final String USER_ID = "user-id";
    private static final String APPLICATION_ID = "app-id";

    private SubscriptionQueryService subscriptionQueryService;
    private ApiPortalMembershipDomainService service;

    @BeforeEach
    void setUp() {
        var membershipQueryService = mock(MembershipQueryService.class);
        subscriptionQueryService = mock(SubscriptionQueryService.class);
        service = new ApiPortalMembershipDomainService(membershipQueryService, subscriptionQueryService, mock(ApiQueryService.class));

        when(
            membershipQueryService.findByMemberIdAndMemberTypeAndReferenceType(
                eq(USER_ID),
                eq(Membership.Type.USER),
                eq(Membership.ReferenceType.APPLICATION)
            )
        ).thenReturn(Set.of(Membership.builder().referenceId(APPLICATION_ID).build()));
    }

    @Test
    void should_ignore_api_product_subscriptions_without_api_id() {
        when(subscriptionQueryService.findActiveByApplicationIdsAndApiIds(any(), any())).thenReturn(
            List.of(SubscriptionEntity.builder().id("product-sub").applicationId(APPLICATION_ID).apiId(null).build())
        );

        assertThat(service.filterAllowedApiIdsBySubscription(USER_ID, Set.of("private-api"))).isEmpty();
    }

    @Test
    void should_keep_api_subscribed_alongside_an_api_product_subscription() {
        when(subscriptionQueryService.findActiveByApplicationIdsAndApiIds(any(), any())).thenReturn(
            List.of(
                SubscriptionEntity.builder().id("product-sub").applicationId(APPLICATION_ID).apiId(null).build(),
                SubscriptionEntity.builder().id("api-sub").applicationId(APPLICATION_ID).apiId("private-api").build()
            )
        );

        assertThat(service.filterAllowedApiIdsBySubscription(USER_ID, Set.of("private-api"))).containsExactly("private-api");
    }
}
