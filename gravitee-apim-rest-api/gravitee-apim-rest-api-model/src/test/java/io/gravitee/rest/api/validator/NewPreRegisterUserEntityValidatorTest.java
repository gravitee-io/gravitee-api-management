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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.NewPreRegisterUserEntity;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewPreRegisterUserEntityValidatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewPreRegisterUserEntityValidatorTest.class);
    private NewPreRegisterUserEntityValidator validator;
    private ConstraintValidatorContext constraintValidatorContext;

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                // email;firstName;lastName;sourceId;isServiceAccount;shouldBeValid
                // sourceId is resolved as sourceId -> lastName -> email, so a service account is only rejected
                // when none of the three can be used to derive a non-null sourceId (APIM-14832)
                { "", null, null, null, true, false },
                { null, null, null, null, true, false },
                { "mail@mail.mail", null, null, null, true, true },
                { "", null, "lastName", null, true, true },
                { null, null, null, "sourceId", true, true },
                { "mail@mail.mail", null, "lastName", "sourceId", true, true },
                { null, "firstName", "lastName", "sourceId", true, false },
                { "mail@mail.mail", "firstName", null, null, true, false },
                { "", "firstName", null, null, false, false },
                { null, "firstName", null, null, false, false },
                { " ", "firstName", null, null, false, false },
                { "mail@mail.mail", "firstName", null, null, false, true },
                // whitespace-only values are not usable identifiers, isNotBlank must reject them too
                { null, null, " ", null, true, false },
                { null, null, null, " ", true, false },
                { " ", null, null, null, true, false },
                // blank firstname is "not set", consistent with isNotBlank used everywhere else
                { "mail@mail.mail", "", null, null, true, true },
                { "mail@mail.mail", " ", null, null, true, true },
            }
        );
    }

    @BeforeEach
    public void setUp() {
        this.validator = new NewPreRegisterUserEntityValidator();
        this.constraintValidatorContext = mock(ConstraintValidatorContext.class);
        var violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(any())).thenReturn(violationBuilder);
    }

    @MethodSource("data")
    @ParameterizedTest
    public void shoultTestNewExternalUserEntityValidation(
        String email,
        String firstName,
        String lastName,
        String sourceId,
        Boolean isServiceAccount,
        boolean shouldBeValid
    ) {
        LOGGER.info(
            "Execute NewExternalUserEntity validation test for mail: '{}', firstName: '{}', lastName: '{}', sourceId: '{}', serviceUser: {}, shouldBeValid: {}",
            email,
            firstName,
            lastName,
            sourceId,
            isServiceAccount,
            shouldBeValid
        );

        final NewPreRegisterUserEntity newPreRegisterUserEntity = new NewPreRegisterUserEntity();
        newPreRegisterUserEntity.setService(isServiceAccount);
        newPreRegisterUserEntity.setFirstname(firstName);
        newPreRegisterUserEntity.setLastname(lastName);
        newPreRegisterUserEntity.setSourceId(sourceId);
        newPreRegisterUserEntity.setEmail(email);

        final boolean isValid = validator.isValid(newPreRegisterUserEntity, constraintValidatorContext);

        Assertions.assertEquals(isValid, shouldBeValid);
    }
}
