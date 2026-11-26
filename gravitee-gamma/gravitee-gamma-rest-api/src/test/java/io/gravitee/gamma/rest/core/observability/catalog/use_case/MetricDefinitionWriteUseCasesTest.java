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
package io.gravitee.gamma.rest.core.observability.catalog.use_case;

import static io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture.ENVIRONMENT;
import static io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture.OTHER_ENVIRONMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.catalog.exception.DuplicateMetricDefinitionException;
import io.gravitee.gamma.rest.core.observability.catalog.exception.MetricDefinitionNotFoundException;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricDefinitionWriteUseCasesTest {

    private static final String BASKET = "additional-metrics.double_checkout_basket-value";
    private static final String MCP_TOOL_KEY = "keyword_mcp-proxy_tools/call";
    private static final String FRAUD_TIER = "custom.fraud-tier";

    private MetricDefinitionFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new MetricDefinitionFixture();
        fixture.ports.givenBuiltIn(MCP_TOOL_KEY, "MCP_PROXY_TOOL").givenBuiltIn("keyword_native-kafka_broker-id");
    }

    @Nested
    class Declare {

        @Test
        void should_store_a_record_with_its_fields_and_exposed_by_default() {
            var declared = fixture.declare
                .execute(new DeclareMetricDefinitionUseCase.Input(ENVIRONMENT, BASKET, "Basket value", "At checkout", "€", null))
                .definition();

            assertThat(declared.environmentId()).isEqualTo(ENVIRONMENT);
            assertThat(declared.field().rawKey()).isEqualTo("double_checkout_basket-value");
            assertThat(declared.field().valueType()).isEqualTo(AdditionalMetricValueType.DOUBLE);
            assertThat(declared.label()).isEqualTo("Basket value");
            assertThat(declared.description()).isEqualTo("At checkout");
            assertThat(declared.unit()).isEqualTo("€");
            assertThat(declared.exposed()).isTrue();
            assertThat(declared.createdAt()).isNotNull().isEqualTo(declared.updatedAt());
            assertThat(fixture.repository.all()).containsExactly(declared);
        }

        @Test
        void should_refuse_a_key_the_environment_already_declares() {
            fixture.givenDeclared(BASKET);

            assertThatThrownBy(() -> fixture.givenDeclared(BASKET))
                .isInstanceOf(DuplicateMetricDefinitionException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.duplicate");
        }

        @Test
        void should_accept_a_key_another_environment_declares() {
            fixture.givenDeclared(OTHER_ENVIRONMENT, BASKET, null, null, true);

            fixture.givenDeclared(BASKET);

            assertThat(fixture.repository.all()).hasSize(2);
        }

        @Test
        void should_refuse_a_key_a_built_in_reads_even_one_read_through_a_constant_only() {
            assertThatThrownBy(() -> fixture.givenDeclared("additional-metrics." + MCP_TOOL_KEY))
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.built_in_key");
            assertThatThrownBy(() -> fixture.givenDeclared("additional-metrics.keyword_native-kafka_broker-id"))
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.built_in_key");
        }

        @Test
        void should_refuse_the_reserved_custom_form() {
            assertThatThrownBy(() -> fixture.givenDeclared("additional-metrics.double_custom_basket-value"))
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.reserved_key");
        }

        /** E2E finding S1: such a record reads another map than the policy's custom.<name>, a decoy matching nothing. */
        @ParameterizedTest
        @CsvSource(
            delimiter = '|',
            value = {
                "additional-metrics.keyword_custom.fraud-tier | custom.fraud-tier",
                "additional-metrics.long_custom.retries | custom.retries",
                "additional-metrics.keyword_custom.a.b | custom.a.b",
            }
        )
        void should_refuse_a_key_naming_a_custom_metric_after_its_type_prefix(String name, String customMetric) {
            assertThatThrownBy(() -> fixture.givenDeclared(name))
                .hasMessage(
                    "'" +
                        name +
                        "' is reserved: it reads the additional metrics map, not the values the Assign metrics policy writes; declare '" +
                        customMetric +
                        "' instead"
                )
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.reserved_key");
            assertThat(fixture.repository.all()).isEmpty();
        }

        @Test
        void should_still_declare_a_key_whose_segment_merely_starts_with_custom() {
            fixture.givenDeclared("additional-metrics.keyword_customer.tier");

            assertThat(fixture.repository.all()).hasSize(1);
        }

        @Test
        void should_declare_a_custom_key_as_a_keyword_without_unit() {
            var declared = fixture.declare
                .execute(new DeclareMetricDefinitionUseCase.Input(ENVIRONMENT, FRAUD_TIER, "Fraud tier", "Tier of the check", null, false))
                .definition();

            assertThat(declared.field().name()).isEqualTo(FRAUD_TIER);
            assertThat(declared.field().root()).isEqualTo(AdditionalMetricField.Root.CUSTOM);
            assertThat(declared.field().valueType()).isEqualTo(AdditionalMetricValueType.KEYWORD);
            assertThat(declared.unit()).isNull();
            assertThat(declared.exposed()).isFalse();
            assertThat(fixture.repository.all()).containsExactly(declared);
        }

        @Test
        void should_refuse_a_unit_on_a_custom_key_before_storing_anything() {
            var input = new DeclareMetricDefinitionUseCase.Input(ENVIRONMENT, FRAUD_TIER, null, null, "COUNT", true);

            assertThatThrownBy(() -> fixture.declare.execute(input))
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.unit_not_allowed");
            assertThat(fixture.repository.all()).isEmpty();
        }

        /** The reserved typed family is additional-metrics.<type>_custom_<name> (OBS-172); under custom. it is a plain key. */
        @Test
        void should_declare_a_custom_key_spelt_like_the_reserved_typed_form() {
            fixture.givenDeclared("custom.keyword_custom_x");

            assertThat(fixture.repository.all())
                .extracting(record -> record.field().name())
                .containsExactly("custom.keyword_custom_x");
        }

        /** Built-ins read keys of the additional-metrics map: a custom key of the same spelling is another field. */
        @Test
        void should_declare_a_custom_key_spelt_like_a_key_a_built_in_reads() {
            fixture.givenDeclared("custom." + MCP_TOOL_KEY);

            assertThat(fixture.repository.all())
                .extracting(record -> record.field().name())
                .containsExactly("custom." + MCP_TOOL_KEY);
        }

        @Test
        void should_hold_a_custom_key_and_an_additional_key_of_the_same_spelling() {
            fixture.givenDeclared("custom.keyword_x");
            fixture.givenDeclared("additional-metrics.keyword_x");

            assertThat(fixture.repository.all())
                .extracting(record -> record.field().name())
                .containsExactlyInAnyOrder("custom.keyword_x", "additional-metrics.keyword_x");
        }

        @Test
        void should_refuse_a_custom_key_the_environment_already_declares() {
            fixture.givenDeclared(FRAUD_TIER);

            assertThatThrownBy(() -> fixture.givenDeclared(FRAUD_TIER))
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.duplicate");
        }

        @Test
        void should_refuse_a_key_with_a_double_quote() {
            assertThatThrownBy(() -> fixture.givenDeclared("additional-metrics.keyword_my\"key"))
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.invalid_name");
            assertThat(fixture.repository.all()).isEmpty();
        }
    }

    @Nested
    class Save {

        @Test
        void should_create_the_record_when_the_environment_holds_none() {
            var output = fixture.save.execute(new SaveMetricDefinitionUseCase.Input(ENVIRONMENT, BASKET, "Basket", null, null, false));

            assertThat(output.created()).isTrue();
            assertThat(output.definition().exposed()).isFalse();
            assertThat(fixture.repository.all()).containsExactly(output.definition());
        }

        @Test
        void should_refuse_a_key_naming_a_custom_metric_after_its_type_prefix() {
            var input = new SaveMetricDefinitionUseCase.Input(
                ENVIRONMENT,
                "additional-metrics.keyword_custom.fraud-tier",
                null,
                null,
                null,
                false
            );

            assertThatThrownBy(() -> fixture.save.execute(input))
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.reserved_key");
            assertThat(fixture.repository.all()).isEmpty();
        }

        @Test
        void should_create_then_replace_the_record_on_a_custom_key() {
            var created = fixture.save.execute(
                new SaveMetricDefinitionUseCase.Input(ENVIRONMENT, FRAUD_TIER, "Fraud tier", null, "", true)
            );
            var replaced = fixture.save.execute(
                new SaveMetricDefinitionUseCase.Input(ENVIRONMENT, FRAUD_TIER, "Fraud level", "Set by the check", null, false)
            );

            assertThat(created.created()).isTrue();
            assertThat(replaced.created()).isFalse();
            assertThat(replaced.definition().id()).isEqualTo(created.definition().id());
            assertThat(fixture.repository.all()).containsExactly(replaced.definition());
            assertThat(replaced.definition().label()).isEqualTo("Fraud level");
            assertThat(replaced.definition().exposed()).isFalse();
        }

        @Test
        void should_refuse_a_unit_on_a_custom_key_and_keep_the_record() {
            var existing = fixture.givenDeclared(FRAUD_TIER);
            var input = new SaveMetricDefinitionUseCase.Input(ENVIRONMENT, FRAUD_TIER, "Fraud tier", null, "COUNT", true);

            assertThatThrownBy(() -> fixture.save.execute(input))
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.unit_not_allowed");
            assertThat(fixture.repository.all()).containsExactly(existing);
        }

        @Test
        void should_replace_every_field_of_the_existing_record_and_keep_its_identity() {
            var existing = fixture.givenDeclared(ENVIRONMENT, BASKET, "Basket value", "€", true);

            var output = fixture.save.execute(new SaveMetricDefinitionUseCase.Input(ENVIRONMENT, BASKET, null, "Cart total", null, false));

            assertThat(output.created()).isFalse();
            assertThat(output.definition().id()).isEqualTo(existing.id());
            assertThat(output.definition().createdAt()).isEqualTo(existing.createdAt());
            assertThat(output.definition().label()).isNull();
            assertThat(output.definition().unit()).isNull();
            assertThat(output.definition().description()).isEqualTo("Cart total");
            assertThat(output.definition().exposed()).isFalse();
            assertThat(fixture.repository.all()).containsExactly(output.definition());
        }

        @Test
        void should_replace_a_record_another_write_created_after_the_read() {
            Instant earlier = Instant.parse("2026-10-29T08:00:00Z");
            fixture.repository.givenConcurrentCreateBeforeNextCreate(
                new AdditionalMetricDefinition(
                    "concurrent",
                    ENVIRONMENT,
                    fixture.keys.field(BASKET),
                    "Theirs",
                    null,
                    null,
                    true,
                    earlier,
                    earlier
                )
            );

            var output = fixture.save.execute(new SaveMetricDefinitionUseCase.Input(ENVIRONMENT, BASKET, "Mine", null, null, null));

            assertThat(output.created()).isFalse();
            assertThat(fixture.repository.all())
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.id()).isEqualTo("concurrent");
                    assertThat(record.label()).isEqualTo("Mine");
                });
        }

        @Test
        void should_write_again_a_record_another_write_deleted_after_the_read() {
            fixture.givenDeclared(ENVIRONMENT, BASKET, "Before", null, true);
            fixture.repository.givenConcurrentDeleteBeforeNextUpdate();

            var output = fixture.save.execute(new SaveMetricDefinitionUseCase.Input(ENVIRONMENT, BASKET, "After", null, null, null));

            assertThat(output.created()).isTrue();
            assertThat(fixture.repository.all()).singleElement().extracting(AdditionalMetricDefinition::label).isEqualTo("After");
        }

        @Test
        void should_refuse_a_built_in_key_as_read_only() {
            assertThatThrownBy(() ->
                fixture.save.execute(
                    new SaveMetricDefinitionUseCase.Input(ENVIRONMENT, "additional-metrics." + MCP_TOOL_KEY, "Tool", null, null, true)
                )
            )
                .extracting("technicalCode")
                .isEqualTo("observability.metric_definition.built_in_key");
        }
    }

    @Nested
    class GetAndDelete {

        @Test
        void should_read_a_record_whose_key_contains_a_slash() {
            var declared = fixture.givenDeclared("additional-metrics.keyword_my-policy_path/segment");

            var read = fixture.get.execute(
                new GetMetricDefinitionUseCase.Input(ENVIRONMENT, "additional-metrics.keyword_my-policy_path/segment")
            );

            assertThat(read.definition()).isEqualTo(declared);
        }

        @Test
        void should_not_find_a_record_of_another_environment() {
            fixture.givenDeclared(OTHER_ENVIRONMENT, BASKET, null, null, true);

            assertThatThrownBy(() -> fixture.get.execute(new GetMetricDefinitionUseCase.Input(ENVIRONMENT, BASKET))).isInstanceOf(
                MetricDefinitionNotFoundException.class
            );
            assertThatThrownBy(() -> fixture.delete.execute(new DeleteMetricDefinitionUseCase.Input(ENVIRONMENT, BASKET))).isInstanceOf(
                MetricDefinitionNotFoundException.class
            );
            assertThat(fixture.repository.all()).hasSize(1);
        }

        @Test
        void should_read_and_delete_the_custom_record_only_when_both_spellings_are_declared() {
            var custom = fixture.givenDeclared("custom.keyword_x");
            var additional = fixture.givenDeclared("additional-metrics.keyword_x");

            assertThat(fixture.get.execute(new GetMetricDefinitionUseCase.Input(ENVIRONMENT, "custom.keyword_x")).definition()).isEqualTo(
                custom
            );
            fixture.delete.execute(new DeleteMetricDefinitionUseCase.Input(ENVIRONMENT, "custom.keyword_x"));

            assertThat(fixture.repository.all()).containsExactly(additional);
        }

        @Test
        void should_delete_the_record() {
            fixture.givenDeclared(BASKET);

            fixture.delete.execute(new DeleteMetricDefinitionUseCase.Input(ENVIRONMENT, BASKET));

            assertThat(fixture.repository.all()).isEmpty();
        }
    }
}
