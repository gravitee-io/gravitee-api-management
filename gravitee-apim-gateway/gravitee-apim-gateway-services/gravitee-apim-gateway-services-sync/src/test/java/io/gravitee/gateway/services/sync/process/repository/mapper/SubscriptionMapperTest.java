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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.api.service.SubscriptionConfiguration;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionMapperTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();
    private SubscriptionMapper cut;
    private io.gravitee.repository.management.model.Subscription subscription;

    @BeforeEach
    public void beforeEach() throws JsonProcessingException {
        cut = new SubscriptionMapper(objectMapper, mock(ApiProductRegistry.class));

        subscription = new Subscription();
        subscription.setApi("api");
        subscription.setReferenceType(SubscriptionReferenceType.API);
        subscription.setReferenceId("api");
        subscription.setApplication("application");
        subscription.setClientId("clientId");
        subscription.setClientCertificate("clientCertificate");
        subscription.setStartingAt(new Date());
        subscription.setEndingAt(new Date());
        subscription.setId("id");
        subscription.setPlan("plan");
        subscription.setStatus(Subscription.Status.ACCEPTED);
        subscription.setConsumerStatus(Subscription.ConsumerStatus.STARTED);
        subscription.setType(Subscription.Type.PUSH);
        subscription.setConfiguration(objectMapper.writeValueAsString(new SubscriptionConfiguration()));
        subscription.setMetadata(Map.of());
    }

    @Test
    void should_map_subscription() throws JsonProcessingException {
        List<io.gravitee.gateway.api.service.Subscription> mapped = cut.to(subscription);
        assertThat(mapped).hasSize(1);
        io.gravitee.gateway.api.service.Subscription subscriptionMapped = mapped.getFirst();

        assertThat(subscriptionMapped.getId()).isEqualTo(subscription.getId());
        assertThat(subscriptionMapped.getApplication()).isEqualTo(subscription.getApplication());
        assertThat(subscriptionMapped.getClientId()).isEqualTo(subscription.getClientId());
        assertThat(subscriptionMapped.getClientCertificate()).isEqualTo(subscription.getClientCertificate());
        assertThat(subscriptionMapped.getStartingAt()).isEqualTo(subscription.getStartingAt());
        assertThat(subscriptionMapped.getEndingAt()).isEqualTo(subscription.getEndingAt());
        assertThat(subscriptionMapped.getId()).isEqualTo(subscription.getId());
        assertThat(subscriptionMapped.getPlan()).isEqualTo(subscription.getPlan());
        assertThat(subscriptionMapped.getStatus()).isEqualTo(subscription.getStatus().name());
        assertThat(subscriptionMapped.getConsumerStatus().name()).isEqualTo(subscription.getConsumerStatus().name());
        assertThat(subscriptionMapped.getType().name()).isEqualTo(subscription.getType().name());
        assertThat(subscriptionMapped.getConfiguration()).isEqualTo(
            objectMapper.readValue(subscription.getConfiguration(), SubscriptionConfiguration.class)
        );
        assertThat(subscriptionMapped.getMetadata()).containsEntry("referenceId", "api").containsEntry("referenceType", "API");
    }

    @Test
    void should_map_subscription_without_all_attributes() {
        subscription.setStatus(null);
        subscription.setConsumerStatus(null);
        subscription.setType(null);
        subscription.setConfiguration(null);
        List<io.gravitee.gateway.api.service.Subscription> mapped = cut.to(subscription);
        assertThat(mapped).hasSize(1);
        io.gravitee.gateway.api.service.Subscription subscriptionMapped = mapped.getFirst();

        assertThat(subscriptionMapped.getId()).isEqualTo(subscription.getId());
        assertThat(subscriptionMapped.getApplication()).isEqualTo(subscription.getApplication());
        assertThat(subscriptionMapped.getClientId()).isEqualTo(subscription.getClientId());
        assertThat(subscriptionMapped.getStartingAt()).isEqualTo(subscription.getStartingAt());
        assertThat(subscriptionMapped.getEndingAt()).isEqualTo(subscription.getEndingAt());
        assertThat(subscriptionMapped.getId()).isEqualTo(subscription.getId());
        assertThat(subscriptionMapped.getPlan()).isEqualTo(subscription.getPlan());
        assertThat(subscriptionMapped.getStatus()).isNull();
        assertThat(subscriptionMapped.getConsumerStatus()).isEqualTo(io.gravitee.gateway.api.service.Subscription.ConsumerStatus.STARTED);
        assertThat(subscriptionMapped.getType()).isEqualTo(io.gravitee.gateway.api.service.Subscription.Type.STANDARD);
        assertThat(subscriptionMapped.getConfiguration()).isNull();
        assertThat(subscriptionMapped.getMetadata()).containsEntry("referenceId", "api").containsEntry("referenceType", "API");
    }

    @Test
    void should_return_empty_list_when_api_product_not_in_registry() {
        subscription.setReferenceType(SubscriptionReferenceType.API_PRODUCT);
        subscription.setReferenceId("product-1");
        subscription.setApi(null);
        subscription.setEnvironmentId("env-1");
        ApiProductRegistry registry = mock(ApiProductRegistry.class);
        when(registry.get("product-1", "env-1")).thenReturn(null);
        cut = new SubscriptionMapper(objectMapper, registry);

        List<io.gravitee.gateway.api.service.Subscription> mapped = cut.to(subscription);

        assertThat(mapped).isEmpty();
    }

    @Test
    void should_explode_api_product_subscription_into_one_per_api() {
        subscription.setReferenceType(SubscriptionReferenceType.API_PRODUCT);
        subscription.setReferenceId("product-1");
        subscription.setApi(null);
        subscription.setEnvironmentId("env-1");
        ApiProductRegistry registry = mock(ApiProductRegistry.class);
        ReactableApiProduct product = ReactableApiProduct.builder().id("product-1").apiIds(Set.of("api1", "api2")).build();
        when(registry.get("product-1", "env-1")).thenReturn(product);
        cut = new SubscriptionMapper(objectMapper, registry);

        List<io.gravitee.gateway.api.service.Subscription> mapped = cut.to(subscription);

        assertThat(mapped).hasSize(2);
        assertThat(mapped).extracting(io.gravitee.gateway.api.service.Subscription::getApi).containsExactlyInAnyOrder("api1", "api2");
        assertThat(mapped).allMatch(s -> "id".equals(s.getId()) && "product-1".equals(s.getMetadata().get("productId")));
    }
}
