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
package io.gravitee.gamma.authorization.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class Validators {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final ExecutableValidator EXECUTABLE_VALIDATOR = VALIDATOR.forExecutables();

    private Validators() {}

    public static <T> void validate(T obj) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(message(violations));
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void validateCtor(Class<T> recordClass, Object... values) {
        Constructor<T> canonical = (Constructor<T>) Arrays.stream(recordClass.getDeclaredConstructors())
            .filter(c -> c.getParameterCount() == values.length)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No canonical constructor for " + recordClass.getName()));
        Set<ConstraintViolation<T>> violations = EXECUTABLE_VALIDATOR.validateConstructorParameters(canonical, values);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(message(violations));
        }
    }

    private static <T> String message(Set<ConstraintViolation<T>> violations) {
        return violations
            .stream()
            .map(v -> v.getPropertyPath() + " " + v.getMessage())
            .sorted()
            .collect(Collectors.joining(", "));
    }
}
