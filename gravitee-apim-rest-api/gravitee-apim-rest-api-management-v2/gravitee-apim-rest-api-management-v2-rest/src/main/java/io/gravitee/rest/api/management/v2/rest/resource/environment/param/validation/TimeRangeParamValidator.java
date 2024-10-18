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
package io.gravitee.rest.api.management.v2.rest.resource.environment.param.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TimeRangeParamValidator implements ConstraintValidator<TimeRangeParamConstraint, TimeRange> {

    @Override
    public boolean isValid(TimeRange timeRange, ConstraintValidatorContext context) {
        Long from = timeRange.getFrom();
        Long to = timeRange.getTo();

        if ((timeRange.getFrom() == null && timeRange.getTo() != null) || (timeRange.getFrom() != null && timeRange.getTo() == null)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Both time range values must be present").addConstraintViolation();
            return false;
        }

        if (from != null && to != null && from > to) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid date order, 'from' must be before 'to'").addConstraintViolation();
            return false;
        }

        return true;
    }
}
