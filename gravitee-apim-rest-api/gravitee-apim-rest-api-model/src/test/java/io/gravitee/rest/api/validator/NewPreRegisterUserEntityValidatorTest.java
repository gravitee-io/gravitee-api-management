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

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                // email;firstName;isServiceAccount;shouldBeValid
                { "", null, true, true },
                { null, null, true, true },
                { "mail@mail.mail", null, true, true },
                { null, "firstName", true, false },
                { "mail@mail.mail", "firstName", true, false },
                { "", "firstName", false, true },
                { null, "firstName", false, false },
                { "mail@mail.mail", "firstName", false, true },
            }
        );
    }

    @BeforeEach
    public void setUp() {
        this.validator = new NewPreRegisterUserEntityValidator();
    }

    @MethodSource("data")
    @ParameterizedTest
    public void shoultTestNewExternalUserEntityValidation(String email, String firstName, Boolean isServiceAccount, boolean shouldBeValid) {
        LOGGER.info(
            "Execute NewExternalUserEntity validation test for mail: '{}', firstName: '{}', serviceUser: {}, shouldBeValid: {}",
            email,
            firstName,
            isServiceAccount,
            shouldBeValid
        );

        final NewPreRegisterUserEntity newPreRegisterUserEntity = new NewPreRegisterUserEntity();
        newPreRegisterUserEntity.setService(isServiceAccount);
        newPreRegisterUserEntity.setFirstname(firstName);
        newPreRegisterUserEntity.setEmail(email);

        final boolean isValid = validator.isValid(newPreRegisterUserEntity, null);

        Assertions.assertEquals(isValid, shouldBeValid);
    }
}
