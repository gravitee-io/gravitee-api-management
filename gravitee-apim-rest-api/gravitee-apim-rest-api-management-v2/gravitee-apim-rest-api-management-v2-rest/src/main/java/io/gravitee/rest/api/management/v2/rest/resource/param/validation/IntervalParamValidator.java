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
package io.gravitee.rest.api.management.v2.rest.resource.param.validation;

import io.gravitee.rest.api.management.v2.rest.resource.param.IntervalParam;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IntervalParamValidator implements ConstraintValidator<IntervalParamConstraint, IntervalParam> {

    @Override
    public void initialize(IntervalParamConstraint constraintAnnotation) {}

    @Override
    public boolean isValid(IntervalParam timestampParam, ConstraintValidatorContext context) {
        if (timestampParam == null) {
            return true;
        }

        Long from = timestampParam.getFrom();
        Long to = timestampParam.getTo();

        if (from != null && to != null && from > to) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid date order, 'from' must be before 'to'").addConstraintViolation();
            return false;
        }

        return true;
    }
}
