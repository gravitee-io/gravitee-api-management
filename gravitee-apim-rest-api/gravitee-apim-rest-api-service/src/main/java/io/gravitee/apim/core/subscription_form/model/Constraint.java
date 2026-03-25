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
package io.gravitee.apim.core.subscription_form.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/**
 * A single validation rule for a subscription form field.
 *
 * <p>Constraints are derived from a {@link SubscriptionFormSchema} at save time and persisted as JSON.
 * At submission time the validator iterates the constraint map and calls
 * {@link #validate(String, String)} — no schema knowledge required.</p>
 *
 * <p>Implementations split rule logic ({@link #check(String)}) from the human-readable message
 * ({@link #formatErrorMessage(String, String)}); the default {@link #validate(String, String)} composes
 * those as an empty list when {@link #check(String)} passes, or a one-element list otherwise.
 * {@link EachOf} joins several per-value messages with newline ({@code \n}) inside that single string.</p>
 *
 * @author Gravitee.io Team
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = Constraint.Required.class, name = "required"),
        @JsonSubTypes.Type(value = Constraint.MustBeTrue.class, name = "mustBeTrue"),
        @JsonSubTypes.Type(value = Constraint.NonEmptySelection.class, name = "nonEmptySelection"),
        @JsonSubTypes.Type(value = Constraint.ReadOnly.class, name = "readOnly"),
        @JsonSubTypes.Type(value = Constraint.MinLength.class, name = "minLength"),
        @JsonSubTypes.Type(value = Constraint.MaxLength.class, name = "maxLength"),
        @JsonSubTypes.Type(value = Constraint.MatchesPattern.class, name = "matchesPattern"),
        @JsonSubTypes.Type(value = Constraint.OneOf.class, name = "oneOf"),
        @JsonSubTypes.Type(value = Constraint.EachOf.class, name = "eachOf"),
        @JsonSubTypes.Type(value = Constraint.DynamicOneOf.class, name = "dynamicOneOf"),
        @JsonSubTypes.Type(value = Constraint.DynamicEachOf.class, name = "dynamicEachOf"),
    }
)
public sealed interface Constraint
    permits
        Constraint.Required,
        Constraint.MustBeTrue,
        Constraint.NonEmptySelection,
        Constraint.ReadOnly,
        Constraint.MinLength,
        Constraint.MaxLength,
        Constraint.MatchesPattern,
        Constraint.OneOf,
        Constraint.EachOf,
        Constraint.DynamicOneOf,
        Constraint.DynamicEachOf {
    /**
     * Whether the submitted value satisfies this constraint. The value is already trimmed; an empty string means absent.
     */
    boolean check(String value);

    /**
     * Message when {@link #check(String)} is {@code false}. {@link EachOf} may return several lines separated by {@code \n}.
     */
    String formatErrorMessage(String fieldKey, String value);

    /**
     * Validates the given value against this constraint.
     *
     * @param fieldKey the field identifier (used in error messages)
     * @param value    the submitted value, already trimmed; empty string when absent
     * @return an empty list if the value is valid, or otherwise typically one message per constraint (several lines
     *         in one string for {@link EachOf})
     */
    default List<String> validate(String fieldKey, String value) {
        return check(value) ? List.of() : List.of(formatErrorMessage(fieldKey, value));
    }

    /** Text field must not be blank. */
    record Required() implements Constraint {
        @Override
        public boolean check(String value) {
            return !value.isEmpty();
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            return String.format("Field '%s' is required", fieldKey);
        }
    }

    /** Checkbox must be checked ({@code "true"}). */
    record MustBeTrue() implements Constraint {
        @Override
        public boolean check(String value) {
            return "true".equals(value);
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            return String.format("Field '%s' is required", fieldKey);
        }
    }

    /** Checkbox-group must have at least one selected value in a comma-separated string. */
    record NonEmptySelection() implements Constraint {
        @Override
        public boolean check(String value) {
            return splitCsv(value).findAny().isPresent();
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            return String.format("Field '%s' is required", fieldKey);
        }
    }

    /** Field value must equal the preset read-only reference. */
    record ReadOnly(String reference) implements Constraint {
        @Override
        public boolean check(String value) {
            return reference.equals(value);
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            return String.format("Field '%s': read-only field cannot be modified", fieldKey);
        }
    }

    /** Value must be at least {@code min} characters long (skipped when empty). */
    record MinLength(int min) implements Constraint {
        @Override
        public boolean check(String value) {
            return value.isEmpty() || value.length() >= min;
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            return String.format("Field '%s' must be at least %d characters long", fieldKey, min);
        }
    }

    /** Value must be at most {@code max} characters long (skipped when empty). */
    record MaxLength(int max) implements Constraint {
        public static final int INPUT_MAX_LENGTH = 256;

        public static final int TEXTAREA_MAX_LENGTH = 1024;

        public static MaxLength forInput() {
            return new MaxLength(INPUT_MAX_LENGTH);
        }

        public static MaxLength forInput(int userDefined) {
            return new MaxLength(Math.min(userDefined, INPUT_MAX_LENGTH));
        }

        public static MaxLength forTextarea() {
            return new MaxLength(TEXTAREA_MAX_LENGTH);
        }

        public static MaxLength forTextarea(int userDefined) {
            return new MaxLength(Math.min(userDefined, TEXTAREA_MAX_LENGTH));
        }

        @Override
        public boolean check(String value) {
            return value.isEmpty() || value.length() <= max;
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            return String.format("Field '%s' must be at most %d characters long", fieldKey, max);
        }
    }

    /** Value must match the given regular expression (skipped when empty). */
    record MatchesPattern(String pattern) implements Constraint {
        @Override
        public boolean check(String value) {
            if (value.isEmpty()) {
                return true;
            }
            try {
                return value.matches(pattern);
            } catch (PatternSyntaxException e) {
                return false;
            }
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            if (value.isEmpty()) {
                return "";
            }
            try {
                return value.matches(pattern) ? "" : String.format("Field '%s' does not match the required pattern", fieldKey);
            } catch (PatternSyntaxException e) {
                return String.format("Field '%s' has an invalid pattern configuration", fieldKey);
            }
        }
    }

    /** Single value must be one of the allowed options (skipped when empty). */
    record OneOf(List<String> options) implements Constraint {
        @Override
        public boolean check(String value) {
            return options == null || options.isEmpty() || value.isEmpty() || options.contains(value);
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            return String.format("Field '%s': value '%s' is not among the allowed options", fieldKey, value);
        }
    }

    /** Every value in a comma-separated string must be one of the allowed options (skipped when empty). */
    record EachOf(List<String> options) implements Constraint {
        @Override
        public boolean check(String value) {
            if (options == null || options.isEmpty() || value.isEmpty()) {
                return true;
            }
            Set<String> allowed = new HashSet<>(options);
            return splitCsv(value).allMatch(allowed::contains);
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            if (options == null || options.isEmpty() || value.isEmpty()) {
                return "";
            }
            Set<String> allowed = new HashSet<>(options);
            var lines = splitCsv(value)
                .filter(item -> !allowed.contains(item))
                .map(item -> String.format("Field '%s': value '%s' is not among the allowed options", fieldKey, item))
                .toList();
            return String.join("\n", lines);
        }
    }

    /**
     * Placeholder for a single-value option constraint whose allowed set is resolved at runtime from an EL expression.
     *
     * <p>This constraint is stored in the database and must be pre-resolved to a {@link OneOf} constraint
     * (using the expression against the target API's metadata) before being passed to the validator.
     * Its {@link #check(String)} throws {@link IllegalStateException} — it must never be evaluated directly.</p>
     */
    record DynamicOneOf(String expression, List<String> fallback) implements Constraint {
        @Override
        public boolean check(String value) {
            throw new IllegalStateException("DynamicOneOf must be resolved before validation — call resolveConstraints() first");
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            throw new IllegalStateException("DynamicOneOf must be resolved before validation — call resolveConstraints() first");
        }
    }

    /**
     * Placeholder for a multi-value option constraint whose allowed set is resolved at runtime from an EL expression.
     *
     * <p>Same pre-resolution semantics as {@link DynamicOneOf}, but resolves to an {@link EachOf} constraint.</p>
     */
    record DynamicEachOf(String expression, List<String> fallback) implements Constraint {
        @Override
        public boolean check(String value) {
            throw new IllegalStateException("DynamicEachOf must be resolved before validation — call resolveConstraints() first");
        }

        @Override
        public String formatErrorMessage(String fieldKey, String value) {
            throw new IllegalStateException("DynamicEachOf must be resolved before validation — call resolveConstraints() first");
        }
    }

    private static Stream<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty());
    }
}
