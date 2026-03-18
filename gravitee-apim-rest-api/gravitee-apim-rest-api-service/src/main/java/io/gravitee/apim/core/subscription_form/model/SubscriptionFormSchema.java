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
    public record InputField(
        String fieldKey,
        boolean required,
        String readonlyValue,
        Integer minLength,
        Integer maxLength,
        String pattern
    ) implements Field, ReadOnlyValueAttribute, MinLengthAttribute, MaxLengthAttribute, PatternAttribute {}

    /** Multi-line textarea — supports required, readonly, minLength, maxLength. */
    public record TextareaField(String fieldKey, boolean required, String readonlyValue, Integer minLength, Integer maxLength) implements
        Field, ReadOnlyValueAttribute, MinLengthAttribute, MaxLengthAttribute {}

    /** Single-value dropdown — supports required, options. No readonly (not supported by the GMD component). */
    public record SelectField(String fieldKey, boolean required, List<String> options) implements Field, OptionsAttribute {}

    /** Single-value radio group — supports required, readonly, options. */
    public record RadioField(String fieldKey, boolean required, String readonlyValue, List<String> options) implements
        Field, ReadOnlyValueAttribute, OptionsAttribute {}

    /** Boolean checkbox — supports required, readonly. Value is "true"/"false". */
    public record CheckboxField(String fieldKey, boolean required, String readonlyValue) implements Field, ReadOnlyValueAttribute {}

    /** Multi-value checkbox group — supports required, options. No readonly (not supported by the GMD component). */
    public record CheckboxGroupField(String fieldKey, boolean required, List<String> options) implements Field, OptionsAttribute {}
}
