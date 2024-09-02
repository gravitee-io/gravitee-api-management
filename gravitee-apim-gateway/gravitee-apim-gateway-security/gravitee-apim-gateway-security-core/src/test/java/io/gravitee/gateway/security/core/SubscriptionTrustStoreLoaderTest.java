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
package io.gravitee.gateway.security.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionTrustStoreLoaderTest {

    @Test
    void should_build_id_based_on_subscription_id() {
        final SubscriptionTrustStoreLoader loader = new SubscriptionTrustStoreLoader(Subscription.builder().id("subscriptionId").build());
        assertThat(loader.id()).isEqualTo("subscription_cert_subscriptionId");
    }

    @Test
    void should_start_a_loader() {
        final SubscriptionTrustStoreLoader loader = new SubscriptionTrustStoreLoader(
            Subscription.builder().clientCertificate(BASE_64_CERTIFICATE).id("subscriptionId").build()
        );
        final List<KeyStoreEvent> keyStoreEvents = new ArrayList<>();
        loader.setEventHandler(keyStoreEvents::add);

        loader.start();
        assertThat(keyStoreEvents)
            .hasSize(1)
            .first()
            .satisfies(event -> {
                assertThat(event.loaderId()).isEqualTo(loader.id());
                assertThat(event instanceof KeyStoreEvent.LoadEvent).isTrue();
            });
    }

    @Test
    void should_not_start_a_loader_already_started() {
        final SubscriptionTrustStoreLoader loader = new SubscriptionTrustStoreLoader(
            Subscription.builder().clientCertificate(BASE_64_CERTIFICATE).id("subscriptionId").build()
        );
        final List<KeyStoreEvent> keyStoreEvents = new ArrayList<>();
        loader.setEventHandler(keyStoreEvents::add);

        loader.start();
        assertThat(keyStoreEvents)
            .hasSize(1)
            .first()
            .satisfies(event -> {
                assertThat(event.loaderId()).isEqualTo(loader.id());
                assertThat(event instanceof KeyStoreEvent.LoadEvent).isTrue();
            });

        // Try to start again
        keyStoreEvents.clear();
        loader.start();
        assertThat(keyStoreEvents).isEmpty();
    }

    private static final String BASE_64_CERTIFICATE =
        "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUYzekNDQThlZ0F3SUJBZ0lCWlRBTkJna3Foa2lHOXcwQkFRc0ZBRENCa0RFcE1DY0dDU3FHU0liM0RRRUoKQVJZYVkyOXVkR0ZqZEVCbmNtRjJhWFJsWlhOdmRYSmpaUzVqYjIweEVEQU9CZ05WQkFNTUIwRlFTVTFmUTA0eApEVEFMQmdOVkJBc01CRUZRU1UweEZEQVNCZ05WQkFvTUMwRlFTVTFmVkdWemRHVnlNUTR3REFZRFZRUUhEQVZNCmFXeHNaVEVQTUEwR0ExVUVDQXdHUm5KaGJtTmxNUXN3Q1FZRFZRUUdFd0pHVWpBZUZ3MHlOREE0TWpnd05qVTAKTkRWYUZ3MHlOVEE0TWpnd05qVTBORFZhTUlHUU1Ta3dKd1lKS29aSWh2Y05BUWtCRmhwamIyNTBZV04wUUdkeQpZWFpwZEdWbGMyOTFjbU5sTG1OdmJURVFNQTRHQTFVRUF3d0hRVkJKVFY5RFRqRU5NQXNHQTFVRUN3d0VRVkJKClRURVVNQklHQTFVRUNnd0xRVkJKVFY5VVpYTjBaWEl4RGpBTUJnTlZCQWNNQlV4cGJHeGxNUTh3RFFZRFZRUUkKREFaR2NtRnVZMlV4Q3pBSkJnTlZCQVlUQWtaU01JSUNJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBZzhBTUlJQwpDZ0tDQWdFQStncWxkMnkvRlhiUHRDY1pnREl6cGlsQVpJc3FIcEJwREZBeCthYkRNUTVlV1ozUTNBYXhSeGh5ClFCeE1maFhsMkZvZEJmNzZtQVQ4UVpvVHUwdHdTSnIrTGx2eVQ3NTE1RnZiYUxYNGg2bEZSbWs2dXExeE5DdHgKU0FzMC93dkttNVc1QmtHeHJFQ3JYcUV1dWtPSkw2dW5VK3RqRXFBdUtuRFhrQ2QxVXA0WlVWalJJV0RnR0lYVwozQXViazVLeDBPdUZTWlhNWmkvOGFDcTBJc3FyZnk5amVLWjllZjRkMkJ1aENjcVFQQXo2dDFpTTVWVGtvUGNmCnVOQzJYQlVCOEI4YUNpd1FRWm0yTVVsWFo0aGE1V2xnYzZzb3VGVVFyM2kzN2hlaVhiQVJUb2xnVmhPMU9oQ3AKN1VtMDBEbEdtQmFsaXNkODhCdjU4MFplbFNGNldSamkvb2RpVUwrcnZyOElOY1Z5MDVQb04rb1ljckQ1TWZtdQpUZUdUUFdFNmpIMXVXb1llZDAyV0t6LzV4VC9Xc3psMEZ2QVc5MExLUklhWnZveWpvdVdnWGExSFFFM1hrdnlIClZXeXc4eEZHTEIxN09aRGs4LzRMN2NqeDZMVTErZnpvak56WlBhcnJ6UTRSaFNFWE1KYUdZQitZOEVuYUhDWUoKbHVBeVVWMkNVOVdpc0tvaTZaWW9IR3lvUnJVa3JXT3U0MFlkNkJzUmhlQWI3anNqalV6Rjc0UWQ3UVY1enlNagpZZHF4M1dtTzVKdzFyZXNoei9LY1RGYS9SM2VhK1BPTTZ3bEtHWTVNdFlpWThJbWlodms1VVBPamJyR2RpU3BvCmZoSng5RHNWYmY3eTI0OFFROGZZd052TFlUMVVLVmRQaXR0eEhKTml4SEhYS0RIWEdhc0NBd0VBQWFOQ01FQXcKSFFZRFZSME9CQllFRkF3SjNWWlBzYXlsSUMrUkxUeXhqaWd6TWsxa01COEdBMVVkSXdRWU1CYUFGRGNnaUVNMwo3WVdLRk9Wc09NTWc5MWVNZ1RNWU1BMEdDU3FHU0liM0RRRUJDd1VBQTRJQ0FRQkNLczRSUklPZmhhTWt5QmdBCmlmUmZKdU04NFVXSFJqQ0ZzSTVFRElqY3BJaVE1LzVGcm9CNjRPUDBnNUhFNDc0UEtqTVdSQzVMZDhTUDVWY20KK1l6VllUVDJJOGEycDVvbG1iWnhPeElZU0Q0NWF6NDhXRUtLRXNxR091TUw2M29CcUVYeWhCN2hCQjZTaUphcApuOXFLWTZNWXB0RFNER3h1dGxrZC94YnU1VGxXSzBMTEhNZTBCKzdOR2U1UDdaN1AvblpraURiVlg3VG05MVBICi9PUDgwUGluVTJJZHJmUWVCUDZKdVpYem5XU3dVd3NKWlJrNDNTNVBBYkkrQW81M0lCbVR4aGhvbkNSWFF2WGsKbW1kcmsrK2doODVEWTFUazdVQ2xGbEsyNDZvTFNjeEF6aHoxWWxuZktxRm1CK2pLZVVRdXNsNytZc01VMVRBSgoxakxGNUtNU2xBaHRXdG8rMTN0cHBWbFBaYlFYaGZTV0I3bEZBWFpDTkMycjlzNWs0S1RnQU1OQ3E1VEpUUFNyCngvRnQ3UTJSV3F5RnZ3WHErR3VxOEpLYjQyUXpJTkFTVmhsQkJzYVgvNmYyU1h6OWZja2F0cW16ZVh0MHpwMUoKWWFRUmxSUTI2ZWgyUWdyRjN2ejVOaVFNV3pmRzlleWdZY2xEUE1rMlhqWDZVSkhWY2ZUMjcrMElhY251QzhlYQp5Rjl0bCtjRnMyRmpJVENVaFlTWTJRdjNjVDFRUnA0Q3JNUTVYMHdlcFFGVnp1QmZyYW1xMVZKdkRDemFuMlNmClZpMVl1MmdqOGJHVGYzamhzK3k5RW5yYWRxbzZSWHh1T2NzMTNHVWVYMFVHZG41aFREOE03aEtNUzN2V0hOUjkKY0toeGFxY3B5ZTh6S0o4K0lEc0pDMitsTVE9PQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0t";
}
