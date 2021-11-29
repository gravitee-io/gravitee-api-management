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
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(Parameterized.class)
public class NewPreRegisterUserEntityValidatorTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewPreRegisterUserEntityValidatorTest.class);
    private NewPreRegisterUserEntityValidator validator;

    @Parameterized.Parameters
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

    @Parameterized.Parameter(0)
    public String email;

    @Parameterized.Parameter(1)
    public String firstName;

    @Parameterized.Parameter(2)
    public Boolean isServiceAccount;

    @Parameterized.Parameter(3)
    public boolean shouldBeValid;

    @Before
    public void setUp() {
        this.validator = new NewPreRegisterUserEntityValidator();
    }

    @Test
    public void shoultTestNewExternalUserEntityValidation() {
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

        Assert.assertEquals(isValid, shouldBeValid);
    }
}
