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
package io.gravitee.apim.core.validation;

import io.gravitee.apim.core.utils.CollectionUtils;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Validator<I extends Validator.Input> {
    interface Input {}

    @Value
    class Error {

        enum Severity {
            WARNING,
            SEVERE,
        }

        Severity severity;
        String message;

        public static Error severe(String format, Object... args) {
            return new Error(Severity.SEVERE, String.format(format, args));
        }

        public static Error warning(String format, Object... args) {
            return new Error(Severity.WARNING, String.format(format, args));
        }

        public boolean isWarning() {
            return severity == Severity.WARNING;
        }

        public boolean isSevere() {
            return severity == Severity.SEVERE;
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Result<V> {

        private final V value;
        private final List<Error> errors;

        public static <V> Result<V> ofValue(V value) {
            return new Result<>(value, List.of());
        }

        public static <V> Result<V> ofErrors(List<Error> errors) {
            return new Result<>(null, errors);
        }

        public static <V> Result<V> ofBoth(V value, List<Error> errors) {
            return new Result<>(value, errors);
        }

        public Result<V> peek(Consumer<V> peekValue, Consumer<List<Error>> peekErrors) {
            value().ifPresent(peekValue);
            errors().ifPresent(peekErrors);
            return this;
        }

        public <T> Result<T> map(Function<V, T> mapper) {
            return Result.ofBoth(value().map(mapper).orElse(null), errors);
        }

        public Optional<V> value() {
            return Optional.ofNullable(value);
        }

        public Optional<List<Error>> errors() {
            return Optional.ofNullable(errors);
        }

        public Optional<List<Error>> warning() {
            return Optional
                .of(errors().stream().flatMap(List::stream).filter(Error::isWarning).toList())
                .filter(CollectionUtils::isNotEmpty);
        }

        public Optional<List<Error>> severe() {
            return Optional
                .of(errors().stream().flatMap(List::stream).filter(Error::isSevere).toList())
                .filter(CollectionUtils::isNotEmpty);
        }
    }

    Result<I> validateAndSanitize(I input);
}
