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
package io.gravitee.rest.api.service.validator;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.BrandedSenderConfig;
import io.gravitee.rest.api.model.settings.Email;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * Checks the email sender settings ({@code email.from} and {@code email.branded_senders}) that a settings save changes.
 * <p>
 * Settings pages send the whole settings object back, including sender values locked by the system configuration or
 * stored before a check got stricter. Checking those would reject every save on the page for a value the user did not
 * touch and often cannot change, so only changed values are checked. A branded sender is compared as a whole entry: if
 * any of its fields changed, the entire entry is checked, so editing an older invalid entry requires fixing it.
 *
 * @author GraviteeSource Team
 */
@Component
public class SenderSettingsValidator implements DisposableBean {

    // The sender constraint messages use no expression language, so no EL implementation is required.
    private final ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class)
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory();

    private final Validator validator = validatorFactory.getValidator();

    /**
     * @param submitted the email settings being saved
     * @param stored the email settings currently in effect for the same scope, as a settings read returns them
     * @param skipped the keys whose submitted value the save does not persist (system-configured, hidden for trial)
     * @throws ConstraintViolationException when a changed sender value is invalid
     */
    public void validateChanges(Email submitted, Email stored, Predicate<Key> skipped) {
        Set<ConstraintViolation<?>> violations = new LinkedHashSet<>();

        if (!skipped.test(Key.EMAIL_FROM) && !Objects.equals(submitted.getFrom(), stored.getFrom())) {
            // validateValue, not validateProperty: the latter also follows the @Valid on Email#getBrandedSenders, which
            // would check the branded senders even when they are skipped or unchanged.
            violations.addAll(validator.validateValue(Email.class, "from", submitted.getFrom()));
        }

        if (!skipped.test(Key.EMAIL_BRANDED_SENDERS)) {
            List<BrandedSenderConfig> storedBrandedSenders = stored.getBrandedSenders();
            submitted
                .getBrandedSenders()
                .stream()
                .filter(brandedSender -> !storedBrandedSenders.contains(brandedSender))
                .forEach(brandedSender -> violations.addAll(validator.validate(brandedSender)));
        }

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    @Override
    public void destroy() {
        validatorFactory.close();
    }
}
