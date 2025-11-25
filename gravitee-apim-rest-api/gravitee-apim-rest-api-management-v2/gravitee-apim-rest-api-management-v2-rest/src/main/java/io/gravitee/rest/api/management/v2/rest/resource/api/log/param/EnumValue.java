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
package io.gravitee.rest.api.management.v2.rest.resource.api.log.param;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used to validate whether a string value matches one of the constants
 * of a specified enum type. This is typically applied to fields to ensure that the
 * value adheres to a restricted set of valid values defined by the associated enum.
 * <br/>
 * This annotation uses the {@link EnumValueValidator} class to perform the actual
 * validation logic.
 */
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValueValidator.class)
@Documented
public @interface EnumValue {
    /**
     * Defines a custom error message to be used when the validation constraint fails.
     * This message is typically displayed to provide feedback about why a value is invalid.
     *
     * @return the custom error message to be used in case of validation failure
     */
    String message() default "";

    /**
     * Specifies the validation groups with which the annotated validation constraint is associated.
     * This attribute is used for grouping constraints during the validation process.
     *
     * @return an array of classes*/
    Class<?>[] groups() default {};

    /**
     * Specifies an array of payload types that can be used by clients of the annotation.
     * This is typically used to carry metadata information during validation.
     *
     * @return an array of classes extending {@code Payload} that can be used to provide metadata
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Specifies the target Enum class to be used for validation. The string value being validated
     * must correspond to one of the defined constants in the specified Enum class.
     *
     * @return the Class object of the Enum type that is used for validation
     */
    Class<? extends Enum<?>> value();

    /**
     * Indicates whether the validation should be case-sensitive.
     * If set to {@code true}, the validation will consider the case of the string
     * being validated against the enum constants. If {@code false}, the validation
     * will be case-insensitive.
     *
     * @return {@code true} if case sensitivity is preserved during validation, otherwise {@code false}
     */
    boolean preserveCase() default false;
}
