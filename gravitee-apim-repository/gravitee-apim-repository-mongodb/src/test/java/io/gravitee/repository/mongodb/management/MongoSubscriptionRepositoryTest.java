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
package io.gravitee.repository.mongodb.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import io.gravitee.repository.mongodb.management.internal.model.SubscriptionMongo;
import io.gravitee.repository.mongodb.management.internal.plan.SubscriptionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class MongoSubscriptionRepositoryTest {

    private MongoSubscriptionRepository mongoSubscriptionRepository;
    private SubscriptionMongoRepository internalSubscriptionRepository;
    private GraviteeMapper mapper;

    @Before
    public void setUp() {
        internalSubscriptionRepository = mock(SubscriptionMongoRepository.class);
        mapper = mock(GraviteeMapper.class);

        mongoSubscriptionRepository = new MongoSubscriptionRepository();

        // Inject collaborators via reflection (since in production they are autowired)
        try {
            var internalField = MongoSubscriptionRepository.class.getDeclaredField("internalSubscriptionRepository");
            internalField.setAccessible(true);
            internalField.set(mongoSubscriptionRepository, internalSubscriptionRepository);

            var mapperField = MongoSubscriptionRepository.class.getDeclaredField("mapper");
            mapperField.setAccessible(true);
            mapperField.set(mongoSubscriptionRepository, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldFindByReferenceIdAndReferenceType() throws TechnicalException {
        SubscriptionMongo subscriptionMongo = new SubscriptionMongo();
        subscriptionMongo.setId("sub-api-product-1");

        when(
            internalSubscriptionRepository.findByReferenceIdAndReferenceType(
                "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
                SubscriptionReferenceType.API_PRODUCT.name()
            )
        ).thenReturn(List.of(subscriptionMongo));

        Subscription mapped = new Subscription();
        mapped.setId("sub-api-product-1");
        when(mapper.map(subscriptionMongo)).thenReturn(mapped);

        Set<Subscription> result = mongoSubscriptionRepository.findByReferenceIdAndReferenceType(
            "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
            SubscriptionReferenceType.API_PRODUCT
        );

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getId()).isEqualTo("sub-api-product-1");
        verify(internalSubscriptionRepository).findByReferenceIdAndReferenceType(
            "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
            SubscriptionReferenceType.API_PRODUCT.name()
        );
    }

    @Test
    public void shouldWrapExceptionWhenFindingByReferenceIdAndReferenceType() {
        when(internalSubscriptionRepository.findByReferenceIdAndReferenceType(anyString(), anyString())).thenThrow(
            new RuntimeException("mongo failure")
        );

        assertThatThrownBy(() ->
            mongoSubscriptionRepository.findByReferenceIdAndReferenceType("ref", SubscriptionReferenceType.API_PRODUCT)
        )
            .isInstanceOf(TechnicalException.class)
            .hasMessageContaining("find subscriptions by reference");
    }

    @Test
    public void shouldFindByIdAndReferenceIdAndReferenceType() throws TechnicalException {
        SubscriptionMongo subscriptionMongo = new SubscriptionMongo();
        subscriptionMongo.setId("sub-api-product-1");

        when(
            internalSubscriptionRepository.findByIdAndReferenceIdAndReferenceType(
                "sub-api-product-1",
                "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
                SubscriptionReferenceType.API_PRODUCT.name()
            )
        ).thenReturn(Optional.of(subscriptionMongo));

        Subscription mapped = new Subscription();
        mapped.setId("sub-api-product-1");
        when(mapper.map(subscriptionMongo)).thenReturn(mapped);

        Optional<Subscription> result = mongoSubscriptionRepository.findByIdAndReferenceIdAndReferenceType(
            "sub-api-product-1",
            "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
            SubscriptionReferenceType.API_PRODUCT
        );

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("sub-api-product-1");
        verify(internalSubscriptionRepository).findByIdAndReferenceIdAndReferenceType(
            "sub-api-product-1",
            "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
            SubscriptionReferenceType.API_PRODUCT.name()
        );
    }

    @Test
    public void shouldWrapExceptionWhenFindingByIdAndReferenceIdAndReferenceType() {
        when(internalSubscriptionRepository.findByIdAndReferenceIdAndReferenceType(anyString(), anyString(), anyString())).thenThrow(
            new RuntimeException("mongo failure")
        );

        assertThatThrownBy(() ->
            mongoSubscriptionRepository.findByIdAndReferenceIdAndReferenceType("sub-id", "ref", SubscriptionReferenceType.API_PRODUCT)
        )
            .isInstanceOf(TechnicalException.class)
            .hasMessageContaining("find subscription by id and reference");
    }
}
