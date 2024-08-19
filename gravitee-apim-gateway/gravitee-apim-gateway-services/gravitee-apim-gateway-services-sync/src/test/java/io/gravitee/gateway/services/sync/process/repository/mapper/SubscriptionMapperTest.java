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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.api.service.SubscriptionConfiguration;
import io.gravitee.repository.management.model.Subscription;
import java.util.Date;
import java.util.Map;
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
        cut = new SubscriptionMapper(objectMapper);

        subscription = new Subscription();
        subscription.setApi("api");
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
        io.gravitee.gateway.api.service.Subscription subscriptionMapped = cut.to(subscription);

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
        assertThat(subscriptionMapped.getConfiguration())
            .isEqualTo(objectMapper.readValue(subscription.getConfiguration(), SubscriptionConfiguration.class));
        assertThat(subscriptionMapped.getMetadata()).isEqualTo(subscription.getMetadata());
    }

    @Test
    void should_map_subscription_without_all_attributes() {
        subscription.setStatus(null);
        subscription.setConsumerStatus(null);
        subscription.setType(null);
        subscription.setConfiguration(null);
        io.gravitee.gateway.api.service.Subscription subscriptionMapped = cut.to(subscription);

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
        assertThat(subscriptionMapped.getMetadata()).isEqualTo(subscription.getMetadata());
    }
}
