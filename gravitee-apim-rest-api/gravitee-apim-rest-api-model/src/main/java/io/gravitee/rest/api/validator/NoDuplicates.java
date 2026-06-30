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
package io.gravitee.rest.api.validator;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Validates that a {@code List<String>} contains no duplicate entries. A {@code null} list is
 * considered valid, and {@code null} elements are ignored. Entries are compared after trimming
 * surrounding whitespace; with {@link #ignoreCase()} enabled they are additionally compared
 * case-insensitively (so {@code "Acme.com"} and {@code " acme.com "} count as duplicates).
 *
 * @author GraviteeSource Team
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = { NoDuplicatesValidator.class })
@Documented
public @interface NoDuplicates {
    String message() default "must not contain duplicates";

    boolean ignoreCase() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
