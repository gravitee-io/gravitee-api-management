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
package io.gravitee.apim.core.subscription_form.domain_service;

import static fixtures.core.model.SubscriptionFormSchemaFixtures.schema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormValidationException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormFieldConstraints;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.CheckboxGroupField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.InputField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.RadioField;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormSchema.SelectField;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SubscriptionFormSubmissionValidatorTest {

    @Nested
    class WhenSchemaIsAbsent {

        @Test
        void should_skip_validation_when_schema_has_no_fields() {
            var schema = schema();
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, Map.of()));
        }
    }

    @Nested
    class WhenValidatingRequired {

        @Test
        void should_throw_when_required_field_is_missing() {
            var schema = schema(InputField.builder().fieldKey("company").required(true).build());
            assertThatThrownBy(() -> validateSubmission(schema, Map.of()))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'company' is required"));
        }

        @Test
        void should_throw_when_required_field_is_blank() {
            var schema = schema(InputField.builder().fieldKey("company").required(true).build());
            var values = Map.of("company", "  ");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'company' is required"));
        }

        @Test
        void should_not_throw_when_required_field_has_value() {
            var schema = schema(InputField.builder().fieldKey("company").required(true).build());
            var values = Map.of("company", "Acme");
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, values));
        }

        @Test
        void should_not_throw_when_optional_field_is_missing() {
            var schema = schema(InputField.builder().fieldKey("notes").required(false).build());
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, Map.of()));
        }

        @Test
        void should_throw_when_required_checkbox_is_unchecked() {
            var schema = schema(CheckboxField.builder().fieldKey("terms").required(true).build());
            var values = Map.of("terms", "false");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'terms' is required"));
        }

        @Test
        void should_not_throw_when_required_checkbox_is_checked() {
            var schema = schema(CheckboxField.builder().fieldKey("terms").required(true).build());
            var values = Map.of("terms", "true");
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, values));
        }

        @Test
        void should_throw_when_required_checkbox_group_has_only_separators() {
            var schema = schema(CheckboxGroupField.builder().fieldKey("tags").required(true).options(List.of("A", "B")).build());
            var values = Map.of("tags", " , , ");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'tags' is required"));
        }
    }

    @Nested
    class WhenValidatingOptions {

        @Test
        void should_throw_when_value_not_in_allowed_options() {
            var schema = schema(
                SelectField.builder().fieldKey("plan").required(true).options(List.of("Free", "Pro", "Enterprise")).build()
            );
            var values = Map.of("plan", "Premium");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'plan': value 'Premium' is not among the allowed options"));
        }

        @Test
        void should_not_throw_when_value_is_in_allowed_options() {
            var schema = schema(
                SelectField.builder().fieldKey("plan").required(true).options(List.of("Free", "Pro", "Enterprise")).build()
            );
            var values = Map.of("plan", "Pro");
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, values));
        }

        @Test
        void should_not_throw_when_optional_select_is_missing() {
            var schema = schema(SelectField.builder().fieldKey("plan").required(false).options(List.of("Free", "Pro")).build());
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, Map.of()));
        }
    }

    @Nested
    class WhenValidatingCheckboxGroup {

        @Test
        void should_throw_when_one_value_not_in_options() {
            var schema = schema(
                CheckboxGroupField.builder().fieldKey("tags").required(false).options(List.of("Alpha", "Beta", "Gamma")).build()
            );
            var values = Map.of("tags", "Alpha,Delta");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'tags': value 'Delta' is not among the allowed options"));
        }

        @Test
        void should_not_throw_when_all_values_are_valid() {
            var schema = schema(
                CheckboxGroupField.builder().fieldKey("tags").required(false).options(List.of("Alpha", "Beta", "Gamma")).build()
            );
            var values = Map.of("tags", "Alpha,Gamma");
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, values));
        }

        @Test
        void should_report_one_error_per_invalid_value() {
            var schema = schema(CheckboxGroupField.builder().fieldKey("tags").required(false).options(List.of("Alpha", "Beta")).build());
            var values = Map.of("tags", "Alpha,Delta,Omega");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).lines().toList()).containsExactly(
                        "Field 'tags': value 'Delta' is not among the allowed options",
                        "Field 'tags': value 'Omega' is not among the allowed options"
                    );
                });
        }
    }

    @Nested
    class WhenValidatingLength {

        @Test
        void should_throw_when_value_is_shorter_than_minLength() {
            var schema = schema(InputField.builder().fieldKey("company").required(false).minLength(5).build());
            var values = Map.of("company", "Acm");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'company' must be at least 5 characters long"));
        }

        @Test
        void should_throw_when_value_is_longer_than_maxLength() {
            var schema = schema(InputField.builder().fieldKey("company").required(false).maxLength(10).build());
            var values = Map.of("company", "A very long company name indeed");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'company' must be at most 10 characters long"));
        }

        @Test
        void should_not_throw_when_value_is_within_boundaries() {
            var schema = schema(InputField.builder().fieldKey("company").required(false).minLength(3).maxLength(10).build());
            var values = Map.of("company", "Acme");
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, values));
        }
    }

    @Nested
    class WhenValidatingPattern {

        @Test
        void should_throw_when_value_does_not_match_pattern() {
            var schema = schema(InputField.builder().fieldKey("code").required(false).pattern("[A-Z]{3}-\\d{4}").build());
            var values = Map.of("code", "invalid-code");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'code' does not match the required pattern"));
        }

        @Test
        void should_not_throw_when_value_matches_pattern() {
            var schema = schema(InputField.builder().fieldKey("code").required(false).pattern("[A-Z]{3}-\\d{4}").build());
            var values = Map.of("code", "ABC-1234");
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, values));
        }

        @Test
        void should_add_error_when_pattern_is_invalid_regex() {
            var schema = schema(InputField.builder().fieldKey("code").required(false).pattern("[invalid(regex").build());
            var values = Map.of("code", "anything");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'code' has an invalid pattern configuration"));
        }
    }

    @Nested
    class WhenValidatingMultipleFields {

        @Test
        void should_collect_errors_from_all_fields_before_throwing() {
            var schema = schema(
                InputField.builder().fieldKey("company").required(true).build(),
                SelectField.builder().fieldKey("plan").required(true).options(List.of("Free", "Pro")).build(),
                InputField.builder().fieldKey("code").required(false).pattern("[A-Z]+").build()
            );
            var values = Map.of("company", "", "plan", "Enterprise", "code", "123");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).hasSize(3));
        }

        @Test
        void should_not_throw_for_valid_full_submission() {
            var schema = schema(
                InputField.builder().fieldKey("company").required(true).build(),
                SelectField.builder().fieldKey("plan").required(true).options(List.of("Free", "Pro")).build(),
                CheckboxGroupField.builder().fieldKey("tags").required(false).options(List.of("A", "B", "C")).build()
            );
            var values = Map.of("company", "Acme Corp", "plan", "Pro", "tags", "A,C");
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, values));
        }
    }

    @Nested
    class WhenValidatingReadonlyFields {

        @Test
        void should_not_throw_when_readonly_value_matches_preset() {
            var schema = schema(InputField.builder().fieldKey("ref").required(false).readonlyValue("REF-123").build());
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, Map.of("ref", "REF-123")));
        }

        @Test
        void should_throw_when_readonly_value_does_not_match_preset() {
            var schema = schema(InputField.builder().fieldKey("ref").required(false).readonlyValue("REF-123").build());
            var values = Map.of("ref", "REF-999");
            assertThatThrownBy(() -> validateSubmission(schema, values))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors -> assertThat(errors).containsExactly("Field 'ref': read-only field cannot be modified"));
        }

        @Test
        void should_not_apply_other_validations_to_readonly_fields() {
            // readonly RadioField has required=true and options, but readonly check takes precedence
            var schema = schema(
                RadioField.builder().fieldKey("plan").required(true).readonlyValue("Free").options(List.of("Free", "Pro")).build()
            );
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, Map.of("plan", "Free")));
        }
    }

    @Nested
    class WhenValidatingMetadataCount {

        @Test
        void should_throw_when_submission_exceeds_max_metadata_count() {
            var schema = schema(InputField.builder().fieldKey("company").required(true).build());
            Map<String, String> tooMany = new java.util.HashMap<>();
            for (int i = 0; i < SubscriptionFormSubmissionValidator.MAX_METADATA_COUNT + 1; i++) {
                tooMany.put("key_" + i, "value");
            }
            assertThatThrownBy(() -> validateSubmission(schema, tooMany))
                .isInstanceOf(SubscriptionFormValidationException.class)
                .extracting(e -> ((SubscriptionFormValidationException) e).getErrors())
                .satisfies(errors ->
                    assertThat(errors).containsExactly(
                        "Subscription metadata must not exceed " + SubscriptionFormSubmissionValidator.MAX_METADATA_COUNT + " entries"
                    )
                );
        }

        @Test
        void should_not_throw_when_submission_is_at_max_metadata_count() {
            var schema = schema(InputField.builder().fieldKey("notes").required(false).build());
            Map<String, String> exactlyMax = new java.util.HashMap<>();
            for (int i = 0; i < SubscriptionFormSubmissionValidator.MAX_METADATA_COUNT; i++) {
                exactlyMax.put("key_" + i, "value");
            }
            assertThatNoException().isThrownBy(() -> validateSubmission(schema, exactlyMax));
        }
    }

    @Nested
    class WhenUsingPrebuiltFieldConstraints {

        @Test
        void should_validate_submission_from_field_constraints_without_schema() {
            var schema = schema(InputField.builder().fieldKey("company").required(true).build());
            var constraints = SubscriptionFormConstraintsFactory.fromSchema(schema);
            assertThatNoException().isThrownBy(() -> validateSubmission(constraints, Map.of("company", "Acme")));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validateSubmission(SubscriptionFormSchema schema, Map<String, String> values) {
        new SubscriptionFormSubmissionValidator(schema).validate(values);
    }

    private void validateSubmission(SubscriptionFormFieldConstraints constraints, Map<String, String> values) {
        new SubscriptionFormSubmissionValidator(constraints).validate(values);
    }
}
