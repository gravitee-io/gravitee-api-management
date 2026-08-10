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

import io.gravitee.rest.api.model.NewPreRegisterUserEntity;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewPreRegisterUserEntityValidator implements ConstraintValidator<ValidNewPreRegisterUser, NewPreRegisterUserEntity> {

    static final String SERVICE_ACCOUNT_FIRSTNAME_MESSAGE = "a service account must not have a firstname";
    static final String SERVICE_ACCOUNT_IDENTIFIER_MESSAGE =
        "a service account requires a sourceId, lastname or email to derive a unique identifier";
    static final String EMAIL_REQUIRED_MESSAGE = "email is required";

    @Override
    public void initialize(ValidNewPreRegisterUser constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    /**
     * Is valid when:
     * - Is service account, firstName is blank, and at least one of sourceId, lastName or email is non-blank
     *   (the service layer derives sourceId as sourceId -> lastName -> email, so one of the three must be present).
     * - Is not service account and have a non-blank email
     */
    @Override
    public boolean isValid(NewPreRegisterUserEntity value, ConstraintValidatorContext context) {
        if (Boolean.TRUE.equals(value.isService())) {
            if (StringUtils.isNotBlank(value.getFirstname())) {
                addViolation(context, SERVICE_ACCOUNT_FIRSTNAME_MESSAGE);
                return false;
            }
            if (
                StringUtils.isBlank(value.getSourceId()) &&
                StringUtils.isBlank(value.getLastname()) &&
                StringUtils.isBlank(value.getEmail())
            ) {
                addViolation(context, SERVICE_ACCOUNT_IDENTIFIER_MESSAGE);
                return false;
            }
            return true;
        } else {
            if (StringUtils.isBlank(value.getEmail())) {
                addViolation(context, EMAIL_REQUIRED_MESSAGE);
                return false;
            }
            return true;
        }
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
