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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

    private Class<? extends Enum<?>> enumType;
    private boolean preserveCase;

    @Override
    public void initialize(EnumValue annotation) {
        this.enumType = annotation.value();
        this.preserveCase = annotation.preserveCase();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        boolean valid;
        Enum<?>[] enumConstants = enumType.getEnumConstants();

        for (Enum<?> enumConstant : enumConstants) {
            valid = (preserveCase && enumConstant.name().equals(value)) || (!preserveCase && enumConstant.name().equalsIgnoreCase(value));
            if (valid) {
                return valid;
            }
        }

        // invalid value
        if (constraintValidatorContext != null) {
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                .buildConstraintViolationWithTemplate(
                    "Invalid value '%s'. Allowed values [%s] (%s)".formatted(
                        value,
                        String.join(", ", java.util.Arrays.stream(enumConstants).map(Enum::name).toArray(String[]::new)),
                        preserveCase ? "case sensitive" : "case insensitive"
                    )
                )
                .addConstraintViolation();
        }

        return false;
    }
}
