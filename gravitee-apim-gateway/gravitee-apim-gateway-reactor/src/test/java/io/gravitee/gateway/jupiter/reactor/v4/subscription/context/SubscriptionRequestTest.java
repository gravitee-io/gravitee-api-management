/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.reactor.v4.subscription.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.service.Subscription;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class SubscriptionRequestTest {

    @Test
    void should_create_subscription_request() {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub-id1");

        SubscriptionRequest request = new SubscriptionRequest(subscription, new UUID());

        assertThat(request.id()).isNotNull();
        assertThat(request.transactionId()).isNull();
        assertThat(request.contextPath()).isEqualTo("");
        assertThat(request.uri()).isEqualTo("");
        assertThat(request.path()).isEqualTo("");
        assertThat(request.pathInfo()).isEqualTo("");
        assertThat(request.scheme()).isEqualTo("");
        assertThat(request.method()).isEqualTo(HttpMethod.OTHER);
        assertThat(request.version()).isNull();
        assertThat(request.host()).isEqualTo("");
        assertThat(request.remoteAddress()).isEqualTo("");
        assertThat(request.localAddress()).isEqualTo("");
        assertThat(request.sslSession()).isNull();
        assertThat(request.ended()).isTrue();
        assertThat(request.body()).isEqualTo(Maybe.empty());
        assertThat(request.messages()).isEqualTo(Flowable.empty());
    }
}
