/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.validator;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Size(min = 8, max = 64, message = "Should have length between 8 and 64 characters")
@Pattern(regexp = "[^#%@/;=?|^~, \\\\]*", message = "Should not contain: ^ # % @ \\ / ; = ? | ~ , (space)")
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface CustomApiKey {
    String message() default "Bad format for custom API key";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
