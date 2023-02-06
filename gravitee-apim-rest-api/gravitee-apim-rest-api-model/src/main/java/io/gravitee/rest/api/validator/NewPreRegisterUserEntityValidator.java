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

import io.gravitee.rest.api.model.NewPreRegisterUserEntity;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewPreRegisterUserEntityValidator implements ConstraintValidator<ValidNewPreRegisterUser, NewPreRegisterUserEntity> {

    @Override
    public void initialize(ValidNewPreRegisterUser constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    /**
     * Is valid when:
     * - Is service account and firstName is null.
     * - Is not service account and have an email
     */
    @Override
    public boolean isValid(NewPreRegisterUserEntity value, ConstraintValidatorContext context) {
        if (Boolean.TRUE.equals(value.isService())) {
            return value.getFirstname() == null;
        } else {
            return value.getEmail() != null;
        }
    }
}
