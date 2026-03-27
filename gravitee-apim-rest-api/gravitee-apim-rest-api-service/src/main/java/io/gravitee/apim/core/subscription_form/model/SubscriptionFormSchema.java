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

import java.util.List;
import lombok.Builder;

/**
 * Represents the validation schema extracted from a subscription form's GMD content.
 *
 * <p>The schema captures field-level validation rules (required, length constraints, allowed values)
 * for use at subscription submit time. It is generated from the GMD content when the form is saved
 * and persisted alongside the form content.</p>
 *
 * <p>At subscription time, submitted values are validated against this schema without reparsing
 * the GMD template.</p>
 *
 * @author Gravitee.io Team
 */
public record SubscriptionFormSchema(List<Field> fields) {
    public boolean isEmpty() {
        return fields == null || fields.isEmpty();
    }

    public interface RequiredAttribute {
        boolean required();
    }

    public interface RequiredValueAttribute extends RequiredAttribute {}

    public interface RequiredTrueAttribute extends RequiredAttribute {}

    public interface RequiredSelectionAttribute extends RequiredAttribute {}

    public interface ReadOnlyValueAttribute {
        String readonlyValue();
    }

    public interface MinLengthAttribute {
        Integer minLength();
    }

    public interface MaxLengthAttribute {
        Integer maxLength();
    }

    public interface PatternAttribute {
        String pattern();
    }

    public interface OptionsAttribute {
        List<String> options();
    }

    public interface DynamicOptionsAttribute {
        DynamicOptions dynamicOptions();
    }

    public interface SingleValueOptionsField extends OptionsAttribute, DynamicOptionsAttribute {}

    public interface MultiValueOptionsField extends OptionsAttribute, DynamicOptionsAttribute {}

    /**
     * Holds a Gravitee EL expression and its fallback option list for option-bearing fields.
     *
     * <p>GMD syntax: {@code options="{#expression}:fallback1,fallback2"}</p>
     * <p>When present on a field, the expression is resolved at retrieval time against the
     * target API's metadata. If resolution fails, {@code fallback} is used instead.</p>
     */
    public record DynamicOptions(String expression, List<String> fallback) {}

    /**
     * Marker interface for all field types in a subscription form schema.
     * Each implementation captures only the validation attributes relevant to its component type.
     */
    public sealed interface Field
        extends RequiredAttribute
        permits InputField, TextareaField, SelectField, RadioField, CheckboxField, CheckboxGroupField {
        String fieldKey();
    }

    /** Text input — supports required, readonly, minLength, maxLength, pattern. */
    @Builder
    public record InputField(
        String fieldKey,
        boolean required,
        String readonlyValue,
        Integer minLength,
        Integer maxLength,
        String pattern
    ) implements Field, RequiredValueAttribute, ReadOnlyValueAttribute, MinLengthAttribute, MaxLengthAttribute, PatternAttribute {}

    /** Multi-line textarea — supports required, readonly, minLength, maxLength. */
    @Builder
    public record TextareaField(String fieldKey, boolean required, String readonlyValue, Integer minLength, Integer maxLength) implements
        Field, RequiredValueAttribute, ReadOnlyValueAttribute, MinLengthAttribute, MaxLengthAttribute {}

    /** Single-value dropdown — supports required, static options, or dynamic EL options. No readonly (not supported by the GMD component). */
    @Builder
    public record SelectField(String fieldKey, boolean required, List<String> options, DynamicOptions dynamicOptions) implements
        Field, RequiredValueAttribute, SingleValueOptionsField {
        public SelectField {
            if (options != null && dynamicOptions != null) {
                throw new IllegalArgumentException("SelectField cannot define both static options and dynamic options");
            }
        }
    }

    /** Single-value radio group — supports required, readonly, static options, or dynamic EL options. */
    @Builder
    public record RadioField(
        String fieldKey,
        boolean required,
        String readonlyValue,
        List<String> options,
        DynamicOptions dynamicOptions
    ) implements Field, RequiredValueAttribute, ReadOnlyValueAttribute, SingleValueOptionsField {
        public RadioField {
            if (options != null && dynamicOptions != null) {
                throw new IllegalArgumentException("RadioField cannot define both static options and dynamic options");
            }
        }
    }

    /** Boolean checkbox — supports required, readonly. Value is "true"/"false". */
    @Builder
    public record CheckboxField(String fieldKey, boolean required, String readonlyValue) implements
        Field, RequiredTrueAttribute, ReadOnlyValueAttribute {}

    /** Multi-value checkbox group — supports required, static options, or dynamic EL options. No readonly (not supported by the GMD component). */
    @Builder
    public record CheckboxGroupField(String fieldKey, boolean required, List<String> options, DynamicOptions dynamicOptions) implements
        Field, RequiredSelectionAttribute, MultiValueOptionsField {
        public CheckboxGroupField {
            if (options != null && dynamicOptions != null) {
                throw new IllegalArgumentException("CheckboxGroupField cannot define both static options and dynamic options");
            }
        }
    }
}
