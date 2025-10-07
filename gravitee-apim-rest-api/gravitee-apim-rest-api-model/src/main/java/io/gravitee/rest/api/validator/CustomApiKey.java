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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Size(min = 8, max = 128, message = "Should have length between 8 and 128 characters")
@Pattern(regexp = "[^#%@/;=?|^~, \\\\]*", message = "Should not contain: ^ # % @ \\ / ; = ? | ~ , (space)")
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface CustomApiKey {
    String message() default "Bad format for custom API Key";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
