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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.gamma.rest.core.observability.catalog.exception.DuplicateMetricDefinitionException;
import io.gravitee.gamma.rest.core.observability.catalog.exception.MetricDefinitionNotFoundException;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.GammaMetricDefinitionRepository;
import io.gravitee.repository.management.model.GammaMetricDefinition;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GammaMetricDefinitionRepositoryAdapterTest {

    private static final Instant CREATED = Instant.parse("2026-10-29T08:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-10-29T09:00:00Z");

    private final GammaMetricDefinitionRepository repository = mock(GammaMetricDefinitionRepository.class);
    private final GammaMetricDefinitionRepositoryAdapter adapter = new GammaMetricDefinitionRepositoryAdapter(repository);

    private static final AdditionalMetricDefinition DEFINITION = new AdditionalMetricDefinition(
        "id-1",
        "env-1",
        new AdditionalMetricField("double_checkout_basket-value", AdditionalMetricValueType.DOUBLE),
        "Basket value",
        "At checkout",
        "€",
        false,
        CREATED,
        UPDATED
    );

    private static final GammaMetricDefinition STORED = GammaMetricDefinition.builder()
        .id("id-1")
        .environmentId("env-1")
        .rawKey("double_checkout_basket-value")
        .label("Basket value")
        .description("At checkout")
        .unit("€")
        .exposed(false)
        .createdAt(Date.from(CREATED))
        .updatedAt(Date.from(UPDATED))
        .build();

    @Test
    void should_map_a_record_both_ways() throws Exception {
        when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adapter.create(DEFINITION)).isEqualTo(DEFINITION);
        when(repository.findByEnvironmentId("env-1")).thenReturn(List.of(STORED));
        assertThat(adapter.findByEnvironmentId("env-1")).containsExactly(DEFINITION);
        when(repository.findByEnvironmentIdAndRawKey("env-1", "double_checkout_basket-value")).thenReturn(Optional.of(STORED));
        assertThat(adapter.findByEnvironmentIdAndRawKey("env-1", "double_checkout_basket-value")).contains(DEFINITION);
    }

    @Test
    void should_answer_a_creation_that_lost_a_race_as_a_duplicate() throws Exception {
        when(repository.create(any())).thenThrow(new DuplicateKeyException("taken"));

        assertThatThrownBy(() -> adapter.create(DEFINITION))
            .isInstanceOf(DuplicateMetricDefinitionException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.duplicate");
    }

    @Test
    void should_answer_an_update_of_a_record_deleted_meanwhile_as_not_found() throws Exception {
        when(repository.update(any())).thenThrow(new IllegalStateException("No gamma metric definition found with id [id-1]"));

        assertThatThrownBy(() -> adapter.update(DEFINITION)).isInstanceOf(MetricDefinitionNotFoundException.class);
    }

    @Test
    void should_wrap_any_other_storage_failure() throws Exception {
        when(repository.create(any())).thenThrow(new TechnicalException("down"));

        assertThatThrownBy(() -> adapter.create(DEFINITION)).isInstanceOf(TechnicalDomainException.class);
    }

    @Test
    void should_fail_on_a_stored_key_without_a_known_type_prefix() throws Exception {
        when(repository.findByEnvironmentId("env-1")).thenReturn(List.of(STORED.toBuilder().rawKey("float_x").build()));

        assertThatThrownBy(() -> adapter.findByEnvironmentId("env-1")).isInstanceOf(TechnicalDomainException.class);
    }
}
