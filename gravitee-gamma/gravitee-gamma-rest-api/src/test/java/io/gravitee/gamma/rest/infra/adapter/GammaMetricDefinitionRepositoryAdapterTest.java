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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GammaMetricDefinitionRepositoryAdapterTest {

    private static final Instant CREATED = Instant.parse("2026-10-29T08:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-10-29T09:00:00Z");

    private final GammaMetricDefinitionRepository repository = mock(GammaMetricDefinitionRepository.class);
    private final GammaMetricDefinitionRepositoryAdapter adapter = new GammaMetricDefinitionRepositoryAdapter(
        repository,
        new AdditionalMetricGrammarPortAdapter()
    );

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

    private static final AdditionalMetricDefinition CUSTOM_DEFINITION = new AdditionalMetricDefinition(
        "id-2",
        "env-1",
        new AdditionalMetricField(AdditionalMetricField.Root.CUSTOM, "fraud-tier", AdditionalMetricValueType.KEYWORD),
        "Fraud tier",
        "Tier the fraud check assigns",
        null,
        true,
        CREATED,
        UPDATED
    );

    private static final GammaMetricDefinition CUSTOM_STORED = GammaMetricDefinition.builder()
        .id("id-2")
        .environmentId("env-1")
        .rawKey("custom.fraud-tier")
        .label("Fraud tier")
        .description("Tier the fraud check assigns")
        .exposed(true)
        .createdAt(Date.from(CREATED))
        .updatedAt(Date.from(UPDATED))
        .build();

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
        assertThat(adapter.findByEnvironmentIdAndField("env-1", DEFINITION.field())).contains(DEFINITION);
    }

    /** Records stored before custom keys had any keep their bare additional key and read back unchanged. */
    @Test
    void should_store_an_additional_key_bare_and_a_custom_key_with_its_root() throws Exception {
        when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var captor = ArgumentCaptor.forClass(GammaMetricDefinition.class);

        adapter.create(DEFINITION);
        adapter.create(CUSTOM_DEFINITION);

        verify(repository, times(2)).create(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(GammaMetricDefinition::getRawKey)
            .containsExactly("double_checkout_basket-value", "custom.fraud-tier");
    }

    @Test
    void should_read_back_records_of_both_roots() throws Exception {
        when(repository.findByEnvironmentId("env-1")).thenReturn(List.of(STORED, CUSTOM_STORED));

        assertThat(adapter.findByEnvironmentId("env-1")).containsExactly(DEFINITION, CUSTOM_DEFINITION);
    }

    /** custom.keyword_x and additional-metrics.keyword_x are two keys: each is looked up under its own stored key. */
    @Test
    void should_look_a_custom_key_up_under_its_root_never_under_the_bare_key_of_the_same_spelling() throws Exception {
        var customField = new AdditionalMetricField(AdditionalMetricField.Root.CUSTOM, "keyword_x", AdditionalMetricValueType.KEYWORD);
        var additionalField = new AdditionalMetricField("keyword_x", AdditionalMetricValueType.KEYWORD);
        when(repository.findByEnvironmentIdAndRawKey("env-1", "custom.keyword_x")).thenReturn(
            Optional.of(CUSTOM_STORED.toBuilder().rawKey("custom.keyword_x").build())
        );
        when(repository.findByEnvironmentIdAndRawKey("env-1", "keyword_x")).thenReturn(
            Optional.of(STORED.toBuilder().id("id-bare").rawKey("keyword_x").build())
        );

        assertThat(adapter.findByEnvironmentIdAndField("env-1", customField))
            .get()
            .extracting(AdditionalMetricDefinition::field)
            .isEqualTo(customField);
        assertThat(adapter.findByEnvironmentIdAndField("env-1", additionalField))
            .get()
            .extracting(AdditionalMetricDefinition::id)
            .isEqualTo("id-bare");
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

    /** Names reach these messages from requests and stores: a raw line break or NUL never does. */
    @Test
    void should_never_echo_a_raw_control_character_in_a_duplicate_or_technical_error_message() throws Exception {
        var field = new AdditionalMetricField(AdditionalMetricField.Root.CUSTOM, "fraud\r\ntier", AdditionalMetricValueType.KEYWORD);
        when(repository.findByEnvironmentIdAndRawKey("env-1", "custom.fraud\r\ntier")).thenThrow(new TechnicalException("down"));
        when(repository.findByEnvironmentId("env-1")).thenReturn(List.of(STORED.toBuilder().rawKey("float_\u0000x").build()));

        assertThatThrownBy(() -> adapter.findByEnvironmentIdAndField("env-1", field))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessageContaining("custom.fraud\\u000D\\u000Atier")
            .message()
            .doesNotContainPattern("\\p{Cntrl}");
        assertThatThrownBy(() -> adapter.findByEnvironmentId("env-1"))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessageContaining("float_\\u0000x");
        assertThat(new DuplicateMetricDefinitionException("custom.fraud\r\ntier").getMessage())
            .contains("custom.fraud\\u000D\\u000Atier")
            .doesNotContainPattern("\\p{Cntrl}");
    }
}
