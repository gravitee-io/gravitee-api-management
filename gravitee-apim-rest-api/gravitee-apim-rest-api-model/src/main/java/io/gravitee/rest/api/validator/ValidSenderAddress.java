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
 * Validates an email sender address. Accepts either a bare address ({@code user@example.com}) or the
 * RFC&nbsp;822 personal-name form ({@code Name <user@example.com>}) that the SMTP send-path supports.
 * A {@code null} or blank value is considered valid — combine with {@code @NotBlank} where a value is
 * required.
 *
 * @author GraviteeSource Team
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = { SenderAddressValidator.class })
@Documented
public @interface ValidSenderAddress {
    String message() default "must be a valid email address, optionally with a display name (e.g. \"Name <user@example.com>\")";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
