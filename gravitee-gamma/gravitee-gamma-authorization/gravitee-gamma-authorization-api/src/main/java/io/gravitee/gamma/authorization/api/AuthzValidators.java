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
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuthzValidators {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final ExecutableValidator EXECUTABLE_VALIDATOR = VALIDATOR.forExecutables();

    private AuthzValidators() {}

    public static <T> void validate(T obj) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(message(violations));
        }
    }

    public static <T> void validateCtor(Class<T> recordClass, Object... values) {
        RecordComponent[] components = recordClass.getRecordComponents();
        if (components == null) {
            throw new IllegalStateException(recordClass.getName() + " is not a record");
        }
        Class<?>[] paramTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Constructor<T> canonical;
        try {
            canonical = recordClass.getDeclaredConstructor(paramTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No canonical constructor for " + recordClass.getName(), e);
        }
        Set<ConstraintViolation<T>> violations = EXECUTABLE_VALIDATOR.validateConstructorParameters(canonical, values);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(message(violations));
        }
    }

    public static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public static void requireMatchingEnv(AuthzCallerContext caller, String commandEnvId) {
        requireNonBlank(commandEnvId, "command.environmentId");
        if (!caller.environmentId().equals(commandEnvId)) {
            throw new IllegalArgumentException(
                "command.environmentId (" + commandEnvId + ") does not match caller.environmentId (" + caller.environmentId() + ")"
            );
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
