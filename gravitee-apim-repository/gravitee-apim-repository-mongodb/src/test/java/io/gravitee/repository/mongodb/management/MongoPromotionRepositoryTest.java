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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.Promotion;
import io.gravitee.repository.mongodb.management.internal.promotion.PromotionMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MongoPromotionRepositoryTest {

    private MongoPromotionRepository mongoPromotionRepository;
    private PromotionMongoRepository internalRepository;
    private GraviteeMapper mapper;

    @BeforeEach
    public void setUp() {
        internalRepository = mock(PromotionMongoRepository.class);
        mapper = mock(GraviteeMapper.class);

        mongoPromotionRepository = new MongoPromotionRepository();

        // Inject collaborators via reflection (since in production they are autowired)
        try {
            var internalField = MongoPromotionRepository.class.getDeclaredField("internalRepository");
            internalField.setAccessible(true);
            internalField.set(mongoPromotionRepository, internalRepository);

            var mapperField = MongoPromotionRepository.class.getDeclaredField("mapper");
            mapperField.setAccessible(true);
            mapperField.set(mongoPromotionRepository, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // A read may be served by a lagging secondary and miss a promotion another node has just written. Deciding
    // existence from such a read refused updates the database would have accepted.
    @Test
    public void shouldUpdateAPromotionThatReadsCannotSeeYet() throws TechnicalException {
        Promotion promotion = new Promotion();
        promotion.setId("promotion#1");

        when(internalRepository.findById("promotion#1")).thenReturn(Optional.empty());
        when(internalRepository.replace(any())).thenReturn(true);

        Promotion updated = mongoPromotionRepository.update(promotion);

        assertThat(updated).isEqualTo(promotion);
        verify(internalRepository, never()).save(any());
    }

    @Test
    public void shouldRejectTheUpdateWhenNoPromotionCarriesThatId() {
        Promotion promotion = new Promotion();
        promotion.setId("unknown");

        when(internalRepository.replace(any())).thenReturn(false);

        assertThatThrownBy(() -> mongoPromotionRepository.update(promotion))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("unknown");
    }

    @Test
    public void shouldRejectANullPromotion() {
        assertThatThrownBy(() -> mongoPromotionRepository.update(null)).isInstanceOf(IllegalStateException.class);
    }
}
